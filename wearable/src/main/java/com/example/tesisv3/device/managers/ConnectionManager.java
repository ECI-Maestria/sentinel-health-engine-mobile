
package com.example.tesisv3.device.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.samsung.android.service.health.tracking.ConnectionListener;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.HealthTrackerException;
import com.samsung.android.service.health.tracking.HealthTrackingService;
import com.samsung.android.service.health.tracking.data.HealthTrackerType;
import com.example.tesisv3.R;
import com.example.tesisv3.device.listeners.BaseListener;
import com.example.tesisv3.device.listeners.EcgListener;
import com.example.tesisv3.device.listeners.HeartRateListener;
import com.example.tesisv3.device.listeners.SpO2Listener;

import java.util.List;

public class ConnectionManager {
    private final static String TAG = "Connection Manager";
    private final ConnectionObserver connectionObserver;
    private HealthTrackingService healthTrackingService = null;
    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onConnectionSuccess() {
            Log.i(TAG, "Connected");
            connectionObserver.onConnectionResult(R.string.ConnectedToHs);
            if (!isSpO2Available(healthTrackingService)) {
                Log.i(TAG, "Device does not support SpO2 tracking");
                connectionObserver.onConnectionResult(R.string.NoSpo2Support);
            }
            if (!isHeartRateAvailable(healthTrackingService)) {
                Log.i(TAG, "Device does not support Heart Rate tracking");
                connectionObserver.onConnectionResult(R.string.NoHrSupport);
            }
            if (!isEcgRateAvailable(healthTrackingService)) {
                Log.i(TAG, "Device does not support ECG Rate tracking");
                connectionObserver.onConnectionResult(R.string.NoHrSupport);
            }
        }

        @Override
        public void onConnectionEnded() {
            Log.i(TAG, "Disconnected");
        }

        @Override
        public void onConnectionFailed(HealthTrackerException e) {
            connectionObserver.onError(e);
        }
    };

    public ConnectionManager(ConnectionObserver observer) {
        connectionObserver = observer;
    }

    public void connect(Context context) {
        healthTrackingService = new HealthTrackingService(connectionListener, context);
        healthTrackingService.connectService();
    }

    public void disconnect() {
        if (healthTrackingService != null)
            healthTrackingService.disconnectService();
    }

    public void initSpO2(SpO2Listener spO2Listener) {
        final HealthTracker healthTracker;
        healthTracker = healthTrackingService.getHealthTracker(HealthTrackerType.SPO2_ON_DEMAND);
        spO2Listener.setHealthTracker(healthTracker);
        setHandlerForBaseListener(spO2Listener);
    }

    public void initHeartRate(HeartRateListener heartRateListener) {
        final HealthTracker healthTracker;
        healthTracker = healthTrackingService.getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS);
        heartRateListener.setHealthTracker(healthTracker);
        setHandlerForBaseListener(heartRateListener);
    }

    public void initEcg(EcgListener listener) {
        final HealthTracker healthTracker;
        try {
            healthTracker = healthTrackingService.getHealthTracker(HealthTrackerType.ECG_ON_DEMAND);
            listener.setHealthTracker(healthTracker);
            setHandlerForBaseListener(listener);
            Log.d("ConnectionManager", "ECG tracker inicializado");
        } catch (Exception e) {
            Log.e("ConnectionManager", "Error init ECG: " + e.getMessage());
        }
    }


    private void setHandlerForBaseListener(BaseListener baseListener) {
        baseListener.setHandler(new Handler(Looper.getMainLooper()));
    }

    private boolean isSpO2Available(@NonNull HealthTrackingService healthTrackingService) {
        final List<HealthTrackerType> availableTrackers = healthTrackingService.getTrackingCapability().getSupportHealthTrackerTypes();
        return availableTrackers.contains(HealthTrackerType.SPO2_ON_DEMAND);
    }

    private boolean isHeartRateAvailable(@NonNull HealthTrackingService healthTrackingService) {
        final List<HealthTrackerType> availableTrackers = healthTrackingService.getTrackingCapability().getSupportHealthTrackerTypes();
        return availableTrackers.contains(HealthTrackerType.HEART_RATE_CONTINUOUS);
    }

    private boolean isEcgRateAvailable(@NonNull HealthTrackingService healthTrackingService) {
        final List<HealthTrackerType> availableTrackers = healthTrackingService.getTrackingCapability().getSupportHealthTrackerTypes();
        return availableTrackers.contains(HealthTrackerType.ECG_ON_DEMAND);
    }
}
