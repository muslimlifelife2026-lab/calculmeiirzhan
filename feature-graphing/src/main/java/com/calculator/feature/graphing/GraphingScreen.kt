package com.calculator.feature.graphing

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.SurfaceBorder
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.engine.solver.GraphEvaluator
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GraphingScreen(modifier: Modifier = Modifier, initialExpressions: List<String>? = null, initialRequest: Long = 0L) {
    var mode by rememberSaveable { mutableStateOf(GraphMode.CARTESIAN) }
    var cartesianExpressions by rememberSaveable { mutableStateOf(initialExpressions?.take(3) ?: listOf("sin(x)", "0.5*x - 1")) }
    var appliedImportRequest by rememberSaveable { mutableLongStateOf(initialRequest) }
    var polarExpressions by rememberSaveable { mutableStateOf(listOf("cos(2*theta)")) }
    var parametricExpressions by rememberSaveable { mutableStateOf(listOf("sin(3*t)", "sin(2*t)")) }
    val expressions = when (mode) {
        GraphMode.CARTESIAN -> cartesianExpressions
        GraphMode.POLAR -> polarExpressions
        GraphMode.PARAMETRIC -> parametricExpressions
    }
    fun updateExpressions(values: List<String>) {
        when (mode) {
            GraphMode.CARTESIAN -> cartesianExpressions = values
            GraphMode.POLAR -> polarExpressions = values
            GraphMode.PARAMETRIC -> parametricExpressions = values
        }
    }
    var showRoots by rememberSaveable { mutableStateOf(true) }
    var showExtremums by rememberSaveable { mutableStateOf(true) }
    var showIntersections by rememberSaveable { mutableStateOf(true) }
    var showTangent by rememberSaveable { mutableStateOf(false) }
    var showIntegral by rememberSaveable { mutableStateOf(false) }
    var integralAText by rememberSaveable { mutableStateOf("0") }
    var integralBText by rememberSaveable { mutableStateOf("2") }
    var resetTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(initialRequest, initialExpressions) {
        if (!initialExpressions.isNullOrEmpty() && initialRequest != appliedImportRequest) {
            mode = GraphMode.CARTESIAN
            cartesianExpressions = initialExpressions.take(3)
            appliedImportRequest = initialRequest
            resetTrigger++
        }
    }
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showValuesTable by remember { mutableStateOf(false) }
    var paramValues by rememberSaveable { mutableStateOf(mapOf("a" to 1.0, "b" to 1.0, "c" to 0.0, "k" to 1.0)) }

    val detectedParams = remember(expressions, mode) { GraphParameters.discover(expressions, mode) }
    val syntaxErrors = remember(expressions) {
        expressions.map { expression ->
            if (expression.isBlank()) null else try { GraphEvaluator(expression); null } catch (exception: IllegalArgumentException) { exception.message }
        }
    }
    LaunchedEffect(detectedParams) {
        val missing = detectedParams.filterNot { it in paramValues }
        if (missing.isNotEmpty()) paramValues = paramValues + missing.associateWith { 1.0 }
    }
    val integralA = integralAText.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val integralB = integralBText.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    val graphColors = listOf(Color.White, Color(0xFFB9C6D6), Color(0xFFB9ADC8))

    BoxWithConstraints(modifier.fillMaxSize()) {
        val graphHeight = (maxHeight * 0.53f).coerceIn(260.dp, 440.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("graph_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Графики", style = MaterialTheme.typography.headlineMedium)
            Text("Исследуйте функции и меняйте параметры", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { showPresetsSheet = true }, label = { Text("Библиотека") })
                AssistChip(onClick = { showValuesTable = true }, label = { Text("Таблица") })
                AssistChip(onClick = { resetTrigger++ }, label = { Text("К центру") })
            }
            Box(Modifier.fillMaxWidth().height(graphHeight).clip(RoundedCornerShape(24.dp)).border(0.5.dp, SurfaceBorder, RoundedCornerShape(24.dp))) {
                GraphCanvas(
                    expressions = expressions,
                    mode = mode,
                    params = paramValues,
                    showRoots = showRoots,
                    showExtremums = showExtremums,
                    showIntersections = showIntersections,
                    showTangent = showTangent,
                    showIntegral = showIntegral && integralA != null && integralB != null,
                    integralA = integralA ?: 0.0,
                    integralB = integralB ?: 2.0,
                    resetTrigger = resetTrigger,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text("Два пальца — масштаб. Перетаскивание — сдвиг. Нажатие — значение функции. Углы в радианах.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Функции", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(GraphMode.CARTESIAN to "y = f(x)", GraphMode.POLAR to "r = f(θ)", GraphMode.PARAMETRIC to "x(t), y(t)").forEach { (value, title) ->
                            FilterChip(selected = mode == value, onClick = {
                                if (mode != value) {
                                    mode = value
                                    resetTrigger++
                                }
                            }, label = { Text(title) })
                        }
                    }
                    expressions.forEachIndexed { index, expression ->
                        val label = when (mode) {
                            GraphMode.CARTESIAN -> "y${index + 1}"
                            GraphMode.POLAR -> "r${index + 1}(θ)"
                            GraphMode.PARAMETRIC -> if (index == 0) "x(t)" else "y(t)"
                        }
                        OutlinedTextField(
                            value = expression,
                            onValueChange = { if (it.length <= 256) updateExpressions(expressions.toMutableList().also { updated -> updated[index] = it }) },
                            label = { Text(label, color = graphColors[index % graphColors.size]) },
                            modifier = Modifier.fillMaxWidth().testTag("graph_expression_$index"),
                            singleLine = true,
                            isError = syntaxErrors[index] != null,
                            supportingText = syntaxErrors[index]?.let { error -> { Text(error) } },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            trailingIcon = if (expressions.size > 1 && mode != GraphMode.PARAMETRIC) {
                                { IconButton(onClick = { updateExpressions(expressions.filterIndexed { i, _ -> i != index }) }) { Icon(Icons.Rounded.Close, "Удалить $label") } }
                            } else null
                        )
                    }
                    if (expressions.size < 3 && mode != GraphMode.PARAMETRIC) {
                        TextButton(onClick = { updateExpressions(expressions + "") }) { Text("Добавить функцию") }
                    }
                    Text("Примеры: x^2, sin(x), a*x+b. Степень — ^, умножение — *. Константы: pi, e.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            if (detectedParams.isNotEmpty()) GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Параметры", style = MaterialTheme.typography.titleLarge)
                    Text("Изменения сразу видны на графике и в таблице", color = TextSecondary)
                    detectedParams.forEach { name ->
                        val value = paramValues[name] ?: 1.0
                        Text("$name = ${String.format(Locale.getDefault(), "%.1f", value)}", fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("graph_value_$name"))
                        Slider(
                            value = value.toFloat().coerceIn(-5f, 5f),
                            onValueChange = { paramValues = paramValues + (name to GraphParameters.sliderValue(it)) },
                            valueRange = -5f..5f,
                            steps = 99,
                            modifier = Modifier.fillMaxWidth().testTag("graph_param_$name").semantics { contentDescription = "Параметр $name" }
                        )
                    }
                }
            }
            if (mode == GraphMode.CARTESIAN) GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Анализ", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(showRoots, { showRoots = !showRoots }, { Text("Корни") })
                        FilterChip(showExtremums, { showExtremums = !showExtremums }, { Text("Экстремумы") })
                        FilterChip(showIntersections, { showIntersections = !showIntersections }, { Text("Пересечения") })
                        FilterChip(showTangent, { showTangent = !showTangent }, { Text("Касательная") })
                        FilterChip(showIntegral, { showIntegral = !showIntegral }, { Text("Интеграл") })
                    }
                    if (showIntegral) {
                        OutlinedTextField(integralAText, { if (it.length <= 20) integralAText = it }, Modifier.fillMaxWidth(),
                            label = { Text("Нижний предел a") }, isError = integralA == null, singleLine = true)
                        OutlinedTextField(integralBText, { if (it.length <= 20) integralBText = it }, Modifier.fillMaxWidth(),
                            label = { Text("Верхний предел b") }, isError = integralB == null, singleLine = true)
                        if (integralA == null || integralB == null) Text("Укажите конечные числовые пределы", color = MaterialTheme.colorScheme.error)
                    }
                    Text("Анализ численный и приближённый. На видимом участке могут обнаруживаться не все корни и экстремумы. Разрывы и очень быстрые колебания требуют отдельной проверки.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
    if (showPresetsSheet) PresetsSheet(
        onSelectPreset = { mode = it.mode; updateExpressions(it.expressions); paramValues = paramValues + it.initialParameters; resetTrigger++ },
        onDismiss = { showPresetsSheet = false }
    )
    if (showValuesTable) ValuesTableDialog(expressions = expressions, mode = mode, params = paramValues, onDismiss = { showValuesTable = false })
}
