package com.example.tesisv3.device.listeners;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.tesisv3.R;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.ValueKey;
//import com.example.wearable.R;
import com.example.tesisv3.domain.entities.EcgData;
import com.example.tesisv3.domain.entities.TrackerDataNotifier;

import java.util.List;

public class EcgListener extends BaseListener {
    private final static String APP_TAG = "EcgListener";
    private static final int NO_CONTACT = 5;  // LEAD_OFF value para sin contacto (del sample ECG)

    public EcgListener() {
        final HealthTracker.TrackerEventListener trackerEventListener = new HealthTracker.TrackerEventListener() {
            @Override
            public void onDataReceived(@NonNull List<DataPoint> list) {
                if (list.size() == 0) return;

                // Verifica contacto en el primer DataPoint (como en sample ECG)
                final DataPoint firstPoint = list.get(0);
                final int leadOff = firstPoint.getValue(ValueKey.EcgSet.LEAD_OFF);
                if (leadOff == NO_CONTACT) {
                    Log.w(APP_TAG, "Sin contacto de electrodos (LEAD_OFF = " + leadOff + ")");
                    final EcgData ecgData = new EcgData(0f, true, 1);  // Status 1: Sin contacto
                    TrackerDataNotifier.getInstance().notifyEcgTrackerObservers(ecgData);
                    return;
                }

                // Procesa el batch: Promedia ECG_MV (voltaje en mV)
                float sum = 0f;
                int validPoints = 0;
                for (DataPoint dataPoint : list) {
                    final Float ecgMv = dataPoint.getValue(ValueKey.EcgSet.ECG_MV);
                    if (ecgMv != null) {
                        sum += ecgMv;
                        validPoints++;
                    }
                }

                if (validPoints == 0) {
                    Log.w(APP_TAG, "No hay datos válidos en el batch");
                    return;
                }

                final float avgEcg = sum / validPoints;
                final EcgData ecgData = new EcgData(avgEcg, false, 0);  // Status 0: OK
                TrackerDataNotifier.getInstance().notifyEcgTrackerObservers(ecgData);
                Log.d(APP_TAG, "ECG promedio del batch: " + avgEcg + " mV (puntos válidos: " + validPoints + ")");
            }

            @Override
            public void onFlushCompleted() {
                Log.i(APP_TAG, "onFlushCompleted called (ECG batch flushed)");
            }

            @Override
            public void onError(HealthTracker.TrackerError trackerError) {
                Log.e(APP_TAG, "onError called: " + trackerError);
                setHandlerRunning(false);
                int errorResId = R.string.UnknownError;
                switch (trackerError) {
                    case PERMISSION_ERROR:
                        errorResId = R.string.NoPermission;
                        break;
                    case SDK_POLICY_ERROR:
                        errorResId = R.string.SdkPolicyError;
                        break;
                    default:
                        errorResId = R.string.EcgSensorError;
                        break;
                }
                TrackerDataNotifier.getInstance().notifyError(errorResId);
            }
        };
        setTrackerEventListener(trackerEventListener);
    }

    public void readValuesFromDataPoint(DataPoint dataPoint) {
        final EcgData ecgData = new EcgData();
        final Integer leadOff = dataPoint.getValue(ValueKey.EcgSet.LEAD_OFF);
        final Float ecgMv = dataPoint.getValue(ValueKey.EcgSet.ECG_MV);

        ecgData.setLeadOff((leadOff != null && leadOff == NO_CONTACT));
        ecgData.setAvgEcg((ecgMv != null) ? ecgMv : 0f);
        ecgData.setStatus(ecgData.isLeadOff() ? 1 : 0);

        TrackerDataNotifier.getInstance().notifyEcgTrackerObservers(ecgData);
        Log.d(APP_TAG, dataPoint.toString() + " -> " + ecgData.toString());
    }
}