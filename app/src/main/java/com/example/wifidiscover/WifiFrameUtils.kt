package com.example.wifidiscover

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import androidx.appcompat.app.AppCompatActivity
import java.util.HashMap
import java.util.UUID

class WifiFrameUtils {



    companion object {

        var deviceMultihop = ""
        var deviceIdMultiHop = ""
        var idDevice = ""
        fun wifiFrameToHashMap(wifiFrame: WifiFrame?): HashMap<String, String> {
            val message = HashMap<String, String>()
            deviceMultihop = ""
            deviceIdMultiHop = ""

            if (wifiFrame == null) {
                return message
            }


            message["o"] = wifiFrame.sendMessage
            message["d"] = wifiFrame.dateSend
            message["h"] = idDevice

            return message
        }

        fun wifiFrameToHashMapMultihop(
            deviceName: WifiP2pDevice,
            messageMulti: String = "",
            id: String,
            dateSend: String
        ): HashMap<String, String> {
            val message = HashMap<String, String>()


            message["d"] = dateSend
            message["g"] = deviceName.deviceName
            message["o"] = messageMulti
            message["h"] = id

            return message
        }

        fun hashMapToWiFiFrame(message: MutableMap<String, String>): WifiFrame {
            return WifiFrame().apply {
                sendMessage = message["o"]?: ""
                dateSend = message["d"]?: "0L"
                dateReceived =  getFormattedDateTime()
                deviceMultihop = message["g"]?: ""
                deviceIdMultiHop = message["h"]?: ""


            }
        }

        fun buildMyWiFiFrame(context: Context): WifiFrame {
            val sharedPreferences = context.getSharedPreferences(
                Constants.PREFERENCES_KEY,
                AppCompatActivity.MODE_PRIVATE
            )

            return WifiFrame().apply {
                sendMessage = sharedPreferences.getString(Constants.MESSAGE, "mensaje").toString()
                dateSend = getFormattedDateTime()

            }
        }

        fun getUUIDWiFiFrame(context: Context) {
            val sharedPreferences = context.getSharedPreferences(
                Constants.PREFERENCES_KEY,
                AppCompatActivity.MODE_PRIVATE
            )
            idDevice = sharedPreferences.getString(Constants.UUID, UUID.randomUUID().toString())?: UUID.randomUUID().toString()
            val editor = sharedPreferences.edit()
            editor.putString(Constants.UUID, idDevice)
            editor.apply()

        }



    }


}