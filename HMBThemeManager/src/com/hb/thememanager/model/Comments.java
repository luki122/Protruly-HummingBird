package com.hb.thememanager.model;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import com.hb.thememanager.R;
public class Comments implements Parcelable,Comparable<Comments>{

	private static final int M = 60*1000;
	private static final int H = 60*M;
	private static final int D = 24*H;

	public String nickname;
	public String contentTime;
	public int starLevel;
	public String content;
	public Comments(){}

	protected Comments(Parcel in) {
		nickname = in.readString();
		contentTime = in.readString();
		starLevel = in.readInt();
		content = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(nickname);
		dest.writeString(contentTime);
		dest.writeInt(starLevel);
		dest.writeString(content);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<Comments> CREATOR = new Creator<Comments>() {
		@Override
		public Comments createFromParcel(Parcel in) {
			return new Comments(in);
		}

		@Override
		public Comments[] newArray(int size) {
			return new Comments[size];
		}
	};

	@Override
	public String toString() {
		return "Comments{" +
				"nickname='" + nickname + '\'' +
				", contentTime='" + contentTime + '\'' +
				", startlevel=" + starLevel +
				", content='" + content + '\'' +
				'}';
	}

	public String getContent(){
		try {
			return URLDecoder.decode(content,"UTF-8");
		}catch (UnsupportedEncodingException e){
			return  content;
		}
	}


	/**
	 * 时间在1分钟内包含1分钟则文案显示“刚刚”，超过x分钟则显示“x分钟前”，当时间按小时计算时，则显示“x小时前”，超过一天按具体日期展示“17-05-26”
	 * @return 评论时间
	 */
	public String getRealTime(Resources res){
		String currentT = contentTime;
		Calendar cd = Calendar.getInstance();
		long now = cd.getTimeInMillis();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		try{
			Date d = df.parse(currentT);
			long leftTime = now - d.getTime();
			if(leftTime <= M){
				return res.getString(R.string.comment_time_now);
			}else if(leftTime > M && leftTime < H){
				return res.getString(R.string.comment_time_minute
						,String.valueOf(leftTime / M));
			}else if(leftTime >= H && leftTime < D){
				return res.getString(R.string.comment_time_hour
						,String.valueOf(leftTime / H));
			}else{
				df = new SimpleDateFormat("yy-MM-dd");
				return df.format(d);
			}
		}catch (Exception e){

		}
		return "";
	}

	@Override
	public int compareTo(Comments comments) {
		return dateCompare(this.contentTime,comments.contentTime);
	}


	private int dateCompare(String date1,String date2){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		try{
			Date d1 = df.parse(date1);
			Date d2 = df.parse(date2);
			if(d1.getTime() > d2.getTime()){
				return -1;
			}else if(d1.getTime() < d2.getTime()){
				return 1;
			}else{
				return 0;
			}
		}catch (Exception e){

		}

		return 0;
	}

}
