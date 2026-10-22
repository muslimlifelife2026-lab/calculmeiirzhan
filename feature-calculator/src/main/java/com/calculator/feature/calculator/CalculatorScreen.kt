package com.calculator.feature.calculator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calculator.core.ui.components.MathKeyboard
import com.calculator.core.ui.theme.*
import com.calculator.core.ui.utils.PdfGenerator
import com.calculator.feature.calculator.scanner.ScannerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier, viewModel: CalculatorViewModel = viewModel(), onOpenAlgebra: ((String) -> Unit)? = null) {
    val expression by viewModel.displayExpression.collectAsState()
    val result by viewModel.calculatedResult.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val steps by viewModel.calculationSteps.collectAsState()
    val degrees by viewModel.isDegrees.collectAsState()
    val history by viewModel.recentHistory.collectAsState()
    var scanner by rememberSaveable { mutableStateOf(false) }
    var scannedExpression by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var completedExpression by rememberSaveable { mutableStateOf<String?>(null) }
    var completedResult by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shownExpression = completedExpression ?: expression
    val shownResult = completedResult ?: result
    // Capture the document at launch, so edits while the picker is open cannot change it.
    var exportExpression by rememberSaveable { mutableStateOf("") }
    var exportResult by rememberSaveable { mutableStateOf("") }
    var exportSteps by rememberSaveable { mutableStateOf(listOf<String>()) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            val success = PdfGenerator.generateAndSavePdf(context, uri, "AeroCalc · Решение", exportExpression, exportResult, exportSteps)
            android.widget.Toast.makeText(context, if (success) "PDF сохранён" else "Не удалось сохранить PDF", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val onKey: (String) -> Unit = { key ->
        if (key == "=") {
            val original = expression
            viewModel.onKeyPressed(key)
            if (original.isNotBlank() && viewModel.errorMessage.value.isEmpty()) {
                completedExpression = original
                completedResult = viewModel.displayExpression.value
            }
        } else {
            if (completedResult != null && (key.firstOrNull()?.isDigit() == true || key in listOf(".", "(", "π", "e") || key.endsWith("("))) viewModel.clearDisplay()
            completedExpression = null
            completedResult = null
            viewModel.onKeyPressed(key)
        }
    }
    val display: @Composable (Modifier) -> Unit = { displayModifier ->
        CalculatorDisplay(
            expression = shownExpression, result = shownResult, error = error, degrees = degrees,
            isResultFinal = completedResult != null,
            hasSteps = steps.isNotEmpty(),
            onAngle = {
                completedExpression = null; completedResult = null
                viewModel.toggleAngleMode()
            },
            onScanner = { scanner = true }, onHistory = { sheet = "history" }, onSteps = { sheet = "steps" },
            modifier = displayModifier
        )
    }
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    if (landscape) {
        Row(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            display(Modifier.weight(1f).fillMaxHeight())
            MathKeyboard(Modifier.weight(1.2f).fillMaxHeight(), onKeyPressed = onKey)
        }
    } else {
        Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp)) {
            display(Modifier.fillMaxWidth().weight(0.9f))
            MathKeyboard(Modifier.fillMaxWidth().weight(1.5f), onKeyPressed = onKey)
        }
    }
    if (scanner) Dialog(onDismissRequest = { scanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ScannerScreen(onClose = { scanner = false }, onResult = { text ->
            scannedExpression = text
            scanner = false
        })
    }
    scannedExpression?.let { candidate ->
        val isAlgebra = candidate.contains('=') || Regex("(?i)(?<![a-z])[xy](?![a-z])").containsMatchIn(candidate)
        AlertDialog(
            onDismissRequest = { scannedExpression = null },
            title = { Text("Проверьте распознанный пример") },
            text = {
                OutlinedTextField(
                    value = candidate,
                    onValueChange = { scannedExpression = it },
                    label = { Text("Выражение") },
                    minLines = 2,
                    maxLines = 4,
                    supportingText = { Text(if (isAlgebra) "Проверьте обе части уравнения. Две строки — система с x и y." else "Проверьте числа, скобки и знаки действий.") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isAlgebra) {
                        onOpenAlgebra?.invoke(candidate)
                    } else {
                        completedExpression = null
                        completedResult = null
                        viewModel.clearDisplay()
                        viewModel.pasteFromHistory(candidate)
                    }
                    scannedExpression = null
                }, enabled = candidate.isNotBlank() && (!isAlgebra || onOpenAlgebra != null)) {
                    Text(if (isAlgebra) "Открыть решение" else "Вставить")
                }
            },
            dismissButton = { TextButton(onClick = { scannedExpression = null }) { Text("Отмена") } }
        )
    }
    if (sheet != null) ModalBottomSheet(
        onDismissRequest = { sheet = null },
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        if (sheet == "steps") CalculationSteps(
            expression = shownExpression, result = shownResult, steps = steps,
            onExport = {
                exportExpression = shownExpression
                exportResult = shownResult
                exportSteps = steps.map { "${it.description}: ${it.expression}" }
                exportLauncher.launch("AeroCalc-решение.pdf")
            }
        ) else RecentCalculations(
            items = history.filter { it.subject == "MATH" && it.result.isNotBlank() },
            onSelect = {
                viewModel.clearDisplay()
                viewModel.pasteFromHistory(it)
                completedExpression = null; completedResult = null; sheet = null
            },
            onDelete = viewModel::deleteHistoryItem
        )
    }
}
