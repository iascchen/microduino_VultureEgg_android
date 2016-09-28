package me.iasc.vultureegg.app.ble;

import android.util.Log;
import me.iasc.vultureegg.app.ble.characteristics.MyMeasureValue;

import java.util.UUID;

/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/3/22.
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
public class BleReturnDataProcessor {
    private static final String TAG = BleReturnDataProcessor.class.getSimpleName();

    public final static String EXTRA_SERIAL_MSG = "ble.SERIAL";

    public static String getExraName(final UUID characteristicUuid) {
        String ret = null;
        Log.d(TAG, "BleReturnDataProcessor : " + characteristicUuid.toString());

        // TODO: Please add your code, access more characteristics
        if (MyGattCharacteristic.MD_RX_TX.equals(characteristicUuid)) {
            Log.d(TAG, characteristicUuid.toString());
            ret = EXTRA_SERIAL_MSG;
        }
        return ret;
    }

    public static String process(final String extraName, byte[] value) {
        String ret = null;

        // TODO: Please add your code, access more characteristics
        if (EXTRA_SERIAL_MSG.equals(extraName)) {
            MyMeasureValue mmv = new MyMeasureValue(value);
            ret = mmv.getMessage();
        }

        Log.d(TAG, ret);
        return ret;
    }
}
