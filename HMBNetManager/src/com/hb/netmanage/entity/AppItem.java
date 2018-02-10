package com.hb.netmanage.entity;

import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 应用相关信息
 * 
 * @author zhaolaichao
 *
 */
public class AppItem implements Comparable<AppItem>, Parcelable {

	/**
	 * 应用移动数据
	 */
	private long appDataBySim1;
	private long appDataBySim2;
	/**
	 * 应用UID
	 */
	private int appUid;
	/**
	 * 应用信息
	 */
	private ResolveInfo resolveInfo;
	/**
	 * 联多状态
	 */
    private boolean policyStatus;
	private String tag;

	public AppItem(Parcel source) {
		super();
		appDataBySim1 = source.readLong();
		appDataBySim2 = source.readLong();
		appUid = source.readInt();
		resolveInfo = source.readParcelable(ResolveInfo.class.getClassLoader());
	}
   
	public AppItem() {
		super();
	}


	
	public boolean isPolicyStatus() {
		return policyStatus;
	}

	public void setPolicyStatus(boolean policyStatus) {
		this.policyStatus = policyStatus;
	}

	public ResolveInfo getResolveInfo() {
		return resolveInfo;
	}

	/**
	 * 应用信息
	 * @param resolveInfo
	 */
	public void setResolveInfo(ResolveInfo resolveInfo) {
		this.resolveInfo = resolveInfo;
	}


	/**
	 * 应用移动数据 sim1
	 * @return
     */
	public long getAppDataBySim1() {
		return appDataBySim1;
	}

	public void setAppDataBySim1(long appDataBySim1) {
		this.appDataBySim1 = appDataBySim1;
	}

	/**
	 * 应用移动数据 sim2
	 * @return
	 */
	public long getAppDataBySim2() {
		return appDataBySim2;
	}

	public void setAppDataBySim2(long appDataBySim2) {
		this.appDataBySim2 = appDataBySim2;
	}

	public int getAppUid() {
		return appUid;
	}

	/**
	 * 应用UIdD
	 * @param appUid
	 */
	public void setAppUid(int appUid) {
		this.appUid = appUid;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public int compareTo(AppItem another) {
		return Long.compare(another.appDataBySim1 + another.appDataBySim2, appDataBySim1 + appDataBySim2);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(appDataBySim1);
		dest.writeLong(appDataBySim2);
		dest.writeInt(appUid);
		dest.writeParcelable(resolveInfo, flags);
	}
  
	public static final Parcelable.Creator<AppItem> CREATOR = new Parcelable.Creator<AppItem>() {

		@Override
		public AppItem createFromParcel(Parcel source) {

			return new AppItem(source);
		}

		@Override
		public AppItem[] newArray(int size) {

			return new AppItem[size];
		}
	};

	@Override
	public String toString() {
		return "AppItem{" +
				"appDataBySim1=" + appDataBySim1 +
				", appDataBySim2=" + appDataBySim2 +
				", appUid=" + appUid +
				", packageInfo=" + resolveInfo +
				", policyStatus=" + policyStatus +
				", tag='" + tag + '\'' +
				'}';
	}
}
