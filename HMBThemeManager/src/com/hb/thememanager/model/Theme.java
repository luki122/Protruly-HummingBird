package com.hb.thememanager.model;

import java.util.ArrayList;

import com.hb.thememanager.utils.Config;

import android.os.Parcel;
import android.os.Parcelable;
/**
 * 
 * Base class for Theme.app may to extends this 
 * to declare sub Theme(such as,Wallpaper,RingTong).
 *
 */
public class Theme implements Parcelable {
	
	/**
	 * UnKown theme type
	 */
	public static final int THEME_NULL = 0x00;
	/**
	 * Global theme package
	 */
	public static final int THEME_PKG = 0x01;
	/**
	 * Rintong
	 */
	public static final int RINGTONG = 0x02;
	/**
	 * Desktop wallpaper
	 */
	public static final int WALLPAPER = 0x03;
	/**
	 * Fonts
	 */
	public static final int FONTS = 0x04;
	/**
	 * Lockscreen wallpaper
	 */
	public static final int LOCKSCREEN_WALLPAPER = 0x05;
	
	
	/**
	 * 当前主题已被使用
	 */
	public static final int APPLIED = 1;
	/**
	 * 当前主题未被使用
	 */
	public static final int UN_APPLIED = 0;
	
	/**
	 * 当前主题为系统内置主题
	 */
	public static final int SYSTEM_THEME = 1;
	/**
	 * 当前主题为在线主题
	 */
	public static final int LOCAL_THEME = 0;
	
	/**
	 * 当前主题已经加载到数据库中
	 */
	public static final int LOADED = 1;
	/**
	 * 当前主题未加载到数据库中
	 */
	public static final int UNLOADED = 0;
	
	/**
	 * 当前主题类型，see{@link #THEME_PKG,#THEME_NULL,#WALLPAPER,#RINGTONG,#FONTS}
	 */
	public int type = THEME_PKG;
	/**
	 * 主题ID，唯一标识一个主题
	 */
	public int id;
	/**
	 * 主题名字
	 */
	public String name = "";
	/**
	 * 主题设计者
	 */
	public String designer = "";
	/**
	 * 主题版本号，用于主题更新
	 */
	public String version = "";
	/**
	 * 主题包大小
	 */
	public String size;
	
	public int sizeCount;
	/**
	 * 主题描述信息
	 */
	public String description = "";
	/**
	 * 主题文件存放路径
	 */
	public String themeFilePath = "";
	/**
	 * 主题对于的文件
	 */
	public ThemeZip themeZipFile;
	/**
	 * 主题下载路径，在线主题才有这个属性
	 */
	public String downloadUrl;
	/**
	 * 主题被加载后加载信息存放的路径
	 */
	public String loadedPath = "";
	/**
	 * 主题使用状态
	 */
	public int applyStatus;
	/**
	 * 主题加载状态
	 */
	public int loaded;
	/**
	 * 主题是否是系统内置
	 */
	public int isSystemTheme;
	/**
	 * 主题下载状态，在线主题才有这个属性
	 */
	public int downloadStatus;
	public long lastModifiedTime;
	public long totalBytes;
	public long currentBytes;
	
	public ArrayList<String> previewArrays = new ArrayList<String>();
	
	public ArrayList<String> wallpaperArrays = new ArrayList<String>();

	public Theme() {
		// TODO Auto-generated constructor stub
	}

	public boolean isSystemTheme(){
		return isSystemTheme == SYSTEM_THEME;
	}
	
	@SuppressWarnings("unchecked")
	public Theme(Parcel in) {
		id = in.readInt();
		sizeCount = in.readInt();
		type = in.readInt();
		name = in.readString();
		description = in.readString();
		designer = in.readString();
		version = in.readString();
		size = in.readString();
		themeFilePath = in.readString();
		loadedPath = in.readString();
		totalBytes = in.readLong();
		applyStatus = in.readInt();
		loaded = in.readInt();
		isSystemTheme = in.readInt();
		in.readStringList(previewArrays);
		currentBytes = in.readLong();
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "Name->"+name+" type->"+type+" version->"+version;
	}
	
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeInt(id);
		dest.writeInt(sizeCount);
		dest.writeInt(type);
		dest.writeString(name);
		dest.writeString(description);
		dest.writeString(designer);
		dest.writeString(version);
		
		dest.writeString(size);
		dest.writeString(themeFilePath);
		dest.writeString(loadedPath);
		dest.writeLong(totalBytes);
		dest.writeInt(applyStatus);
		dest.writeInt(loaded);
		dest.writeInt(isSystemTheme);
		dest.writeStringList(previewArrays);
		dest.writeLong(currentBytes);

	}
	public static void writeToParcel(Theme theme, Parcel out) {
		if (theme != null) {
			theme.writeToParcel(out, 0);
		} else {
			out.writeString(null);
		}
	}

	public static Theme readFromParcel(Parcel in) {
		return in != null ? new Theme(in) : null;
	}
	public static final Parcelable.Creator<Theme> CREATOR = new Parcelable.Creator<Theme>() {
		public Theme createFromParcel(Parcel in) {
			return new Theme(in);
		}

		public Theme[] newArray(int size) {
			return new Theme[size];
		}
	};
	
	public boolean isDefaultTheme(){
		return id == Config.DEFAULT_THEME_ID;
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		Theme other = (Theme)obj;
		if(obj == null){
			return false;
		}
		
		return name.equals(other.name) && designer.equals(other.designer)
				&& description.equals(other.description) && type==other.type 
				&& totalBytes==other.totalBytes
				&& version.equals(other.version);
	}
	

}
