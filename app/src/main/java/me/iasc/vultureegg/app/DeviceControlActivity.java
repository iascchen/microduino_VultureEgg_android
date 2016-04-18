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
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;
import im.delight.android.ddp.ResultListener;
import im.delight.android.ddp.db.memory.InMemoryDatabase;
import me.iasc.vultureegg.app.ble.*;
import me.iasc.vultureegg.app.ble.characteristics.MyMeasureValue;
import me.iasc.vultureegg.app.db.DeviceDAO;
import me.iasc.vultureegg.app.db.DeviceModel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private Tracker mTracker;

    public static String mCottonServer = "ws://192.168.199.240:3000/websocket";

    private static boolean isCottonReady = false;
    private static Meteor mMeteor;
    private static InMemoryDatabase meteorDb;

    public static String EGG_PROJECT_NAME = "Vulture Egg", STATION_PROJECT_NAME = "Vulture Egg Station";
    public static String[] PROJECT_NAMES = new String[]{EGG_PROJECT_NAME, STATION_PROJECT_NAME};

    public static String M_COTTON_LAN = "192.168.199.240:3000";

    private static String email = "iasc@163.com", password = "123456";

    private Map mCottonDevices = new HashMap<String, String>();
    private ArrayList spinnerList = new ArrayList<String>();

    public static final String ARG_DEVICE = "curr_device";

    static SharedPreferences settings;

    private DeviceModel currDevice;
    private String currDeviceAddress;

    private BleDevStatus devStatus;
    private StringBuilder buffer = new StringBuilder();

    private static DeviceDAO deviceDAO = null;

    private EditText editName, editDeviceId;
    private RadioGroup radioTypeGroup;
    private RadioButton radioEgg, radioStation;

    private TextView isSerial, mConnectionState, status;
    private ImageButton buttonDelete, buttonSave;
    private ImageView infoButton;
    private Spinner devSpinner;
    private ArrayAdapter<String> devSpinnerAdapter;

    private BluetoothLeServiceN mBluetoothLeService;
    private boolean mConnected = false;

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

            try {
                mBluetoothLeService.connect(currDevice.getAddress());
                // mBluetoothLeService.waitIdle();
            } catch (Exception e) {
                // Ignore
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            try {
                mBluetoothLeService.disconnect(currDevice.getAddress());
                // mBluetoothLeService.waitIdle();
            } catch (Exception e) {
                // Ignore
            }

            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            String address = intent.getStringExtra(BluetoothLeServiceN.EXTRA_ADDRESS);
            assert (currDeviceAddress.equals(address));

            if (BluetoothLeServiceN.ACTION_GATT_CONNECTED.equals(action)) {

                mConnected = true;
                updateConnectionState(true);
                invalidateOptionsMenu();

            } else if (BluetoothLeServiceN.ACTION_GATT_DISCONNECTED.equals(action)) {

                mConnected = false;
                updateConnectionState(false);
                invalidateOptionsMenu();

            } else if (BluetoothLeServiceN.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                Log.v(TAG, "BroadcastReceiver.ACTION_GATT_SERVICES_DISCOVERED");

                EnableNotificationTask task = new EnableNotificationTask();
                task.execute(address);

            } else if (BluetoothLeServiceN.ACTION_DATA_AVAILABLE.equals(action)) {

                String uuidStr = intent.getStringExtra(BluetoothLeServiceN.EXTRA_UUID);
                UUID characteristicUuid = UUID.fromString(uuidStr);

                String extra = BleReturnDataProcessor.getExraName(characteristicUuid);
                if (extra == null) return;

                byte[] value = intent.getByteArrayExtra(BluetoothLeServiceN.EXTRA_DATA);

                String data = BleReturnDataProcessor.process(extra, value);
                if (data != null) {
                    Log.v(TAG, "BroadcastReceiver.ACTION_DATA_AVAILABLE : \n" + extra + " , " + data);

                    devStatus.props.put(extra, data);
                }

                updateTextInfo();
            } else if (BluetoothLeServiceN.ACTION_REMOTE_RSSI.equals(action)) {
                String rssi = intent.getStringExtra(BluetoothLeServiceN.EXTRA_REMOTE_RSSI);
                devStatus.props.put(BluetoothLeServiceN.EXTRA_REMOTE_RSSI, rssi);
                updateTextInfo();
            }
        }
    };

    View.OnClickListener onClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v == buttonDelete) {
                Log.v(TAG, "buttonDel Clicked");

                mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                        .setAction("DeviceDelete").setLabel(currDevice.getType()).build());

                deviceDAO.deleteDevice(currDevice);

                Intent returnIntent = new Intent();
                setResult(RESULT_OK, returnIntent);
                finish();
            } else if (v == buttonSave) {
                Log.v(TAG, "buttonSave Clicked");

                updateDeviceInfo();
                deviceDAO.update(currDevice);

                Intent returnIntent = new Intent();
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        }
    };

    private void renewMeteor() {
        if (mMeteor == null) {
            Log.v(TAG, "mCottonServer mMeteor new");

            try {
                mCottonServer = "ws://" + settings.getString(SettingActivity.SET_M_COTTON, M_COTTON_LAN) + "/websocket";
                email = settings.getString(SettingActivity.SET_M_MCOTTON_USER, "");
                password = settings.getString(SettingActivity.SET_M_MCOTTON_PASSWORD, "");

                Log.d(TAG, "mCottonServer init : " + mCottonServer);

                // mMeteor = new Meteor(this, mCottonServer);
                meteorDb = new InMemoryDatabase();
                mMeteor = new Meteor(this, mCottonServer, meteorDb);
                mMeteor.addCallback(mMeteorCallback);
                mMeteor.connect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.v(TAG, "mCottonServer mMeteor old");

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
                    Toast.makeText(getApplicationContext(), "mCotton Logon", Toast.LENGTH_LONG).show();

                    isCottonReady = true;
                    mMeteor.subscribe("projectDevicesOfMine", new Object[]{PROJECT_NAMES});
                }

                @Override
                public void onError(String s, String s1, String s2) {
                    Log.v(TAG, "Login Error : " + s);
                    Toast.makeText(getApplicationContext(), "Please check your mCotton account", Toast.LENGTH_LONG).show();

                    isCottonReady = false;
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
            Log.v(TAG, "mCottonServer onDataAdded:" + collectionName + " ==> " + documentID + " , " + newValuesJson);

            if (collectionName.equals("devices")) {
                try {
                    JSONObject obj = new JSONObject(newValuesJson);
                    String name = obj.getString("name");

                    mCottonDevices.put(name, documentID);
                    if (!spinnerList.contains(name)) {
                        devSpinnerAdapter.add(name);
                        devSpinnerAdapter.notifyDataSetChanged();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        VultureEggApplication application = (VultureEggApplication) getApplication();
        mTracker = application.getDefaultTracker();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        deviceDAO = new DeviceDAO(this);

        final Intent intent = getIntent();
        currDevice = intent.getParcelableExtra(ARG_DEVICE);

        currDeviceAddress = currDevice.getAddress();
        mCottonDevices.put(currDevice.getName(), currDevice.getDeviceId());
        spinnerList.add(currDevice.getName());

        devStatus = new BleDevStatus();
        devStatus.name = currDevice.getName();
        devStatus.deviceId = currDevice.getDeviceId();
        devStatus.address = currDevice.getAddress();
        devStatus.type = currDevice.getType();

        // Sets up UI references.
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        // is serial present?
        isSerial = (TextView) findViewById(R.id.isSerial);

        status = (TextView) findViewById(R.id.status);

        editName = (EditText) findViewById(R.id.editName);
        editDeviceId = (EditText) findViewById(R.id.editDeviceID);

        devSpinner = (Spinner) findViewById(R.id.devIdSpinner);
        devSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerList);
        devSpinner.setAdapter(devSpinnerAdapter);

        devSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                String devName = devSpinnerAdapter.getItem(pos);
                // Log.v(TAG, devName);
                String devId = (String) mCottonDevices.get(devName);
                editName.setText(devName);
                editDeviceId.setText(devId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        radioTypeGroup = (RadioGroup) findViewById(R.id.radioType);
        radioEgg = (RadioButton) radioTypeGroup.findViewById(R.id.eggRadio);
        radioStation = (RadioButton) radioTypeGroup.findViewById(R.id.stationRadio);

        buttonSave = (ImageButton) findViewById(R.id.buttonSave);
        buttonSave.setOnClickListener(onClickListener);

        buttonDelete = (ImageButton) findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(onClickListener);

        infoButton = (ImageView) findViewById(R.id.infoImage);
        infoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                iascDialog();
            }
        });

        getActionBar().setTitle(currDevice.getName());
        getActionBar().setDisplayHomeAsUpEnabled(true);

        showDeviceInfo(currDevice);

        Intent gattServiceIntent = new Intent(this, BluetoothLeServiceN.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        renewMeteor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buffer = new StringBuilder();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(currDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        mTracker.setScreenName("DeviceControlActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);

        updateDeviceInfo();
        deviceDAO.update(currDevice);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        if (mMeteor != null && mMeteor.isConnected()) {
            mMeteor.disconnect();
        }
        mMeteor = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(currDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect(currDeviceAddress);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeServiceN.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeServiceN.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeServiceN.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeServiceN.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(final boolean val) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (val)
                    mConnectionState.setText(R.string.connected);
                else
                    mConnectionState.setText(R.string.disconnected);

                devStatus.connected = val;

                updateTextInfo();
            }
        });
    }

    public void updateTextInfo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String tmpmsg = devStatus.getProperty(BleReturnDataProcessor.EXTRA_SERIAL_MSG);
                Log.d(TAG, "message tmp: " + tmpmsg);

                if (tmpmsg != null) {
                    if (DeviceModel.TYPE_EGG.equals(devStatus.type)) {

                        buffer.append(tmpmsg);
                        // Log.d(TAG, "message buffer: " + buffer.toString());

                        int start = buffer.indexOf(EggMessage.MSG_START);     //msg start
                        if (start >= 0) {
                            int end = buffer.indexOf(EggMessage.MSG_END, start);   //msg end

                            if (end >= 0) {
                                String msg = buffer.substring(start, end + EggMessage.MSG_END.length());
                                Log.d(TAG, "message data  : " + msg);

                                buffer = new StringBuilder(buffer.substring(end + EggMessage.MSG_END.length()));

                                ConcurrentHashMap map = EggMessage.parse(currDevice.getDeviceId(), msg);
                                status.setText(new JSONObject(map).toString());
                            }
                        }
                    } else if (DeviceModel.TYPE_STATION.equals(devStatus.type)) {
                        String txt = new String(MyMeasureValue.hexToBytes(tmpmsg));

                        buffer.append(txt);
                        // Log.d(TAG, "message buffer: " + buffer.toString());

                        if (txt.endsWith("\n")) {
                            String msg = buffer.toString().trim();
                            Log.d(TAG, "message data: " + msg);

                            if (msg.startsWith(EggStationMessage.PI)) {
                                ConcurrentHashMap map = EggStationMessage.parse(currDevice.getDeviceId(), msg);
                                status.setText(new JSONObject(map).toString());
                            }
                            buffer = new StringBuilder();
                        }
                    }
                } else {
                    status.setText(devStatus.toString());
                }
            }
        });
    }

    private void iascDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.iasc_dialog,
                (ViewGroup) findViewById(R.id.dialog));
        new AlertDialog.Builder(this).setView(layout)
                .setPositiveButton("OK", null).show();
    }

    public void showDeviceInfo(DeviceModel device) {
        Log.d(TAG, device.toString());

        String _name = device.getName();
        if (_name == null || _name.equals("")) {
            _name = getString(R.string.name);
        }
        editName.setText(_name);

        String _deviceId = device.getDeviceId();
        if (_deviceId == null || _deviceId.equals("")) {
            _deviceId = getString(R.string.deviceId);
        }
        editDeviceId.setText(_deviceId);

        String _type = device.getType();
        if (DeviceModel.TYPE_STATION.equals(_type))
            radioStation.setChecked(true);
        else
            radioEgg.setChecked(true);

        //editName.setFocusable(false);
        //editName.selectAll();
    }

    public void updateDeviceInfo() {
        currDevice.setName(editName.getText().toString());
        currDevice.setDeviceId(editDeviceId.getText().toString());
        currDevice.setType(radioEgg.isChecked() ? DeviceModel.TYPE_EGG : DeviceModel.TYPE_STATION);

        mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                .setAction("DeviceSave").setLabel(currDevice.getType()).build());

        Log.d(TAG, currDevice.toString());
    }

    /**
     * EnableNotificationTask       enable all BEL notification, and should be read BLECharacteristics.
     * <p/>
     * If you want to start some timer, please add them in method onPostExecute
     * <p/>
     * <code>
     * private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
     *
     * @Override public void onReceive(Context context, Intent intent) {
     * final String action = intent.getAction();
     * String address = intent.getStringExtra(BluetoothLeServiceN.EXTRA_ADDRESS);
     * assert (currDeviceAddress.equals(address));
     * <p/>
     * if (BluetoothLeServiceN.ACTION_GATT_CONNECTED.equals(action)) {
     * ...
     * }
     * else if (BluetoothLeServiceN.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
     * <p/>
     * Log.v(TAG, "BroadcastReceiver.ACTION_GATT_SERVICES_DISCOVERED");
     * <p/>
     * EnableNotificationTask task = new EnableNotificationTask();
     * task.execute(address);
     * }
     * </code>
     */
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

            boolean ret = false;

            // TODO: Please add your code, enable ble notification

            // Enable notify
            if (mBluetoothLeService == null) return "Failed";

            ret = mBluetoothLeService.enableGattCharacteristicNotification(address,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_TRANS, true);
            if (ret) waitIdle();
            else Log.v(TAG, "Error Enable COMMAND_DATA");

            // Write CMD

            // MPU

            if (mBluetoothLeService == null) return "Failed";
            ret = mBluetoothLeService.writeGattCharacteristicNoResponse(address,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                    CommandMessage.getVultureStop(CommandMessage.MPU));

            if (ret) waitIdle();
            else Log.v(TAG, "Error MPU_STOP");

            if (mBluetoothLeService == null) return "Failed";
            ret = mBluetoothLeService.writeGattCharacteristicNoResponse(address,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                    CommandMessage.getVultureCmd(CommandMessage.MPU, 3));

            if (ret) waitIdle();
            else Log.v(TAG, "Error MPU_START");

            // TEM

            if (mBluetoothLeService == null) return "Failed";
            ret = mBluetoothLeService.writeGattCharacteristicNoResponse(address,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                    CommandMessage.getVultureStop(CommandMessage.TEM));

            if (ret) waitIdle();
            else Log.v(TAG, "Error TEM_STOP");

            if (mBluetoothLeService == null) return "Failed";
            ret = mBluetoothLeService.writeGattCharacteristicNoResponse(address,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                    CommandMessage.getVultureCmd(CommandMessage.TEM, 3));
            if (ret) waitIdle();
            else Log.v(TAG, "Error TEM_START");

            // HUM

            if (mBluetoothLeService == null) return "Failed";
            ret = mBluetoothLeService.writeGattCharacteristicNoResponse(address,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                    CommandMessage.getVultureStop(CommandMessage.HUM));

            if (ret) waitIdle();
            else Log.v(TAG, "Error HUM_STOP");

            if (mBluetoothLeService == null) return "Failed";
            ret = mBluetoothLeService.writeGattCharacteristicNoResponse(address,
                    MyGattService.VULTURE_SERVICE, MyGattCharacteristic.COMMAND_CMD,
                    CommandMessage.getVultureCmd(CommandMessage.HUM, 3));
            if (ret) waitIdle();
            else Log.v(TAG, "Error HUM_START");

            return "Done";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "EnableNotificationTask onPostExecute called :" + address + " , " + result);
            updateConnectionState(true);
        }
    }
}

