package com.example.tesisv3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private val TAG = "MainActivity"
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"
    private val WEAR_DATA_PATH = "/wear/json"
    private val MESSAGE_ITEM_RECEIVED_PATH = "/message-item-received"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var mobileDeviceConnected: Boolean = false
    private var mobileNodeUri: String? = null

    private var connectionManager: ConnectionManager? = null
    private var heartRateListener: HeartRateListener? = null
    private var spO2Listener: SpO2Listener? = null
    private var ecgListener: EcgListener? = null
    private var connected = false
    private var permissionGranted = false

    private val trackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {
            runOnUiThread {
                if (hrData.status == HeartRateStatus.HR_STATUS_FIND_HR) {
                    val hrValue = hrData.hr
                    binding.txtHeartRate.text = hrValue.toString()
                    binding.messagelogTextView.append("\nHR: $hrValue bpm")
                    sendSensorDataToPhone("heart_rate", hrValue)
                } else {
                    binding.txtHeartRate.text = "--"
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

    private val connectionObserver = object : ConnectionObserver {
        override fun onConnectionResult(stringResourceId: Int) {
            runOnUiThread {
                Toast.makeText(applicationContext, getString(stringResourceId), Toast.LENGTH_SHORT).show()
            }
            if (stringResourceId != R.string.ConnectedToHs) {
                return
            }

            connected = true
            heartRateListener = HeartRateListener(applicationContext)
            connectionManager?.initHeartRate(heartRateListener)
            heartRateListener?.startTracker()

            spO2Listener = SpO2Listener()
            connectionManager?.initSpO2(spO2Listener)

            ecgListener = EcgListener()
            connectionManager?.initEcg(ecgListener)

            TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
            discoverAndNotifyMobileDevice()
        }

        override fun onError(e: HealthTrackerException) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Health error", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityContext = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ambientController = AmbientModeSupport.attach(this)

        binding.startSpo2Button.setOnClickListener {
            if (isPermissionsOrConnectionInvalid()) return@setOnClickListener
            spO2Listener?.let {
                it.startTracker()
                binding.startSpo2Button.text = "..."
                binding.messagelogTextView.append("\nStarting SpO2...")
            }
        }

        binding.startEcgButton.setOnClickListener {
            if (isPermissionsOrConnectionInvalid()) return@setOnClickListener
            ecgListener?.let {
                it.startTracker()
                binding.startEcgButton.text = "..."
                binding.messagelogTextView.append("\nStarting ECG...")
            }
        }

        binding.sendmessageButton.setOnClickListener {
            val messageText = binding.messagecontentEditText.text.toString()
            if (messageText.isNotEmpty()) {
                sendManualMessageToPhone(messageText)
                binding.messagecontentEditText.setText("")
            }
        }

        createConnectionManager()
        TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
        
        if (!isPermissionGranted()) {
            requestPermissions()
        }
    }

    private fun isPermissionsOrConnectionInvalid(): Boolean {
        if (!isPermissionGranted()) {
            requestPermissions()
            return true
        }
        if (!connected) {
            Toast.makeText(applicationContext, "Not connected to HS", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    private fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            (ActivityCompat.checkSelfPermission(this, HealthPermissions.READ_HEART_RATE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, HealthPermissions.READ_OXYGEN_SATURATION) == PackageManager.PERMISSION_GRANTED)
        } else {
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityCompat.requestPermissions(this, arrayOf(HealthPermissions.READ_HEART_RATE, HealthPermissions.READ_OXYGEN_SATURATION), 0)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            permissionGranted = true
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionGranted = false
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    break
                }
            }
            if (permissionGranted) {
                createConnectionManager()
            }
        }
    }

    private fun createConnectionManager() {
        try {
            connectionManager = ConnectionManager(connectionObserver)
            connectionManager?.connect(applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, t.message ?: "Error")
        }
    }

    override fun onResume() {
        super.onResume()
        permissionGranted = isPermissionGranted()
        try {
            Wearable.getDataClient(this).addListener(this)
            Wearable.getMessageClient(this).addListener(this)
            Wearable.getCapabilityClient(this).addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
            discoverAndNotifyMobileDevice()
        } catch (e: Exception) { e.printStackTrace() }
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

    private fun discoverAndNotifyMobileDevice() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                mobileNodeUri = node.id
                mobileDeviceConnected = true
                val payload = wearableAppCheckPayloadReturnACK.toByteArray()
                Wearable.getMessageClient(this).sendMessage(node.id, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
                runOnUiThread { binding.deviceconnectionStatusTv.text = "Connected: ${node.displayName}" }
                break
            }
        }
    }

    private fun sendSensorDataToPhone(dataType: String, value: Any) {
        if (!mobileDeviceConnected || mobileNodeUri == null) return
        val data = mapOf("type" to dataType, "value" to value.toString(), "timestamp" to System.currentTimeMillis())
        val json = Gson().toJson(data)
        Wearable.getMessageClient(this).sendMessage(mobileNodeUri!!, WEAR_DATA_PATH, json.toByteArray(StandardCharsets.UTF_8))
    }

    private fun sendManualMessageToPhone(text: String) {
        if (!mobileDeviceConnected || mobileNodeUri == null) return
        val payload = text.toByteArray(StandardCharsets.UTF_8)
        Wearable.getMessageClient(this).sendMessage(mobileNodeUri!!, MESSAGE_ITEM_RECEIVED_PATH, payload)
            .addOnSuccessListener { binding.messagelogTextView.append("\nSent manual: $text") }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
            mobileNodeUri = event.sourceNodeId
            mobileDeviceConnected = true
            val payload = wearableAppCheckPayloadReturnACK.toByteArray()
            Wearable.getMessageClient(this).sendMessage(event.sourceNodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
            runOnUiThread { binding.deviceconnectionStatusTv.text = "Mobile connected" }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) { discoverAndNotifyMobileDevice() }
    override fun onDataChanged(p0: DataEventBuffer) {}
    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback()
    private inner class MyAmbientCallback : AmbientModeSupport.AmbientCallback()
}
