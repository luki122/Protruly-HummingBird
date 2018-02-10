package com.hb.thememanager.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Wallpaper extends Theme implements Parcelable{
	
	
	public Wallpaper() {
		this.type = WALLPAPER;
	}
	
	public Wallpaper(Parcel in) {
		super(in);
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
	
	
	public static void writeToParcel(Wallpaper theme, Parcel out) {
		if (theme != null) {
			theme.writeToParcel(out, 0);
		} else {
			out.writeString(null);
		}
	}

	public static Wallpaper readFromParcel(Parcel in) {
		return in != null ? new Wallpaper(in) : null;
	}
	public static final Parcelable.Creator<Wallpaper> CREATOR = new Parcelable.Creator<Wallpaper>() {
		public Wallpaper createFromParcel(Parcel in) {
			return new Wallpaper(in);
		}

		public Wallpaper[] newArray(int size) {
			return new Wallpaper[size];
		}
	};
	
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(obj == null){
			return false;
		}
		if(obj instanceof Theme) {
			Theme other = (Theme)obj;
			if(themeFilePath != null && themeFilePath.length() >0) {
				return themeFilePath.equals(other.themeFilePath);
			}else if(coverUrl != null && coverUrl.length() > 0){
				return coverUrl.equals(other.coverUrl);
			}else {
				return false;
			}
		}else {
			Wallpaper other = (Wallpaper)obj;
			if(themeFilePath != null && themeFilePath.length() >0) {
				return themeFilePath.equals(other.themeFilePath);
			}else if(coverUrl != null && coverUrl.length() > 0){
				return coverUrl.equals(other.coverUrl);
			}else {
				return false;
			}
		}
	}
	
	
	

}
