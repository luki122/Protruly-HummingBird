package com.hb.thememanager.model;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * 专题类
 */
public class Topic implements Parcelable{
	private int id;
	private String name;
	private String icon;
	private String sdate;
	private String edate;

	public Topic(){}

	protected Topic(Parcel in) {
		id = in.readInt();
		name = in.readString();
		icon = in.readString();
		sdate = in.readString();
		edate = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(name);
		dest.writeString(icon);
		dest.writeString(sdate);
		dest.writeString(edate);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<Topic> CREATOR = new Creator<Topic>() {
		@Override
		public Topic createFromParcel(Parcel in) {
			return new Topic(in);
		}

		@Override
		public Topic[] newArray(int size) {
			return new Topic[size];
		}
	};

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getSdate() {
		return sdate;
	}
	public void setSdate(String sdate) {
		this.sdate = sdate;
	}
	public String getEdate() {
		return edate;
	}
	public void setEdate(String edate) {
		this.edate = edate;
	}
	@Override
	public String toString() {
		return "Topic [id=" + id + ", name=" + name + ", icon=" + icon
				+ ", sdate=" + sdate + ", edate=" + edate + "]";
	}
}

