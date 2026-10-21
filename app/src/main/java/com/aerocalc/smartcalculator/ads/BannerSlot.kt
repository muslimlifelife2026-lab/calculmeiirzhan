package com.aerocalc.smartcalculator.ads

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aerocalc.smartcalculator.BuildConfig
import com.calculator.core.ui.theme.TextMuted
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

@Composable
fun BannerSlot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // A fixed 320dp creative must never be clipped on compact/split-screen windows.
        if (maxWidth < 320.dp || preview) return@BoxWithConstraints
        val adView = remember(context) { AdView(context) }
        var loaded by remember(adView) { mutableStateOf(false) }
        DisposableEffect(adView) {
            val main = android.os.Handler(android.os.Looper.getMainLooper())
            var active = true
            adView.loadAd(
                BannerAdRequest.Builder(BuildConfig.ADMOB_BANNER_UNIT_ID, AdSize.BANNER).build(),
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) { main.post { if (active) loaded = true } }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        main.post { if (active) loaded = false }
                        Log.w("AeroCalcAds", "Banner load: $adError")
                    }
                }
            )
            onDispose {
                active = false
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView.destroy()
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
            // Keep space stable while loading; allow the label to grow with the system font.
            Text(if (loaded) "РЕКЛАМА" else " ", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            AndroidView(factory = { adView }, modifier = Modifier.width(320.dp).height(50.dp))
        }
    }
}
