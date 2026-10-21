package com.calculator.feature.chemistry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.TextSecondary
import com.calculator.domain.model.AtomicMassType
import com.calculator.engine.solver.ChemistrySolver
import java.util.Locale

@Composable
fun MolarMassSection(formulaInput: String, onFormulaChange: (String) -> Unit, molarPresets: List<ChemicalPreset>, modifier: Modifier = Modifier) {
    val result = remember(formulaInput) { formulaInput.takeIf { it.isNotBlank() }?.let(ChemistrySolver::solveMolarMass) }
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Молярная масса", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Введите формулу — увидите массу одного моля и состав вещества.", color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            ChemistryInputCard("Формула вещества", formulaInput, onFormulaChange, "H2SO4 или Ca(OH)2",
                listOf("H", "O", "C", "N", "Na", "Cl", "Fe", "Ca", "S", "K", "Al", "Cu", "(", ")", "2", "3", "4", "5", "6", "⌫"))
        }
        item { ChemistryExamples(molarPresets.map { "${it.title} · ${it.formula}" to it.formula }, onFormulaChange) }
        if (result?.success == true) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Масса одного моля", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        Text(String.format(Locale.US, "≈ %.3f г/моль", result.molarMass),
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text("По справочным атомным массам; результат зависит от изотопного состава.",
                            color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        if (result.elementCounts.keys.any { it.atomicMassType == AtomicMassType.REPRESENTATIVE_ISOTOPE_MASS }) {
                            Text("Для элементов без стандартной атомной массы использована масса выбранного изотопа из PubChem.",
                                color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Text("Состав и массовые доли", style = MaterialTheme.typography.titleLarge) }
            items(result.elementCounts.toList(), key = { it.first.symbol }) { (element, count) ->
                val percent = element.atomicMass * count / result.molarMass * 100.0
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${element.symbol} · ${element.nameRu}", style = MaterialTheme.typography.titleMedium)
                        Text("Атомов: $count · Массовая доля: ${String.format(Locale.US, "%.1f", percent)} %",
                            color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else if (result != null) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(chemistryErrorMessage(result.errorMessage), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
