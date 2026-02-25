
package com.example.tesisv3.domain.entities;

public interface TrackerDataObserver {
    void onHeartRateTrackerDataChanged(HeartRateData hrData);

    void onSpO2TrackerDataChanged(int status, int spO2Value);

    void onError(int errorResourceId);

    void onEcgTrackerDataChanged(EcgData ecgData);

}
