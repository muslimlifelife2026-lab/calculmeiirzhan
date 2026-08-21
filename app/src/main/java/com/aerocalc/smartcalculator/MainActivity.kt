package com.aerocalc.smartcalculator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aerocalc.smartcalculator.ui.main.MainScreen
import com.aerocalc.smartcalculator.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val sharedPrefs = getSharedPreferences("aerocalc_prefs", Context.MODE_PRIVATE)
    val isFirstLaunch = !sharedPrefs.getBoolean("onboarding_completed", false)

    enableEdgeToEdge()
    setContent {
      var showOnboarding by remember { mutableStateOf(isFirstLaunch) }

      if (showOnboarding) {
        OnboardingScreen(
          onFinish = {
            sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
            showOnboarding = false
          }
        )
      } else {
        MainScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
