/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/3/19.
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

package me.iasc.vultureegg.app.ble;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class MyGattCharacteristic {
    private static HashMap<UUID, String> attributes = new HashMap<UUID, String>();

    //public final static UUID MD_RX_TX = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");

    public static final UUID PERIPERAL_PREFFERED_CONNECTION_PARAMETERS = new UUID((0x2A04L << 32) | 0x1000, GattUtils.leastSigBits);

    public final static UUID COMMAND_CMD = UUID.fromString("0000f0c1-0000-1000-8000-00805f9b34fb");
    public final static UUID COMMAND_TRANS = UUID.fromString("0000f0c2-0000-1000-8000-00805f9b34fb");

    static {
        // attributes.put(MD_RX_TX, "Microduino BLE Serial");

        // attributes.put(PERIPERAL_PREFFERED_CONNECTION_PARAMETERS, "Peripheral Preferred Connection Parameters");

        attributes.put(COMMAND_CMD, "VultureEgg Command.Cmd");
        attributes.put(COMMAND_TRANS, "VultureEgg Command.Data");
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
