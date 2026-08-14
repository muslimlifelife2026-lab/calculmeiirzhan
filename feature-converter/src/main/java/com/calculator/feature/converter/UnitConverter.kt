package com.calculator.feature.converter

enum class ConversionCategory(val title: String) {
    LENGTH("Длина"),
    WEIGHT("Вес"),
    TEMPERATURE("Температура"),
    AREA("Площадь"),
    VOLUME("Объем"),
    SPEED("Скорость")
}

data class UnitItem(val name: String, val symbol: String, val baseMultiplier: Double)

object UnitConverter {
    val lengthUnits = listOf(
        UnitItem("Метры", "м", 1.0),
        UnitItem("Километры", "км", 1000.0),
        UnitItem("Сантиметры", "см", 0.01),
        UnitItem("Миллиметры", "мм", 0.001),
        UnitItem("Микрометры", "мкм", 1e-6),
        UnitItem("Нанометры", "нм", 1e-9),
        UnitItem("Мили", "ми", 1609.344),
        UnitItem("Ярды", "ярд", 0.9144),
        UnitItem("Футы", "фут", 0.3048),
        UnitItem("Дюймы", "дюйм", 0.0254)
    )

    val weightUnits = listOf(
        UnitItem("Килограммы", "кг", 1.0),
        UnitItem("Граммы", "г", 0.001),
        UnitItem("Миллиграммы", "мг", 1e-6),
        UnitItem("Тонны", "т", 1000.0),
        UnitItem("Фунты", "фунт", 0.45359237),
        UnitItem("Унции", "унц", 0.02834952),
        UnitItem("Караты", "кар", 0.0002)
    )

    val temperatureUnits = listOf(
        UnitItem("Цельсий", "°C", 1.0), // Base
        UnitItem("Фаренгейт", "°F", 1.0),
        UnitItem("Кельвин", "K", 1.0)
    )

    val areaUnits = listOf(
        UnitItem("Кв. метры", "м²", 1.0),
        UnitItem("Кв. километры", "км²", 1e6),
        UnitItem("Гектары", "га", 10000.0),
        UnitItem("Акры", "акр", 4046.856),
        UnitItem("Кв. футы", "фт²", 0.092903)
    )

    val volumeUnits = listOf(
        UnitItem("Куб. метры", "м³", 1.0),
        UnitItem("Литры", "л", 0.001),
        UnitItem("Миллилитры", "мл", 1e-6),
        UnitItem("Галлоны (США)", "гал", 0.00378541),
        UnitItem("Пинты", "пт", 0.000473176)
    )

    val speedUnits = listOf(
        UnitItem("Метры в секунду", "м/с", 1.0),
        UnitItem("Километры в час", "км/ч", 1.0 / 3.6),
        UnitItem("Мили в час", "миль/ч", 0.44704),
        UnitItem("Узлы", "узел", 0.514444)
    )

    fun getUnitsForCategory(category: ConversionCategory): List<UnitItem> {
        return when(category) {
            ConversionCategory.LENGTH -> lengthUnits
            ConversionCategory.WEIGHT -> weightUnits
            ConversionCategory.TEMPERATURE -> temperatureUnits
            ConversionCategory.AREA -> areaUnits
            ConversionCategory.VOLUME -> volumeUnits
            ConversionCategory.SPEED -> speedUnits
        }
    }

    fun convert(value: Double, from: UnitItem, to: UnitItem, category: ConversionCategory): Double {
        if (category == ConversionCategory.TEMPERATURE) {
            // Temperature requires specific offset logic
            val celsius = when (from.symbol) {
                "°C" -> value
                "°F" -> (value - 32.0) * 5.0 / 9.0
                "K" -> value - 273.15
                else -> value
            }
            return when (to.symbol) {
                "°C" -> celsius
                "°F" -> celsius * 9.0 / 5.0 + 32.0
                "K" -> celsius + 273.15
                else -> celsius
            }
        }
        
        // General multiplier logic: value * fromBase / toBase
        val inBase = value * from.baseMultiplier
        return inBase / to.baseMultiplier
    }
}
