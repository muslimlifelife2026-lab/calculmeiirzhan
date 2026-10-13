package com.aerocalc.smartcalculator.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.theme.*
import com.calculator.feature.calculator.CalculatorScreen
import com.calculator.feature.chemistry.ChemistryScreen
import com.calculator.feature.converter.ConverterScreen
import com.calculator.feature.geometry.GeometryScreen
import com.calculator.feature.graphing.GraphingScreen
import com.calculator.feature.history.HistoryScreen
import com.calculator.feature.physics.PhysicsScreen
import com.aerocalc.smartcalculator.ads.BannerSlot

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    adsReady: Boolean = false,
    privacyOptionsRequired: Boolean = false,
    onPrivacyOptions: () -> Unit = {},
    onChangeAgeGroup: () -> Unit = {}
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toolId by rememberSaveable { mutableStateOf<String?>(null) }
    var learningPractice by rememberSaveable { mutableStateOf(false) }
    val holder = rememberSaveableStateHolder()
    val tool = scienceTools.firstOrNull { it.id == toolId }
    BackHandler(enabled = tool != null && tab == 1) { toolId = null }
    BackHandler(enabled = learningPractice && tab == 2) { learningPractice = false }
    BackHandler(enabled = tab != 0 && (tab != 1 || tool == null) && (tab != 2 || !learningPractice)) { tab = 0 }

    AeroCalcTheme {
        Box(
            modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF29333D), Color(0xFF1B222A), Background, Color(0xFF10151A)))
            ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier.widthIn(max = 900.dp).fillMaxSize()
                    .statusBarsPadding().navigationBarsPadding().imePadding()
            ) {
                if ((tab == 1 && tool != null) || (tab == 2 && learningPractice)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (tab == 2) learningPractice = false else toolId = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, if (tab == 2) "Вернуться к обучению" else "Все инструменты", tint = TextPrimary)
                        }
                        Text(if (tab == 2) "Тренажёр" else tool?.title.orEmpty(), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val route = when (tab) {
                        0 -> "calculator"
                        1 -> toolId ?: "tools"
                        2 -> if (learningPractice) "physics_practice" else "learning"
                        else -> "history"
                    }
                    AnimatedContent(
                        targetState = route,
                        transitionSpec = {
                            fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220)) togetherWith
                                fadeOut(tween(140))
                        },
                        label = "glassScreenTransition"
                    ) { visibleRoute ->
                        holder.SaveableStateProvider(visibleRoute) {
                        when (visibleRoute) {
                            "calculator" -> CalculatorScreen()
                            "history" -> HistoryScreen()
                            "learning" -> LearningScreen(
                                onOpenPhysicsPractice = { learningPractice = true },
                                onOpenConverter = { tab = 1; toolId = "converter" }
                            )
                            "physics_practice" -> PhysicsScreen(initialTab = 3)
                            "physics" -> PhysicsScreen()
                            "geometry" -> GeometryScreen()
                            "chemistry" -> ChemistryScreen()
                            "graphs" -> GraphingScreen()
                            "converter" -> ConverterScreen()
                            else -> ToolsScreen(
                                onSelect = { toolId = it },
                                privacyOptionsRequired = privacyOptionsRequired,
                                onPrivacyOptions = onPrivacyOptions,
                                onChangeAgeGroup = onChangeAgeGroup
                            )
                        }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = adsReady && ((tab == 1 && tool == null) || (tab == 2 && !learningPractice) || tab == 3),
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
                ) {
                    BannerSlot(Modifier.padding(horizontal = 16.dp))
                }
                GlassNavigation(
                    selected = tab,
                    items = listOf(
                        NavigationItem("Калькулятор", Icons.Rounded.Calculate),
                        NavigationItem("Инструменты", Icons.Rounded.GridView),
                        NavigationItem("Обучение", Icons.Rounded.AutoStories),
                        NavigationItem("История", Icons.Rounded.History)
                    ),
                    onSelect = {
                        tab = it
                        if (it == 1) toolId = null
                        if (it == 2) learningPractice = false
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
