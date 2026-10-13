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
    val adView = remember(context) { AdView(context) }
    var loaded by remember(adView) { mutableStateOf(false) }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        LaunchedEffect(adView, preview) {
            if (!preview) {
                adView.loadAd(
                    BannerAdRequest.Builder(BuildConfig.ADMOB_BANNER_UNIT_ID, AdSize.BANNER).build(),
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) { loaded = true }
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            loaded = false
                            Log.w("AeroCalcAds", "Banner load: $adError")
                        }
                    }
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loaded) {
                Text("РЕКЛАМА", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
            }
            AndroidView(factory = { adView }, modifier = Modifier.wrapContentSize())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            (adView.parent as? ViewGroup)?.removeView(adView)
            adView.destroy()
        }
    }
}
