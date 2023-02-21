package com.example.asdashboard.model;

import android.graphics.drawable.Drawable;

public class ConnectRecyclerItem {
    private Drawable device_img;
    private String device_name;
    private String device_address;

    public Drawable getDevice_img() {
        return device_img;
    }

    public void setDevice_img(Drawable device_img) {
        this.device_img = device_img;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getDevice_address() {
        return device_address;
    }

    public void setDevice_address(String device_address) {
        this.device_address = device_address;
    }

    public ConnectRecyclerItem(Drawable device_img, String device_name, String device_address) {
        this.device_img = device_img;
        this.device_name = device_name;
        this.device_address = device_address;
    }
}
