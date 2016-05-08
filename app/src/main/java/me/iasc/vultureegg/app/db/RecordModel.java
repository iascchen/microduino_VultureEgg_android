package me.iasc.vultureegg.app.db;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class RecordModel implements Parcelable {
    // These keys MUST match with EggMessage and EggStationMessage, and mCotton
    public static final String[] DATA_MAP_KEYS = {
            // Egg
            "Temperature 01", "Temperature 02", "Temperature 03", "Temperature 04",
            "Temperature 05", "Temperature 06", "Temperature 07", "Temperature 08",
            "Temperature 09", "Temperature 10", "Temperature 11", "Temperature 12",
            "Temperature 13", "Temperature 14", "Temperature 15", "Temperature 16",

            "Quaternion 1", "Quaternion 2", "Quaternion 3", "Quaternion 4",
            "Humidity",

            // Environment
            "Env Temperature", "Env Humidity", "Env Lightness"
    };

    public static final String DUMP_SEPRATOR = ",";

    private int id;

    private String time, deviceId, value;

    public RecordModel() {
        super();
    }

    public RecordModel(Parcel in) {
        super();
        this.id = in.readInt();
        this.time = in.readString();
        this.deviceId = in.readString();
        this.value = in.readString();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        if (time == null) {
            time = "" + (new Date().getTime());
        }
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String ret = "id:" + id
                + ", time:" + getTime()
                + ", deviceId:" + deviceId
                + ", value:" + value;

        return ret;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(getId());
        parcel.writeString(getTime());
        parcel.writeString(getDeviceId());
        parcel.writeString(getValue());
    }

    public static final Creator<RecordModel> CREATOR = new Creator<RecordModel>() {
        public RecordModel createFromParcel(Parcel in) {
            return new RecordModel(in);
        }

        public RecordModel[] newArray(int size) {
            return new RecordModel[size];
        }
    };

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        RecordModel other = (RecordModel) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
