package com.aerocalc.smartcalculator.ui.main

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.rules.RuleChain
import org.junit.rules.ExternalResource
import com.aerocalc.smartcalculator.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainActivityLaunchTest {
    private val activityRule = ActivityScenarioRule(MainActivity::class.java)
    private val savedAdvertisingAudience = object : ExternalResource() {
        override fun before() {
            ApplicationProvider.getApplicationContext<Context>()
                .getSharedPreferences("aerocalc_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("onboarding_completed", true).putString("ad_age_group", "TEEN").commit()
        }
    }
    @get:Rule val rule: RuleChain = RuleChain.outerRule(savedAdvertisingAudience).around(activityRule)

    @Test fun launcherActivityCreatesContentWindow() {
        activityRule.scenario.onActivity { activity ->
            assertTrue(activity.window.decorView.isAttachedToWindow)
        }
        activityRule.scenario.recreate()
        activityRule.scenario.onActivity { activity ->
            assertTrue(activity.window.decorView.isAttachedToWindow)
        }
    }
}
