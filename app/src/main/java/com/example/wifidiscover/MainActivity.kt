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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityMainBinding

    //WiFi Direct variables
    private lateinit var wifiManager: WifiManager
    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: WifiP2pManager.Channel

    private var peers: MutableList<WifiP2pDevice> = mutableListOf()
    private var deviceArray: MutableList<MessageModel> = mutableListOf()


    private val connectedDevices: MutableList<WifiP2pDevice> = mutableListOf()
    private var info: WifiFrame = WifiFrame()
    private var selectedDevice: WifiP2pDevice? = null


    var text: CharSequence = "Activa el wifi"
    var duration = Toast.LENGTH_SHORT


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sharedPreferences = getSharedPreferences(Constants.PREFERENCES_KEY, MODE_PRIVATE)

        init()
        addServiceRequest()
        //startTimer()
        startDiscover()

        exqListener()
        binding.peerListView.setOnItemClickListener { parent, viiew, pos, id ->
            selectedDevice = deviceArray[pos].device
            startTimer()

            // updateDescription(pos)

        }


    }

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var task: Runnable? = null
    private var interval: Long = 10000
    private val devicesWithReceivedMessages: MutableSet<String> = mutableSetOf()

    //Realiza la busqueda de servicios cada x tiempo
    fun startDiscover() {
        // Crea un nuevo Runnable que se ejecutará después de cada intervalo
        task = Runnable {
            discoverServices()
            clearReceivedDevicesAfterDelay()

        }
        // Programa el primer ciclo del temporizador con un retraso inicial de 0 y un intervalo especificado
        executor.scheduleAtFixedRate(task!!, 0, interval, TimeUnit.MILLISECONDS)
    }

    private fun startTimer() {
        executor.scheduleAtFixedRate({

            selectedDevice?.let { device ->
                // Buscar el índice del dispositivo seleccionado en la lista
                val index = deviceArray.indexOfFirst { it.device.deviceName == device.deviceName }
                if (index != -1) {
                    // Llamar a updateDescription con el índice del dispositivo seleccionado
                    if (!devicesWithReceivedMessages.contains(device.deviceName)) {
                        // Si es la primera vez que recibes un mensaje del dispositivo, procésalo
                        runOnUiThread {
                            // Actualiza la descripción o realiza las operaciones necesarias con el dispositivo
                            updateDescription(index)
                        }
                        // Marca el dispositivo como uno del que ya has recibido un mensaje
                        devicesWithReceivedMessages.add(device.deviceName)
                    }
                }
            }
        }, 0, interval, TimeUnit.MILLISECONDS)
    }

    private fun clearReceivedDevicesAfterDelay() {
        // Limpia el conjunto de dispositivos después de cierto tiempo (por ejemplo, 30 segundos)
        executor.schedule({
            devicesWithReceivedMessages.clear()
        }, 500, TimeUnit.MILLISECONDS)
    }

    // Guarda el mensaje y lo envia al momento de oprimir el botón
    fun exqListener() {

        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text.toString()

            if (message.isNotBlank()) {
                Toast.makeText(
                    applicationContext,
                    "Mensaje guardado",
                    Toast.LENGTH_LONG
                ).show()
                saveMessage(message)

                clearLocalServices {
                    startRegistration()
                }

            } else {
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

            clearServiceRequests {
                addServiceRequest()
            }

        }

        binding.refresh.setOnClickListener {
            discoverServices()
        }

    }

    //Configuración para utilizar los servicio de WifiP2p
    fun init() {

        wifiManager = this.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(this, mainLooper, null)

        WifiFrameUtils.getUUIDWiFiFrame(this)


    }

    private fun addDeviceList(record: WifiP2pDevice, wifiFrame: WifiFrame) {

        val deviceSame = deviceArray.firstOrNull { it.device.deviceName == record.deviceName }

        if (deviceSame != null) {
            val messageExist = deviceSame.message.any {
                it.dateSend == wifiFrame.dateSend
            }

            if (!messageExist)
                deviceSame.message.add(wifiFrame)

        } else {
            var message =
                MessageModel(record, mutableListOf(wifiFrame), WifiFrameUtils.deviceIdMultiHop)

            deviceArray.add(message)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            deviceArray

        )
        binding.peerListView.adapter = adapter
        multihop(WifiFrameUtils.deviceIdMultiHop)
    }

    private fun addDeviceMultiHop(record: WifiP2pDevice, wifiFrame: WifiFrame) {

        val deviceSame = deviceArray.firstOrNull { it.device.deviceName == record.deviceName }
        if (deviceSame != null) {
            val messageExist = deviceSame.message.any {
                it.dateSend == wifiFrame.dateSend
            }

            if (!messageExist) {
                Toast.makeText(
                    this,
                    "Se ha agregado el mensaje retransmitido por " + record.deviceName,
                    Toast.LENGTH_SHORT
                ).show()
                deviceSame.message.add(wifiFrame)
            }

        } else {
            var device =
                MessageModel(record, mutableListOf(wifiFrame), WifiFrameUtils.deviceIdMultiHop)

            deviceArray.add(device)
            Toast.makeText(
                this,
                "Se ha agregado el dispositivo retransmitido por " + record.deviceName,
                Toast.LENGTH_SHORT
            ).show()
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            deviceArray

        )
        binding.peerListView.adapter = adapter

    }


    private fun saveMessage(message: String) {
        val editor = sharedPreferences.edit()
        editor.putString(Constants.MESSAGE, message.trim())
        editor.apply()
    }


    private fun updateDescription(selectedDevice: Int) {

        val selectedDeviceInfo = deviceArray[selectedDevice]
        binding.messageTextView.text = selectedDeviceInfo.getAllMessage()

    }

    //Agregando servicio local para ofrecer la información del dispositivo actual a traves del servicio
    private fun startRegistration() {


        info = WifiFrameUtils.buildMyWiFiFrame(this)

        val record = WifiFrameUtils.wifiFrameToHashMap(info)


        // Service information.  Pass it an instance name, service type
        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_networkChat", "_chatApp._tcp", record)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
                    )
        ) {

            Toast.makeText(this, "Faltan pemisos 1", Toast.LENGTH_SHORT).show()
            return
        }
        mManager.addLocalService(mChannel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {

            }

            override fun onFailure(arg0: Int) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(this@MainActivity, "Fail local service", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun multihop(deviceId: String) {

        var deviceMulti = deviceArray.first {
            it.id == deviceId
        }

        val record = WifiFrameUtils.wifiFrameToHashMapMultihop(
            deviceMulti.device,
            deviceMulti.message.last().sendMessage,
            deviceMulti.id,
            deviceMulti.message.last().dateSend
        )


        // Service information.  Pass it an instance name, service type
        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance("_networkChat", "_chatApp._tcp", record)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
                    )
        ) {

            Toast.makeText(this, "Faltan pemisos 1", Toast.LENGTH_SHORT).show()
            return
        }
        mManager.addLocalService(mChannel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {

            }

            override fun onFailure(arg0: Int) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Toast.makeText(this@MainActivity, "Fail local service", Toast.LENGTH_SHORT).show()
            }
        })
    }

    //Configura la solicitud para realizar el descubrimiento e informa si la solictud se realizo correctamente
    private fun addServiceRequest() {
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(
            "_networkChat",
            "_chatApp._tcp"
        )

        mManager.addServiceRequest(
            mChannel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    discoverListener()

                }

                override fun onFailure(code: Int) {
                    Toast.makeText(this@MainActivity, "Failure addService", Toast.LENGTH_SHORT)
                        .show()
                    Log.e(TAG_WIFI, "Add service request has failed. $code")
                }
            }
        )
    }


    //Gestiona la información de los servicios recibidos
    private fun discoverListener() {

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, record, srcDevice ->
            Log.d(TAG, "DnsSdTxtRecord available -$record")
            val wifiFrame = WifiFrameUtils.hashMapToWiFiFrame(record)
            if (record.isEmpty() || srcDevice.deviceName == "" || WifiFrameUtils.deviceIdMultiHop == WifiFrameUtils.idDevice.toString() || wifiFrame.sendMessage.isEmpty()) return@DnsSdTxtRecordListener

            if (WifiFrameUtils.deviceMultihop.isNotEmpty()) {
                val deviceP2p = WifiP2pDevice().apply {
                    deviceName = WifiFrameUtils.deviceMultihop
                }
                wifiFrame.apply {
                    nameMultiHop = srcDevice.deviceName
                }
                addDeviceMultiHop(deviceP2p, wifiFrame)
            } else {
                addDeviceList(srcDevice, wifiFrame)
            }


            //Toast.makeText(this, "Servicio encontrado : mensaje ${wifiFrame.sendMessage}" , Toast.LENGTH_SHORT ).show()
        }

        val servListener =
            WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
                Log.d("chat", "BonjourService available! instanceName: $instanceName")
                Log.d("chat", "BonjourService available! registrationType: $registrationType")
                Log.d("chat", "BonjourService available! resourceType: $resourceType")
            }


        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener)
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

            Toast.makeText(this, "Faltan pemisos 3", Toast.LENGTH_SHORT).show()
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

                        Toast.makeText(this@MainActivity, "Faltan pemisos 7", Toast.LENGTH_SHORT)
                            .show()
                        return
                    }

                    //    mManager.requestPeers(mChannel, this@MainActivity)
                }

                override fun onFailure(error: Int) {
                    Toast.makeText(this@MainActivity, "failure peers", Toast.LENGTH_SHORT).show()
                    Log.e(TAG_WIFI, "Discover peers has failed. $error")
                }
            })
    }


    //Inicia el descubrimiento
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

            Toast.makeText(this, "Faltan pemisos 4", Toast.LENGTH_SHORT).show()
            return
        }
        mManager.discoverServices(
            mChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(this@MainActivity, "success discover", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(code: Int) {
                    Toast.makeText(this@MainActivity, "Failure discover", Toast.LENGTH_SHORT).show()
                    Log.e(TAG_WIFI, "Discover services has failed. $code")
                }
            }
        )
    }

    private fun clearServiceRequests(onSuccessCallback: () -> Unit) {
        mManager?.clearServiceRequests(mChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("success", "clearServiceRequests result: Success")
                    Toast.makeText(this@MainActivity, "Success clear request", Toast.LENGTH_SHORT)
                        .show()
                    onSuccessCallback.invoke()
                }

                override fun onFailure(code: Int) {
                    Log.e("failed", "clearServiceRequests result: Failure with code $code")
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to clear service requests: $code",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun clearLocalServices(onSuccessCallback: () -> Unit) {
        mManager?.clearLocalServices(mChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("success", "clearLocalServices result: Success")
                    Toast.makeText(
                        this@MainActivity,
                        "Success clear local services",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccessCallback.invoke()
                }

                override fun onFailure(code: Int) {
                    Log.e("Failed", "clearLocalServices result:  Failure with code $code")
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to clear local services: $code",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}