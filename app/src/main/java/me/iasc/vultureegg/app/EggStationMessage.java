package me.iasc.vultureegg.app;

import android.util.Log;

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

public class EggStationMessage {
    private final static String TAG = EggStationMessage.class.getSimpleName();

    static String PI = "Pi";
    static String DEV_ID = "device_id";

    // public static String pi = "Pi;35.2;65.5;1200;1.00";
    static String[] PI_FIELDS = {"type", "Env Temperature", "Env Humidity", "Env Lightness","Env PM 10", "Env Air Pollution"};

    public static ConcurrentHashMap<String, String> parse(String deviceId, String msg) {
        Log.d(TAG, "Parse : " + msg);

        ConcurrentHashMap<String, String> message = new ConcurrentHashMap<String, String>();
        String[] _msgs = msg.split(";");

        String type = _msgs[0];
        if ("Pi".equals(type)) {
            message.clear();

            message.put(DEV_ID, deviceId);

            for (int i = 1; i < PI_FIELDS.length; i++) {
                if (_msgs[i].length() > 0)
                    message.put(PI_FIELDS[i], _msgs[i]);
            }

            return message;
        }

        return null;
    }
}
