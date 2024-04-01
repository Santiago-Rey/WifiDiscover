package com.example.wifidiscover

import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wifidiscover.Constants.SOCKET_PORT
import com.example.wifidiscover.Constants.SOCKET_TIMEOUT
import com.example.wifidiscover.Constants.TAG
import com.example.wifidiscover.Constants.TAG_WIFI
import com.example.wifidiscover.databinding.ActivityMainBinding
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    //WiFi Direct variables
    private lateinit var wifiManager: WifiManager
    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: WifiP2pManager.Channel

    private lateinit var mReceiver: BroadcastReceiver
    private lateinit var mIntentFilter: IntentFilter
    private var peers: MutableList<WifiP2pDevice> = mutableListOf()
    private var deviceNameArray: MutableList<String> = mutableListOf()
    private var deviceArray: MutableList<WifiP2pDevice> = mutableListOf()

    private val connectedDevices: MutableList<WifiP2pDevice> = mutableListOf()

    var text: CharSequence = "Activa el wifi"
    var duration = Toast.LENGTH_SHORT

    private val buddies = mutableMapOf<String, String>()

    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        if (!peerList.deviceList.equals(peers)) {
            peers.clear()
            peers.addAll(peerList.deviceList)
            deviceNameArray.clear()
            deviceArray.clear()
            for (device in peerList.deviceList) {
                deviceNameArray.add(device.deviceName)
                deviceArray.add(device)
            }

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                deviceNameArray
            )
            binding.peerListView.adapter = adapter
        }
        // Actualiza la lista de dispositivos conectados
        connectedDevices.clear()
        connectedDevices.addAll(peerList.deviceList)

        if (peers.size == 0) {
            Toast.makeText(this, "No Device Found", Toast.LENGTH_SHORT).show()
            return@PeerListListener
        }

        discoverPeers()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        init()
        startRegistration()
        addServiceRequest()
        exqListener()
        discoverService()
    }

    fun exqListener(){

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            wifiManager.isWifiEnabled = true

        } else if (!wifiManager.isWifiEnabled) {
            //Wifi activation
            val toast: Toast = Toast.makeText(applicationContext, text, duration)
            toast.show()
        }

        binding.discover.setOnClickListener {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                    this,
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
                Toast.makeText(this, "Faltan pemisos 5" , Toast.LENGTH_SHORT ).show()
                return@setOnClickListener
            }
            mManager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    binding.connectionStatus.text = "Discovery Started"
                }

                override fun onFailure(reason: Int) {
                    binding.connectionStatus.text = "Discovery Starting Failed"
                }
            })

        }

    }

    fun init(){

        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(this, mainLooper) {
            Toast.makeText(this, "Channel Disconnected", Toast.LENGTH_SHORT).show()
        }
        mReceiver = WifiDirectBroadcastReceiver(mManager, mChannel, this)
        mIntentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        binding.sendButton.setOnClickListener { sendMessageToConnectedDevices() }

    }

    //Agregando servicio local
    private fun startRegistration() {
        //  Create a string map containing information about your service.
        val record: Map<String, String> = mapOf(
            "listenport" to SOCKET_PORT.toString(),
            "buddyname" to "John Doe${(Math.random() * 1000).toInt()}",
            "available" to "visible"
        )

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record)

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        )) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Faltan pemisos 1" , Toast.LENGTH_SHORT ).show()
            return
        }
        mManager.addLocalService(mChannel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
                discoverPeers()
            }

            override fun onFailure(arg0: Int) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(this@MainActivity, "Fail local service" , Toast.LENGTH_SHORT ).show()
            }
        })
    }



    //onreceivelocation en BP
    private fun discoverService() {

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { _, record, device ->
            Log.d(TAG, "DnsSdTxtRecord available -$record")
            record["buddyname"]?.also {
                buddies[device.deviceAddress] = it
            }
            if (record.isEmpty()) return@DnsSdTxtRecordListener
        }

        mManager.setDnsSdResponseListeners(mChannel,
            {  instanceName, registrationType, srcDevice ->
                Toast.makeText(this, "Servicio encontrado : Nombre ${srcDevice.deviceName}" , Toast.LENGTH_SHORT ).show()}, txtListener)
    }

    private fun discoverPeers() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this,
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
            Toast.makeText(this, "Faltan pemisos 3" , Toast.LENGTH_SHORT ).show()
            return
        }
            mManager.discoverPeers(
            mChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED ||
                        (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                            this@MainActivity,
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
                        Toast.makeText(this@MainActivity, "Faltan pemisos 7" , Toast.LENGTH_SHORT ).show()
                        return
                    }
                    //mManager.requestPeers(mChannel, this@MainActivity)
                }

                override fun onFailure(error: Int) {
                    Toast.makeText(this@MainActivity, "failure peers" , Toast.LENGTH_SHORT ).show()
                    Log.e(TAG_WIFI, "Discover peers has failed. $error")
                }
            })
    }

    private fun discoverServices() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                this,
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
            Toast.makeText(this, "Faltan pemisos 4" , Toast.LENGTH_SHORT ).show()
            return
        }
        mManager.discoverServices(
            mChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(this@MainActivity, "success discover" , Toast.LENGTH_SHORT ).show()
                }

                override fun onFailure(code: Int) {
                    Toast.makeText(this@MainActivity, "Failure discover" , Toast.LENGTH_SHORT ).show()
                    Log.e(TAG_WIFI, "Discover services has failed. $code")
                }
            }
        )
    }

    private fun addServiceRequest() {
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(
            "_test",
            "_presence._tcp"
        )

        mManager.addServiceRequest(
            mChannel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    discoverServices()
                }

                override fun onFailure(code: Int) {
                    Toast.makeText(this@MainActivity, "Failure addService" , Toast.LENGTH_SHORT ).show()
                    Log.e(TAG_WIFI, "Add service request has failed. $code")
                }
            }
        )
    }



    private fun sendMessageToConnectedDevices() {
        val message: String = binding.messageEditText.text.toString()
        if (message.isEmpty()) {
            Toast.makeText(this@MainActivity, "Por favor, ingrese un mensaje", Toast.LENGTH_SHORT)
                .show()
            return
        }
        for (device in connectedDevices) {
            sendSingleMessage(device, message)
        }
    }

    private fun sendSingleMessage(device: WifiP2pDevice, message: String) {
        Thread {
            val messageBytes = message.toByteArray()
            try {
                // Obtener el socket de salida para el dispositivo
                val socket = Socket()

                // Conectar al dispositivo
                socket.bind(null)
                socket.connect(
                    InetSocketAddress(
                        device.deviceAddress,
                        SOCKET_PORT
                    ),  SOCKET_TIMEOUT
                )

                // Obtener el flujo de salida
                val outputStream = socket.getOutputStream()

                // Enviar el mensaje
                outputStream.write(messageBytes)
                outputStream.flush()

                // Manejar el envío exitoso
                Log.d(TAG, "Mensaje enviado a " + device.deviceName)

                // Cerrar el socket después de enviar el mensaje
                socket.close()
            } catch (e: Exception) {
                // Manejar errores de envío
                Log.e(
                    TAG,"Error al enviar mensaje a " + device.deviceName + ": " + e.message
                )
            }
        }.start()
    }



    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }



}