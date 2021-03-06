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
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONObject;

import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;
import im.delight.android.ddp.MeteorSingleton;
import im.delight.android.ddp.ResultListener;
import im.delight.android.ddp.db.memory.InMemoryDatabase;
import me.iasc.vultureegg.app.db.DeviceDAO;
import me.iasc.vultureegg.app.db.DeviceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class MyDeviceActivity extends ListActivity {
    private static final String TAG = MyDeviceActivity.class.getSimpleName();
    private Tracker mTracker;

    private static final int MAX_DEVICE_NUMBER = 5;
    public static final String ARG_USER_INDEX = "curr_user_index";

    private List<DeviceModel> devicelist;

    private DeviceListAdapter mDeviceListAdapter;

    public DeviceDAO deviceDAO = null;

    private static final int REQUEST_ADD_DEVICE = 1;

    public static String M_COTTON_LAN = "192.168.199.240:3000";
    private static String email = "iasc@163.com", password = "123456";

    public static String mCottonServer = "ws://192.168.199.240:3000/websocket";

    //    private static Meteor mMeteor;
    private static InMemoryDatabase meteorDb;

    static SharedPreferences settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VultureEggApplication application = (VultureEggApplication) getApplication();
        mTracker = application.getDefaultTracker();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        deviceDAO = new DeviceDAO(this);
        devicelist = deviceDAO.getDevices();
        Log.v(TAG, "device count : " + devicelist.size());

        getActionBar().setTitle(getString(R.string.app_name));

        renewMeteor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_devices, menu);
        menu.findItem(R.id.menu_add).setVisible(true);
        menu.findItem(R.id.menu_refresh).setVisible(true);
        menu.findItem(R.id.menu_show).setVisible(true);
        menu.findItem(R.id.menu_setting).setVisible(true);
        menu.findItem(R.id.menu_data).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                if (MAX_DEVICE_NUMBER > devicelist.size()) {
                    item.setVisible(true);

                    mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                            .setAction("AddDevice").build());

                    final Intent intent = new Intent(this, DeviceScanActivity.class);
                    startActivityForResult(intent, REQUEST_ADD_DEVICE);
                } else {
                    item.setVisible(false);
                }
                break;
            case R.id.menu_refresh:
                queryMyDevices();
                break;
            case R.id.menu_show:
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                        .setAction("ShowStart").setLabel("DeviceCounts").setValue(devicelist.size()).build());

                final Intent intent2 = new Intent(this, MyDevicesShowActivity.class);
                intent2.putParcelableArrayListExtra(MyDevicesShowActivity.ARG_DEVICES, (ArrayList<DeviceModel>) devicelist);
                startActivity(intent2);
                break;
            case R.id.menu_setting:
                final Intent intent3 = new Intent(this, SettingActivity.class);
                startActivity(intent3);
                break;
            case R.id.menu_data:
                mTracker.send(new HitBuilders.EventBuilder().setCategory("Action")
                        .setAction("PreviewData").build());

                final Intent intent4 = new Intent(this, DataListActivity.class);
                startActivity(intent4);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initializes list view adapter.
        mDeviceListAdapter = new DeviceListAdapter();
        setListAdapter(mDeviceListAdapter);

        queryMyDevices();

        String newServer = "ws://" + settings.getString(SettingActivity.SET_M_COTTON, M_COTTON_LAN) + "/websocket";
        String newEmail = settings.getString(SettingActivity.SET_M_MCOTTON_USER, "");
        String newPassword = settings.getString(SettingActivity.SET_M_MCOTTON_PASSWORD, "");
        if (!mCottonServer.equals(newServer)
                || !email.equals(newEmail)
                || !password.equals(newPassword)) {
            renewMeteor();
        }

        mTracker.setScreenName("MyDeviceActivity");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ADD_DEVICE && resultCode == Activity.RESULT_OK) {
            Log.v(TAG, "Scan Returned");
            queryMyDevices();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final DeviceModel device = mDeviceListAdapter.getDevice(position);
        if (device == null) return;

        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.ARG_DEVICE, device);
        startActivity(intent);
    }

    final MeteorCallback mMeteorCallback = new MeteorCallback() {
        @Override
        public void onConnect(boolean signedInAutomatically) {
            Log.v(TAG, "mCottonServer onConnect");

            MeteorSingleton.getInstance().loginWithEmail(email, password, new ResultListener() {
                @Override
                public void onSuccess(String s) {
                    Log.v(TAG, "mCottonServer Logon");
                    Toast.makeText(getApplicationContext(), "mCotton Logon", Toast.LENGTH_LONG).show();
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
        }

        @Override
        public void onDataAdded(String collectionName, String documentID, String newValuesJson) {
            Log.v(TAG, "mCottonServer onDataAdded:" + collectionName + " ==> " + documentID + " , " + newValuesJson);
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

    private void renewMeteor() {
        try {
            if (MeteorSingleton.getInstance() != null) {
                MeteorSingleton.getInstance().destroyInstance();
            }
        } catch (Exception e) {
            // ignore
        }

        Log.v(TAG, "mCottonServer mMeteor new");

        try {
            mCottonServer = "ws://" + settings.getString(SettingActivity.SET_M_COTTON, M_COTTON_LAN) + "/websocket";
            email = settings.getString(SettingActivity.SET_M_MCOTTON_USER, "");
            password = settings.getString(SettingActivity.SET_M_MCOTTON_PASSWORD, "");

            Log.d(TAG, "mCottonServer init : " + mCottonServer);

            meteorDb = new InMemoryDatabase();

            MeteorSingleton.createInstance(this, mCottonServer, meteorDb);
            MeteorSingleton.getInstance().addCallback(mMeteorCallback);
            MeteorSingleton.getInstance().connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void queryMyDevices() {
        mDeviceListAdapter.clear();
        devicelist = deviceDAO.getDevices();

        for (DeviceModel _d : devicelist) {
            mDeviceListAdapter.addDevice(_d);
        }
        mDeviceListAdapter.notifyDataSetChanged();

        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<DeviceModel> mDevices;
        private LayoutInflater mInflator;

        public DeviceListAdapter() {
            super();
            mDevices = new ArrayList<DeviceModel>();
            mInflator = MyDeviceActivity.this.getLayoutInflater();
        }

        public void addDevice(DeviceModel device) {
            if (!mDevices.contains(device)) {
                mDevices.add(device);
            }
        }

        public void removeDevice(DeviceModel device) {
            if (!mDevices.contains(device)) {
                mDevices.remove(device);
            }
        }

        public DeviceModel getDevice(int position) {
            return mDevices.get(position);
        }

        public void clear() {
            mDevices.clear();
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_device_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceType = (TextView) view.findViewById(R.id.device_type);
                viewHolder.deviceDevId = (TextView) view.findViewById(R.id.device_devid);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            DeviceModel device = mDevices.get(i);
            final String deviceName = device.getName();
            final String deviceType = device.getType();
            final String deviceDevId = device.getDeviceId();

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            viewHolder.deviceAddress.setText(device.getAddress());

            if (DeviceModel.TYPE_STATION.equals(deviceType))
                viewHolder.deviceType.setText("(Station)");
            else
                viewHolder.deviceType.setText("(Egg)");

            if (deviceDevId != null && deviceDevId.length() > 0)
                viewHolder.deviceDevId.setText(deviceDevId);
            else
                viewHolder.deviceDevId.setText(R.string.deviceId);

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName, deviceAddress, deviceType, deviceDevId;
    }
}