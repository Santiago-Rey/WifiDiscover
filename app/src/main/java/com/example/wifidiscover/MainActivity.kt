package com.example.wifidiscover

import android.Manifest
import android.content.SharedPreferences
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.wifidiscover.Constants.TAG
import com.example.wifidiscover.Constants.TAG_WIFI
import com.example.wifidiscover.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding : ActivityMainBinding

    //WiFi Direct variables
    private lateinit var wifiManager: WifiManager
    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: WifiP2pManager.Channel

    private var peers: MutableList<WifiP2pDevice> = mutableListOf()
    private var deviceNameArray: MutableList<String> = mutableListOf()
    private var deviceArray: MutableList<WifiP2pDevice> = mutableListOf()


    private val connectedDevices: MutableList<WifiP2pDevice> = mutableListOf()
    private var info : WifiFrame = WifiFrame()

    var text: CharSequence = "Activa el wifi"
    var duration = Toast.LENGTH_SHORT


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

        sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)


        exqListener()


    }



    fun exqListener(){

        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text.toString()

            if(message.isNotBlank()){
                Toast.makeText(
                    applicationContext,
                    "Mensaje guardado",
                    Toast.LENGTH_LONG
                ).show()
                saveMessage(message)

            }else{
                Toast.makeText(
                    applicationContext,
                    "Guarda un mensaje",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

      /*  if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            wifiManager.isWifiEnabled = true

        } else if (!wifiManager.isWifiEnabled) {
            //Wifi activation
            val toast: Toast = Toast.makeText(applicationContext, text, duration)
            toast.show()
        }*/

        binding.discover.setOnClickListener {


            init()
            startRegistration()
            addServiceRequest()
            discoverService()
       }

    }

    fun init(){

        info = WifiFrameUtils.buildMyWiFiFrame( this, WifiP2pDevice())

        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(applicationContext, mainLooper, null)



        val wInfo = wifiManager.connectionInfo
        val macAddress = wInfo.macAddress

       // expandableListAdapter = IngredientAdapter(this, collection)
       // binding.peerListView.setAdapter(expandableListAdapter)
        Log.e(TAG, "MAC Address : $macAddress")

    }


    private fun saveMessage(message: String){
        val editor = sharedPreferences.edit()
        editor.putString(Constants.MESSAGE, message.trim())
        editor.apply()
    }

    private fun updateDescription(wifiP2pDevice: WifiP2pDevice){

        val deviceInfo = "Nombre: ${wifiP2pDevice.deviceName}"
        val deviceMAC = "MAC: ${wifiP2pDevice.deviceAddress}"
        val message =  "Mensaje:"
        //val messageSend = "Mensaje: "
        val formattedDateTime = getFormattedDateTime() // Obtener la hora y la fecha actualizada

        // Actualizar el TextView con la información del dispositivo y la hora/fecha actualizada
         binding.messageTextView.text = "$deviceInfo\n$message Prueba de mensaje\n$deviceMAC\n$formattedDateTime"


    }

    private fun getFormattedDateTime(): String {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        return "Hora del ultimo mensaje: $currentTime\nFecha : $currentDate"
    }

    //Agregando servicio local
    private fun startRegistration() {
        //Guardar el nombre del dispositvo y la MAC
        val record =  WifiFrameUtils.wifiFrameToHashMap(info)


        // Service information.  Pass it an instance name, service type
        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        )) {

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



    //onreceivelocation en BP
    private fun discoverService() {

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { _, record, _ ->
            Log.d(TAG, "DnsSdTxtRecord available -$record")

            if (record.isEmpty()) return@DnsSdTxtRecordListener

            val wifiFrame = WifiFrameUtils.hashMapToWiFiFrame(record)
            Toast.makeText(this, "Servicio encontrado : mensaje ${wifiFrame.sendMessage}" , Toast.LENGTH_SHORT ).show()
        }


        mManager.setDnsSdResponseListeners(mChannel, null, txtListener)
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
}