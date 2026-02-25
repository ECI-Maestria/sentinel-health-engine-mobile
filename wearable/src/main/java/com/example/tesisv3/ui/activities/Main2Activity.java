/*
 * Copyright 2022 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.tesisv3.ui.activities;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.tesisv3.databinding.ActivityMain2Binding;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;
import com.samsung.android.service.health.tracking.HealthTrackerException;
import com.example.tesisv3.device.listeners.HeartRateListener;
import com.example.tesisv3.domain.entities.EcgData;
import com.example.tesisv3.device.listeners.SpO2Listener;
import com.example.tesisv3.device.managers.ConnectionManager;
import com.example.tesisv3.device.managers.ConnectionObserver;
import com.example.tesisv3.domain.entities.HeartRateData;
import com.example.tesisv3.domain.entities.HeartRateStatus;
import com.example.tesisv3.domain.entities.SpO2Status;
import com.example.tesisv3.device.listeners.EcgListener;
import com.example.tesisv3.domain.entities.TrackerDataNotifier;
import com.example.tesisv3.domain.entities.TrackerDataObserver;
import com.example.tesisv3.domain.entities.WearData;
import com.example.tesisv3.R;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main2Activity extends Activity {

    private final static String APP_TAG = "MainActivity";
    private final static int MEASUREMENT_DURATION = 35000;
    private final static Long MEASUREMENT_TICK = 250L;

    private final AtomicBoolean isMeasurementRunning = new AtomicBoolean(false);
    Thread uiUpdateThread = null;
    private ConnectionManager connectionManager;
    private HeartRateListener heartRateListener = null;
    private SpO2Listener spO2Listener = null;
    private EcgListener ecgListener = null;
    private boolean connected = false;
    private boolean permissionGranted = false;
    private int previousStatus = SpO2Status.INITIAL_STATUS;
    private HeartRateData heartRateDataLast = new HeartRateData();
    private TextView txtHeartRate;
    private TextView txtStatus;
    private TextView txtSpo2;
    private TextView txtEcg;
    private EcgData ecgDataLast = new EcgData();

    private Button butStart;
    private CircularProgressIndicator measurementProgress = null;

    private Button butEcgStart;
    private LinearProgressIndicator ecgProgress;
    private final AtomicBoolean isEcgRunning = new AtomicBoolean(false);
    private static final int ECG_DURATION = 30000;
    final CountDownTimer countDownTimer = new CountDownTimer(MEASUREMENT_DURATION, MEASUREMENT_TICK) {
        @Override
        public void onTick(long timeLeft) {
            if (isMeasurementRunning.get()) {
                runOnUiThread(() ->
                        measurementProgress.setProgress(measurementProgress.getProgress() + 1, true));
            } else
                cancel();
        }

        @Override
        public void onFinish() {
            if (!isMeasurementRunning.get())
                return;
            Log.i(APP_TAG, "Failed measurement");
            spO2Listener.stopTracker();
            isMeasurementRunning.set(false);
            runOnUiThread(() ->
            {
                txtStatus.setText(R.string.MeasurementFailed);
                txtStatus.invalidate();
                txtSpo2.setText(R.string.SpO2DefaultValue);
                txtSpo2.invalidate();
                butStart.setText(R.string.StartLabel);
                measurementProgress.setProgress(measurementProgress.getMax(), true);
                measurementProgress.invalidate();
            });
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    };
    final TrackerDataObserver trackerDataObserver = new TrackerDataObserver() {
        @Override
        public void onHeartRateTrackerDataChanged(HeartRateData hrData) {
            Main2Activity.this.runOnUiThread(() -> {
                heartRateDataLast = hrData;
                Log.i(APP_TAG, "HR Status: " + hrData.getStatus());
                if (hrData.getStatus() == HeartRateStatus.HR_STATUS_FIND_HR) {
                    txtHeartRate.setText(String.valueOf(hrData.getHr()));
                    Log.i(APP_TAG, "HR: " + hrData.getHr());
                    enviarDatosAlTelefono(Main2Activity.this);
                    sendDataToPhone("{ \"test\": \"hola\" }");
                    sendData(Main2Activity.this, "{ \"test\": \"hola\" }");
                } else {
                    txtHeartRate.setText(getString(R.string.HeartRateDefaultValue));

                }
            });
        }

        @Override
        public void onSpO2TrackerDataChanged(int status, int spO2Value) {
            if(status == previousStatus) {
                return;
            }
            previousStatus = status;
            switch (status) {
                case SpO2Status.CALCULATING:
                    Log.i(APP_TAG, "Calculating measurement");
                    runOnUiThread(() -> {
                                txtStatus.setText(R.string.StatusCalculating);
                                txtStatus.invalidate();
                            }
                    );
                    break;
                case SpO2Status.DEVICE_MOVING:
                    Log.i(APP_TAG, "Device is moving");
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), R.string.StatusDeviceMoving, Toast.LENGTH_SHORT).show());
                    break;
                case SpO2Status.LOW_SIGNAL:
                    Log.i(APP_TAG, "Low signal quality");
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), R.string.StatusLowSignal, Toast.LENGTH_SHORT).show());
                    break;
                case SpO2Status.MEASUREMENT_COMPLETED:
                    Log.i(APP_TAG, "Measurement completed");
                    isMeasurementRunning.set(false);
                    spO2Listener.stopTracker();
                    runOnUiThread(() -> {
                        txtStatus.setText(R.string.StatusCompleted);
                        txtStatus.invalidate();
                        txtSpo2.setText(String.valueOf(spO2Value));
                        txtSpo2.invalidate();
                        butStart.setText(R.string.StartLabel);
                        measurementProgress.setProgress(measurementProgress.getMax(), true);
                    });
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
            }
        }

        @Override
        public void onError(int errorResourceId) {
            runOnUiThread(() ->
                    Toast.makeText(getApplicationContext(), getString(errorResourceId), Toast.LENGTH_LONG).show());
            countDownTimer.onFinish();
        }

        @Override
        public void onEcgTrackerDataChanged(EcgData ecgData) {
            Main2Activity.this.runOnUiThread(() -> {
                ecgDataLast = ecgData;

                Log.i(APP_TAG, "ECG recibido → avg: " + ecgData.getAvgEcg() + " mV | leadOff: " + ecgData.isLeadOff());

                if (ecgData.isLeadOff()) {
                    txtEcg.setText(R.string.UnknownError);
                    txtEcg.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    String ecgText = String.format("%.2f mV", ecgData.getAvgEcg());
                    txtEcg.setText(ecgText);
                    txtEcg.setTextColor(getResources().getColor(android.R.color.white));

                    txtEcg.setText(String.format("%.1f µV", ecgData.getAvgEcg() * 1000));
                }

                txtEcg.invalidate();
            });
        }

        public String buildJson(HeartRateData hrData) {
            WearData data = new WearData(
                    "heart_rate",
                    hrData.getHr(),
                    System.currentTimeMillis()
            );
            return new Gson().toJson(data);
        }

        public  void enviarDatosAlTelefono(Context context) {

            DataClient dataClient = Wearable.getDataClient(context);

            // 1. Crear la solicitud con una ruta única
            PutDataMapRequest putDataMapReq =
                    PutDataMapRequest.create("/path_datos");

            // 2. Agregar los datos
            putDataMapReq.getDataMap()
                    .putString("key_mensaje", "¡Hola desde el reloj!");
            putDataMapReq.getDataMap()
                    .putLong("timestamp", System.currentTimeMillis());

            // 3. Convertir y enviar
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent(); // Opcional: acelera la entrega

            dataClient.putDataItem(putDataReq)
                    .addOnSuccessListener(
                            dataItem -> System.out.println("Datos sincronizados con éxito")
                    )
                    .addOnFailureListener(
                            e -> System.out.println("Error al enviar: " + e.getMessage())
                    );
        }

        public void sendData(Context context, String json) {
            Log.d(APP_TAG,"JESUS");
            Wearable.getNodeClient(context)
                    .getConnectedNodes()
                    .addOnSuccessListener(nodes -> {
                        Log.d(APP_TAG, "Nodos conectados: " + nodes.size());
                        for (Node node : nodes) {
                            Log.d(APP_TAG, "Nodo: " + node.getDisplayName() + " / " + node.getId());
                        }
                    });
            Wearable.getNodeClient(context)
                    .getConnectedNodes()
                    .addOnSuccessListener(nodes -> {
                        for (Node node : nodes) {
                            Wearable.getMessageClient(context)
                                    .sendMessage(
                                            node.getId(),
                                            "/wear/json",
                                            json.getBytes(StandardCharsets.UTF_8)
                                    );
                        }
                    });
        }

        // 1. Define el nombre de la capacidad (debe ser el mismo en el teléfono)
        private static final String CAPABILITY_PHONE_APP = "verify_remote_android_phone_app";
        private static final String WEAR_PATH = "/wear/json";

        public void sendDataToPhone(String json) {
            Log.d(APP_TAG,"POKEMON");
            // Usamos CapabilityClient para encontrar solo dispositivos que tengan nuestra app instalada
            Wearable.getCapabilityClient(Main2Activity.this)
                    .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
                    .addOnSuccessListener(capabilityInfo -> {
                        for (Node node : capabilityInfo.getNodes()) {
                            Wearable.getMessageClient(Main2Activity.this)
                                    .sendMessage(node.getId(), WEAR_PATH, json.getBytes(StandardCharsets.UTF_8))
                                    .addOnSuccessListener(requestId -> Log.d(APP_TAG, "Mensaje enviado con éxito"))
                                    .addOnFailureListener(e -> Log.e(APP_TAG, "Error al enviar: " + e.getMessage()));
                        }
                    });
        }


    };
    private final ConnectionObserver connectionObserver = new ConnectionObserver() {
        @Override
        public void onConnectionResult(int stringResourceId) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(stringResourceId)
                    , Toast.LENGTH_LONG).show());

            if (stringResourceId != R.string.ConnectedToHs) {
                finish();
            }

            connected = true;
            TrackerDataNotifier.getInstance().addObserver(trackerDataObserver);

            spO2Listener = new SpO2Listener();
            heartRateListener = new HeartRateListener(getApplicationContext());
            ecgListener = new EcgListener();

            connectionManager.initSpO2(spO2Listener);
            connectionManager.initEcg(ecgListener);
            connectionManager.initHeartRate(heartRateListener);
            heartRateListener.startTracker();

            connected = true;
            TrackerDataNotifier.getInstance().addObserver(trackerDataObserver);


        }

        @Override
        public void onError(HealthTrackerException e) {
            if (e.getErrorCode() == HealthTrackerException.OLD_PLATFORM_VERSION || e.getErrorCode() == HealthTrackerException.PACKAGE_NOT_INSTALLED)
                runOnUiThread(() -> Toast.makeText(getApplicationContext()
                        , getString(R.string.HealthPlatformVersionIsOutdated), Toast.LENGTH_LONG).show());
            if (e.hasResolution()) {
                e.resolve(Main2Activity.this);
            } else {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(R.string.ConnectionError)
                        , Toast.LENGTH_LONG).show());
                Log.e(APP_TAG, "Could not connect to Health Tracking Service: " + e.getMessage());
            }
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActivityMain2Binding binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        butEcgStart = binding.butEcgStart;
        txtHeartRate = binding.txtHeartRate;
        txtStatus = binding.txtStatus;
        txtSpo2 = binding.txtSpO2;
        txtEcg = binding.txtEcg;
        butStart = binding.butStart;
        ecgProgress = binding.ecgProgress;
        measurementProgress = binding.progressBar;
        adjustProgressBar(measurementProgress);
        createConnectionManager();
        TrackerDataNotifier.getInstance().addObserver(trackerDataObserver);
        if (!isPermissionGranted()) {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionGranted = isPermissionGranted();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (heartRateListener != null)
            heartRateListener.stopTracker();
        if (spO2Listener != null)
            spO2Listener.stopTracker();
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver);
        if (connectionManager != null) {
            connectionManager.disconnect();
        }
    }

    void createConnectionManager() {
        try {
            connectionManager = new ConnectionManager(connectionObserver);
            connectionManager.connect(getApplicationContext());

        } catch (Throwable t) {
            Log.e(APP_TAG, t.getMessage());
        }
    }

    void adjustProgressBar(CircularProgressIndicator progressBar) {
        final DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        final int pxWidth = displayMetrics.widthPixels;
        final int padding = 1;
        progressBar.setPadding(padding, padding, padding, padding);
        final int trackThickness = progressBar.getTrackThickness();

        final int progressBarSize = pxWidth - trackThickness - 2 * padding;
        progressBar.setIndicatorSize(progressBarSize);
    }

    public void onDetails(View view) {
        if (isPermissionsOrConnectionInvalid()) {
            return;
        }

        final Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra(getString(R.string.ExtraHr), heartRateDataLast.getHr());
        intent.putExtra(getString(R.string.ExtraHrStatus), heartRateDataLast.getStatus());
        intent.putExtra(getString(R.string.ExtraIbi), heartRateDataLast.getIbi());
        intent.putExtra(getString(R.string.ExtraQualityIbi), heartRateDataLast.getQIbi());
        startActivity(intent);
    }

    public void performMeasurement(View view) {
        if (isPermissionsOrConnectionInvalid()) {
            return;
        }

        if (!isMeasurementRunning.get()) {
            previousStatus = SpO2Status.INITIAL_STATUS;
            butStart.setText(R.string.StopLabel);
            txtSpo2.setText(R.string.SpO2DefaultValue);
            measurementProgress.setProgress(0);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            spO2Listener.startTracker();
            isMeasurementRunning.set(true);
            uiUpdateThread = new Thread(countDownTimer::start);
            uiUpdateThread.start();
        } else {
            butStart.setEnabled(false);
            isMeasurementRunning.set(false);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            spO2Listener.stopTracker();
            final Handler progressHandler = new Handler(Looper.getMainLooper());
            progressHandler.postDelayed(() ->
                    {
                        butStart.setText(R.string.StartLabel);
                        txtStatus.setText(R.string.StatusDefaultValue);
                        measurementProgress.setProgress(0);
                        butStart.setEnabled(true);
                    }, MEASUREMENT_TICK * 2
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            permissionGranted = true;
            for (int i = 0; i < permissions.length; ++i) {
                if (grantResults[i] == PERMISSION_DENIED) {
                    //User denied permissions twice - permanent denial:
                    if (!shouldShowRequestPermissionRationale(permissions[i]))
                        Toast.makeText(getApplicationContext(), getString(R.string.PermissionDeniedPermanently), Toast.LENGTH_LONG).show();

                    else
                        Toast.makeText(getApplicationContext(), getString(R.string.PermissionDeniedRationale), Toast.LENGTH_LONG).show();
                    permissionGranted = false;
                    break;
                }
            }
            if (permissionGranted) {
                createConnectionManager();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean isPermissionsOrConnectionInvalid() {
        if (!isPermissionGranted()) {
            requestPermissions();
        }
        if (!permissionGranted) {
            Log.i(APP_TAG, "Could not get permissions. Terminating measurement");
            return true;
        }
        if (!connected) {
            Toast.makeText(getApplicationContext(), getString(R.string.ConnectionError), Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    private boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            return (ActivityCompat.checkSelfPermission(getApplicationContext(), HealthPermissions.READ_HEART_RATE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), HealthPermissions.READ_OXYGEN_SATURATION) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            requestPermissions(new String[]{HealthPermissions.READ_HEART_RATE, HealthPermissions.READ_OXYGEN_SATURATION}, 0);
        } else {
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 0);
        }
    }
    public void startEcgMeasurement(View view) {
        if (isPermissionsOrConnectionInvalid()) return;

        if (!isEcgRunning.get()) {
            butEcgStart.setText("Detener ECG");
            txtEcg.setText("Toca el botón Home del reloj...");
            ecgProgress.setProgress(0);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            ecgListener.startTracker();
            isEcgRunning.set(true);

            new CountDownTimer(ECG_DURATION, 1000) {
                @Override public void onTick(long millisUntilFinished) {
                    if (!isEcgRunning.get()) cancel();
                    int progress = (int) ((ECG_DURATION - millisUntilFinished) / 1000);
                    ecgProgress.setProgress(progress * 100 / 30, true); // 0-100%
                }

                @Override public void onFinish() {
                    stopEcgMeasurement();
                }
            }.start();

        } else {
            stopEcgMeasurement();
        }
    }

    private void stopEcgMeasurement() {
        isEcgRunning.set(false);
        ecgListener.stopTracker();
        butEcgStart.setText("Iniciar ECG");
        ecgProgress.setProgress(100, true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}