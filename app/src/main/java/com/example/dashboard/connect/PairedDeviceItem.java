package com.example.dashboard.connect;

import android.graphics.drawable.Drawable;

public class PairedDeviceItem {
    private Drawable icon;
    private String name;
    private String address;

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public PairedDeviceItem(Drawable icon, String name, String address) {
        this.icon = icon;
        this.name = name;
        this.address = address;
    }
}
