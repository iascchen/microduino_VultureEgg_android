package me.iasc.vultureegg.app.db;

import android.os.Parcel;
import android.os.Parcelable;

public class DeviceModel implements Parcelable {
    public static String TYPE_EGG = "E", TYPE_STATION = "S";

    private int id;

    private String address, name, type, deviceId;

    public DeviceModel() {
        super();
    }

    public DeviceModel(Parcel in) {
        super();
        this.id = in.readInt();
        this.address = in.readString();
        this.name = in.readString();
        this.deviceId = in.readString();
        this.type = in.readString();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        String ret = "id:" + id
                + ", address:" + address
                + ", name:" + name
                + ", deviceId:" + deviceId
                + ", type:" + type;

        return ret;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(getId());
        parcel.writeString(getAddress());
        parcel.writeString(getName());
        parcel.writeString(getDeviceId());
        parcel.writeString(getType());
    }

    public static final Parcelable.Creator<DeviceModel> CREATOR = new Parcelable.Creator<DeviceModel>() {
        public DeviceModel createFromParcel(Parcel in) {
            return new DeviceModel(in);
        }

        public DeviceModel[] newArray(int size) {
            return new DeviceModel[size];
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

        DeviceModel other = (DeviceModel) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
