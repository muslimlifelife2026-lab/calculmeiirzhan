package com.calculator.domain.utils

import com.calculator.domain.model.Formula
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

object FormulaLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val formulas: List<Formula> by lazy {
        loadAllFormulas()
    }

    private fun loadAllFormulas(): List<Formula> {
        val loaded = mutableListOf<Formula>()
        try {
            // 1. Read index.json
            val indexStream = javaClass.classLoader.getResourceAsStream("formulas/index.json") 
                ?: return emptyList()
            
            val indexContent = BufferedReader(InputStreamReader(indexStream, "UTF-8")).use { it.readText() }
            val formulaIds: List<String> = json.decodeFromString(indexContent)

            // 2. Load each formula file
            for (id in formulaIds) {
                val stream = javaClass.classLoader.getResourceAsStream("formulas/$id.json") ?: continue
                val content = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
                val formula: Formula = json.decodeFromString(content)
                loaded.add(formula)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return loaded
    }
}
