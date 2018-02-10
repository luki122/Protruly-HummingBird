package com.hmb.manager.aidl;

//import com.hb.tms.MarkManager;

import android.os.Parcel;
import android.os.Parcelable;

public class RejectSmsResult implements Parcelable {
    public int reject;

    public int getReject() {
        return reject;
    }

    public void setReject(int reject) {
        this.reject = reject;
    }

    public String rejectTag;

    public String getRejectTag() {
        return rejectTag;
    }

    public void setRejectTag(String rejectTag) {
        this.rejectTag = rejectTag;
    }

    public RejectSmsResult(int reject, String rejectTag) {
        this.reject = reject;
        this.rejectTag = rejectTag;
    }

    @Override
    public int describeContents() {
        return 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //dest.writeByte((byte)(reject ?1:0));//if myBoolean == true, byte == 1
        dest.writeInt(reject);
        dest.writeString(rejectTag);
    }

    public static final Parcelable.Creator<RejectSmsResult> CREATOR = new Parcelable.Creator<RejectSmsResult>() {

        @Override
        public RejectSmsResult createFromParcel(Parcel source) {
            return new RejectSmsResult(source.readInt(), source.readString());
        }

        @Override
        public RejectSmsResult[] newArray(int size) {
            return new RejectSmsResult[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("是否拦截:[" + reject + "]\n");
        strBuilder.append("拦截标记:[" + rejectTag + "]\n");
        return strBuilder.toString();
    }
}
