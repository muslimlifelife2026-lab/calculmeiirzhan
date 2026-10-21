package com.calculator.feature.chemistry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.engine.solver.ChemistrySolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun BalancerSection(equationInput: String, onEquationChange: (String) -> Unit, reactionPresets: List<ReactionPreset>, modifier: Modifier = Modifier) {
    val result by produceState<String?>(null, equationInput) {
        value = null
        if (equationInput.isNotBlank()) {
            delay(250)
            value = withContext(Dispatchers.Default) { ChemistrySolver.balanceReaction(equationInput) }
        }
    }
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Химические реакции", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Задайте реагенты и продукты — подберём целые коэффициенты.", color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            ChemistryInputCard("Уравнение реакции", equationInput, onEquationChange, "H2 + O2 -> H2O",
                listOf("+", " -> ", "H", "O", "C", "N", "Fe", "Na", "Cl", "2", "3", "4", "(", ")", "⌫"))
        }
        item { ChemistryExamples(reactionPresets.map { it.title to it.equation }, onEquationChange) }
        result?.let { response ->
            // An error explaining the separator also contains '='; that is not a balanced reaction.
            val success = response.contains(" = ") && !response.startsWith("Ошибка") && !response.startsWith("Не удалось")
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (success) "Уравненная реакция" else "Проверьте уравнение", style = MaterialTheme.typography.titleMedium)
                        Text(if (success) response else chemistryErrorMessage(response),
                            color = if (success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            style = if (success) MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace)
                                else MaterialTheme.typography.bodyMedium)
                        if (success) {
                            Text("Сохраняется число атомов каждого элемента. Подбор коэффициентов не доказывает, что реакция протекает в выбранных условиях.",
                                color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
