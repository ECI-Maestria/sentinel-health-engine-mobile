package com.example.tesisv3.domain.entities;

public class HeartRateData {
    public final static int IBI_QUALITY_SHIFT = 15;
    public final static int IBI_MASK = 0x1;
    public final static int IBI_QUALITY_MASK = 0x7FFF;

    private int status = HeartRateStatus.HR_STATUS_NONE;
    private int hr = 0;
    private int ibi = 0;
    private int qIbi = 1;

    public HeartRateData() {
    }

    public HeartRateData(int status, int hr, int ibi, int qIbi) {
        this.status = status;
        this.hr = hr;
        this.ibi = ibi;
        this.qIbi = qIbi;
    }

    public int getStatus() {
        return status;
    }

    public int getHr() {
        return hr;
    }

    public int getIbi() {
        return ibi;
    }

    public int getQIbi() {
        return qIbi;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setHr(int hr) {
        this.hr = hr;
    }

    public void setIbi(int ibi) {
        this.ibi = ibi;
    }

    public void setQIbi(int qIbi) {
        this.qIbi = qIbi;
    }

    public int getHrIbi() {
        return (qIbi << IBI_QUALITY_SHIFT) | ibi;
    }

    @Override
    public String toString() {
        return "HeartRateData{" +
                "status=" + status +
                ", hr=" + hr +
                ", ibi=" + ibi +
                ", qIbi=" + qIbi +
                ", hrIbi=" + getHrIbi() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HeartRateData that = (HeartRateData) o;
        return status == that.status &&
                hr == that.hr &&
                ibi == that.ibi &&
                qIbi == that.qIbi;
    }
    public int getIbiQuality() {
        return (getHrIbi() >> IBI_QUALITY_SHIFT) & IBI_QUALITY_MASK;
    }

    public int getIbiValue() {
        return getHrIbi() & IBI_MASK;
    }

    public boolean isValidHeartRate() {
        return hr > 0 && hr < 250; // Rango razonable para frecuencia cardíaca
    }
}
