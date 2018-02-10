package com.hb.thememanager.model;

import java.util.ArrayList;

import com.hb.thememanager.utils.Config;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
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
	 * Fonts
	 */
	public static final int FONTS = 0x02;

	/**
	 * Desktop wallpaper
	 */
	public static final int WALLPAPER = 0x03;


	/**
	 * Rintong
	 */
	public static final int RINGTONE = 0x04;

	/**
	 * Lockscreen wallpaper
	 */
	public static final int LOCKSCREEN_WALLPAPER = 0x05;

	/**
	 * 表示当前主题有评论
	 */
	public static final int HAS_COMMENT = 1;

	/**
	 * 表示当前主没有评论
	 */
	public static final int HAS_NOT_COMMENT = 0;



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

	public static final int USER_IMPORT = 2;
	
	/**
	 * 当前主题已经加载到数据库中
	 */
	public static final int LOADED = 1;
	/**
	 * 当前主题未加载到数据库中
	 */
	public static final int UNLOADED = 0;

	/**
	 * 当前主题已购买
	 */
	public static final int PAID = 1;

	/**
	 * 当前主题未购买
	 */
	public static final int UN_PAID = 0;

	/**
	 * 对应的在线主题已下载
	 */
	public static final int DOWNLOADED = 1;
	/**
	 * 对应的在线主题为下载
	 */
	public static final int UN_DOWNLOADED = 0;

	public static final int HAS_NEW_VERSION = 1;
	public static final int NO_NEWVERSION = 0;

	
	/**
	 * 当前主题类型，see{@link #THEME_PKG,#THEME_NULL,#WALLPAPER,#RINGTONE ,#FONTS}
	 */
	public int type = THEME_PKG;
	/**
	 * 主题ID，唯一标识一个主题
	 */
	public String id="";
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

	public int designerId;

	public int userId;
	
	/**
	 * 主题价格
	 */
	public String price;

	public int isCharge = 0;

	/**
	 * 用于显示该主题是否下载，或者已购买，在主题列表中显示在
	 * 价格的位置
	 */
	public String downloadStateStr;
	
	/**
	 * 主题分类
	 */
	public int category = THEME_PKG;
	
	/**
	 * 主题下载次数
	 */
	public String downloadTimes = "1";
	
	/**
	 * 主题评分
	 */
	public float grade;

	public int hasNewVersion = NO_NEWVERSION;
	
	/**
	 * 主题下载状态，在线主题才有这个属性
	 */
	public int downloadStatus;
	public long lastModifiedTime;
	public long totalBytes;
	public long currentBytes;

	public int buyStatus;
	public int hasComment;
	
	public String coverUrl;
	
	public String detailUrl;
	
	public ArrayList<String> previewArrays = new ArrayList<String>();
	
	public ArrayList<String> wallpaperArrays = new ArrayList<String>();

	
	/**
	 * 该主题对应的评论
	 */
	private SparseArray<Comments> mMyComments = new SparseArray<Comments>();
	
	public Theme() {
		// TODO Auto-generated constructor stub
		this.type = THEME_PKG;
	}

	public boolean isSystemTheme(){
		return isSystemTheme == SYSTEM_THEME;
	}

	public boolean isUserImport(){
		return isSystemTheme == USER_IMPORT;
	}
	
	@SuppressWarnings("unchecked")
	public Theme(Parcel in) {
		id = in.readString();
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
		hasComment = in.readInt();
		coverUrl = in.readString();
		price = in.readString();
		buyStatus = in.readInt();
		downloadUrl = in.readString();
		hasNewVersion = in.readInt();
		designerId = in.readInt();
		downloadStateStr = in.readString();
		isCharge = in.readInt();
		userId = in.readInt();
		downloadStatus = in.readInt();
	}
	
	public Theme(int type, String id, String name, String uri) {
		this.type =  type;
		this.id = id;
		this.name = name;
		this.downloadUrl = uri;
	}

	@Override
	public String toString() {
		return "Theme{" +
				"type=" + type +
				", id='" + id + '\'' +
				", name='" + name + '\'' +
				", designer='" + designer + '\'' +
				", version='" + version + '\'' +
				'}';
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isFree(){
		return isCharge == 0;
	}

	public boolean hasNewVersion(){
		return hasNewVersion == HAS_NEW_VERSION;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(id);
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
		dest.writeInt(hasComment);
		dest.writeString(coverUrl);
		dest.writeString(price);
		dest.writeInt(buyStatus);
		dest.writeString(downloadUrl);
		dest.writeInt(hasNewVersion);
		dest.writeInt(designerId);
		dest.writeString(downloadStateStr);
		dest.writeInt(isCharge);
		dest.writeInt(userId);
		dest.writeInt(downloadStatus);
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
		return Config.DEFAULT_THEME_ID.equals(id) ;
	}

	public boolean hasComments(){
		//内置主题和系统默认主题没有评论
		return !(isSystemTheme() || isDefaultTheme())&& hasComment == HAS_COMMENT;
	}

	public boolean isPaid(){
		return buyStatus == PAID;
	}


	public String getPrice(){
		if(TextUtils.isEmpty(downloadStateStr)){
			return  price;
		}

		return downloadStateStr;
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
				&& version.equals(other.version)
				&& id.equals(other.id);
	}
	

}
