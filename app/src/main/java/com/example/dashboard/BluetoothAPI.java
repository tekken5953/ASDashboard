package com.example.dashboard;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class BluetoothAPI {
    public static final byte[] STX = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    public static final byte[] ETX = {(byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE};

    public static final byte REQUEST_INDIVIDUAL_STATE = (byte) 0x01;
    public static final byte RESPONSE_INDIVIDUAL_STATE = (byte) 0x81;
    public static final byte REQUEST_ALL_STATE = (byte) 0x03;
    public static final byte RESPONSE_ALL_STATE = (byte) 0x83;
    public static final byte REQUEST_CONTROL = (byte) 0x02;
    public static final byte RESPONSE_CONTROL = (byte) 0x82;
    public static final byte CALL_EVENTS = (byte) 0x84;

    public static final int SENSOR_EQUIP_COUNT = 13;
    public static final int SENSOR_TOTAL_COUNT = 15;

    private static final int BYTE_UNSIGNED_SHORT = 2;
    private static final int BYTE_INTEGER = 4;
    private static final int BYTE_DOUBLE = 8;
    private static final int MAX_BUFFER = 1024;

    public static byte[] makeFrame(byte[] command, byte[] body, int sequence) {
        byte[] byteSequence = hexStringToByteArray(String.format("%04x", sequence));
        byteSequence = changeByteOrder(byteSequence, true);

        int iLength = byteSequence.length + command.length + body.length;
        byte[] totalLength = hexStringToByteArray(String.format("%08x", iLength));
        totalLength = changeByteOrder(totalLength, true);

        return combineArray(new byte[][]{STX, totalLength, byteSequence, command, body, ETX});
    }

    public static byte[][] separatedFrame(byte[] recvArray) {
        // header + body + tail
        // STX + head = header
        byte[] stx = new byte[4];
        byte[] length = new byte[4];
        byte[] sequence = new byte[2];
        byte[] command = new byte[1];
        byte[] body;
        byte[] etx = new byte[4];

        int totalLength = 0;

        System.arraycopy(recvArray, totalLength, stx, 0, stx.length);
        totalLength += stx.length;
        System.arraycopy(recvArray, totalLength, length, 0, length.length);
        totalLength += length.length;
        System.arraycopy(recvArray, totalLength, sequence, 0, sequence.length);
        totalLength += sequence.length;
        System.arraycopy(recvArray, totalLength, command, 0, command.length);
        totalLength += command.length;

        byte[] orderedLength = changeByteOrder(length, true);

        int intLength = Integer.parseUnsignedInt(byteArrayToHexString(orderedLength), 16) - 3;
        body = new byte[intLength];

        System.arraycopy(recvArray, totalLength, body, 0, body.length);
        totalLength += body.length;
        System.arraycopy(recvArray, totalLength, etx, 0, etx.length);
        totalLength += etx.length;

        if (totalLength == recvArray.length)
            return new byte[][]{stx, length, sequence, command, body, etx};
        else return new byte[][]{};
    }

    public static Bundle analyzedRequestBody(byte[] recvBody) {
        int totalLength = recvBody.length;
        byte[] tag = new byte[recvBody.length];

        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        Bundle bundle = new Bundle();

        byte tag_id;
        byte[] tag_length = new byte[2];
        byte[] tag_content;
        int pos = 0;

        System.arraycopy(recvBody, 0, tag, 0, recvBody.length);

        while (totalLength != 0) {
            tag_id = tag[0];
            System.arraycopy(tag, 1, tag_length, 0, tag_length.length);
            tag_length = changeByteOrder(tag_length, true);

            int int_tag_length = 3 + ByteBuffer.wrap(tag_length).getShort();
            totalLength -= int_tag_length;
            pos += int_tag_length;

            tag_content = new byte[int_tag_length - 3];
            System.arraycopy(tag, 1 + tag_length.length, tag_content, 0, tag_content.length);

            tag = new byte[totalLength];
            System.arraycopy(recvBody, pos, tag, 0, totalLength);

            tag_content = changeByteOrder(tag_content, true);
            arrayList.add(tag_id & 0xff);

            String id = byteArrayToHexString(new byte[]{tag_id});

            // TAG ID별로 분기 처리 추가
            switch (tag_id) {
                case 0x01:
                case 0x0F:
                case 0x14:
                case 0x17:
                case 0x1A:
                case 0x1D:
                case 0x20:
                case 0x23:
                case 0x26:
                case 0x29:
                case 0x2C:
                case 0x2F:
                case 0x32:
                    // PM 유효정보  TH 유효 정보  H2 유효 정보  CH2O 유효정보  CO 유효정보
                    // CO2 유효정보 TVOC 유효정보 O3 유효정보   NH3 유효정보   H2S 유효정보
                    // CH4 유효정보 C3H8 유효정보 NO2 유효정보
                    //arrayList.add(getIsValid(tag_content));
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x02:
                case 0x04:
                case 0x06:
                case 0x09:
                case 0x0C:
                case 0x10:
                case 0x12:
                case 0x15:
                case 0x18:
                case 0x1B:
                case 0x1E:
                case 0x21:
                case 0x24:
                case 0x27:
                case 0x2A:
                case 0x2D:
                case 0x30:
                case 0x33:
                    // PM 0.3 값   PM 0.5 값   PM 1.0 값   PM 2.5 값   PM 10 값
                    // 온도 값      습도 값      H2 값       CH2O 값     CO 값
                    // CO2 값      TVOC 값     O3 값       NH3 값      H2S 값
                    // CH4 값      C3H8 값     NO2 값
                    //arrayList.add(getFloatValue(tag_content));
                    bundle.putString(id, bytesToStrFloat(tag_content));
                    break;
                case 0x03:
                case 0x05:
                case 0x07:
                case 0x0A:
                case 0x0D:
                case 0x11:
                case 0x13:
                case 0x16:
                case 0x19:
                case 0x1C:
                case 0x1F:
                case 0x22:
                case 0x25:
                case 0x28:
                case 0x2B:
                case 0x2E:
                case 0x31:
                case 0x34:
                    // PM 0.3 등급   PM 0.5 등급   PM 1.0 등급   PM 2.5 등급   PM 10 등급
                    // 온도 등급      습도 등급      H2 등급       CH2O 등급     CO 등급
                    // CO2 등급      TVOC 등급     O3 등급       NH3 등급      H2S 등급
                    // CH4 등급      C3H8 등급     NO2 등급
                    //arrayList.add(getGrade(tag_content));
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x08:
                case 0x0B:
                case 0x0E:
                    // PM 1.0 AQI   PM 2.5 AQI   PM 10 AQI
                    bundle.putShort(id, bytesToShort(tag_content));
                    break;
                case 0x35:
                    // 센서 장착 정보
                    bundle.putString(id, bytesToBinary(tag_content));
                    break;
                case 0x37:
                case 0x38:
                case 0x39:
                    // Status LED On/Off, Fan On/Off
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x3A:
                case 0x3B:
                    // Fan 동작 lvl, Fan 제어(Forced)
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x41:
                    // CAI 값
                    bundle.putString(id, bytesToStrFloat(tag_content));
                    break;
                case 0x42:
                    // CAI 등급
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x43:
                case 0x44:
                    // GPS Latitude, GPS Longitude
                    bundle.putDouble(id, bytesToStrDouble(tag_content));
                    break;
                case 0x45:
                    // 펌웨어 버젼
                    // [0].[1].[2].[3]
                    bundle.putString(id, bytesToCharString(changeByteOrder(tag_content, true)));
                    break;
                case 0x46:
                    // 설치 날짜
                    bundle.putInt(id, bytesToInteger(tag_content));
                    break;
                case 0x47:
                    // 디바이스 타입 분류
                    //bundle.putChar(id, (char) tag_content[0]);
                    bundle.putCharArray(id, new String(changeByteOrder(tag_content, true), StandardCharsets.UTF_8).toCharArray());
                    break;
                case 0x48:
                    // Serial Number
                    bundle.putCharArray(id, new String(changeByteOrder(tag_content, true), StandardCharsets.UTF_8).toCharArray());
                    break;
                case 0x49:
                    // Serial Number
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x50:
                case 0x51:
                case 0x52:
                    // 전원 제어, 시스템 재시작, 동작 모드
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x55:
                case 0x56:
                case 0x57:
                    // 온 타임, 오프 타임, 전송 간격
                    bundle.putShort(id, bytesToShort(tag_content));
                    break;
                case 0x58:
                case 0x59:
                    // 배터리 상태, 배터리 잔량
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x5A:
                case 0x5B:
                    // 펌웨어 버젼
                    // [0]:[1]:[2] => H:m:s
                    bundle.putStringArray(id, bytesToCharStringArray(changeByteOrder(tag_content, true)));
                    break;
                case 0x65:
                    // Fan 동작 lvl, Fan 제어(Forced)
                    bundle.putByte(id, tag_content[0]);
                    break;
                case 0x66:
                case 0x67:
                case 0x69:
                    bundle.putString(id, new String(changeByteOrder(tag_content, true), StandardCharsets.UTF_8));
                    break;
            }
        }
        bundle.putIntegerArrayList("id", arrayList);
        return bundle;
    }

    public static Bundle analyzedControlBody(byte[] recvBody) {
        Bundle bundle = new Bundle();
        int totalLength = recvBody.length;
        int pos = 0;

        while (totalLength != 0) {
            byte tag_id = recvBody[0];
            byte[] tag_length = new byte[]{recvBody[1], recvBody[2]};
            tag_length = changeByteOrder(tag_length, true);
            int length = ByteBuffer.wrap(tag_length).getShort();
            byte[] tag_content = new byte[length];

            System.arraycopy(recvBody, 3, tag_content, 0, length);
            if (length != 1) tag_content = changeByteOrder(tag_content, true);
            pos = pos + 3 + length;

            totalLength = totalLength - (3 + length);

            bundle.putByte(byteArrayToHexString(new byte[]{tag_id}), tag_content[0]);
        }
        return bundle;
    }

    public static byte[] generateTag(byte id, byte[] data) {
        // Unsigned Short = 2 Byte
        byte[] length = ByteBuffer.allocate(BYTE_UNSIGNED_SHORT).putShort((short) data.length).array();
        length = changeByteOrder(length, true);
        data = changeByteOrder(data, true);

        return combineArray(new byte[][]{new byte[]{id}, length, data});
    }

    // 형변환 메소드
    public static byte[] doubleToByteArray(double input) {
        // Double = 8 Byte
        return ByteBuffer.allocate(BYTE_DOUBLE).putDouble(input).array();
    }

    public static byte[] integerToByteArray(int input) {
        // Integer = 4 Byte
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (input >> 24);
        bytes[1] = (byte) (input >> 16);
        bytes[2] = (byte) (input >> 8);
        bytes[3] = (byte) (input);
        return bytes;
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s == null) return null;

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static char[] byteArrayToCharArray(byte[] bytes) {
        char[] chars = new char[bytes.length / 2];

        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (((bytes[(i * 2)] & 0xff) << 8) + (bytes[(i * 2 + 1)] & 0xff));
        }
        return chars;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xff));
        }
        return sb.toString();
    }

    public static String bytesToCharString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%X", b & 0xff));
            sb.append(".");
        }
        return sb.toString();
    }

    public static String[] bytesToCharStringArray(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%X", b & 0xff));
            sb.append(".");
        }
        return sb.toString().split("\\.");
    }

    public static int bytesToInteger(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
        //return (bytes[0] << 24) + ((bytes[1] & 0xFF) << 16) + ((bytes[2] & 0xFF) << 8) + (bytes[3] & 0xFF);
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getShort();
    }

    public static String bytesToStrFloat(byte[] bytes) {
        float value = (ByteBuffer.wrap(bytes)).getFloat();
        String strValue = String.format("%.2f", value);
        return strValue;
    }

    public static Double bytesToStrDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static String bytesToBinary(byte[] bytes) {
        String strHex = byteArrayToHexString(bytes);
        int iHex = Integer.parseInt(strHex, 16);
        String strBinary = Integer.toBinaryString(iHex);

        int len = strBinary.length();
        if (len < SENSOR_EQUIP_COUNT) {
            for (int i = 0; i < SENSOR_EQUIP_COUNT - len; i++) {
                strBinary = "0" + strBinary;
            }
        }

        // 배열 Reverse
        //StringBuilder stringBuilder = new StringBuilder("1111111111111");
        StringBuilder stringBuilder = new StringBuilder(strBinary);
        String reversedBinary = stringBuilder.reverse().toString();

        return reversedBinary;
    }

    public static byte[] changeByteOrder(byte[] value, boolean isLittle) {
        int byteIndex = value.length;
        byte[] orderedByte = new byte[byteIndex];

        if (isLittle == true) {
            for (int i = 0; i < byteIndex; i++) {
                orderedByte[i] = value[byteIndex - (i + 1)];
            }
        } else {
            orderedByte = value;
        }
        return orderedByte;
    }

    public static byte[] combineArray(byte[] headArray, byte[] tailArray) {
        byte[] resultArray = new byte[headArray.length + tailArray.length];
        System.arraycopy(headArray, 0, resultArray, 0, headArray.length);
        System.arraycopy(tailArray, 0, resultArray, headArray.length, tailArray.length);
        return resultArray;
    }

    public static byte[] combineArray(byte[][] bundleOfByte) {
        byte[] tempArray = new byte[MAX_BUFFER];
        int totalLength = 0;
        for (int i = 0; i < bundleOfByte.length; i++) {
            System.arraycopy(bundleOfByte[i], 0, tempArray, totalLength, bundleOfByte[i].length);
            totalLength += bundleOfByte[i].length;
        }
        byte[] resultArray = new byte[totalLength];
        System.arraycopy(tempArray, 0, resultArray, 0, totalLength);
        return resultArray;
    }

    public static Point getScreenSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    public static int[] getStandardSize(Activity activity) {
        Point ScreenSize = getScreenSize(activity);
        float density = activity.getResources().getDisplayMetrics().density;

        int standardSize_x = (int) (ScreenSize.x / density);
        int standardSize_y = (int) (ScreenSize.y / density);

        return new int[]{standardSize_x, standardSize_y};
    }


    public static int getDeviceType(char[] type) {
        int deviceType;
        switch (String.valueOf(type)) {
            case "SI":
//                deviceType = DeviceFragment.DEVICE_TYPE_S;
                deviceType = 1;
                break;
            case "PI":
//                deviceType = DeviceFragment.DEVICE_TYPE_S_PLUS;
                deviceType = 2;
                break;
            case "TI":
//                deviceType = DeviceFragment.DEVICE_TYPE_MINI;
                deviceType = 3;
                break;
            case "MI":
//                deviceType = DeviceFragment.DEVICE_TYPE_PRO;
                deviceType = 4;
                break;
            default:
//                deviceType = DeviceFragment.DEVICE_TYPE_ERROR;
                deviceType = 5;
                break;
        }
        return deviceType;
    }
//
//    public static int getDeviceType(char[] type) {
//        int deviceType;
//        switch (String.valueOf(type)) {
//            case "SI":
//                deviceType = DeviceFragment.DEVICE_TYPE_S;
//                break;
//            case "PI":
//                deviceType = DeviceFragment.DEVICE_TYPE_S_PLUS;
//                break;
//            case "TI":
//                deviceType = DeviceFragment.DEVICE_TYPE_MINI;
//                break;
//            case "MI":
//                deviceType = DeviceFragment.DEVICE_TYPE_PRO;
//                break;
//            default:
//                deviceType = DeviceFragment.DEVICE_TYPE_ERROR;
//                break;
//        }
//        return deviceType;
//    }
}

