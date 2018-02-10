package com.protruly.powermanager.purebackground.Info;

import com.alibaba.fastjson.JSONObject;

/**
 * Record AutoStart Info.
 */
public class AutoStartRecordInfo extends ItemInfo {
	private static final String TAG = AutoStartRecordInfo.class.getSimpleName();

	public AutoStartRecordInfo() {
		super(TAG);
	}
	
	private boolean isOpen = false;
	private String packageName ;
	
	public void setIsOpen(boolean isOpen){
		this.isOpen = isOpen;
	}
	
	public boolean getIsOpen(){
		return this.isOpen;
	}
	
	public void setPackageName(String packageName) {
		 this.packageName = packageName;
	}
	 
	 public String getPackageName() {
		 return this.packageName;
	 }
	 
	 public JSONObject getJson() {				
		JSONObject json = new JSONObject();	
		json.put("packageName", packageName);
		json.put("isOpen", isOpen);
		return json;
	 }

    /**
	 * 解析json对象
	 * @param json
	 * @return true 解析成功  false 解析失败
	 * @throws Exception
     */
	 public boolean parseJson(JSONObject json) throws Exception {
		 boolean result = false;
		 if (json != null && !json.isEmpty()) {
			 packageName = json.getString("packageName");
			 isOpen = json.getBoolean("isOpen");
			 result = true;
		 }
		 return result;
	 }
}