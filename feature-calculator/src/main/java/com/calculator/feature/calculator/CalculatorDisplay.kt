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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AeroCalc", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
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
            if (result.isNotBlank()) GlassToolbarButton(Icons.Rounded.ContentCopy, "Скопировать результат", onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Результат", result))
                android.widget.Toast.makeText(context, "Результат скопирован", android.widget.Toast.LENGTH_SHORT).show()
            })
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.End) {
            Text(
                text = expression.ifBlank { "0" },
                color = if (result.isBlank()) TextPrimary else TextSecondary,
                fontSize = if (result.isBlank()) 48.sp else 26.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
            if (result.isNotBlank()) Text(
                text = result, color = TextPrimary, fontSize = 48.sp, fontWeight = FontWeight.Light,
                maxLines = 1, modifier = Modifier.horizontalScroll(rememberScrollState())
            )
            if (error.isNotBlank()) Text(ErrorLocalizer.localize(error), color = ErrorRed, fontSize = 14.sp)
            if (expression.isBlank()) Text("Введите пример или используйте камеру", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            if (hasSteps) TextButton(onClick = onSteps) {
                Text("Как получен ответ", color = TextSecondary, fontSize = 13.sp)
                Icon(Icons.Rounded.KeyboardArrowRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
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
