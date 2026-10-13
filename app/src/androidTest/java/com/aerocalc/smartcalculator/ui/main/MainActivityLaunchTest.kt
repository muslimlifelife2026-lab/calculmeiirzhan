package com.aerocalc.smartcalculator.ui.main

import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.aerocalc.smartcalculator.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainActivityLaunchTest {
    @get:Rule val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test fun launcherActivityCreatesContentWindow() {
        rule.scenario.onActivity { activity ->
            assertTrue(activity.window.decorView.isAttachedToWindow)
        }
    }
}
