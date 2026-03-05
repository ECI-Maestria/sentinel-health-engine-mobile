package com.example.tesisv3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.wear.ambient.AmbientModeSupport
import com.example.tesisv3.databinding.ActivityMainBinding
import com.example.tesisv3.device.listeners.EcgListener
import com.example.tesisv3.device.listeners.HeartRateListener
import com.example.tesisv3.device.listeners.SpO2Listener
import com.example.tesisv3.device.managers.ConnectionManager
import com.example.tesisv3.device.managers.ConnectionObserver
import com.example.tesisv3.domain.entities.*
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import com.samsung.android.service.health.tracking.HealthTrackerException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider,
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var activityContext: Context? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var ambientController: AmbientModeSupport.AmbientController

    // --- Variables de Comunicación ---
    private val TAG = "MainActivityWear"
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"
    private val WEAR_DATA_PATH = "/wear/json"
    private val MESSAGE_ITEM_RECEIVED_PATH = "/message-item-received"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var mobileDeviceConnected: Boolean = false
    private var mobileNodeUri: String? = null

    // --- Variables de Sensores ---
    private val permissionGranted = AtomicBoolean(false)
    private var connectionManager: ConnectionManager? = null
    private var heartRateListener: HeartRateListener? = null
    private var spO2Listener: SpO2Listener? = null
    private var ecgListener: EcgListener? = null

    // ----------------------------------------------------------------------------------
    //  OBSERVER DE SENSORES
    // ----------------------------------------------------------------------------------
    private val trackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {
            runOnUiThread {
                if (hrData.status == HeartRateStatus.HR_STATUS_FIND_HR) {
                    val hrValue = hrData.hr
                    binding.messagelogTextView.append("\nHR: $hrValue bpm")
                    sendSensorDataToPhone("heart_rate", hrValue)
                }
            }
        }

        override fun onSpO2TrackerDataChanged(status: Int, spO2Value: Int) {
            runOnUiThread {
                if (status == SpO2Status.MEASUREMENT_COMPLETED) {
                    binding.messagelogTextView.append("\nSpO2: $spO2Value %")
                    sendSensorDataToPhone("spo2", spO2Value)
                    binding.startSpo2Button.text = "SpO2"
                }
            }
        }

        override fun onEcgTrackerDataChanged(ecgData: EcgData) {
            runOnUiThread {
                if (!ecgData.isLeadOff) {
                    val ecgValue = ecgData.avgEcg
                    binding.messagelogTextView.append(String.format(Locale.US, "\nECG: %.1f µV", ecgValue * 1000))
                    sendSensorDataToPhone("ecg", ecgValue)
                } else {
                    binding.messagelogTextView.append("\nECG: Lead Off")
                }
                binding.startEcgButton.text = "ECG"
            }
        }

        override fun onError(errorResourceId: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, getString(errorResourceId), Toast.LENGTH_LONG).show()
            }
        }
    }

    // ----------------------------------------------------------------------------------
    //  OBSERVER DE CONEXIÓN A SAMSUNG HEALTH SDK
    // ----------------------------------------------------------------------------------
    private val connectionObserver = object : ConnectionObserver {
        override fun onConnectionResult(stringResourceId: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, getString(stringResourceId), Toast.LENGTH_SHORT).show()
            }
            if (stringResourceId != R.string.ConnectedToHs) {
                finish()
                return
            }

            heartRateListener = HeartRateListener(applicationContext)
            connectionManager?.initHeartRate(heartRateListener)
            heartRateListener?.startTracker()

            spO2Listener = SpO2Listener()
            connectionManager?.initSpO2(spO2Listener)

            ecgListener = EcgListener()
            connectionManager?.initEcg(ecgListener)

            TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
            
            // Notificar al teléfono proactivamente al conectar los sensores
            discoverAndNotifyMobileDevice()
        }

        override fun onError(e: HealthTrackerException) {
            runOnUiThread {
                Toast.makeText(applicationContext, getString(R.string.ConnectionError), Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    // ==================================================================================
    //  LIFECYCLE METHODS
    // ==================================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityContext = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ambientController = AmbientModeSupport.attach(this)

        // Listeners de sensores
        binding.startSpo2Button.setOnClickListener {
            spO2Listener?.let {
                it.startTracker()
                binding.startSpo2Button.text = "..."
                binding.messagelogTextView.append("\nIniciando medición SpO2...")
            }
        }

        binding.startEcgButton.setOnClickListener {
            ecgListener?.let {
                it.startTracker()
                binding.startEcgButton.text = "..."
                binding.messagelogTextView.append("\nIniciando medición ECG...")
            }
        }

        // Listener manual
        binding.sendmessageButton.setOnClickListener {
            val messageText = binding.messagecontentEditText.text.toString()
            if (messageText.isNotEmpty()) {
                sendManualMessageToPhone(messageText)
                binding.messagecontentEditText.setText("")
            } else {
                Toast.makeText(this, "Message is empty", Toast.LENGTH_SHORT).show()
            }
        }

        if (!isPermissionGranted()) {
            requestPermissions()
        } else {
            createConnectionManager()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(this).addListener(this)
            Wearable.getMessageClient(this).addListener(this)
            Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
            
            discoverAndNotifyMobileDevice()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(this).removeListener(this)
            Wearable.getMessageClient(this).removeListener(this)
            Wearable.getCapabilityClient(this).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateListener?.stopTracker()
        spO2Listener?.stopTracker()
        ecgListener?.stopTracker()
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver)
        connectionManager?.disconnect()
    }

    // ==================================================================================
    //  COMUNICACIÓN
    // ==================================================================================

    private fun discoverAndNotifyMobileDevice() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                mobileNodeUri = node.id
                mobileDeviceConnected = true
                
                // Enviar señal de vida al teléfono
                val payload = wearableAppCheckPayloadReturnACK.toByteArray()
                Wearable.getMessageClient(this).sendMessage(node.id, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
                
                runOnUiThread {
                    binding.deviceconnectionStatusTv.text = "Mobile device connected: ${node.displayName}"
                }
                break
            }
        }
    }

    private fun sendSensorDataToPhone(dataType: String, value: Any) {
        if (!mobileDeviceConnected || mobileNodeUri == null) return

        val data = mapOf(
            "type" to dataType,
            "value" to value.toString(),
            "timestamp" to System.currentTimeMillis()
        )
        val json = Gson().toJson(data)

        Wearable.getMessageClient(this)
            .sendMessage(mobileNodeUri!!, WEAR_DATA_PATH, json.toByteArray(StandardCharsets.UTF_8))
            .addOnSuccessListener { Log.d(TAG, "Sent $dataType: $json") }
    }

    private fun sendManualMessageToPhone(text: String) {
        if (!mobileDeviceConnected || mobileNodeUri == null) {
            Toast.makeText(this, "Phone not connected", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = text.toByteArray(StandardCharsets.UTF_8)
        Wearable.getMessageClient(this)
            .sendMessage(mobileNodeUri!!, MESSAGE_ITEM_RECEIVED_PATH, payload)
            .addOnSuccessListener {
                binding.messagelogTextView.append("\nSent manual: $text")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
            mobileNodeUri = event.sourceNodeId
            mobileDeviceConnected = true
            
            // Responder al ACK del teléfono
            val payload = wearableAppCheckPayloadReturnACK.toByteArray()
            Wearable.getMessageClient(this).sendMessage(event.sourceNodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
            
            runOnUiThread {
                binding.deviceconnectionStatusTv.text = "Mobile device connected"
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        discoverAndNotifyMobileDevice()
    }

    override fun onDataChanged(p0: DataEventBuffer) {}

    // --- Otros métodos (Permisos, etc.) sin cambios ---
    private fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createConnectionManager()
        } else finish()
    }

    private fun createConnectionManager() {
        try {
            connectionManager = ConnectionManager(connectionObserver)
            connectionManager?.connect(this)
        } catch (t: Throwable) {
            Log.e(TAG, "ConnectionManager error", t)
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback()
    private inner class MyAmbientCallback : AmbientModeSupport.AmbientCallback()
}
