package com.calculator.engine.scanner

/** Conservative OCR cleanup. It never drops a side of an equation or joins separate lines. */
object MathScanNormalizer {
    const val MAX_INPUT_LENGTH = 256
    private val words = Regex("[A-Za-z]+")
    private val allowedWords = setOf(
        "x", "y", "pi", "e", "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "sqrt", "ln", "log", "exp", "abs", "fact"
    )
    private val characters = Regex("[0-9A-Za-zπ+\\-*/().^%=\\s]+")

    fun normalize(raw: String): String? {
        if (raw.length > MAX_INPUT_LENGTH * 4) return null
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size !in 1..2) return null
        val normalized = lines.map { line ->
            line.replace('×', '*').replace('·', '*').replace('÷', '/')
                .replace('−', '-').replace('–', '-').replace(',', '.')
                .replace("²", "^2").replace("³", "^3")
                .let { words.replace(it) { match -> match.value.lowercase() } }
        }
        if (normalized.any { line ->
                !characters.matches(line) || words.findAll(line).any { it.value !in allowedWords } ||
                    !line.any(Char::isDigit) && !line.contains('π') &&
                    words.findAll(line).none { it.value in setOf("x", "y", "e", "pi") } ||
                    line.count { it == '=' } > 1 ||
                    ('=' in line && line.split('=').any { it.isBlank() })
            }) return null
        if (normalized.size == 2 && (normalized.any { it.count { char -> char == '=' } != 1 } ||
                normalized.none { line -> words.findAll(line).any { it.value in setOf("x", "y") } })) return null
        return normalized.joinToString("\n").takeIf { it.length <= MAX_INPUT_LENGTH }
    }
}
