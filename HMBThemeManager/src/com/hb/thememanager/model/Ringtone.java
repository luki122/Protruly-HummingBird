package com.hb.thememanager.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Ringtone extends Theme {
	
	public Ringtone() {
		this.type = RINGTONE;
	}
	
	public Ringtone(Parcel in) {
		super(in);
	}
	
	public Ringtone(int type, String id, String name, String uri) {
		this.type =  type;
		this.id = id;
		this.name = name;
		this.downloadUrl = uri;
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		super.writeToParcel(dest, flags);

	}
	
	public static void writeToParcel(Ringtone theme, Parcel out) {
		if (theme != null) {
			theme.writeToParcel(out, 0);
		} else {
			out.writeString(null);
		}
	}

	public static Ringtone readFromParcel(Parcel in) {
		return in != null ? new Ringtone(in) : null;
	}
	public static final Parcelable.Creator<Ringtone> CREATOR = new Parcelable.Creator<Ringtone>() {
		public Ringtone createFromParcel(Parcel in) {
			return new Ringtone(in);
		}

		public Ringtone[] newArray(int size) {
			return new Ringtone[size];
		}
	};
	
}
