package com.hb.thememanager.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 广告信息类
 * @author alexluo
 *
 */
public class Advertising implements Parcelable{
	public static final int TYPE_TOPIC = 1;
	public static final int TYPE_HOT_RECOMMEND = 2;
	public int id;
	public String icon;
	public String  name;
	public int type;
	public int waresType;
	public String parameter;

	public Advertising(){

	}


	protected Advertising(Parcel in) {
		id = in.readInt();
		icon = in.readString();
		name = in.readString();
		type = in.readInt();
		waresType = in.readInt();
		parameter = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(icon);
		dest.writeString(name);
		dest.writeInt(type);
		dest.writeInt(waresType);
		dest.writeString(parameter);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<Advertising> CREATOR = new Creator<Advertising>() {
		@Override
		public Advertising createFromParcel(Parcel in) {
			return new Advertising(in);
		}

		@Override
		public Advertising[] newArray(int size) {
			return new Advertising[size];
		}
	};

	@Override
	public String toString() {
		return "body{" +
				"id=" + id +
				", icon='" + icon + '\'' +
				", name='" + name + '\'' +
				", type=" + type +
				", parameter='" + parameter + '\'' +
				'}';
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getWaresType() {
		return waresType;
	}

	public void setWaresType(int waresType) {
		this.waresType = waresType;
	}

	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
}
