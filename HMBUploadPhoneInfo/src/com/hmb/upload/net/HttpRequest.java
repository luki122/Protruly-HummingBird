package com.hmb.upload.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 *  HTTP请求处理.
 *  
 *  @author Zhaolaichao.
 */
public class HttpRequest {
	private static final String TAG = "HttpRequest";

	public static final String mUrl = "http://point.bqlcloud.com/bee/collect";
	
	/**
	 * 是否有网络连接.
	 */
	public boolean mNetAvailable;
	
	/**
	 * 网络类型.
	 */
	public String mNetType = "";
	
	/**
	 * 网络接入点.
	 */
	public String mAPNType = "";
	
	/**
	 * 是否有异常.
	 */
	public boolean mErrorFlag = false;
	
	/**
	 * 错误信息.
	 */
	public String mErrorMsg = "";
	
	/**
	 * HTTP响应代码.
	 */
	public int mRespondCode;
	
	/**
	 * HTTP请求返回 InputStream.
	 */
	public InputStream mInStream;
	
	/**
	 * 设置连接服务器超时时间.
	 */
	public static final int CONNECT_TIME_OUT = 20000;
	
	/**
	 * 设置从服务器读取数据超时时间.
	 */
	public static final int READ_TIME_OUT = 20000;
	
	private static final String BOUNDARY = java.util.UUID.randomUUID().toString();
	
	private static final String PREFIX = "--";
	/**
	 * new line.
	 */
	private static final String NEWLINE = "\r\n";
	
	private static final String MULTIPART_FROM_DATA = "multipart/form-data";
	
	/**
	 * HttpRequest Constructor. 
	 */
	public HttpRequest() {
		
	}
	
	/**
	 * 检查网络环境.
	 *
	 * @param context android.content.Context.
	 */
	public void checkNetwork(Context context) {
		ConnectivityManager cwjManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cwjManager.getActiveNetworkInfo();
		if (info != null && info.isAvailable() && info.isConnected()) {
			// 有联网
			mNetAvailable = true;
			mNetType = info.getTypeName();
			mAPNType = info.getExtraInfo();
			return;
		}
		mNetAvailable = false;
		return;
	}
	
	/**
	 * HTTP GET请求.
	 *
	 * @param context android.content.Context
	 * @param url     URL
	 * @param params  请求的参数
	 * @return HttpRequest Object
	 */
	public static HttpRequest get(Context context, String url, String params) {
		HttpRequest http = new HttpRequest();
		if (null != params && !params.equals("") && !params.equals(" ")) {
			url = url + "?" + params;
		}
		HttpURLConnection conn = getHttpURLConnection(context, url, http);
		if (null == conn) {
			return http;
		}
		
		// 增加如果连接失败，则重连一次.
		int connCount = 0;
		while (connCount < 2) {
			try {
				conn.setDoInput(true);
				conn.setDoOutput(false);
				conn.setConnectTimeout(CONNECT_TIME_OUT);
				conn.setReadTimeout(READ_TIME_OUT);
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Charset", "utf-8");
				http.mRespondCode = conn.getResponseCode();
				Log.e(TAG,"HttpRequest get mRespondCode: " + http.mRespondCode);
				// 跳转
				String encoding = conn.getHeaderField("Content-Encoding");
				if(encoding != null && "gzip".equals(encoding)){
					http.mInStream = new GZIPInputStream(conn.getInputStream());
				} else {
					http.mInStream = conn.getInputStream();
				}
				http.mRespondCode = conn.getResponseCode();
				http.mErrorFlag = false;
				break;
			} catch (Exception e) {
				connCount++;
				http.mErrorFlag = true;
				http.mErrorMsg = e.toString();
				Log.e(TAG, "HttpRequest error :" + e.toString());
			}
		}
		return http;
	}
	
	/**
	 * HTTP POST请求.
	 *
	 * @param context android.content.Context
	 * @param url     URL
	 * @param params  请求的参数
	 * @return HttpRequest Object
	 */
	public static HttpRequest post(Context context, String url, String params) {
		Log.e(TAG, "POST URL : " + url + " PARAMS : " + params);
		HttpRequest http = new HttpRequest();
		HttpURLConnection conn = getHttpURLConnection(context, url, http);
		if (null == conn) {
			return http;
		}
		Log.e(TAG, "HttpRequest CONNECTION OK");
		try {
			// 设置是否向connection输出，因为这个是post请求，参数要放在HTTP正文内，因此需要设为true
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setConnectTimeout(CONNECT_TIME_OUT);
			conn.setReadTimeout(READ_TIME_OUT);
			// Post 请求不能使用缓存
			conn.setUseCaches(false);

			// 配置本次连接的Content-type，配置为application/x- www-form-urlencoded的
			// 意思是正文是urlencoded编码过的form参数，下面我们可以看到我们对正文内容使用URLEncoder.encode
			// 进行编码
			//conn.setRequestProperty("Charset", "utf-8");
//			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
			conn.setRequestProperty("Content-Type", "application/octet-stream;charset=utf-8");
			conn.setRequestMethod("POST");
//			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(),"utf-8");
			OutputStream out = conn.getOutputStream();
			out.write(params.getBytes("utf-8"));
			out.flush();
			out.close();

			http.mRespondCode = conn.getResponseCode();
			Log.e(TAG, "HttpRequest post mRespondCode: " + http.mRespondCode);
			http.mErrorFlag = false;
			http.mInStream = conn.getInputStream();
			Log.e(TAG, "HttpRequest post InputStream: " + http.mInStream);
		} catch (Exception e) {
			e.printStackTrace();
			http.mErrorFlag = true;
			http.mErrorMsg = e.toString();
			Log.e(TAG, "HttpRequest post error, msg " + e.toString());
		}
		return http;
	}
	
	/**
	 * HTTP POST请求.
	 *  json请求
	 * @param context android.content.Context
	 * @param url     URL
	 * @param params  请求的参数
	 * @return HttpRequest Object
	 */
	public static HttpRequest postJson(Context context, String url, String params) {
		Log.e(TAG, "统一资源定位器：" +url);
		HttpRequest http = new HttpRequest();
		HttpURLConnection conn = getHttpURLConnection(context, url, http);
		if (null == conn) {
			Log.e(TAG, "HttpRequest post: conn is NULL.");
			return http;
		}
		Log.e(TAG, "HttpRequest post OK CONNECTION: " + conn);
		try {
			// 设置是否向connection输出，因为这个是post请求，参数要放在HTTP正文内，因此需要设为true
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setConnectTimeout(CONNECT_TIME_OUT);
			conn.setReadTimeout(READ_TIME_OUT);
			conn.setRequestMethod("POST");
			// Post 请求不能使用缓存
			conn.setUseCaches(false);
			
			// 配置本次连接的Content-type，配置为application/x- www-form-urlencoded的
			// 意思是正文是urlencoded编码过的form参数，下面我们可以看到我们对正文内容使用URLEncoder.encode
			// 进行编码
			//conn.setRequestProperty("Charset", "utf-8");
			conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(),"utf-8");
			out.write(params);
			out.flush();
			out.close();

			http.mRespondCode = conn.getResponseCode();
			Log.e(TAG, "HttpRequest post mRespondCode: " + http.mRespondCode);
			http.mErrorFlag = false;
			http.mInStream = conn.getInputStream();
			Log.e(TAG, "HttpRequest post InputStream: " + http.mInStream);
		} catch (Exception e) {
			e.printStackTrace();
			http.mErrorFlag = true;
			http.mErrorMsg = e.toString();
			Log.e(TAG, "HttpRequest post error, msg " + e.toString());
		}
		return http;
	}
	/**
	 * 获取HTTP连接.
	 *
	 * @param context android.content.Context
	 * @param httpurl URL
	 * @param http HttpRequest Object
	 * @return java.net.HttpURLConnection
	 */
	private static HttpURLConnection getHttpURLConnection(Context context, String httpurl, HttpRequest http) {
		http.checkNetwork(context);
		if (!http.mNetAvailable) {
			http.mErrorFlag = true;
			return null;
		}
		HttpURLConnection conn = null;
		URL url = null;
		
		try {
			url = new URL(httpurl);
			conn = (HttpURLConnection) url.openConnection();
		} catch (Exception e) {
			Log.e(TAG, "HttpRequest error :" + e.toString());
		}
		return conn;
	}
	
	/**
	 * 上传操作的进度监听.
	 */
	public interface OnPostedListener {
		public void posted(long postedSize, long totalSize);
	}
	

	/**
	 * 设置POST连接属性.
	 *
	 * @param conn HttpURLConnection
	 * @return 
	 */
	private static boolean setPostConnection(HttpURLConnection conn) {
		conn.setReadTimeout(1800 * 1000); // 缓存的最长时间
		conn.setDoInput(true);    // 允许输入
		conn.setDoOutput(true);   // 允许输出
		conn.setUseCaches(false); // 不允许使用缓存
		try {
			conn.setRequestMethod("POST");
		} catch (ProtocolException e) {
			e.printStackTrace();
			return false;
		}
		
		conn.setRequestProperty("Charsert", "utf-8");
		conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA + ";boundary=" + BOUNDARY);
		conn.setChunkedStreamingMode(1024 * 100);
		return true;
	}
	

	/**
	 * java.net.URLEncoder编码URL.
	 *
	 * @param url URL链接.
	 * @return java.net.URLEncoder编码后的URL.
	 */
	public static String encodeUrl(String url) {
		if (TextUtils.isEmpty(url)) {			
			return "";
		}
		String encodeUrl = null;
		try {
			encodeUrl = java.net.URLEncoder.encode(decodeUrl(url), "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return encodeUrl;
	}

	/**
	 * java.net.URLDecoder解码URL.
	 *
	 * @param url URL链接.
	 * @return java.net.URLDecoder解码后的URL.
	 */
	public static String decodeUrl(String url) {
		if (TextUtils.isEmpty(url)) {			
			return "";
		}
		String decodeUrl = null;
		try {
			decodeUrl = java.net.URLDecoder.decode(url, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decodeUrl;
	}

	private static int updateTimes(int count, HttpRequest httpRequest) {
		if (count < 3 || httpRequest == null) {
			return count = -1;
		}
		if (httpRequest.mErrorFlag) {
			count ++;
		}

		return count;

	}
}

