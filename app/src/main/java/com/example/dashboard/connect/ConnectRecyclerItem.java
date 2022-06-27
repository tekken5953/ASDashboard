package com.example.dashboard.connect;

public class ConnectRecyclerItem {
    private String device_name;
    private String connect;

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getConnect() {
        return connect;
    }

    public void setConnect(String connect) {
        this.connect = connect;
    }

    public ConnectRecyclerItem(String device_name, String connect) {
        this.device_name = device_name;
        this.connect = connect;
    }
}
