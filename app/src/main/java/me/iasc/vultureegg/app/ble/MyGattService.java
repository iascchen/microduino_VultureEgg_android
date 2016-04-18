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
public class MyGattService {
    private static HashMap<UUID, String> attributes = new HashMap<UUID, String>();

    // public final static UUID SOFT_SERIAL_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");

    public static final UUID GENERIC_ACCESS = new UUID((0x1800L << 32) | 0x1000,GattUtils.leastSigBits);

    public final static UUID VULTURE_SERVICE = UUID.fromString("0000f0c0-0000-1000-8000-00805f9b34fb");

    static {
        //attributes.put(SOFT_SERIAL_SERVICE, "Microduino BLE Serial");

        attributes.put(GENERIC_ACCESS, "Generic Access");

        attributes.put(VULTURE_SERVICE, "Microduino VultureEgg Command");
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
