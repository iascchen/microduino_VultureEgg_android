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

package me.iasc.vultureegg.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Properties;

public class BleDevStatus {
//    static final byte[] DISABLE = {0x00};
//    static final byte[] ENABLE = {0x01};

    public String name, deviceId, address, type;
    public boolean connected = false;
    public Properties props = new Properties();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Name: ").append(name).append(" ; ")
                .append("DevID: ").append(deviceId).append(" ; ")
                .append("Adr: ").append(address).append(" ; ")
//                .append("Type: ").append(type).append(" ; ")
                .append("Connected: ").append(connected).append("\n");

//        for (Object _key : props.keySet()) {
//            sb.append(_key).append(": ").append(props.getProperty((String) _key)).append(" ; ");
//        }

        return sb.toString();
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }
}