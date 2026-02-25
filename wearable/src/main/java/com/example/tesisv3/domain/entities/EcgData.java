package com.example.tesisv3.domain.entities;

public class EcgData {
    public static final int STATUS_OK = 0;
    public static final int STATUS_LEAD_OFF = 1;
    public static final int STATUS_ERROR = 2;

    private float avgEcg;      // Promedio de voltaje en mV
    private boolean leadOff;   // true si sin contacto
    private int status;        // 0=OK, 1=Sin contacto, 2=Error

    public EcgData() {
        this(0f, false, STATUS_OK);
    }

    public EcgData(float avgEcg, boolean leadOff, int status) {
        this.avgEcg = avgEcg;
        this.leadOff = leadOff;
        this.status = status;
    }

    // GETTERS
    public float getAvgEcg() {
        return avgEcg;
    }

    public boolean isLeadOff() {
        return leadOff;
    }

    public int getStatus() {
        return status;
    }

    // SETTERS
    public void setAvgEcg(float avgEcg) {
        this.avgEcg = avgEcg;
    }

    public void setLeadOff(boolean leadOff) {
        this.leadOff = leadOff;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isStatusOk() {
        return status == STATUS_OK;
    }

    public boolean isStatusLeadOff() {
        return status == STATUS_LEAD_OFF;
    }

    public boolean isStatusError() {
        return status == STATUS_ERROR;
    }

    public String getStatusDescription() {
        switch (status) {
            case STATUS_OK:
                return "OK";
            case STATUS_LEAD_OFF:
                return "Sin contacto";
            case STATUS_ERROR:
                return "Error";
            default:
                return "Desconocido";
        }
    }

    public boolean isValid() {
        return isStatusOk() && !leadOff;
    }

    public String getFormattedVoltage() {
        return String.format("%.2f mV", avgEcg);
    }

    @Override
    public String toString() {
        return "EcgData{" +
                "avgEcg=" + avgEcg +
                " mV, leadOff=" + leadOff +
                ", status=" + getStatusDescription() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EcgData ecgData = (EcgData) o;
        return Float.compare(ecgData.avgEcg, avgEcg) == 0 &&
                leadOff == ecgData.leadOff &&
                status == ecgData.status;
    }

    public EcgData copy() {
        return new EcgData(avgEcg, leadOff, status);
    }

    public void reset() {
        this.avgEcg = 0f;
        this.leadOff = false;
        this.status = STATUS_OK;
    }
}