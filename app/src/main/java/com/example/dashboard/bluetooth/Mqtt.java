package com.example.dashboard.bluetooth;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class Mqtt {

    private final String MQTT_ADDRESS = "ascloud.kr"; // ascloud.kr
    private final String MQTT_ADDRESS_SUB = "192.168.0.177";
    //    private final String MQTT_ADDRESS_ADMIN = "192.168.0.69";
    private final String MQTT_PORT = ":1883";

    private final String MQTT_TOPIC_BASE = "/nodes/envi/";
    private final String MQTT_TOPIC_MEASURE = "measured";
    private final String MQTT_TOPIC_MAINT_REQ = "maint_req";
    private final String MQTT_TOPIC_MAINT_RES = "maint_res";
    private final String MQTT_TOPIC_STATUS = "status";
//    private final String MQTT_WIFI_STATUS = "wifiConnected";

    // 이벤트
    private mqttRequestReceiveListener mMqttRequestReceiveListener;

    private IMqttAsyncClient mqttClient;
    private IMqttToken token;

    private Thread thread;

    private final Context context;
    private final String device_id;

    private String topic_base;
    private int errCnt;

    public Mqtt(Context context, String device_id) {
        this.context = context;
        this.device_id = device_id;
//    this.device_id = MqttClient.generateClientId();
        errCnt = 0;
    }

    public void connect() {
//    clientConnect(MQTT_ADDRESS_ADMIN);
        clientConnect(MQTT_ADDRESS_SUB);
        topic_base = MQTT_TOPIC_BASE + device_id + "/";
    }

    public void disconnect() {
        publish_state(getJsonConnectState(0));
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void clientConnect(String server_address) {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
//                mqttClient = new MqttAndroidClient(context, "tcp://" + server_address + MQTT_PORT, device_id);
                mqttClient = new MqttAndroidClient(context, "tcp://" + server_address + MQTT_PORT, device_id, Ack.AUTO_ACK);

                try {
                    token = mqttClient.connect(getMqttConnectionOption());

                    token.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            mqttClient.setBufferOpts(getDisconnectedBufferOptions());
                            subscribe();
                            Log.d("MqttLog","MQTT Connect Success! :)");

                            publish_state(getJsonConnectState(1));
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.d("MqttLog","MQTT Connect Failure! :(");

                            if (server_address.equals(MQTT_ADDRESS)) {
                                clientConnect(MQTT_ADDRESS_SUB);
                            }
                        }
                    });
                } catch (MqttException e) {
                    Log.d("MqttLog","MQTT Connect Error!");
                    e.printStackTrace();
                }
                mqttClient.setCallback(getMqttCallBack());
            }
        });
        thread.start();
    }

    private String classifyCommand(MqttMessage message) {
        String strMessage = new String(message.getPayload());
        String command = null;

        try {
            JSONObject jsonMessage = new JSONObject(strMessage);

            // 키 추출해서 커맨드 뽑아내기
            jsonMessage.keys();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return null;
    }

    public void subscribe() throws NullPointerException {
        Log.e("MQTT", "Subscribe Topic is " + topic_base);
        try {
            mqttClient.subscribe(topic_base + MQTT_TOPIC_MEASURE, 0);
            mqttClient.subscribe(topic_base + MQTT_TOPIC_MAINT_REQ, 0);
        } catch (MqttException e) {
            Log.e("MqttLog", "Subscribe Error : " + e);
        }
    }

    public boolean publish_measured(JSONObject jsonData) throws NullPointerException {
        String strData = jsonData.toString();

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(strData.getBytes());

        try {
            mqttClient.publish(topic_base + MQTT_TOPIC_MEASURE, message);
            System.out.println("Mqtt Publish Topic " + topic_base + MQTT_TOPIC_MEASURE);
            System.out.println("Mqtt Publish Data " + strData);
            return true;
        } catch (MqttException e) {
            Log.e("MqttLog", "MQTT publish_measured Error : " + e);
            return false;
        }
    }

    public boolean publishMeasureChk(JSONObject jsonData) throws NullPointerException {
        String strData = String.valueOf(jsonData);

        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(strData.getBytes());

        try {
            mqttClient.publish(topic_base + MQTT_TOPIC_MAINT_RES, message);
            System.out.println("Mqtt Publish Topic " + topic_base + MQTT_TOPIC_MAINT_RES);
            System.out.println("Mqtt Publish Data " + strData);
            return true;
        } catch (MqttException e) {
            Log.e("MqttLog", "MQTT publish_measured Error : " + e);
            return false;
        }
    }

    public boolean publish_state(String strData) throws NullPointerException {
        MqttMessage message = new MqttMessage();
        message.setQos(0);
        message.setPayload(strData.getBytes());

        try {
            mqttClient.publish(topic_base + MQTT_TOPIC_STATUS, message);
            System.out.println("Mqtt Publish Topic " + topic_base + MQTT_TOPIC_STATUS);
            System.out.println("Mqtt Publish Data " + strData);
            return true;
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getJsonConnectState(int iState) {
        JSONObject jsonConnectState = new JSONObject();

        try {
            jsonConnectState.put("service", device_id);
            jsonConnectState.put("state", iState);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonConnectState.toString();
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    private MqttCallback getMqttCallBack() {
        return new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
//        Toast.makeText(context, "MQTT 연결 실패", Toast.LENGTH_SHORT).show();
                System.out.println("MQTT Connection Lost :(");
                //System.out.println(cause.toString());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String[] separatedTopic = topic.split("/");
                String order = separatedTopic[separatedTopic.length - 1];

                // topic 분기 처리
                switch (order) {
                    case MQTT_TOPIC_MEASURE:
                        // 받을 일이 있나?
                        break;
                    case MQTT_TOPIC_MAINT_REQ:
                        // 요청 받으면 처리
                        String request = classifyCommand(message);
                        mMqttRequestReceiveListener.onMqttRequestReceiveListener(request);
                        Log.d("MqttLog", message.toString());
                        break;
                    default:
                        System.out.println("MQTT Receive Topic : " + order);
                        System.out.println("MQTT Receive Data : " + new String(message.getPayload()));
                        break;
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        };
    }

    private DisconnectedBufferOptions getDisconnectedBufferOptions() {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(100);
        disconnectedBufferOptions.setPersistBuffer(true);
        disconnectedBufferOptions.setDeleteOldestMessages(false);
        return disconnectedBufferOptions;
    }

    private MqttConnectOptions getMqttConnectionOption() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setAutomaticReconnect(true);

        // Disconnect 시 마지막 메시지 발송
        mqttConnectOptions.setWill(MQTT_TOPIC_BASE + device_id + "/" + MQTT_TOPIC_STATUS,
                getJsonConnectState(0).getBytes(), 1, true);
        return mqttConnectOptions;
    }

    public boolean chkErrorCount() {
        errCnt++;
        return errCnt <= 3;
    }

    public void resetErrorCount() {
        errCnt = 0;
    }

    public void interrupt() {
        thread.interrupt();
    }


    // 이벤트 인터페이스
    public interface mqttRequestReceiveListener {
        void onMqttRequestReceiveListener(String command);
    }

    public void setMqttRequestReceiveListener(mqttRequestReceiveListener listener) {
        mMqttRequestReceiveListener = listener;
    }
}
