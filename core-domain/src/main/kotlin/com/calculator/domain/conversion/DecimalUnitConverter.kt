package com.calculator.domain.conversion

import java.math.BigDecimal
import java.math.MathContext

enum class ConversionCategory(val title: String) {
    LENGTH("Длина"), WEIGHT("Масса"), TEMPERATURE("Температура"), AREA("Площадь"),
    VOLUME("Объём"), SPEED("Скорость"), FORCE("Сила"), PRESSURE("Давление"),
    ENERGY("Энергия"), TIME("Время")
}

data class UnitItem(val name: String, val symbol: String, val numerator: String, val denominator: String = "1")

/** Defined unit factors, not measured precision. Repeating ratios use DECIMAL128 (34 digits).
 * NIST SP811: international foot=0.3048m, pound=0.45359237kg;
 * US liquid gallon=231in³; international acre=43560ft²; knot=1852m/3600s.
 */
object DecimalUnitConverter {
    private val mc = MathContext.DECIMAL128
    private val offset = BigDecimal("273.15")
    private val five = BigDecimal("5")
    private val nine = BigDecimal("9")
    private val thirtyTwo = BigDecimal("32")
    val units: Map<ConversionCategory, List<UnitItem>> = mapOf(
        ConversionCategory.LENGTH to listOf(
            UnitItem("Метры", "м", "1"), UnitItem("Километры", "км", "1000"),
            UnitItem("Сантиметры", "см", "0.01"), UnitItem("Миллиметры", "мм", "0.001"),
            UnitItem("Микрометры", "мкм", "0.000001"), UnitItem("Нанометры", "нм", "0.000000001"),
            UnitItem("Мили", "ми", "1609.344"), UnitItem("Ярды", "ярд", "0.9144"),
            UnitItem("Футы", "фут", "0.3048"), UnitItem("Дюймы", "дюйм", "0.0254")
        ),
        ConversionCategory.WEIGHT to listOf(
            UnitItem("Килограммы", "кг", "1"), UnitItem("Граммы", "г", "0.001"),
            UnitItem("Миллиграммы", "мг", "0.000001"), UnitItem("Тонны", "т", "1000"),
            UnitItem("Фунты", "фунт", "0.45359237"), UnitItem("Унции (avoirdupois)", "унц", "0.028349523125"),
            UnitItem("Караты", "кар", "0.0002")
        ),
        ConversionCategory.TEMPERATURE to listOf(
            UnitItem("Цельсий", "°C", "1"), UnitItem("Фаренгейт", "°F", "1"), UnitItem("Кельвин", "K", "1")
        ),
        ConversionCategory.AREA to listOf(
            UnitItem("Кв. метры", "м²", "1"), UnitItem("Кв. километры", "км²", "1000000"),
            UnitItem("Гектары", "га", "10000"), UnitItem("Акры (международные)", "акр", "4046.8564224"),
            UnitItem("Кв. футы", "фт²", "0.09290304")
        ),
        ConversionCategory.VOLUME to listOf(
            UnitItem("Куб. метры", "м³", "1"), UnitItem("Литры", "л", "0.001"),
            UnitItem("Миллилитры", "мл", "0.000001"), UnitItem("Галлоны (США, жидкость)", "гал", "0.003785411784"),
            UnitItem("Пинты (США, жидкость)", "пт", "0.000473176473")
        ),
        ConversionCategory.SPEED to listOf(
            UnitItem("Метры в секунду", "м/с", "1"), UnitItem("Километры в час", "км/ч", "5", "18"),
            UnitItem("Мили в час", "миль/ч", "0.44704"), UnitItem("Узлы", "узел", "463", "900")
        ),
        ConversionCategory.FORCE to listOf(
            UnitItem("Ньютоны", "Н", "1"), UnitItem("Килоньютоны", "кН", "1000"),
            UnitItem("Миллиньютоны", "мН", "0.001"), UnitItem("Дины", "дин", "0.00001")
        ),
        ConversionCategory.PRESSURE to listOf(
            UnitItem("Паскали", "Па", "1"), UnitItem("Килопаскали", "кПа", "1000"),
            UnitItem("Мегапаскали", "МПа", "1000000"), UnitItem("Бары", "бар", "100000"),
            UnitItem("Стандартные атмосферы", "атм", "101325")
        ),
        ConversionCategory.ENERGY to listOf(
            UnitItem("Джоули", "Дж", "1"), UnitItem("Килоджоули", "кДж", "1000"),
            UnitItem("Ватт-часы", "Вт·ч", "3600"), UnitItem("Киловатт-часы", "кВт·ч", "3600000")
        ),
        ConversionCategory.TIME to listOf(
            UnitItem("Секунды", "с", "1"), UnitItem("Миллисекунды", "мс", "0.001"),
            UnitItem("Минуты", "мин", "60"), UnitItem("Часы", "ч", "3600"), UnitItem("Сутки", "сут", "86400")
        )
    )

    fun convert(value: BigDecimal, from: UnitItem, to: UnitItem, category: ConversionCategory,
                temperatureDifference: Boolean = false): BigDecimal {
        val catalog = units.getValue(category)
        require(from in catalog && to in catalog) { "Единица не относится к выбранной величине" }
        if (category == ConversionCategory.TEMPERATURE) {
            // Multiply before division to keep Fahrenheit absolute-zero boundary exact.
            val kelvin = when (from.symbol) {
                "°C" -> if (temperatureDifference) value else value.add(offset)
                "°F" -> if (temperatureDifference) value.multiply(five).divide(nine, mc)
                    else value.subtract(thirtyTwo).multiply(five).add(offset.multiply(nine)).divide(nine, mc)
                else -> value
            }
            require(temperatureDifference || kelvin.signum() >= 0) { "Температура ниже абсолютного нуля" }
            if (from == to) return value
            return when (to.symbol) {
                "°C" -> if (temperatureDifference) kelvin else kelvin.subtract(offset)
                "°F" -> if (temperatureDifference) kelvin.multiply(nine).divide(five, mc)
                    else kelvin.subtract(offset).multiply(nine).divide(five, mc).add(thirtyTwo)
                else -> kelvin
            }
        }
        if (from == to) return value
        return value.multiply(BigDecimal(from.numerator)).multiply(BigDecimal(to.denominator))
            .divide(BigDecimal(from.denominator).multiply(BigDecimal(to.numerator)), mc)
    }
}
