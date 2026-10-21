package com.calculator.feature.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.theme.*
import com.calculator.core.ui.components.GlassSurface
import com.calculator.core.ui.utils.ErrorLocalizer

@Composable
fun CalculatorDisplay(
    expression: String, result: String, error: String, degrees: Boolean, hasSteps: Boolean,
    onAngle: () -> Unit, onScanner: () -> Unit, onHistory: () -> Unit, onSteps: () -> Unit,
    isResultFinal: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AeroCalc", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            GlassToolbarButton(Icons.Rounded.History, "Последние вычисления", onHistory)
            GlassToolbarButton(Icons.Rounded.CameraAlt, "Сканировать пример", onScanner)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassSurface(radius = 24.dp, elevation = 4.dp) {
                TextButton(onClick = onAngle, modifier = Modifier.height(48.dp)) {
                    Text(if (degrees) "DEG · Градусы" else "RAD · Радианы", color = AccentSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            GlassToolbarButton(Icons.Rounded.ContentCopy,
                if (isResultFinal) "Скопировать результат" else "Скопировать выражение", onClick = {
                val copyValue = (if (isResultFinal) result else expression).ifBlank { "0" }
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("AeroCalc", copyValue))
                android.widget.Toast.makeText(context, "Скопировано", android.widget.Toast.LENGTH_SHORT).show()
            })
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.End) {
            Text(
                text = expression.ifBlank { "0" },
                color = if (isResultFinal) TextSecondary else TextPrimary,
                fontSize = if (isResultFinal) 24.sp else 52.sp,
                lineHeight = if (isResultFinal) 32.sp else 60.sp,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Light,
                maxLines = 1,
                modifier = Modifier.testTag("calculator_expression").horizontalScroll(rememberScrollState())
            )
            // Reserve the preview line even while its debounced calculation is pending.
            Text(
                text = if (!isResultFinal && result == expression) "" else result,
                color = if (isResultFinal) TextPrimary else TextSecondary,
                fontSize = if (isResultFinal) 52.sp else 24.sp,
                lineHeight = if (isResultFinal) 60.sp else 32.sp,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Light,
                maxLines = 1, modifier = Modifier.horizontalScroll(rememberScrollState())
            )
            Text(
                when { error.isNotBlank() -> ErrorLocalizer.localize(error)
                    expression.isBlank() -> "Посчитайте или отсканируйте пример"
                    else -> "" },
                color = if (error.isNotBlank()) ErrorRed else TextMuted,
                fontSize = 13.sp, lineHeight = 20.sp, minLines = 2
            )
            Box(Modifier.heightIn(min = 48.dp)) {
                if (hasSteps) TextButton(onClick = onSteps) {
                    Text("Как получен ответ", color = TextSecondary, fontSize = 13.sp)
                    Icon(Icons.Rounded.KeyboardArrowRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun GlassToolbarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    GlassSurface(modifier = Modifier.size(48.dp), radius = 24.dp, elevation = 4.dp) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, description, tint = TextPrimary, modifier = Modifier.size(21.dp))
        }
    }
}
