package me.iasc.vultureegg.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DeviceDAO extends AppDBDAO {
    private static final String TAG = DeviceDAO.class.getSimpleName();

    private static final String WHERE_ID_EQUALS = DataBaseHelper.C_ID + " =?";

    public DeviceDAO(Context context) {
        super(context);
    }

    public long save(DeviceModel device) {
        ContentValues values = new ContentValues();

        values.put(DataBaseHelper.C_DEV_ADDRESS, device.getAddress());
        values.put(DataBaseHelper.C_DEV_NAME, device.getName());
        values.put(DataBaseHelper.C_DEV_DEVID, device.getDeviceId());
        values.put(DataBaseHelper.C_DEV_TYPE, device.getType());

        long result = database.insert(DataBaseHelper.DEVICE_TABLE, null, values);
        Log.d(TAG, "New Result = " + result);
        return result;
    }

    public long update(DeviceModel device) {
        ContentValues values = new ContentValues();

        values.put(DataBaseHelper.C_DEV_NAME, device.getName());
        values.put(DataBaseHelper.C_DEV_DEVID, device.getDeviceId());
        values.put(DataBaseHelper.C_DEV_TYPE, device.getType());

        long result = database.update(DataBaseHelper.DEVICE_TABLE, values,
                WHERE_ID_EQUALS,
                new String[]{String.valueOf(device.getId())});
        Log.d(TAG, "Update Result = " + result);
        return result;
    }

    public int deleteDevice(DeviceModel device) {
        return database.delete(DataBaseHelper.DEVICE_TABLE,
                WHERE_ID_EQUALS, new String[]{device.getId() + ""});
    }

    public void deleteAll() {
        List<DeviceModel> items = getDevices();
        for (DeviceModel _i : items) {
            deleteDevice(_i);
        }
    }

    public List<DeviceModel> getDevices() {
        ArrayList<DeviceModel> devices = new ArrayList<DeviceModel>();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DataBaseHelper.DEVICE_TABLE);

        // Get cursor
        Cursor cursor = queryBuilder.query(database, new String[]{
                        DataBaseHelper.C_ID,
                        DataBaseHelper.C_DEV_ADDRESS,
                        DataBaseHelper.C_DEV_NAME,
                        DataBaseHelper.C_DEV_DEVID,
                        DataBaseHelper.C_DEV_TYPE},
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            int i = 0;
            DeviceModel device = new DeviceModel();
            device.setId(cursor.getInt(i++));
            device.setAddress(cursor.getString(i++));
            device.setName(cursor.getString(i++));
            device.setDeviceId(cursor.getString(i++));
            device.setType(cursor.getString(i++));

            devices.add(device);
        }
        return devices;
    }

    public List<String> getDevicesAddress() {
        ArrayList<String> addresses = new ArrayList<String>();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DataBaseHelper.DEVICE_TABLE);

        // Get cursor
        Cursor cursor = queryBuilder.query(database, new String[]{
                        DataBaseHelper.C_DEV_ADDRESS},
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            int i = 0;
            addresses.add(cursor.getString(i++));
        }
        return addresses;
    }
}
