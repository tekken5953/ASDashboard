package com.example.dashboard;

public class RecyclerViewItem {
    private String title;
    private String number;
    private String unit;
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RecyclerViewItem(String title, String number, String unit, String status) {
        this.title = title;
        this.number = number;
        this.unit = unit;
        this.status = status;
    }
}
