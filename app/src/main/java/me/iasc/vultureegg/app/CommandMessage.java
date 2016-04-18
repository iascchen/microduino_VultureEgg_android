package me.iasc.vultureegg.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

public class CommandMessage {
    private final static String TAG = CommandMessage.class.getSimpleName();

    static final int CMD_TEMPLATE = 0xab000100;
    static final int TEM = 1, HUM = 2, MPU = 3;

    static final byte[][] CMD_STOP = {{(byte) 0xab, 0x01, 0x00}, {(byte) 0xab, 0x02, 0x00}, {(byte) 0xab, 0x03, 0x00}};

    static public byte[] getVultureCmd(int mode, int value) {
        int cmd = CMD_TEMPLATE | (mode << 16) | value;

        ByteBuffer ret = ByteBuffer.allocate(4);
        // ret.order(ByteOrder.LITTLE_ENDIAN);
        ret.order(ByteOrder.BIG_ENDIAN);
        ret.putInt(cmd);
        return ret.array();
    }

    static public byte[] getVultureStop(int mode) {
        return CMD_STOP[mode - 1];
    }
}
