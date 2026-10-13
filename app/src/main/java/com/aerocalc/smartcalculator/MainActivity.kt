package com.aerocalc.smartcalculator

import android.content.Context
import android.os.Bundle
import androidx.core.content.edit
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aerocalc.smartcalculator.ui.main.MainScreen
import com.aerocalc.smartcalculator.ui.onboarding.OnboardingScreen
import com.aerocalc.smartcalculator.ui.onboarding.AdAgeGroup
import com.aerocalc.smartcalculator.ui.onboarding.AgeGroupScreen
import com.aerocalc.smartcalculator.ads.AdsController

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val sharedPrefs = getSharedPreferences("aerocalc_prefs", Context.MODE_PRIVATE)
    val isFirstLaunch = !sharedPrefs.getBoolean("onboarding_completed", false)
    val savedAgeGroup = sharedPrefs.getString("ad_age_group", null)
        ?.let { runCatching { AdAgeGroup.valueOf(it) }.getOrNull() }
    val ads = AdsController(this, BuildConfig.ADMOB_APP_ID)
    setContent {
      var showOnboarding by remember { mutableStateOf(isFirstLaunch) }
      var ageGroup by remember { mutableStateOf(savedAgeGroup) }

      if (ageGroup == null) {
        AgeGroupScreen(onSelect = { selected ->
          sharedPrefs.edit { putString("ad_age_group", selected.name) }
          ageGroup = selected
          if (selected != AdAgeGroup.UNDER_13) ads.start(selected == AdAgeGroup.TEEN)
        })
      } else if (showOnboarding) {
        OnboardingScreen(
          onFinish = {
            sharedPrefs.edit { putBoolean("onboarding_completed", true) }
            showOnboarding = false
          }
        )
      } else {
        MainScreen(
          modifier = Modifier.fillMaxSize(),
          adsReady = ads.ready,
          privacyOptionsRequired = ads.privacyOptionsRequired,
          onPrivacyOptions = ads::showPrivacyOptions,
          onLearningActivityCompleted = ads::onLearningActivityCompleted,
          onChangeAgeGroup = {
            sharedPrefs.edit { remove("ad_age_group") }
            recreate()
          }
        )
      }
    }
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )
    // Consent/ads may access the activity window. Install its content first.
    if (savedAgeGroup != null && savedAgeGroup != AdAgeGroup.UNDER_13) {
      ads.start(savedAgeGroup == AdAgeGroup.TEEN)
    }
  }
}
