package com.example.dashboard;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothThread extends Thread {
    private final static String TAG = "BluetoothThread";

    // 스태틱
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private final int MINIMUM_LENGTH = 15;

    // 액티비티
    private Context listContext;
    private Context fragmentContext;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;

    // 블루투스
    private BluetoothSocket mBluetoothSocket = null;
    private BluetoothDevice mBluetoothDevice;
    private String deviceName;
    private String recvData;
    private String macAddress;

    // 스레드
    private boolean isRun = false;
    private boolean isConnect = false;
    private static boolean isConnectionError = false;

    // 이벤트
    private connectedSocketEventListener mConnectedSocketEventListener;
    private disConnectedSocketEventListener mDisconnectedSocketEventListener;

    // 프로토콜
    byte[] STX = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    byte[] ETX = {(byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE};

    // 멤버 변수
    private int sequence;
    private boolean errorFlag;

    // 옵저버
    private final DataShareViewModel viewModel;

    public BluetoothThread(Activity activity) {
        super();
        sequence = 0;
        errorFlag = false;

        viewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(DataShareViewModel.class);

        System.out.println("Thread Created");
    }

    @Override
    public void run() {
        isRun = true;

        while (isRunning()) {
            byte[] readBuffer = new byte[1024];
            byte[] packetBytes;
            int readBufferPosition = 0;
            int bytesAvailable = 0;

            while (isConnected()) {
                try {
                    bytesAvailable = mInputStream.available();
                    packetBytes = new byte[bytesAvailable];

                    int readCount = mInputStream.read(packetBytes, 0, bytesAvailable);
                    if (readCount == 0) continue;

                    try {
                        System.arraycopy(packetBytes, readBufferPosition, readBuffer, 0, packetBytes.length);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // 버퍼 초기화
                        readBufferPosition = 0;
                        readBuffer = new byte[1024];
                        packetBytes = null;

                        e.printStackTrace();

                        continue;
                    }

                    readBufferPosition += packetBytes.length;

                    if (readBufferPosition > MINIMUM_LENGTH) {
                        byte[] exitPoint = new byte[]{
                                readBuffer[readBufferPosition - 4],
                                readBuffer[readBufferPosition - 3],
                                readBuffer[readBufferPosition - 2],
                                readBuffer[readBufferPosition - 1]
                        };
                        if (Arrays.equals(ETX, exitPoint)) {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                    encodedBytes.length);

                            // 수신 받은 메시지
                            recvData = BluetoothAPI.byteArrayToHexString(encodedBytes);
                            // 수신 메시지 전달
                            viewModel.receiveData.postValue(recvData);

                            Log.e(TAG, "recv message (String) : " + recvData);
                            Log.e(TAG, "recv message (Byte) : " + Arrays.toString(encodedBytes));

                            // 버퍼 초기화
                            readBufferPosition = 0;
                            readBuffer = new byte[1024];
                            packetBytes = null;
                        }
                    }

                } catch (IOException | NullPointerException e) {
                    Log.e(TAG, "disconnected", e);
                    isConnect = false;
                    this.closeSocket();
                }
            }
        }
    }

    public boolean connectSocket() {
        //mBluetoothAdapter.cancelDiscovery();
        try {
            mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            mInputStream = mBluetoothSocket.getInputStream();
            mOutputStream = mBluetoothSocket.getOutputStream();

            // 브로드캐스트 미동작으로 인한 Connect 설정 코드
            isConnect = true;

            deviceName = mBluetoothDevice.getName();
            macAddress = mBluetoothDevice.getAddress();

            mConnectedSocketEventListener.onConnectedEvent();
            return true;
        } catch (IOException e) {
            System.out.println("Connect Socket Error");
            mDisconnectedSocketEventListener.onDisconnectedEvent();
            return false;
        }
    }

    public boolean closeSocket() {
        //if(isConnect == false) return;
        try {
            isConnect = false;
            deviceName = null;
            mBluetoothSocket.close();
            mDisconnectedSocketEventListener.onDisconnectedEvent();
            System.out.println("Thread Removed");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "unable to close() " +
                    " socket during connection failure", e);
        } catch (NullPointerException e) {
            // Disconnect
            mDisconnectedSocketEventListener.onDisconnectedEvent();
        }
        System.out.println("Thread Removed");
        return false;
    }

    public void writeHex(byte[] hex) {
        try {
            mOutputStream.write(hex);
            mOutputStream.flush();
            errorFlag = false;
            addSequence();

            Log.e(TAG, "Send message (String) : " + BluetoothAPI.byteArrayToHexString(hex));
            Log.e(TAG, "Send message (Byte) : " + Arrays.toString(hex));
        } catch (IOException e) {
            // Bluetooth Connect 여부 점검 필요
            if (errorFlag) closeSocket();
            else errorFlag = true;
            Log.e(TAG, "Exception during send", e);

        } catch (NullPointerException e) {
            e.printStackTrace();
            closeSocket();
        }
    }

    public boolean isRunning() {
        return isRun;
    }

    public boolean isConnected() {
        return isConnect;
    }

    /* Set Method 모음 */
    public void setListContext(Context c) {
        listContext = c;
    }

    public void setFragmentContext(Context c) {
        fragmentContext = c;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }

    public void setRunning(boolean run) {
        isRun = run;
    }

    public void setBluetoothDevice(BluetoothDevice device) {
        mBluetoothDevice = device;
    }

    public void addSequence() {
        sequence++;
        if (sequence > 65535) {
            sequence = 0;
            System.out.println("Sequence Number Reset!!");
        }
    }

    /* Get Method 모음 */
    public String getRecvData() {
        return recvData;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public int getSequence() {
        return sequence;
    }

    /* Event Listener 모음 */
    public interface connectedSocketEventListener {
        void onConnectedEvent();
    }

    public void setConnectedSocketEventListener(connectedSocketEventListener listener) {
        mConnectedSocketEventListener = listener;
    }

    public interface disConnectedSocketEventListener {
        void onDisconnectedEvent();
    }

    public void setDisconnectedSocketEventListener(disConnectedSocketEventListener listener) {
        mDisconnectedSocketEventListener = listener;
    }


    public static class DataShareViewModel extends ViewModel {
        private MutableLiveData<String> receiveData;

        public MutableLiveData<String> getReceiveData() {
            if (receiveData == null) {
                receiveData = new MutableLiveData<>();
            }
            return receiveData;
        }
    }
}
