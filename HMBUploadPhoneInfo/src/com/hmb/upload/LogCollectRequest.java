package com.hmb.upload;

public class LogCollectRequest {
	private String protocalVer;
	private String clientVer;
	private String model;//机型
	private String romVersion;//rom 版本
	private int androidVersionCode;//安卓版本号
	private String imei1;//设备号
	private String imei2;//设备号
	private String meid1;//设备号
	private String meid2;//设备号
	private String accountName;//账号名
	private String activationTime;//激活名称
	private String sim1;//sim号
	private String sim2;//副卡SIM 号
	private String operator1;//运营商类型
	private String operator2;//副卡运营商类型
	private String mac;//mac
	private String sn;//Sn
	private String platform;//芯片平台

	public LogCollectRequest() {
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
	public int getAndroidVersionCode() {
		return androidVersionCode;
	}
	public void setAndroidVersionCode(int androidVersionCode) {
		this.androidVersionCode = androidVersionCode;
	}
	
	public String getAccountName() {
		return accountName;
	}
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	public String getActivationTime() {
		return activationTime;
	}
	public void setActivationTime(String activationTime) {
		this.activationTime = activationTime;
	}

	
	public String getProtocalVer() {
		return protocalVer;
	}
	public void setProtocalVer(String protocalVer) {
		this.protocalVer = protocalVer;
	}
	public String getClientVer() {
		return clientVer;
	}
	public void setClientVer(String clientVer) {
		this.clientVer = clientVer;
	}
	public String getImei1() {
		return imei1;
	}
	public void setImei1(String imei1) {
		this.imei1 = imei1;
	}
	public String getImei2() {
		return imei2;
	}
	public void setImei2(String imei2) {
		this.imei2 = imei2;
	}
	public String getMeid1() {
		return meid1;
	}
	public void setMeid1(String meid1) {
		this.meid1 = meid1;
	}
	public String getMeid2() {
		return meid2;
	}
	public void setMeid2(String meid2) {
		this.meid2 = meid2;
	}
	public String getSim1() {
		return sim1;
	}
	public void setSim1(String sim1) {
		this.sim1 = sim1;
	}
	public String getSim2() {
		return sim2;
	}
	public void setSim2(String sim2) {
		this.sim2 = sim2;
	}
	public String getOperator1() {
		return operator1;
	}
	public void setOperator1(String operator1) {
		this.operator1 = operator1;
	}
	public String getOperator2() {
		return operator2;
	}
	public void setOperator2(String operator2) {
		this.operator2 = operator2;
	}
	public String getMac() {
		return mac;
	}
	public void setMac(String mac) {
		this.mac = mac;
	}
	public String getSn() {
		return sn;
	}
	public void setSn(String sn) {
		this.sn = sn;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}

	
		

}
