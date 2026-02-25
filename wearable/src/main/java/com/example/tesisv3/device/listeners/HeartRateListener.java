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

package com.example.tesisv3.device.listeners;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.ValueKey;
import com.example.tesisv3.R;
import com.example.tesisv3.domain.entities.HeartRateData;
import com.example.tesisv3.domain.entities.TrackerDataNotifier;
import com.example.tesisv3.domain.entities.WearData;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class HeartRateListener extends BaseListener {
    private final Context context;
    private final static String APP_TAG = "HeartRateListener";

    public HeartRateListener(Context context) {
        this.context = context.getApplicationContext();
        final HealthTracker.TrackerEventListener trackerEventListener = new HealthTracker.TrackerEventListener() {
            @Override
            public void onDataReceived(@NonNull List<DataPoint> list) {
                sendData(context, "{ \"test\": \"hola\" }");
                for (DataPoint dataPoint : list) {
                    readValuesFromDataPoint(dataPoint);
                }
            }

            @Override
            public void onFlushCompleted() {
                Log.i(APP_TAG, " onFlushCompleted called");
            }

            @Override
            public void onError(HealthTracker.TrackerError trackerError) {
                Log.e(APP_TAG, " onError called: " + trackerError);
                setHandlerRunning(false);
                if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                    TrackerDataNotifier.getInstance().notifyError(R.string.NoPermission);
                }
                if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                    TrackerDataNotifier.getInstance().notifyError(R.string.SdkPolicyError);
                }
            }
        };
        setTrackerEventListener(trackerEventListener);
    }

    public void readValuesFromDataPoint(DataPoint dataPoint) {
        final HeartRateData hrData = new HeartRateData();
        final List<Integer> hrIbiList = dataPoint.getValue(ValueKey.HeartRateSet.IBI_LIST);
        final List<Integer> hrIbiStatus = dataPoint.getValue(ValueKey.HeartRateSet.IBI_STATUS_LIST);

        hrData.setStatus(dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS));
        hrData.setHr(dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE));
        if (hrIbiList != null && hrIbiList.size() != 0) {
            hrData.setIbi(hrIbiList.get(hrIbiList.size() - 1)); // Inter-Beat Interval (ms)
        }
        if (hrIbiStatus != null && hrIbiStatus.size() != 0) {
            hrData.setQIbi(hrIbiStatus.size() - 1); // 1: bad, 0: good
        }
        TrackerDataNotifier.getInstance().notifyHeartRateTrackerObservers(hrData);
        Log.d(APP_TAG, dataPoint.toString());
        String json = buildJson(hrData);
        sendData(context, json);

    }



    public String buildJson(HeartRateData hrData) {
        WearData data = new WearData(
                "heart_rate",
                hrData.getHr(),
                System.currentTimeMillis()
        );
        return new Gson().toJson(data);
    }




    public void sendData(Context context, String json) {
        Log.d("WEAR","JESUS");
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



}
