package com.hb.thememanager.model;

import android.text.TextUtils;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 专题类
 */
public class TopicDetail{
	private String id;
	private String icon;
	private String name;
	private int isCharge;
	private int price;
	public String  downloadUrl;
	public int  putawayStatus;
	@JSONField(serialize = false)
	public boolean downloaded;
	public String getId() {
		return id;
	}
	public void setId(String id) {
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
	public int getIsCharge() {
		return isCharge;
	}
	public void setIsCharge(int isCharge) {
		this.isCharge = isCharge;
	}
	public double getPrice() {
		return price/100d;
	}
	public void setPrice(int price) {
		this.price = price;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public int getPutawayStatus() {
		return putawayStatus;
	}

	public void setPutawayStatus(int putawayStatus) {
		this.putawayStatus = putawayStatus;
	}

	@Override
	public String toString() {
		return "Topic [id=" + id + ", icon=" + icon + ", name=" + name
				+ ", isCharge=" + isCharge + ", price=" + price + "]";
	}

	public boolean isFree(){
		return isCharge == 0;
	}
}
