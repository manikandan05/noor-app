import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
// Release signing: credentials live in an untracked keystore.properties (see docs/PUBLISHING.md). Without it, release builds are unsigned.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
android {
    namespace = "app.islamiccalendar"; compileSdk = 35
    defaultConfig {
        applicationId = "app.islamiccalendar"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0"
        // Live AdMob IDs. Do not click your own ads; for development use Google's test IDs (see docs/PUBLISHING.md, Part 2.5).
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3799058069645769~3682734218"
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-3799058069645769/8122165969\"")
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) create("release") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release { if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release") }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.android.billingclient:billing-ktx:9.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
}
