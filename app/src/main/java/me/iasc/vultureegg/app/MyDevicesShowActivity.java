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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;
import im.delight.android.ddp.ResultListener;
import me.iasc.vultureegg.app.ble.*;
import me.iasc.vultureegg.app.ble.characteristics.MyMeasureValue;
import me.iasc.vultureegg.app.db.RecordDAO;
import me.iasc.vultureegg.app.db.RecordModel;
import me.iasc.vultureegg.app.db.DeviceModel;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MyDevicesShowActivity extends Activity {
    private static final String TAG = MyDevicesShowActivity.class.getSimpleName();
    private Tracker mTracker;

    public static String SET_INTERVAL_MPU = "Interval_Mpu", SET_INTERVAL_HUM = "Interval_Hum", SET_INTERVAL_TEM = "Interval_Tem";
    public static String SET_ENABLE_MPU = "Enable_Mpu", SET_ENABLE_HUM = "Enable_Hum", SET_ENABLE_TEM = "Enable_Tem";

    public static String CTRL_QUA = "QuaInterval", CTRL_TEM = "TemInterval", CTRL_HUM = "HumInterval";

    public static String M_COTTON_LAN = "192.168.199.240:3000";
    public static String mCottonServer = "ws://192.168.199.240:3000/websocket";

    private static boolean isCottonReady = false;
    private static Meteor mMeteor;

    // private static String email = "iasc@163.com", password = "SucMicro123";
    private static String email = "iasc@163.com", password = "123456";

    public static final String ARG_DEVICES = "all_devices";

    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    private List<DeviceModel> devicelist;

    private BluetoothLeServiceN mBluetoothLeService;

    private ConcurrentHashMap<String, TextView> textViewMap = new ConcurrentHashMap<String, TextView>();
    private ConcurrentHashMap<String, BleDevStatus> statusMap = new ConcurrentHashMap<String, BleDevStatus>();
    private ConcurrentHashMap<String, StringBuilder> buffeerMap = new ConcurrentHashMap<String, StringBuilder>();

    private EditText intervalEditMpu, intervalEditTem, intervalEditHum;
    private CheckBox intervalCheckMpu, intervalCheckTem, intervalCheckHum;
    private Button intervalApply;

    public static int intervalMpu = 2, intervalTem = 70, intervalHum = 20;
    public static boolean enableMpu = true, enableTem = true, enableHum = true;

    private RecordDAO recordDAO = null;

//    private ImageView infoButton;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeServiceN.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.

            if (devicelist != null) {
                for (DeviceModel _d : devicelist) {
                    try {
                        mBluetoothLeService.connect(_d.getAddress());
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (devicelist != null) {
                for (DeviceModel _d : devicelist) {
                    try {
                        mBluetoothLeService.disconnect(_d.getAddress());
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
            mBluetoothLeService = null;
        }
    };

    CheckBox.OnCheckedChangeListener checkboxListener = new CheckBox.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            if (view.getId() == R.id.checkBoxMpu) {
                enableMpu = isChecked;
                intervalEditMpu.setEnabled(isChecked);
            } else if (view.getId() == R.id.checkBoxTem) {
                enableTem = isChecked;
                intervalEditTem.setEnabled(isChecked);
            } else if (view.getId() == R.id.checkBoxHum) {
                enableHum = isChecked;
                intervalEditHum.setEnabled(isChecked);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_devices_show);

        VultureEggApplication application = (VultureEggApplication) getApplication();
        mTracker = application.getDefaultTracker();

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        recordDAO = new RecordDAO(this);

        Intent intent = getIntent();
        devicelist = intent.getParcelableArrayListExtra(ARG_DEVICES);

        getActionBar().setTitle(getString(R.string.title_show));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        intervalEditMpu = (EditText) findViewById(R.id.numberMpu);
        intervalEditTem = (EditText) findViewById(R.id.numberTem);
        intervalEditHum = (EditText) findViewById(R.id.numberHum);

        intervalCheckMpu = (CheckBox) findViewById(R.id.checkBoxMpu);
        intervalCheckTem = (CheckBox) findViewById(R.id.checkBoxTem);
        intervalCheckHum = (CheckBox) findViewById(R.id.checkBoxHum);

        intervalCheckMpu.setOnCheckedChangeListener(checkboxListener);
        intervalCheckTem.setOnCheckedChangeListener(checkboxListener);
        intervalCheckHum.setOnCheckedChangeListener(checkboxListener);

        loadInterval();

        Intent gattServiceIntent = new Intent(this, BluetoothLeServiceN.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        intervalApply = (Button) findViewById(R.id.buttonApply);
        intervalApply.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateInterval(true);
            }
        });

        for (DeviceModel _d : devicelist) {
            String _adr = _d.getAddress();
            Log.v(TAG, "device: " + _adr);

            TextView dynamicTextView = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 10, 0, 0);
            dynamicTextView.setLayoutParams(lp);

            dynamicTextView.setLines(8);
            dynamicTextView.setBackgroundColor(Color.LTGRAY);

            textViewMap.put(_adr, dynamicTextView);

            BleDevStatus status = new BleDevStatus();
            status.name = _d.getName();
            status.deviceId = _d.getDeviceId();
            status.address = _d.getAddress();
            status.type = _d.getType();
            statusMap.put(_adr, status);

            updateTextInfo(_adr);

            layout.addView(dynamicTextView);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        renewMeteor();
    }

    private void loadInterval() {
        intervalMpu = settings.getInt(SET_INTERVAL_MPU, 2);
        intervalTem = settings.getInt(SET_INTERVAL_TEM, 70);
        intervalHum = settings.getInt(SET_INTERVAL_HUM, 20);

        enableMpu = settings.getBoolean(SET_ENABLE_MPU, true);
        enableTem = settings.getBoolean(SET_ENABLE_TEM, true);
        enableHum = settings.getBoolean(SET_ENABLE_HUM, true);

        intervalEditMpu.setText("" + intervalMpu);
        intervalEditTem.setText("" + intervalTem);
        intervalEditHum.setText("" + intervalHum);

        intervalCheckMpu.setChecked(enableMpu);
        intervalCheckTem.setChecked(enableTem);
        intervalCheckHum.setChecked(enableHum);

        Log.v(TAG, "Load Interval : " + intervalMpu + " , " + intervalTem + " , " + intervalHum);
    }

    private void sendControlEvent(String deviceId, String name, String value) {
        Map<String, Object> entity = new HashMap<String, Object>();
        entity.put("device_id", deviceId);
        entity.put("control_name", name);
        entity.put("control_value", value);

        mMeteor.call("controlEventInsert", new Object[]{entity});
    }

    private void updateInterval(boolean sendToMeteor) {
        mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                .setAction("UpdateInterval").build());

        intervalMpu = Integer.parseInt(intervalEditMpu.getText().toString());
        intervalTem = Integer.parseInt(intervalEditTem.getText().toString());
        intervalHum = Integer.parseInt(intervalEditHum.getText().toString());

        enableMpu = intervalCheckMpu.isChecked();
        enableTem = intervalCheckTem.isChecked();
        enableHum = intervalCheckHum.isChecked();

        applyInterval(sendToMeteor);
    }

    private void applyInterval(boolean sendToMeteor) {
        if (intervalMpu < 1) {
            intervalMpu = 0;
            enableMpu = false;
        } else {
            enableMpu = true;
        }

        if (intervalTem < 1) {
            intervalTem = 0;
            enableTem = false;
        } else {
            enableMpu = true;
        }

        if (intervalHum < 1) {
            intervalHum = 0;
            enableHum = false;
        } else {
            enableMpu = true;
        }

        Log.v(TAG, "Apply Interval : " + intervalMpu + " , " + intervalTem + " , " + intervalHum);

        editor = settings.edit();
        editor.putInt(SET_INTERVAL_MPU, intervalMpu);
        editor.putInt(SET_INTERVAL_TEM, intervalTem);
        editor.putInt(SET_INTERVAL_HUM, intervalHum);

        editor.putBoolean(SET_ENABLE_MPU, enableMpu);
        editor.putBoolean(SET_ENABLE_TEM, enableTem);
        editor.putBoolean(SET_ENABLE_HUM, enableHum);
        editor.commit();

        intervalEditMpu.setText("" + intervalMpu);
        intervalEditTem.setText("" + intervalTem);
        intervalEditHum.setText("" + intervalHum);

        intervalCheckMpu.setChecked(enableMpu);
        intervalCheckTem.setChecked(enableTem);
        intervalCheckHum.setChecked(enableHum);

        String label = intervalHum + "," + intervalCheckMpu + "," + intervalCheckTem;
        mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                .setAction("ApplyInterval").setLabel(label).build());

        for (DeviceModel _d : devicelist) {
            String _adr = _d.getAddress();
            if (_d.getType().equals(DeviceModel.TYPE_EGG)) {
                SetIntervalTask task = new SetIntervalTask();
                task.execute(_adr);

                if (sendToMeteor) {
                    sendControlEvent(_d.getDeviceId(), CTRL_QUA, "" + intervalMpu);
                    sendControlEvent(_d.getDeviceId(), CTRL_TEM, "" + intervalTem);
                    sendControlEvent(_d.getDeviceId(), CTRL_HUM, "" + intervalHum);
                }
            }
        }
    }

    private void renewMeteor() {
        if (mMeteor == null) {
            try {
                mCottonServer = "ws://" + settings.getString(SettingActivity.SET_M_COTTON, M_COTTON_LAN) + "/websocket";
                email = settings.getString(SettingActivity.SET_M_MCOTTON_USER, "");
                password = settings.getString(SettingActivity.SET_M_MCOTTON_PASSWORD, "");

                Log.d(TAG, "mCottonServer init : " + mCottonServer);

                mMeteor = new Meteor(this, mCottonServer);
                mMeteor.addCallback(mMeteorCallback);
                mMeteor.connect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (mMeteor.isLoggedIn()) {
                isCottonReady = true;
            } else {
                isCottonReady = false;
            }
        }
    }

    final MeteorCallback mMeteorCallback = new MeteorCallback() {
        @Override
        public void onConnect(boolean signedInAutomatically) {
            Log.v(TAG, "mCottonServer onConnect");

            mMeteor.loginWithEmail(email, password, new ResultListener() {
                @Override
                public void onSuccess(String s) {
                    Log.v(TAG, "mCottonServer Logon");
                    isCottonReady = true;

                    Toast.makeText(getApplicationContext(), "mCotton Logon", Toast.LENGTH_LONG).show();

                    if (devicelist != null) {
                        ArrayList<String> device_ids = new ArrayList<String>();

                        for (DeviceModel _d : devicelist) {
                            try {
                                device_ids.add(_d.getDeviceId());
                            } catch (Exception e) {
                                // Ignore
                            }
                        }
                        mMeteor.subscribe("devicesControlEventsLater", new Object[]{device_ids.toArray()});
                    }
                }

                @Override
                public void onError(String s, String s1, String s2) {
                    Log.v(TAG, "Login Error : " + s);
                    Toast.makeText(getApplicationContext(), "Please check your mCotton account", Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onDisconnect() {
            Log.v(TAG, "mCottonServer onDisconnect");
            isCottonReady = false;
        }

        @Override
        public void onDataAdded(String collectionName, String documentID, String newValuesJson) {
            Log.v(TAG, "mCottonServer onDataAdded:" + collectionName + " ==> " + newValuesJson);

            if (collectionName.equals("controlevents")) {
                try {
                    JSONObject obj = new JSONObject(newValuesJson);
                    int value = obj.getInt("control_value");
                    String name = obj.getString("control_name");

                    boolean changed = false;
                    if (CTRL_QUA.equals(name) && (intervalMpu != value)) {
                        intervalMpu = value;
                        changed = true;
                    } else if (CTRL_TEM.equals(name) && (intervalTem != value)) {
                        intervalTem = value;
                        changed = true;
                    } else if (CTRL_HUM.equals(name) && (intervalHum != value)) {
                        intervalHum = value;
                        changed = true;
                    }

                    if (changed) {
                        applyInterval(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDataChanged(String collectionName, String documentID, String updatedValuesJson, String removedValuesJson) {
            Log.v(TAG, "mCottonServer onDataChanged:" + collectionName);
        }

        @Override
        public void onDataRemoved(String collectionName, String documentID) {
            Log.v(TAG, "mCottonServer onDataRemoved:" + collectionName);
        }

        @Override
        public void onException(Exception e) {
            Log.v(TAG, "mCottonServer onException:" + e);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String newServer = "ws://" + settings.getString(SettingActivity.SET_M_COTTON, M_COTTON_LAN) + "/websocket";
        if (!mCottonServer.equals(newServer)) {
            Log.d(TAG, "mCottonServer changed: " + newServer);

            mCottonServer = newServer;
            mMeteor.disconnect();
            mMeteor = null;

            renewMeteor();
        }

        mTracker.setScreenName("MyDevicesShowActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mGattUpdateReceiver);

        super.onDestroy();

        if (mServiceConnection != null)
            unbindService(mServiceConnection);

        if (mBluetoothLeService != null && devicelist != null) {
            for (DeviceModel _d : devicelist) {
                try {
                    mBluetoothLeService.disconnect(_d.getAddress());
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        mBluetoothLeService = null;

        if (mMeteor != null && mMeteor.isConnected()) {
            mMeteor.disconnect();
        }
        mMeteor = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeServiceN.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeServiceN.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeServiceN.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeServiceN.ACTION_DATA_AVAILABLE);
//        intentFilter.addAction(BluetoothLeServiceN.ACTION_REMOTE_RSSI);
        return intentFilter;
    }

    private void updateConnectionState(final String adr, final boolean value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BleDevStatus si = statusMap.get(adr);
                si.connected = value;

                updateTextInfo(adr);
            }
        });
    }

    private StringBuilder getBuffer(String adr) {
        StringBuilder buffer = buffeerMap.get(adr);
        if (buffer == null) {
            buffer = new StringBuilder();
            buffeerMap.put(adr, buffer);
        }
        return buffer;
    }

    private void resetBuffer(String adr) {
        StringBuilder buffer = buffeerMap.get(adr);
        if (buffer != null) {
            buffeerMap.remove(adr);
        }
    }

    public void updateTextInfo(final String adr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textview = textViewMap.get(adr);
                BleDevStatus si = statusMap.get(adr);
                textview.setText(si.toString());

                String tmpmsg = si.getProperty(BleReturnDataProcessor.EXTRA_SERIAL_MSG);

                if (tmpmsg != null) {

                    if (DeviceModel.TYPE_EGG.equals(si.type)) {
                        StringBuilder buffer = getBuffer(adr);

                        buffer.append(tmpmsg);
                        // Log.d(TAG, "message buffer: " + buffer.toString());

                        int start = buffer.indexOf(EggMessage.MSG_START);     //msg start
                        if (start >= 0) {
                            int end = buffer.indexOf(EggMessage.MSG_END, start);   //msg end

                            if (end >= 0) {
                                String msg = buffer.substring(start, end + EggMessage.MSG_END.length());
                                Log.d(TAG, "message data  : " + msg);

                                buffer = new StringBuilder(buffer.substring(end + EggMessage.MSG_END.length()));
                                buffeerMap.put(adr, buffer);

                                ConcurrentHashMap map = EggMessage.parse(si.deviceId, msg);
                                saveMessageToDB(recordDAO, map);
                                sendMessageToMCotton(mMeteor, map);

                                textview.setText(si.toString() + "\n" + msg);
                            }
                        }
                    } else if (DeviceModel.TYPE_STATION.equals(si.type)) {

                        String txt = new String(MyMeasureValue.hexToBytes(tmpmsg));

                        StringBuilder buffer = getBuffer(adr);
                        buffer.append(txt);
                        // Log.d(TAG, "message buffer: " + buffer.toString());

                        if (txt.endsWith("\n")) {
                            String msg = buffer.toString().trim();
                            Log.d(TAG, "message data: " + msg);

                            if (msg.startsWith(EggStationMessage.PI)) {
                                ConcurrentHashMap map = EggStationMessage.parse(si.deviceId, msg);

                                saveMessageToDB(recordDAO, map);
                                sendMessageToMCotton(mMeteor, map);

                                textview.setText(si.toString() + "\n" + msg);
                            }

                            buffeerMap.remove(adr);
                        }
                    }
                }
            }
        });
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String address = intent.getStringExtra(BluetoothLeServiceN.EXTRA_ADDRESS);

            // Log.v(TAG, "BroadcastReceiver : " + action);

            if (BluetoothLeServiceN.ACTION_GATT_CONNECTED.equals(action)) {
                Log.v(TAG, "BroadcastReceiver.ACTION_GATT_CONNECTED");
            } else if (BluetoothLeServiceN.ACTION_GATT_DISCONNECTED
                    .equals(action)) {

                Log.v(TAG, "BroadcastReceiver.ACTION_GATT_DISCONNECTED");
                updateConnectionState(address, false);

            } else if (BluetoothLeServiceN.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {

                Log.v(TAG, "BroadcastReceiver.ACTION_GATT_SERVICES_DISCOVERED");

                EnableNotificationTask task = new EnableNotificationTask();
                task.execute(address);

            } else if (BluetoothLeServiceN.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.v(TAG, "BroadcastReceiver.ACTION_DATA_AVAILABLE : \n" + intent);

                final String _address = intent.getStringExtra(BluetoothLeServiceN.EXTRA_ADDRESS);
                BleDevStatus _si = statusMap.get(address);

                String _uuidStr = intent.getStringExtra(BluetoothLeServiceN.EXTRA_UUID);
                UUID _characteristicUuid = UUID.fromString(_uuidStr);
                String _extra = BleReturnDataProcessor.getExraName(_characteristicUuid);
                if (_extra == null) return;

                byte[] _value = intent.getByteArrayExtra(BluetoothLeServiceN.EXTRA_DATA);

                String _data = BleReturnDataProcessor.process(_extra, _value);
                if (_data != null) {
                    Log.v(TAG, "BroadcastReceiver.ACTION_DATA_AVAILABLE : \n" + _extra + " , " + _data + " , " + _address);
                    _si.props.put(_extra, _data);
                }

                updateTextInfo(_address);
            }
        }
    };

    private void iascDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.iasc_dialog,
                (ViewGroup) findViewById(R.id.dialog));
        new AlertDialog.Builder(this).setView(layout)
                .setPositiveButton("OK", null).show();
    }

    private class EnableNotificationTask extends BleAsyncTask {
        private final int WAIT_INTERVAL = 3000;

        private String address;

        protected int getInterval() {
            return WAIT_INTERVAL;
        }

        @Override
        protected String doInBackground(String... params) {
            address = params[0];
            Log.v(TAG, "EnableNotificationTask doInBackground call :" + address);
            BleDevStatus si = statusMap.get(address);

            boolean ret = false;

            // TODO: Please add your code, enable ble notification

            // Enable notify

            if (DeviceModel.TYPE_STATION.equals(si.type)) {
                if (mBluetoothLeService == null) return "Failed";
                ret = mBluetoothLeService.enableGattCharacteristicNotification(address,
                        MyGattService.SOFT_SERIAL_SERVICE, MyGattCharacteristic.MD_RX_TX, true);
                if (ret) waitIdle();
                else Log.v(TAG, "Error Enable Microduino Serial");
            } else if (DeviceModel.TYPE_EGG.equals(si.type)) {

                if (mBluetoothLeService == null) return "Failed";
                ret = mBluetoothLeService.enableGattCharacteristicNotification(address,
                        MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_TRANS, true);
                if (ret) waitIdle();
                else Log.v(TAG, "Error Enable COMMAND_DATA");
            }

            return "Done";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "EnableNotificationTask onPostExecute called :" + address + " , " + result);

            BleDevStatus si = statusMap.get(address);

            if (DeviceModel.TYPE_EGG.equals(si.type)) {
                SetIntervalTask task = new SetIntervalTask();
                task.execute(address);
            }
        }
    }

    private class SetIntervalTask extends BleAsyncTask {
        private final int WAIT_INTERVAL = 3000;

        private String address;

        protected int getInterval() {
            return WAIT_INTERVAL;
        }

        private boolean sendCommand(String adr, boolean enable, int mode, int value) {

            if (mBluetoothLeService == null) return false;

            boolean ret = mBluetoothLeService.writeGattCharacteristicNoResponse(adr,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                    CommandMessage.getVultureStop(mode));
            if (ret) waitIdle();
            else return false;

            if (enable) {
                if (mBluetoothLeService == null) return false;

                ret = mBluetoothLeService.writeGattCharacteristicNoResponse(adr,
                        MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                        CommandMessage.getVultureCmd(mode, value));
            }
            return ret;
        }

        @Override
        protected String doInBackground(String... params) {
            address = params[0];
            Log.v(TAG, "EnableNotificationTask doInBackground call :" + address);

            boolean ret = false;

            // TODO: Please add your code, enable ble notification
            if (mBluetoothLeService != null) {
                // Write CMD
                Log.v(TAG, "SetIntervalTask Interval : " + intervalMpu + " , " + intervalTem + " , " + intervalHum);

                ret = sendCommand(address, enableMpu, CommandMessage.MPU, intervalMpu);
                if (ret) waitIdle();
                else Log.v(TAG, "MPU_Setting Error");

                ret = sendCommand(address, enableTem, CommandMessage.TEM, intervalTem);
                if (ret) waitIdle();
                else Log.v(TAG, "TEM_Setting Error");

                ret = sendCommand(address, enableHum, CommandMessage.HUM, intervalHum);
                if (ret) waitIdle();
                else Log.v(TAG, "HUM_Setting Error");
            }
            return "Done";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "SetIntervalTask onPostExecute called :" + address + " , " + result);
            updateConnectionState(address, true);
        }
    }

    public static void sendMessageToMCotton(Meteor meteor, ConcurrentHashMap msg) {
        if (isCottonReady) {
            //Log.v(TAG, "sendMessageToMCotton : " + msg);
            meteor.call("dataMessageInsert", new Object[]{msg});
        }
    }

    public static void saveMessageToDB(RecordDAO recordDAO, ConcurrentHashMap msg) {
        String key, value;
        StringBuilder valueBuffer = new StringBuilder();
        String dev_id = (String) msg.get("device_id");

        for (int i = 0; i < RecordModel.DATA_MAP_KEYS.length; i++) {
            key = RecordModel.DATA_MAP_KEYS[i];

            value = (String) msg.get(key);
            valueBuffer.append(RecordModel.DUMP_SEPRATOR);
            if (value != null) {
                valueBuffer.append(value.trim());
            }
        }

        String values = valueBuffer.toString();
        Log.v(TAG, "saveMessageToDB : " + values);

        RecordModel entity = new RecordModel();
        entity.setDeviceId(dev_id);
        entity.setValue(values);
        recordDAO.save(entity);
    }
}