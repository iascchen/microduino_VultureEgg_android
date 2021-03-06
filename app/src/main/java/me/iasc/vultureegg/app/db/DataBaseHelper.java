package me.iasc.vultureegg.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "vultureegg.db";
    private static final int DATABASE_VERSION = 1;

    public static final String C_ID = "id";
    public static final String C_DEV_ADDRESS = "address";
    public static final String C_DEV_NAME = "name";
    public static final String C_DEV_DEVID = "device_id";
    public static final String C_DEV_TYPE = "type";

    // Device
    public static final String DEVICE_TABLE = "device";

    public static final String CREATE_DEVICE_TABLE = "create table if not exists " + DEVICE_TABLE
            + " (" + C_ID + " INTEGER PRIMARY KEY asc AUTOINCREMENT, "
            + C_DEV_ADDRESS + " text not null, "
            + C_DEV_NAME + " text, "
            + C_DEV_DEVID + " text, "
            + C_DEV_TYPE + " text )";

    // Data

    public static final String RECORD_TABLE = "records";

    public static final String C_DATA_TIME = "time";
    public static final String C_DATA_VALUE = "value";

    public static final String CREATE_RECORD_TABLE = "create table if not exists " + RECORD_TABLE
            + " (" + C_ID + " INTEGER PRIMARY KEY asc AUTOINCREMENT, "
            + C_DATA_TIME + " text not null, "  // long value of time stamp
            + C_DEV_DEVID + " text not null, "
            + C_DATA_VALUE + " text )";

    private static DataBaseHelper instance;

    public static synchronized DataBaseHelper getHelper(Context context) {
        if (instance == null)
            instance = new DataBaseHelper(context);
        return instance;
    }

    private DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DEVICE_TABLE);
        db.execSQL(CREATE_RECORD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
