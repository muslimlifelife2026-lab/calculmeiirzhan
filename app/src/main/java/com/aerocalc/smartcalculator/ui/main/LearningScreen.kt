package com.aerocalc.smartcalculator.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculator.core.ui.components.GlassCard
import com.calculator.core.ui.theme.*
import com.aerocalc.smartcalculator.learning.LearningPractice
import com.aerocalc.smartcalculator.learning.LearningProgress
import com.aerocalc.smartcalculator.learning.LearningProgressRepository
import com.aerocalc.smartcalculator.learning.PracticeAttempt
import com.aerocalc.smartcalculator.learning.PracticeQuestion
import java.util.Calendar
import kotlinx.coroutines.launch

private data class Lesson(val title: String, val example: String, val explanation: String)

private val lessons = listOf(
    Lesson("Порядок действий", "2 + 3 × 4 = 14", "Сначала умножьте 3 × 4 = 12, затем прибавьте 2. Скобки меняют порядок: (2 + 3) × 4 = 20."),
    Lesson("Проценты", "15% от 200 = 30", "Переведите процент в дробь: 15 / 100 = 0,15. Умножьте 200 × 0,15 = 30."),
    Lesson("Перевод единиц", "1,5 км = 1500 м", "В одном километре 1000 метров. Умножьте 1,5 × 1000 и проверьте результат в конвертере."),
    Lesson("Второй закон Ньютона", "F = ma", "Сила равна произведению массы на ускорение. При m = 2 кг и a = 3 м/с² получаем F = 6 Н.")
)

@Composable
fun LearningScreen(
    onOpenPhysicsPractice: () -> Unit,
    onOpenConverter: () -> Unit,
    modifier: Modifier = Modifier,
    onLearningActivityCompleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { LearningProgressRepository(context) }
    var progress by remember(repository) { mutableStateOf(repository.load()) }
    val dailyQuestion = LearningPractice.dailyQuestion(LearningPractice.localEpochDay(Calendar.getInstance()))
    val activeQuestion = LearningPractice.find(progress.activeAttempt?.questionId)
    val nextQuestion = LearningPractice.nextQuestion(progress)
    var selectedTopic by rememberSaveable { mutableStateOf(LearningPractice.topics.first()) }
    val reviewQuestion = LearningPractice.questions.firstOrNull { it.id in progress.reviewIds }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    fun update(updated: LearningProgress) {
        repository.save(updated)
        progress = updated
    }
    fun open(question: PracticeQuestion) {
        update(progress.start(question))
        scope.launch {
            withFrameNanos { }
            listState.animateScrollToItem(2)
        }
    }
    LazyColumn(
        modifier.fillMaxSize().testTag("learning_list"),
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 12.dp)) {
                Text("Обучение", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Короткие объяснения и практика", color = TextSecondary, fontSize = 15.sp)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Задание дня", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text(dailyQuestion.topic, color = AccentSecondary, style = MaterialTheme.typography.labelLarge)
                    Text(dailyQuestion.prompt, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Проверено заданий: ${progress.completedIds.size} из ${LearningPractice.questions.size}", color = TextMuted,
                        style = MaterialTheme.typography.bodySmall)
                    if (activeQuestion?.id != dailyQuestion.id) {
                        Button(onClick = { open(dailyQuestion) }) {
                            Text(if (dailyQuestion.id in progress.completedIds) "Повторить задание" else "Решить задание")
                        }
                    }
                    if (nextQuestion != null && activeQuestion == null && nextQuestion.id != dailyQuestion.id) {
                        OutlinedButton(onClick = { open(nextQuestion) }) { Text("Продолжить обучение") }
                    }
                    if (progress.completedIds.size == LearningPractice.questions.size) {
                        Text("Базовый курс пройден. Закрепляйте знания в любой теме: повторения доступны без ограничения.", color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (activeQuestion != null) {
            item(key = "active-practice") {
                PracticeCard(
                    question = activeQuestion,
                    attempt = progress.activeAttempt!!,
                    onSelect = { index ->
                        progress.activeAttempt?.let { update(progress.updateAttempt(it.select(index, activeQuestion), activeQuestion)) }
                    },
                    onCheck = {
                        progress.activeAttempt?.let { update(progress.updateAttempt(it.check(activeQuestion), activeQuestion)) }
                    },
                    onComplete = {
                        // A deliberate completion is the only event that can trigger a natural ad break.
                        if (progress.activeAttempt?.canComplete(activeQuestion) == true) {
                            val firstCompletion = activeQuestion.id !in progress.completedIds
                            update(progress.complete(activeQuestion))
                            if (firstCompletion) onLearningActivityCompleted()
                        }
                    }
                )
            }
        }
        if (reviewQuestion != null) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Повторить сложное · ${progress.reviewIds.size}", color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium)
                        Text("Здесь остаются задания, в которых была ошибка. Решите их верно ещё раз после завершения, чтобы закрепить материал.",
                            color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { open(reviewQuestion) }) { Text("Разобрать ошибки") }
                    }
                }
            }
        }
        item {
            Text("Практика по темам", color = TextPrimary, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LearningPractice.topics, key = { it }) { topic ->
                    FilterChip(selected = selectedTopic == topic, onClick = { selectedTopic = topic },
                        label = { Text(topic) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        items(LearningPractice.questions.filter { it.topic == selectedTopic }, key = { "catalog-${it.id}" }) { question ->
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = "Открыть задание") { open(question) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(question.prompt, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    val status = when {
                        question.id == activeQuestion?.id -> "Открыто выше"
                        question.id in progress.drafts -> "Продолжить сохранённый ответ"
                        question.id in progress.reviewIds -> "Нужно повторить"
                        question.id in progress.completedIds -> "Решено · повторить"
                        else -> "Начать задание"
                    }
                    Text(status, color = AccentSecondary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item {
            Text("Короткие уроки", color = TextPrimary, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp))
        }
        items(lessons, key = { it.title }) { lesson ->
            var expanded by rememberSaveable(lesson.title) { mutableStateOf(false) }
            GlassCard(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClickLabel = "Открыть урок ${lesson.title}") { expanded = !expanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(lesson.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(lesson.example, color = AccentSecondary, style = MaterialTheme.typography.titleLarge)
                    if (expanded) Text(lesson.explanation, color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                    else Text("Нажмите, чтобы увидеть объяснение", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = "Открыть тренажёр по физике", onClick = onOpenPhysicsPractice)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.AutoStories, null, tint = AccentSecondary)
                    Column(Modifier.weight(1f)) {
                        Text("Тренажёр по физике", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Собирайте формулы и проверяйте ответ", color = TextSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.Rounded.ArrowForward, null, tint = TextMuted)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .clickable(role = Role.Button, onClickLabel = "Открыть конвертер", onClick = onOpenConverter)) {
                Text("Проверить перевод единиц в конвертере", color = TextPrimary)
            }
        }
    }
}

@Composable
private fun PracticeCard(
    question: PracticeQuestion,
    attempt: PracticeAttempt,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit,
    onComplete: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Практика · ${question.topic}", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(question.prompt, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Column(Modifier.selectableGroup()) {
                question.answers.forEachIndexed { index, answer ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = attempt.selectedAnswerIndex == index, role = Role.RadioButton,
                                onClick = { onSelect(index) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = attempt.selectedAnswerIndex == index, onClick = null)
                        Text(answer, color = TextPrimary, modifier = Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (attempt.checked) {
                Text(if (attempt.canComplete(question)) "Верно" else "Пока неверно. Прочитайте объяснение и попробуйте ещё раз.",
                    color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text(question.explanation, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            if (attempt.canComplete(question)) {
                Button(onClick = onComplete) { Text("Завершить") }
            } else {
                Button(onClick = onCheck, enabled = attempt.selectedAnswerIndex != null && !attempt.checked) {
                    Text("Проверить ответ")
                }
            }
        }
    }
}
