package com.example.wifidiscover

import android.net.wifi.p2p.WifiP2pDevice

data class MessageModel(var device: WifiP2pDevice,
    var message: MutableList<WifiFrame>){

    override fun toString(): String {
        return device.deviceName
    }

    fun getAllMessage() : String {
        var finalMessage : String = device.deviceName
        message.forEach{
            finalMessage += "\n ${it.getMessage()}"

        }

        return finalMessage
    }
}
