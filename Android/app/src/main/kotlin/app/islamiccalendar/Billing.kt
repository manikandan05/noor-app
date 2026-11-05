package app.islamiccalendar
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Premium subscription via Google Play Billing (one product, two base plans). No backend and no account:
 * Google Play is the source of truth. The app stores only "premium: yes/no" and the time it last verified it,
 * never purchase tokens or payment details. Every refresh asks Play again, so trial end, cancellation,
 * expiry, refund and revoke all turn Premium off as soon as Play stops returning the purchase.
 */
object Billing {
    const val PRODUCT_ID = "noor_premium"
    const val PLAN_YEARLY = "yearly"
    const val PLAN_MONTHLY = "monthly"
    /** How long a previously verified Premium is honoured when Play cannot be reached (offline, service busy). */
    private const val OFFLINE_GRACE_MS = 7L * 24 * 60 * 60 * 1000

    /** One purchasable plan with the best offer (free-trial offer if Play returned one for this user). */
    class Plan(val basePlanId: String, val details: ProductDetails, val offer: ProductDetails.SubscriptionOfferDetails) {
        private val phases get() = offer.pricingPhases.pricingPhaseList
        private val recurring get() = phases.last()
        val price: String get() = recurring.formattedPrice
        val micros: Long get() = recurring.priceAmountMicros
        /** Length of the free trial in days, or null if this offer has none. */
        val trialDays: Int? get() = phases.firstOrNull()
            ?.takeIf { phases.size > 1 && it.priceAmountMicros == 0L }?.let { periodDays(it.billingPeriod) }
    }

    var isPremium by mutableStateOf(false); private set
    /** False while Premium is active but cancelled: it stays on until the paid period or trial ends. */
    var autoRenewing by mutableStateOf(true); private set
    /** A payment has been started but not completed (e.g. cash or bank payment). Premium is not granted yet. */
    var pending by mutableStateOf(false); private set
    var plans by mutableStateOf<List<Plan>>(emptyList()); private set
    /** Resource key (see Strings.kt) of the last user-facing result, or null. */
    var messageKey by mutableStateOf<String?>(null)
    var paywall by mutableStateOf(false)

    private var client: BillingClient? = null
    private lateinit var appCtx: Context
    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        if (client != null) return
        appCtx = ctx.applicationContext
        prefs = appCtx.getSharedPreferences("s", 0)
        isPremium = prefs.getBoolean("prem_ok", false) &&
            System.currentTimeMillis() - prefs.getLong("prem_at", 0L) < OFFLINE_GRACE_MS
        autoRenewing = prefs.getBoolean("prem_renew", true)
        client = BillingClient.newBuilder(appCtx)
            .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingResponseCode.OK) { loadPlans(); refresh() }
            }
            override fun onBillingServiceDisconnected() {} // auto service reconnection is enabled
        })
    }

    /** Re-checks entitlement with Play. Called at start, on resume and after a purchase. */
    fun refresh(done: ((Boolean) -> Unit)? = null) {
        val c = client
        if (c == null || !c.isReady) { done?.invoke(isPremium); return }
        c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { r, list ->
            // Only an OK answer is authoritative. Errors (offline etc.) keep the cached state until the grace period ends.
            if (r.responseCode == BillingResponseCode.OK) apply(list)
            done?.invoke(isPremium)
        }
    }

    fun restore() {
        messageKey = null
        val c = client
        if (c == null || !c.isReady) { messageKey = "premium_unavailable"; return }
        refresh { messageKey = if (it) "premium_restored" else "premium_none" }
    }

    fun purchase(activity: Activity, plan: Plan) {
        val c = client
        if (c == null || !c.isReady) { messageKey = "premium_unavailable"; return }
        messageKey = null
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(plan.details).setOfferToken(plan.offer.offerToken).build())).build()
        if (c.launchBillingFlow(activity, params).responseCode != BillingResponseCode.OK) messageKey = "premium_error"
    }

    /** Opens this subscription in Google Play, where the user can cancel (including during a trial) or change payment. */
    fun manage(ctx: Context) {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=${ctx.packageName}")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun onPurchasesUpdated(r: BillingResult, purchases: List<Purchase>?) {
        when (r.responseCode) {
            BillingResponseCode.OK -> if (purchases.isNullOrEmpty()) refresh() else { apply(purchases); paywall = false }
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingResponseCode.USER_CANCELED -> {}
            else -> messageKey = "premium_error"
        }
    }

    private fun apply(purchases: List<Purchase>) {
        val mine = purchases.filter { PRODUCT_ID in it.products }
        val active = mine.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        pending = active == null && mine.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        if (active == null) { setPremium(false, true); return }
        // Unacknowledged purchases are refunded by Google after 3 days; retried on every refresh if this fails.
        if (!active.isAcknowledged) client?.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(active.purchaseToken).build()) {}
        setPremium(true, active.isAutoRenewing)
    }

    private fun setPremium(on: Boolean, renewing: Boolean) {
        isPremium = on; autoRenewing = renewing
        prefs.edit().putBoolean("prem_ok", on).putLong("prem_at", System.currentTimeMillis()).putBoolean("prem_renew", renewing).apply()
    }

    private fun loadPlans() {
        val q = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build())).build()
        client?.queryProductDetailsAsync(q) { r, result ->
            if (r.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            val pd = result.productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val offers = pd.subscriptionOfferDetails.orEmpty()
            fun pick(base: String): Plan? {
                val of = offers.filter { it.basePlanId == base }
                val trial = of.firstOrNull { o -> o.pricingPhases.pricingPhaseList.let { it.size > 1 && it.first().priceAmountMicros == 0L } }
                val best = trial ?: of.firstOrNull { it.offerId == null } ?: of.firstOrNull() ?: return null
                return Plan(base, pd, best)
            }
            plans = listOfNotNull(pick(PLAN_YEARLY), pick(PLAN_MONTHLY))
        }
    }

    /** ISO-8601 billing period ("P7D", "P1W", "P1M", "P1Y") to days. */
    private fun periodDays(p: String): Int? {
        val m = Regex("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?").matchEntire(p) ?: return null
        val (y, mo, w, d) = m.destructured
        return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
    }
}
