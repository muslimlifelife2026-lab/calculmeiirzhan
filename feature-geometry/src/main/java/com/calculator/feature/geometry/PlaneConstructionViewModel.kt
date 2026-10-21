package com.calculator.feature.geometry

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.calculator.domain.geometry.PlaneConstruction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ConstructionState(
    val drawing: PlaneConstruction = PlaneConstruction(),
    val canUndo: Boolean = false,
    val error: String? = null
)

/** A bounded local draft; no network or external-storage permission is needed. */
class PlaneConstructionViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("geometry_construction", Context.MODE_PRIVATE)
    private val undo = ArrayDeque<PlaneConstruction>()
    private val _state = MutableStateFlow(loadDraft())
    val state = _state.asStateFlow()

    private fun loadDraft(): ConstructionState {
        val saved = preferences.getString("draft_v1", null) ?: return ConstructionState()
        return try {
            require(saved.length <= 64_000)
            ConstructionState(Json.decodeFromString<PlaneConstruction>(saved).validate())
        } catch (_: Exception) {
            ConstructionState(error = "Не удалось восстановить чертёж. Создайте новый — он сохранится автоматически.")
        }
    }

    fun addPoint(x: String, y: String) {
        val parsedX = x.trim().replace(',', '.').toDoubleOrNull()
        val parsedY = y.trim().replace(',', '.').toDoubleOrNull()
        if (parsedX == null || parsedY == null) {
            _state.value = _state.value.copy(error = "Введите обе координаты числами от −10 до 10.")
            return
        }
        addPoint(parsedX, parsedY)
    }

    fun addPoint(x: Double, y: Double) = change { it.addPoint(x, y) }
    fun addSegment(from: Int, to: Int) = change { it.addSegment(from, to) }
    fun addCircle(center: Int, through: Int) = change { it.addCircle(center, through) }
    fun removePoint(id: Int) = change { it.removePoint(id) }
    fun clear() = change { PlaneConstruction() }
    fun dismissError() { _state.value = _state.value.copy(error = null) }

    fun undo() {
        if (undo.isEmpty()) return
        save(undo.removeLast())
    }

    private fun change(transform: (PlaneConstruction) -> PlaneConstruction) {
        try {
            val before = _state.value.drawing
            val after = transform(before).validate()
            if (after != before) {
                if (undo.size == 32) undo.removeFirst()
                undo.addLast(before)
            }
            save(after)
        } catch (error: IllegalArgumentException) {
            _state.value = _state.value.copy(error = error.message ?: "Проверьте точки и координаты.")
        }
    }

    private fun save(drawing: PlaneConstruction) {
        preferences.edit().putString("draft_v1", Json.encodeToString(drawing)).apply()
        _state.value = ConstructionState(drawing, undo.isNotEmpty())
    }
}
