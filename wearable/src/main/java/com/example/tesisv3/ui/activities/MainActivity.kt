package com.example.tesisv3.ui.activities

import com.example.tesisv3.R
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.example.tesisv3.domain.entities.EcgData
import com.example.tesisv3.domain.entities.HeartRateData
import com.example.tesisv3.domain.entities.HeartRateStatus
import com.example.tesisv3.domain.entities.SpO2Status
import com.example.tesisv3.domain.entities.TrackerDataNotifier
import com.example.tesisv3.domain.entities.TrackerDataObserver
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.samsung.android.service.health.tracking.HealthTrackerException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider,
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var activityContext: Context? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var ambientController: AmbientModeSupport.AmbientController

    private val readAdditionalHealthDataPermission =
        "com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA"

    private val tag = "MainActivity"
    private val appOpenWearablePayloadPath = "/APP_OPEN_WEARABLE_PAYLOAD"
    private val wearDataPath = "/wear/json"
    //private val messageItemReceivedPath = "/wear/json"
    private val messageItemReceivedPath = "/message-item-received"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"

    private var mobileDeviceConnected = false
    private var mobileNodeUri: String? = null

    private var connectionManager: ConnectionManager? = null
    private var heartRateListener: HeartRateListener? = null
    private var spO2Listener: SpO2Listener? = null
    private var ecgListener: EcgListener? = null

    private var connected = false
    private var permissionGranted = false
    private var trackerObserverRegistered = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stopAllSensorsRunnable: Runnable? = null
    private var switchToHeartRateRunnable: Runnable? = null
    private var isAllSensorsRunning = false
    private var lastHeartRateValue: Int? = null
    private var lastSpO2Value: Int? = null

    private val trackerDataObserver = object : TrackerDataObserver {
        override fun onHeartRateTrackerDataChanged(hrData: HeartRateData) {
            runOnUiThread {
                if (hrData.status == HeartRateStatus.HR_STATUS_FIND_HR) {
                    val hrValue = hrData.hr
                    lastHeartRateValue = hrValue
                    binding.txtHeartRate.text = hrValue.toString()
                    binding.messagelogTextView.append("\nHR: $hrValue bpm")
                    if (hrValue == 0) {
                        binding.messagelogTextView.append("\nHR 0 omitido")
                    } else if (isAllSensorsRunning) {
                        sendAggregatedDataToPhone()
                    }
                } else {
                    binding.txtHeartRate.text = "--"
                }
            }
        }

        override fun onSpO2TrackerDataChanged(status: Int, spO2Value: Int) {
            runOnUiThread {
                if (status == SpO2Status.MEASUREMENT_COMPLETED) {
                    lastSpO2Value = spO2Value
                    binding.txtSpO2.text = spO2Value.toString()
                    binding.messagelogTextView.append("\nSpO2: $spO2Value %")
                    binding.startSpo2Button.text = "SpO2"
                    binding.spo2ProgressBar.visibility = android.view.View.GONE
                }
            }
        }

        override fun onEcgTrackerDataChanged(ecgData: EcgData) {
            runOnUiThread {
                if (!ecgData.isLeadOff) {
                    val ecgValue = ecgData.avgEcg
                    val formattedEcg = String.format(Locale.US, "%.1f", ecgValue * 1000)
                    binding.txtEcg.text = formattedEcg
                    binding.messagelogTextView.append("\nECG: $formattedEcg µV")
                } else {
                    binding.txtEcg.text = "Off"
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

            spO2Listener = SpO2Listener()
            connectionManager?.initSpO2(spO2Listener)

            ecgListener = EcgListener()
            connectionManager?.initEcg(ecgListener)

            discoverAndNotifyMobileDevice()
        }

        override fun onError(e: HealthTrackerException) {
            if (
                e.errorCode == HealthTrackerException.OLD_PLATFORM_VERSION ||
                e.errorCode == HealthTrackerException.PACKAGE_NOT_INSTALLED
            ) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.HealthPlatformVersionIsOutdated),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            if (e.hasResolution()) {
                e.resolve(this@MainActivity)
            } else {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.ConnectionError),
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.e(tag, "Could not connect to Health Tracking Service: ${e.message}", e)
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

        permissionGranted = isPermissionGranted()
        registerTrackerObserver()

        binding.startSpo2Button.setOnClickListener {
            if (isPermissionsOrConnectionInvalid()) return@setOnClickListener

            spO2Listener?.let {
                it.startTracker()
                binding.txtSpO2.text = "..."
                binding.startSpo2Button.text = "..."
                binding.messagelogTextView.append("\nStarting SpO2...")
                binding.spo2ProgressBar.visibility = android.view.View.VISIBLE
            }
        }

        binding.startEcgButton.setOnClickListener {
            if (isPermissionsOrConnectionInvalid()) return@setOnClickListener

            ecgListener?.let {
                it.startTracker()
                binding.txtEcg.text = "..."
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

        binding.sendFinalMessageButton.setOnClickListener {
            sendFinalFixedMessageToPhone()
        }

        binding.startAllSensorsButton.setOnClickListener {
            startAllSensorsForOneMinute()
        }

        if (!permissionGranted) {
            requestPermissionsCompat()
        } else {
            createConnectionManager()
        }
    }

    private fun isPermissionsOrConnectionInvalid(): Boolean {
        if (!isPermissionGranted()) {
            permissionGranted = false
            requestPermissionsCompat()
            return true
        }

        permissionGranted = true

        if (!connected) {
            Toast.makeText(applicationContext, getString(R.string.ConnectionError), Toast.LENGTH_SHORT).show()
            return true
        }

        return false
    }

    private fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityCompat.checkSelfPermission(this, HealthPermissions.READ_HEART_RATE) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, HealthPermissions.READ_OXYGEN_SATURATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, readAdditionalHealthDataPermission) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun requestPermissionsCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.READ_OXYGEN_SATURATION,
                    readAdditionalHealthDataPermission
                ),
                0
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                0
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            permissionGranted = true

            for (result in grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    permissionGranted = false
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    break
                }
            }

            if (permissionGranted) {
                createConnectionManager()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun createConnectionManager() {
        if (connectionManager != null) return

        try {
            connectionManager = ConnectionManager(connectionObserver)
            connectionManager?.connect(applicationContext)
        } catch (t: Throwable) {
            Log.e(tag, t.message ?: "Error", t)
        }
    }

    override fun onResume() {
        super.onResume()
        permissionGranted = isPermissionGranted()

        if (permissionGranted) {
            createConnectionManager()
        }

        try {
            Wearable.getDataClient(this).addListener(this)
            Wearable.getMessageClient(this).addListener(this)
            Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
            discoverAndNotifyMobileDevice()
        } catch (e: Exception) {
            Log.e(tag, "Wear listener error", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(this).removeListener(this)
            Wearable.getMessageClient(this).removeListener(this)
            Wearable.getCapabilityClient(this).removeListener(this)
        } catch (e: Exception) {
            Log.e(tag, "Wear remove listener error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllSensorsRunnable?.let { mainHandler.removeCallbacks(it) }
        switchToHeartRateRunnable?.let { mainHandler.removeCallbacks(it) }
        heartRateListener?.stopTracker()
        spO2Listener?.stopTracker()
        ecgListener?.stopTracker()
        unregisterTrackerObserver()
        connectionManager?.disconnect()
        connectionManager = null
    }

    private fun discoverAndNotifyMobileDevice() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                mobileNodeUri = node.id
                mobileDeviceConnected = true
                val payload = wearableAppCheckPayloadReturnACK.toByteArray()
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, appOpenWearablePayloadPath, payload)
                runOnUiThread {
                    binding.deviceconnectionStatusTv.text = "Connected: ${node.displayName}"
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
            .sendMessage(mobileNodeUri!!, wearDataPath, json.toByteArray(StandardCharsets.UTF_8))
    }

    private fun sendAggregatedDataToPhone() {
        if (!mobileDeviceConnected || mobileNodeUri == null) return

        val hr = lastHeartRateValue ?: 0
        if (hr == 0) {
            binding.messagelogTextView.append("\nHR 0 omitido: no se envía agregado")
            return
        }

        val spo2 = lastSpO2Value ?: 0
        if (spo2 == 0) {
            binding.messagelogTextView.append("\nSpO2 0 omitido: no se envía agregado")
            return
        }
        val timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        val json = "{ \"deviceId\": \"UUID\", \"heartRate\": $hr, \"spO2\": $spo2, \"timestamp\": \"${timestamp}\" }"

        Wearable.getMessageClient(this)
            .sendMessage(mobileNodeUri!!, wearDataPath, json.toByteArray(StandardCharsets.UTF_8))
            .addOnSuccessListener {
                binding.messagelogTextView.append("\nEnviado agregado")
            }
    }

    private fun sendFinalFixedMessageToPhone() {
        if (!mobileDeviceConnected || mobileNodeUri == null) return

        val timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        val json = "{ \"deviceId\": \"mobile-gateway-01\", \"heartRate\": 145, \"spO2\": 88, \"timestamp\": \"${timestamp}\" }"

        Wearable.getMessageClient(this)
            .sendMessage(mobileNodeUri!!, wearDataPath, json.toByteArray(StandardCharsets.UTF_8))
            .addOnSuccessListener {
                binding.messagelogTextView.append("\nEnviado final fijo")
            }
    }

    private fun sendManualMessageToPhone(text: String) {
        if (!mobileDeviceConnected || mobileNodeUri == null) return

        val payload = text.toByteArray(StandardCharsets.UTF_8)
        Wearable.getMessageClient(this)
            .sendMessage(mobileNodeUri!!, messageItemReceivedPath, payload)
            .addOnSuccessListener {
                binding.messagelogTextView.append("\nSent manual: $text")
            }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == appOpenWearablePayloadPath) {
            mobileNodeUri = event.sourceNodeId
            mobileDeviceConnected = true
            val payload = wearableAppCheckPayloadReturnACK.toByteArray()

            Wearable.getMessageClient(this)
                .sendMessage(event.sourceNodeId, appOpenWearablePayloadPath, payload)

            runOnUiThread {
                binding.deviceconnectionStatusTv.text = "Mobile connected"
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        discoverAndNotifyMobileDevice()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) = Unit

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback()

    private inner class MyAmbientCallback : AmbientModeSupport.AmbientCallback()

    private fun registerTrackerObserver() {
        if (!trackerObserverRegistered) {
            TrackerDataNotifier.getInstance().addObserver(trackerDataObserver)
            trackerObserverRegistered = true
        }
    }

    private fun unregisterTrackerObserver() {
        if (trackerObserverRegistered) {
            TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver)
            trackerObserverRegistered = false
        }
    }

    private fun startAllSensorsForOneMinute() {
        if (isPermissionsOrConnectionInvalid()) return

        if (isAllSensorsRunning) {
            stopAllSensorsRunnable?.let { mainHandler.removeCallbacks(it) }
            switchToHeartRateRunnable?.let { mainHandler.removeCallbacks(it) }
        }

        binding.messagelogTextView.append("\nIniciando todos los sensores (1 min)...")
        binding.startAllSensorsButton.text = "Midiendo..."
        isAllSensorsRunning = true
        lastHeartRateValue = null
        lastSpO2Value = null

        spO2Listener?.startTracker()
        binding.spo2ProgressBar.visibility = android.view.View.VISIBLE

        switchToHeartRateRunnable = Runnable {
            spO2Listener?.stopTracker()
            binding.spo2ProgressBar.visibility = android.view.View.GONE
            binding.messagelogTextView.append("\nIniciando Heart Rate...")
            heartRateListener?.startTracker()
        }
        mainHandler.postDelayed(switchToHeartRateRunnable!!, 15_000L)

        stopAllSensorsRunnable = Runnable {
            heartRateListener?.stopTracker()
            spO2Listener?.stopTracker()
            binding.messagelogTextView.append("\nSensores detenidos")
            binding.startAllSensorsButton.text = getString(R.string.start_all_sensors)
            isAllSensorsRunning = false
            binding.spo2ProgressBar.visibility = android.view.View.GONE
        }
        mainHandler.postDelayed(stopAllSensorsRunnable!!, 60_000L)
    }
}
