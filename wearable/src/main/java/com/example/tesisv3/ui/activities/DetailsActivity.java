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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

//import com.example.wearable.R;
import com.example.tesisv3.domain.entities.EcgData;
import com.example.tesisv3.databinding.ActivityDetailsBinding;
import com.example.tesisv3.domain.entities.HeartRateData;
import com.example.tesisv3.domain.entities.HeartRateStatus;
import com.example.tesisv3.domain.entities.TrackerDataNotifier;
import com.example.tesisv3.domain.entities.TrackerDataObserver;

public class DetailsActivity extends FragmentActivity {

    private final String APP_TAG = "DetailsActivity";

    TextView txtStatus;
    TextView txtHeartRate;
    TextView txtHeartRateStatus;
    TextView txtIbi;
    TextView txtIbiStatus;
    final TrackerDataObserver trackerDataObserver = new TrackerDataObserver() {
        @Override
        public void onHeartRateTrackerDataChanged(HeartRateData hrData) {
            DetailsActivity.this.runOnUiThread(() -> updateUi(hrData));
        }

        @Override
        public void onSpO2TrackerDataChanged(int status, int spO2Value) {
        }

        @Override
        public void onError(int errorResourceId) {
            runOnUiThread(() ->
                    Toast.makeText(getApplicationContext(), getString(errorResourceId), Toast.LENGTH_LONG));
        }

        @Override
        public void onEcgTrackerDataChanged(EcgData ecgData) {

        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityDetailsBinding binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //setContentView(R.layout.activity_details);

        txtStatus = binding.txtStatus;
        txtHeartRate = binding.txtHeartRate;
        txtHeartRateStatus = binding.txtHeartRateStatus;
        txtIbi = binding.txtIbi;
        txtIbiStatus = binding.txtIbiStatus;

        final Intent intent = getIntent();
        final int status = intent.getIntExtra("HrStatus", HeartRateStatus.HR_STATUS_NONE);
        final int hr = intent.getIntExtra("Hr", 0);
        final int ibi = intent.getIntExtra("Ibi", 0);
        final int qIbi = intent.getIntExtra("QualityIbi", 1);

        final HeartRateData hrData = new HeartRateData(status, hr, ibi, qIbi);
        updateUi(hrData);

        TrackerDataNotifier.getInstance().addObserver(trackerDataObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TrackerDataNotifier.getInstance().removeObserver(trackerDataObserver);
    }

    private void updateUi(HeartRateData hrData) {
        txtHeartRateStatus.setText(String.valueOf(hrData.getStatus()));
        setStatus(hrData.getStatus());

        if (hrData.getStatus() == HeartRateStatus.HR_STATUS_FIND_HR) {
            txtHeartRate.setText(String.valueOf(hrData.getHr()));
            txtHeartRateStatus.setTextColor(Color.WHITE);

            txtIbi.setText(String.valueOf(hrData.getIbi()));
            txtIbiStatus.setText(String.valueOf(hrData.getQIbi()));
            txtIbiStatus.setTextColor((hrData.getQIbi() == 0) ? Color.WHITE : Color.RED);
            Log.d(APP_TAG, "HR : " + hrData.getHr() + " HR_IBI : " + hrData.getIbi() + "(" + hrData.getQIbi() + ") ");

        } else {
            txtHeartRate.setText("0");
            txtHeartRateStatus.setTextColor(Color.RED);
            txtIbi.setText("0");
            txtIbiStatus.setText("1");
            txtIbiStatus.setTextColor(Color.RED);
        }
    }

    private void setStatus(int status) {
        Log.i(APP_TAG, "HR Status: " + status);
        String stringId = "Initial heart rate measuring state.";
        switch (status) {
            case HeartRateStatus.HR_STATUS_FIND_HR:
                stringId = "Successful heart rate measurement.";
                break;
            case HeartRateStatus.HR_STATUS_NONE:
                break;
            case HeartRateStatus.HR_STATUS_ATTACHED:
                stringId = "Wearable is attached.";
                break;
            case HeartRateStatus.HR_STATUS_DETECT_MOVE:
                stringId = "Wearable move detection.";
                break;
            case HeartRateStatus.HR_STATUS_DETACHED:
                stringId = "Wearable is detached.";
                break;
            case HeartRateStatus.HR_STATUS_LOW_RELIABILITY:
                stringId = "PPG signal is weak.";
                break;
            case HeartRateStatus.HR_STATUS_VERY_LOW_RELIABILITY:
                stringId = "PPG signal is too weak.";
                break;
            case HeartRateStatus.HR_STATUS_NO_DATA_FLUSH:
                stringId = "flush() is called but no data.";
                break;
        }

        txtStatus.setText(stringId);
    }
}