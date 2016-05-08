package me.iasc.vultureegg.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class RecordDAO extends AppDBDAO {
    private static final String TAG = RecordDAO.class.getSimpleName();

    private static final String WHERE_ID_EQUALS = DataBaseHelper.C_ID + " =?";
    private static final String ORDER_BY_TIME_DESC = DataBaseHelper.C_DATA_TIME + " DESC";

    public static final int UNLIMIT = -1;

    public RecordDAO(Context context) {
        super(context);
    }

    public long save(RecordModel entity) {
        ContentValues values = new ContentValues();

        values.put(DataBaseHelper.C_DATA_TIME, entity.getTime());
        values.put(DataBaseHelper.C_DEV_DEVID, entity.getDeviceId());
        values.put(DataBaseHelper.C_DATA_VALUE, entity.getValue());

        long result = database.insert(DataBaseHelper.RECORD_TABLE, null, values);
        Log.d(TAG, "New Result = " + result);
        return result;
    }

    public int deleteData(RecordModel entity) {
        return database.delete(DataBaseHelper.RECORD_TABLE,
                WHERE_ID_EQUALS, new String[]{entity.getId() + ""});
    }

    public void deleteAll() {
        List<RecordModel> items = getDatas(UNLIMIT);
        for (RecordModel _i : items) {
            deleteData(_i);
        }
    }

    public List<RecordModel> getDatas(int limit) {
        ArrayList<RecordModel> entitys = new ArrayList<RecordModel>();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DataBaseHelper.RECORD_TABLE);

        // Get cursor
        Cursor cursor = null;
        if (limit > 0) {
            cursor = queryBuilder.query(database, new String[]{
                            DataBaseHelper.C_ID,
                            DataBaseHelper.C_DATA_TIME,
                            DataBaseHelper.C_DEV_DEVID,
                            DataBaseHelper.C_DATA_VALUE},
                    null, null, null, null, ORDER_BY_TIME_DESC, "" + limit);
        } else {
            cursor = queryBuilder.query(database, new String[]{
                            DataBaseHelper.C_ID,
                            DataBaseHelper.C_DATA_TIME,
                            DataBaseHelper.C_DEV_DEVID,
                            DataBaseHelper.C_DATA_VALUE},
                    null, null, null, null, ORDER_BY_TIME_DESC);
        }

        while (cursor.moveToNext()) {
            int i = 0;
            RecordModel entity = new RecordModel();
            entity.setId(cursor.getInt(i++));
            entity.setTime(cursor.getString(i++));
            entity.setDeviceId(cursor.getString(i++));
            entity.setValue(cursor.getString(i++));

            entitys.add(entity);
        }
        return entitys;
    }
}
