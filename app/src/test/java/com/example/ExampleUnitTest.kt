package com.example

import com.example.data.model.AtmosphereMood
import com.example.data.model.PressureUnit
import com.example.data.model.TempUnit
import com.example.data.model.WeatherInterpreter
import com.example.data.model.WeatherUnits
import com.example.data.model.WindUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun unitFormatting_convertsTemperatureWindAndPressure() {
        val metric = WeatherUnits(
            tempUnit = TempUnit.CELSIUS,
            windUnit = WindUnit.MS,
            pressureUnit = PressureUnit.MMHG,
        )
        assertEquals("20°", metric.formatTemp(20.2))
        assertEquals("5,0 м/с", metric.formatWind(18.0))
        assertEquals("760 мм рт. ст.", metric.formatPressure(1013.25))

        val imperial = WeatherUnits(
            tempUnit = TempUnit.FAHRENHEIT,
            windUnit = WindUnit.KMH,
            pressureUnit = PressureUnit.HPA,
        )
        assertEquals("68°F", imperial.formatTemp(20.0, includeUnit = true))
        assertEquals("18 км/ч", imperial.formatWind(18.0))
        assertEquals("1013 гПа", imperial.formatPressure(1013.2))
    }

    @Test
    fun weatherInterpreter_mapsWmoCodesAndWindDirection() {
        val clearDay = WeatherInterpreter.interpret(0, isDay = true)
        assertEquals("Ясно", clearDay.titleRu)
        assertEquals(AtmosphereMood.CLEAR_DAY, clearDay.mood)

        val storm = WeatherInterpreter.interpret(95, isDay = false)
        assertEquals(AtmosphereMood.STORM, storm.mood)

        assertEquals("С", WeatherInterpreter.windCardinalRu(0))
        assertEquals("В", WeatherInterpreter.windCardinalRu(90))
        assertEquals("Ю", WeatherInterpreter.windCardinalRu(180))
        assertEquals("З", WeatherInterpreter.windCardinalRu(270))
    }
}
