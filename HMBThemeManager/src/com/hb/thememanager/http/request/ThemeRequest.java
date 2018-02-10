package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 所有的请求继承自该类，然后构建属于自己的请求参数，
 * 再转换为JSON数据发送给服务器
 */
public abstract class ThemeRequest {

	private static final String TAG = "ThemeRequest";

	public static HashMap<Integer,String> sRequestKeyMap = new HashMap<>();

	private long version;

	private String deviceId;

	private String model;

	private String romVersion;

	private RequestBody body;

	private String mUrl;

	protected int mThemeType = -1;


	protected String mId;

	protected int mPageNumber;

	protected int mPageSize;



	public ThemeRequest(Context context,int themeType){
		if(themeType == -1){
			Log.e(TAG,"request unknown theme type from client");
		}
		setVersion(CommonUtil.getThemeAppVersion(context));
		setDeviceId(CommonUtil.getIMEI(context));
		setModel(CommonUtil.getModel(context));
		setRomVersion(CommonUtil.getRomVersion());
		mThemeType = themeType;
	}

	public ThemeRequest(Context context){
		this(context,-1);
	}


	@JSONField(serialize=false)
	public void setUrl(String url){
		mUrl = url;
	}

	@JSONField(serialize=false)
	public String getMyUrl(){
		return mUrl;

	}



	@JSONField(serialize=false)
	public void setId(String id){
		mId = id;
	}

	@JSONField(serialize=false)
	public String getId(){
		return mId;
	}

	@JSONField(serialize=false)
	public int getThemeType(){
		return mThemeType;
	}


	@JSONField(serialize=false)
	public void setPageNumber(int number){
		mPageNumber = number;
	}

	@JSONField(serialize=false)
	public void setPageSize(int pageSize){
		mPageSize = pageSize;
	}

	@JSONField(serialize=false)
	public int getPageSize(){
		return mPageSize;
	}

	@JSONField(serialize=false)
	public int getPageNumber(){
		return mPageNumber;
	}


	public void request(Http http,IResponseHandler handler){
		if(http == null || handler == null){
			return;
		}
		if(TextUtils.isEmpty(getMyUrl()) || TextUtils.isEmpty(createJsonRequest())){
			handler.onFailure(404,"target url is null or request body is null");
			TLog.e(TAG,"target url is null or request body is null");
			return;
		}
		http.get(getMyUrl(),createJsonRequest(),handler);
	}
	
	public void  request(){

	}


	/**
	 * 构建请求Json数据
	 */
	public String createJsonRequest(){
		generateRequestBody();
		String json = "";
		if(body == null){
			json = JSON.toJSONString(this);
		}else{
			json = JSON.toJSONString(this,body.createPropertyFilter());
		}
		TLog.d(TAG,""+json);
		return json;
	}


	/**
	 * 子类重写该方法来过滤body中不需要的属性
	 */
	protected void generateRequestBody(){

	}


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

	public RequestBody getBody() {
		return body;
	}

	public void setBody(RequestBody body) {
		this.body = body;
	}








	public String getEncoding(InputStream in) throws IOException {
		String encode="UTF-8";
		String strTmp=null;
		byte b[]=null;
		String regExp="gb2312|GB2312|GBK|gbk|utf-8|UTF-8|utf8|UTF8";
		int contentLength=in.available();
		if(contentLength>1000){
			contentLength=1000;
			b=new byte[1000];
		}
		else
			b=new byte[contentLength];
		in.read(b,0,contentLength);
		strTmp=new String(b);
		Pattern p;
		Matcher m;
		p=Pattern.compile(regExp);
		m=p.matcher(strTmp);
		if(m.find())
			return m.group();
		return encode;
	}




	public abstract Response parseResponse(String responseStr);



}
