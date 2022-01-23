package com.example.mqttkotlinsample

class Room(
    var esps: ArrayList<Esp> = ArrayList()
) {

    fun getLedEsps(): List<Esp> {
        return esps.filter { esp -> esp.isLed() }
    }

    fun getTempSensorEsps(): List<Esp> {
        return esps.filter { esp -> esp.isTempSensor() }
    }

    fun isAnyLedOn(): Boolean {
        return esps.any { it.isLedOnOff }
    }
}

class Esp(
    val id: Int,
    var textTitle: String,
    var textUnder: String
) {
    var types: HashSet<EspType> = HashSet()
    var status = EspStatus.NO_PING_YET
    var failedPingCounter = 0

    var isLedOnOff = false;

    fun setIsAlive() {
        this.status = EspStatus.ALIVE
        this.failedPingCounter = 0
    }

    fun setIsNotAlive() {
        this.failedPingCounter++
        if (this.failedPingCounter >= 3) {
            this.status = EspStatus.NOT_ALIVE
        } else {
            this.status = EspStatus.NO_PING_YET
        }
    }

    fun defineAsLed() {
        types.add(EspType.LED)
    }

    fun defineAsTempSensor() {
        types.add(EspType.TEMP_SENSOR)
    }

    fun toggleLed(onOff: Boolean) {
        this.isLedOnOff = onOff
    }

    fun isLed(): Boolean {
        return types.contains(EspType.LED)
    }

    fun isTempSensor(): Boolean {
        return types.contains(EspType.TEMP_SENSOR)
    }
}

enum class EspStatus {
    ALIVE,
    NO_PING_YET,
    NOT_ALIVE
}

enum class EspType {
    LED,
    TEMP_SENSOR
}
