package ch.fhnw.osmdemo.viewmodel

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.round
import kotlin.math.sinh
import kotlin.math.tan

val FHNW = GeoPosition(latitude = 47.480995, longitude = 8.211862)

data class GeoPosition(val longitude: Double, val latitude: Double) {
    fun dms(): String {
        return format(latitude, longitude)
    }

    fun asNormalizedWebMercator(): NormalizedPoint {
        val earthRadius = 6_378_137.0 // in meters
        val latRad = latitude * PI / 180.0
        val lngRad = longitude * PI / 180.0

        val x = earthRadius * lngRad
        val y = earthRadius * ln(tan((PI / 4.0) + (latRad / 2.0)))

        val piR = earthRadius * PI
        val normalizedX = (x + piR) / (2.0 * piR)
        val normalizedY = (piR - y) / (2.0 * piR)

        return NormalizedPoint(normalizedX, normalizedY)
    }

    private fun format(latitude: Double, longitude: Double): String {
        val latCompassDirection = if (latitude > 0.0) "N" else "S"
        val lonCompassDirection = if (longitude > 0.0) "E" else "W"

        return "${getDMS(latitude)} $latCompassDirection, ${getDMS(longitude)} $lonCompassDirection"
    }

    private fun getDMS(value: Double): String {
        val absValue = abs(value)
        val degree = absValue.toInt()
        val minutes = ((absValue - degree) * 60.0).toInt()
        val seconds = (absValue - degree - minutes / 60.0) * 3600.0

        return "${degree}° ${minutes}′ ${(round(seconds * 10000) / 10000)}″"
    }
}

data class NormalizedPoint(val x: Double, val y: Double){
    fun asGeoPosition() : GeoPosition{
        val earthRadius = 6_378_137.0 // in meters
        val piR = earthRadius * PI
        val denormalizedX = x * (2.0 * piR) - piR
        val denormalizedY = -1 * (y * (2.0 * piR) - piR)

        val lonRad = denormalizedX / earthRadius             // In radians
        val latRad = atan(sinh(denormalizedY / earthRadius)) // In radians

        val longitude = lonRad * 180.0 / PI
        val latitude = latRad * 180.0 / PI

        return GeoPosition(longitude, latitude)
    }
}

