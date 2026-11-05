package app.islamiccalendar
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** AdMob setup: asks for consent (GDPR/UMP) first, then starts the SDK. Ads never load before consent allows it. */
object Ads {
    /** True once the SDK may serve ads; the banner stays collapsed until then. */
    var ready by mutableStateOf(false)
        private set
    private var started = false

    fun init(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { start(activity) }
        }, { start(activity) })
        // Consent from a previous session may already allow ads.
        start(activity)
    }

    private fun start(activity: Activity) {
        if (started || !UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) return
        started = true
        MobileAds.initialize(activity.applicationContext) { ready = true }
    }
}

/** Adaptive banner above the bottom menu. Collapses when there is no ad (offline, no fill, no consent). */
@Composable fun AdSlot() {
    if (!Ads.ready || Billing.isPremium) return
    val ctx = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val size = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp) }
    var loaded by remember { mutableStateOf(false) }
    val adView = remember {
        AdView(ctx).apply {
            setAdSize(size)
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() { loaded = true }
                override fun onAdFailedToLoad(e: LoadAdError) { loaded = false }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        adView.resume()
        onDispose { adView.destroy() }
    }
    Box(Modifier.fillMaxWidth().height(if (loaded) size.height.dp else 0.dp)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
        AndroidView({ adView })
    }
}
