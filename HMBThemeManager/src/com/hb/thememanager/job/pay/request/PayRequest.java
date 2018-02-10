package com.hb.thememanager.job.pay.request;

import android.util.Log;
import com.alibaba.fastjson.JSON;

public class PayRequest {
	private static final String TAG = "PayRequest";
	private long version;
	private String deviceId;
	private String model;
	private String romVersion;
	private PayRequestBody body;

	public long getVersion() {
		return version;
	}
	public void setVersion(long version) {
		this.version = version;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public String getRomVersion() {
		return romVersion;
	}
	public void setRomVersion(String romVersion) {
		this.romVersion = romVersion;
	}
	public PayRequestBody getBody() {
		return body;
	}
	public void setBody(PayRequestBody body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "PayRequest{" +
				"version=" + version +
				", deviceId='" + deviceId + '\'' +
				", model='" + model + '\'' +
				", romVersion='" + romVersion + '\'' +
				", body=" + body +
				'}';
	}

	public String createJsonRequest(){
		String json = JSON.toJSONString(this);
		Log.d(TAG,"json->"+json);
		return json;
	}
}
