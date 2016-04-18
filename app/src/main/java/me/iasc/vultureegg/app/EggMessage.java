package me.iasc.vultureegg.app;

import android.util.Log;
import me.iasc.vultureegg.app.ble.GattUtils;
import me.iasc.vultureegg.app.ble.characteristics.MyMeasureValue;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/10/24.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class EggMessage {
    private final static String TAG = EggMessage.class.getSimpleName();
    static int DATA_LEN = 4;
    static int DATA_START_INDEX = 6;

    static String DEV_ID = "device_id";

    static String TEMPERATURE = "Temperature ";
    static int TEMPERATURE_NUM = 16;

    static String QUATERNION = "Quaternion ";
    static int QUATERNION_NUM = 4;

    static String HUMIDITY = "Humidity";
    static int HUMIDITY_NUM = 1;

    static String MSG_START = "aabb";
    static String MSG_END = "0d0a";

    static String MSG_CMD_MPU = "aa";
    static String MSG_CMD_TEM = "bb";
    static String MSG_CMD_HUM = "cc";

    public static String hex2Quat(String hexStr) {
        // Log.d(TAG, "Parse hex2Quat: " + hexStr);

        byte[] b = MyMeasureValue.hexToBytes(hexStr);
        float value = (float) GattUtils.getIntValue(b, GattUtils.FORMAT_SINT16, 0);
        float ret = value / 16384.0f;
        // Log.d(TAG, "Parse hex2Quat: " + ret);

        return String.format("%.03f", ret);
    }

    public static String hex2Tem(String hexStr) {
        // Log.d(TAG, "Parse hex2Tem: " + hexStr);

        byte[] b = MyMeasureValue.hexToBytes(hexStr);
        float ret = 0.125f * (GattUtils.getIntValue(b, GattUtils.FORMAT_SINT16, 0) >> 5);
        // Log.d(TAG, "Parse hex2Tem: " + ret);

        return String.format("%.03f", ret);
    }

    public static String hex2Hum(String hexStr) {
        // Log.d(TAG, "Parse hex2Hum: " + hexStr);

        byte[] b = MyMeasureValue.hexToBytes(hexStr);
        float value = (float) GattUtils.getIntValue(b, GattUtils.FORMAT_UINT16, 0);
        float ret = -6.0f + 125.0f * (value / 65535.0f);
        // Log.d(TAG, "Parse hex2Hum: " + ret);

        return String.format("%.03f", ret);
    }

    public static ConcurrentHashMap<String, String> parse(String deviceId, String hexMsg) {
        Log.d(TAG, "Parse : " + hexMsg);

        ConcurrentHashMap<String, String> message = new ConcurrentHashMap<String, String>();

        message.clear();
        message.put(DEV_ID, deviceId);

        String cmd = hexMsg.substring(4, DATA_START_INDEX);
        // Log.d(TAG, "Parse cmd: " + cmd);

        int index_start = 0;
        if (MSG_CMD_MPU.equals(cmd) && hexMsg.length() == 38) {
            for (int i = 0; i < QUATERNION_NUM; i++) {
                index_start = DATA_START_INDEX + (i + 3) * DATA_LEN;    // Skip 3 number: acc_x, acc_y, acc_z
                try {
                    String t = hexMsg.substring(index_start, index_start + DATA_LEN);
                    message.put(QUATERNION + String.format("%01d", i + 1), hex2Quat(t));
                } catch (Exception e) {
                    e.printStackTrace();
                    //ignore
                }
            }
        } else if (MSG_CMD_TEM.equals(cmd) && hexMsg.length() == 74) {
            for (int i = 0; i < TEMPERATURE_NUM; i++) {
                index_start = DATA_START_INDEX + i * DATA_LEN;
                try {
                    String t = hexMsg.substring(index_start, index_start + DATA_LEN);
                    message.put(TEMPERATURE + String.format("%02d", i + 1), hex2Tem(t));
                } catch (Exception e) {
                    e.printStackTrace();
                    //ignore
                }
            }
        } else if (MSG_CMD_HUM.equals(cmd) && hexMsg.length() == 14) {
            for (int i = 0; i < HUMIDITY_NUM; i++) {
                index_start = DATA_START_INDEX + i * DATA_LEN;
                try {
                    String t = hexMsg.substring(index_start, index_start + DATA_LEN);
                    message.put(HUMIDITY, hex2Hum(t));
                } catch (Exception e) {
                    e.printStackTrace();
                    //ignore
                }
            }
        }

        return message;
    }
}
