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

import android.app.Service;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String EXTRA_ADDRESS = "dev_address";
    public final static String EXTRA_REMOTE_RSSI = "remote_rssi";
    public final static String EXTRA_UUID = "characteristic_uuid";
    public final static String EXTRA_DATA = "return_data";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGatt mBluetoothGatt;
    private int mBluetoothState;

    public final static String ACTION_GATT_CONNECTED = "me.iasc.multiconnectble.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "me.iasc.multiconnectble.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "me.iasc.multiconnectble.ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "me.iasc.multiconnectble.ble.ACTION_DATA_AVAILABLE";
    public final static String ACTION_REMOTE_RSSI = "me.iasc.multiconnectble.ble.ACTION_REMOTE_RSSI";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            String address = gatt.getDevice().getAddress();

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;

                mBluetoothGatt = gatt;
                mBluetoothState = STATE_CONNECTED;

                broadcastUpdate(intentAction);

                Log.i(TAG, "Connected to GATT server:" + address);
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothState = STATE_DISCONNECTED;

                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String address = gatt.getDevice().getAddress();
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            String address = gatt.getDevice().getAddress();
            broadcastUpdate(ACTION_REMOTE_RSSI, rssi);
        }
    };

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 19 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mBluetoothState = STATE_CONNECTING;
                return true;
            } else {
                final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");

        mBluetoothState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Write to a given char
     *
     * @param characteristic The characteristic to write to
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, int remote_rssi) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_REMOTE_RSSI, "" + remote_rssi);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        String characteristicUuid = characteristic.getUuid().toString();
        byte[] value = characteristic.getValue();
        Log.v(TAG, "broadcastUpdate : " + characteristicUuid + " , " + byteArrayToHexString(value));

        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristicUuid);
        intent.putExtra(EXTRA_DATA, value);

        sendBroadcast(intent);
    }

    public BluetoothGattService getGattService(UUID serviceUuid) {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getService(serviceUuid);
    }

    public BluetoothGattCharacteristic getGattCharacteristic(UUID serviceUuid, UUID charUuid) {
        BluetoothGattService _service = getGattService(serviceUuid);

        if (_service == null)
            return null;

        return _service.getCharacteristic(charUuid);
    }

    public boolean enableGattCharacteristicNotification(UUID serviceUuid, UUID charUuid, boolean enable) {
        BluetoothGattCharacteristic _nc = getGattCharacteristic(serviceUuid, charUuid);

        if (_nc == null)
            return false;

        boolean success = mBluetoothGatt.setCharacteristicNotification(_nc, enable);
        Log.v(TAG, "setCharacteristicNotification = " + success);

        BluetoothGattDescriptor _descriptor = _nc.getDescriptor(Descriptor.CLIENT_CHARACTERISTIC_CONFIGURATION);

        if (_descriptor == null)
            return false;

        byte[] val = enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
        _descriptor.setValue(val);
        return mBluetoothGatt.writeDescriptor(_descriptor);
    }

    public boolean readGattCharacteristic(UUID serviceUuid, UUID charUuid) {
        BluetoothGattCharacteristic _nc = getGattCharacteristic(serviceUuid, charUuid);

        if (_nc == null)
            return false;

        readCharacteristic(_nc);
        return true;
    }

    public boolean writeGattCharacteristic(UUID serviceUuid, UUID charUuid, byte[] value) {
        BluetoothGattCharacteristic _nc = getGattCharacteristic(serviceUuid, charUuid);

        if (_nc == null)
            return false;

        _nc.setValue(value);
        writeCharacteristic(_nc);
        return true;
    }

    public boolean readRemoteRssi() {
        if (mBluetoothGatt == null)
            return false;

        mBluetoothGatt.readRemoteRssi();
        return true;
    }

    public static String byteArrayToHexString(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
