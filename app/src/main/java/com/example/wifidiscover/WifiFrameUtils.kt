package com.example.wifidiscover

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import java.util.Date
import java.util.HashMap
import java.util.Locale

class WifiFrameUtils {

    companion object {
        fun wifiFrameToHashMap(wifiFrame: WifiFrame?): HashMap<String, String> {
            val message = HashMap<String, String>()
            if (wifiFrame == null) {
                return message
            }


            message["o"] = wifiFrame.sendMessage

            return message
        }

        fun hashMapToWiFiFrame(message: MutableMap<String, String>): WifiFrame {
            return WifiFrame().apply {
                sendMessage = message["o"]?: "Dispositivo x"

            }
        }

        fun buildMyWiFiFrame(context: Context, deviceMAC: WifiP2pDevice): WifiFrame {
            val sharedPreferences = context.getSharedPreferences(
                Constants.PREFERENCES_KEY,
                AppCompatActivity.MODE_PRIVATE
            )

            return WifiFrame().apply {
                sendMessage = sharedPreferences.getString(Constants.MESSAGE, "mensaje").toString()

            }
        }
    }
}