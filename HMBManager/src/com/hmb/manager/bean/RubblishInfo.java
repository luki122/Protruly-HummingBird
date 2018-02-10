package com.hmb.manager.bean;



import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import tmsdk.fg.module.cleanV2.RubbishEntity;
public class RubblishInfo implements Parcelable {

	private int mLine;
	
	
	private String mDescription;
	
	private long mSize;
	
	private long mDataSize;
	
	private int isSuggest;
	
	private String mAppName;
	
	private String mPackageName;
	
	private int mStatus;
	
	private RubbishEntity aRubbish;
	
	public int getmStatus() {
		return mStatus;
	}

	public void setmStatus(int mStatus) {
		this.mStatus = mStatus;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public RubblishInfo(Parcel in){
		readFromParcel(in);
	}
	public RubblishInfo(){

	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mLine);
		dest.writeString(mDescription);
		dest.writeLong(mSize);
		dest.writeLong(mDataSize);
		dest.writeInt(isSuggest);
		dest.writeString(mAppName);
        dest.writeString(mPackageName);
        dest.writeInt(mStatus);
	}
	
	private void readFromParcel(Parcel in) {
		setmLine(in.readInt());
		setmDescription(in.readString());
		setmSize(in.readLong());
		setmDataSize(in.readLong());
		setIsSuggest(in.readInt());
		setmAppName(in.readString());
        setmPackageName(in.readString());
        setmStatus(in.readInt());
	}

	public long getmSize() {
		return mSize;
	}

	public void setmSize(long mSize) {
		this.mSize = mSize;
	}

	public long getmDataSize() {
		return mDataSize;
	}

	public void setmDataSize(long mDataSize) {
		this.mDataSize = mDataSize;
	}

	public int getIsSuggest() {
		return isSuggest;
	}

	public void setIsSuggest(int isSuggest) {
		this.isSuggest = isSuggest;
	}



	public String getmPackageName() {
		return mPackageName;
	}

	public void setmPackageName(String mPackageName) {
		this.mPackageName = mPackageName;
	}

	public String getmAppName() {
		return mAppName;
	}

	public void setmAppName(String mAppName) {
		this.mAppName = mAppName;
	}


    public static final Parcelable.Creator<RubblishInfo> CREATOR = new Creator<RubblishInfo>() {  
        public RubblishInfo createFromParcel(Parcel source) {  
        	return new RubblishInfo(source);
        }  
        public RubblishInfo[] newArray(int size) {  
            return new RubblishInfo[size];  
        }  
    };

	public int getmLine() {
		return mLine;
	}

	public void setmLine(int mLine) {
		this.mLine = mLine;
	}

	public String getmDescription() {
		return mDescription;
	}

	public void setmDescription(String mDescription) {
		this.mDescription = mDescription;
	}

	public RubbishEntity getaRubbish() {
		return aRubbish;
	}

	public void setaRubbish(RubbishEntity aRubbish) {
		this.aRubbish = aRubbish;
	} 

}
