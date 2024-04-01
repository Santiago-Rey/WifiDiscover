package com.example.wifidiscover

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat

class WifiDirectBroadcastReceiver(
    private var mManager: WifiP2pManager,
    private var mChannel: WifiP2pManager.Channel,
    private var mainActivity: MainActivity
) : BroadcastReceiver() {


    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null){
            Toast.makeText(context, "Parametros nulos" , Toast.LENGTH_SHORT ).show()
        }
        val action = intent!!.action

        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                when (state) {
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                        // Wifi P2P is enabled
                    }
                    else -> {
                        // Wi-Fi P2P is not enabled
                        Toast.makeText(context, "No enable WifiP2P" , Toast.LENGTH_SHORT ).show()
                    }
                }
            }
        }
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(context, "Faltan pemisos 2" , Toast.LENGTH_SHORT ).show()
                return
            }

            mManager.requestPeers(mChannel, mainActivity.peerListListener)
        }

    }
}
