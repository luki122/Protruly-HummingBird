package com.hb.thememanager.http;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.hb.thememanager.http.body.ProgressRequestBody;
import com.hb.thememanager.http.body.ResponseProgressBody;
import com.hb.thememanager.http.postcache.CacheOperator;
import com.hb.thememanager.http.response.DownloadResponseHandler;
import com.hb.thememanager.http.response.FastJsonResponseHandler;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.JsonResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.hb.thememanager.utils.CommonUtil;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

/**
 * 简易封装的Okhttp
 * 依赖okhttp3，okio，gson
 */
public class Http {
	private static final String TAG = "Http";
	public static final int STATUS_CODE_NETWORK_ERROR = 0;
	public static final int STATUS_CODE_SERVER_ERROR = 1;
	public static final MediaType TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
	private static final int CACHE_MAX_AGE = 3600 * 24 * 3;//Cache for 3 days
	private OkHttpClient mClient;
	private static Http instance;
	private long mStartPoint;
	private Feature[] mFeature = null;
	private CacheOperator mCacheOperator;
	private Context mContext;
	private Http(Context context) {
		mContext = context.getApplicationContext();
		mCacheOperator = CacheOperator.getInstance(mContext);
	    mClient = new OkHttpClient.Builder()
	            .addInterceptor(new Interceptor() {
	                @Override
	                public Response intercept(Chain chain) throws IOException {
						final boolean hasNetwork = CommonUtil.hasNetwork(mContext);
						final Request request = chain.request();
						final String url = request.url().toString();
						final RequestBody body = request.body();
						Response response;
						if(body == null) {
							Response originalResponse = chain.proceed(request);
							response = originalResponse.newBuilder()
									.removeHeader("Pragma")
									.removeHeader("Cache-Control")
									.header("Cache-Control", "public, max-age=" + CACHE_MAX_AGE)
									.build();
							return response;
						}
						okio.Buffer buffer = new okio.Buffer();
						body.writeTo(buffer);
						if(hasNetwork){
							Response originalResponse = chain.proceed(request);
							MediaType type = originalResponse.body().contentType();
							byte[] datas = originalResponse.body().bytes();
							response = originalResponse.newBuilder()
									.removeHeader("Pragma")
									.removeHeader("Cache-Control")
									.header("Cache-Control", "public, max-age=" + CACHE_MAX_AGE)
									.body(ResponseBody.create(type, datas))
									.build();

							mCacheOperator.insertResponse(url,decrypt(buffer.readString(body.contentType().charset())),
									new String(datas,"utf-8"));

						}else{

							String responseStr = mCacheOperator.queryResponse(url, buffer.readString(body.contentType().charset()));
							int maxStale = 60 * 60 * 24 * 28;
							boolean noCache = TextUtils.isEmpty( responseStr);
							ResponseBody responseBody = ResponseBody.create(TYPE_JSON,responseStr);
							response = new Response.Builder()
									.removeHeader("Pragma")
									.header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
									.body(responseBody)
									.request(request)
									.protocol(Protocol.HTTP_1_1)
									.code(noCache?404:200)
									.message("cache")
									.build();

						}

						return response;
	                }
	            })
	            .cache(new Cache(new File(context.getExternalCacheDir(),"theme_cache"), 1024*1024*20))       //cache for 20M
	            .readTimeout(10, TimeUnit.SECONDS)
	            .connectTimeout(10, TimeUnit.SECONDS)
	            .writeTimeout(10, TimeUnit.SECONDS)
	            .build();

	}
	
	/**
	 * @return 获取实例
	 */
	public static Http getHttp(Context context) {
	    if(instance == null) {
	    	synchronized (Http.class) {
				if(instance == null) {
		            instance = new Http(context);
				}
			}
	    }
	    return instance;
	}
	
	/**
	 * post 请求
	 * @param url url
	 * @param params 参数
	 * @param responseHandler 回调
	 */
	public void post(final String url, final Map<String, String> params, final IResponseHandler responseHandler) {
	    post(null, url, params, responseHandler);
	}
	
	/**
	 * post 请求
	 * @param context 发起请求的context，建议使用 用于取消请求
	 * @param url url
	 * @param params 参数
	 * @param responseHandler 回调
	 */
	public void post(Context context, final String url, final Map<String, String> params, final IResponseHandler responseHandler) {
	    FormBody.Builder builder = new FormBody.Builder();
	    if(params != null && params.size() > 0) {
	        for (Map.Entry<String, String> entry : params.entrySet()) {
	            builder.add(entry.getKey(), entry.getValue());
	        }
	    }
	
	    Request request;
	
	    if(context == null) {
	        request = new Request.Builder()
	                .url(url)
	                .post(builder.build())
	                .build();
	    } else {
	        request = new Request.Builder()
	                .url(url)
	                .post(builder.build())
	                .tag(context)
	                .build();
	    }
	
	    mClient.newCall(request).enqueue(new MyCallback(new Handler(), responseHandler, false));
	}
	
	public void post(final String url, final String sendJson, final IResponseHandler responseHandler) {
		post(null, url, sendJson, responseHandler);
	}
	public void post(Context context, final String url, final String sendJson, final IResponseHandler responseHandler) {
	    RequestBody requestBody = RequestBody.create(TYPE_JSON, encrypt(sendJson));
	    Request request;
	
	    if(context == null) {
	        request = new Request.Builder()
	                .url(url)
	                .post(requestBody)
	                .build();
	    } else {
	        request = new Request.Builder()
	                .url(url)
	                .post(requestBody)
	                .tag(context)
	                .build();
	    }
	
	    mClient.newCall(request).enqueue(new MyCallback(new Handler(), responseHandler, true));
	}
	
	public void get(final String url, final String json, final IResponseHandler responseHandler) {
	    get(null, url, json, responseHandler);
	}
	public void get(Context context, final String url, String json, final IResponseHandler responseHandler) {
	    RequestBody requestBody = RequestBody.create(TYPE_JSON, encrypt(json));
	    Request request;
	
	    if(context == null) {
	        request = new Request.Builder()
	                .url(url)
	                .post(requestBody)
	                .build();
	    } else {
	        request = new Request.Builder()
	                .url(url)
	                .post(requestBody)
	                .tag(context)
	                .build();
	    }
	
	    mClient.newCall(request).enqueue(new MyCallback(new Handler(), responseHandler, true));
	}
	
	/**
	 * get 请求
	 * @param url url
	 * @param params 参数
	 * @param responseHandler 回调
	 */
	public void get(final String url, final Map<String, String> params, final IResponseHandler responseHandler) {
	    get(null, url, params, responseHandler);
	}
	
	/**
	 * get 请求
	 * @param context 发起请求的context，建议使用 用于取消请求
	 * @param url url
	 * @param params 参数
	 * @param responseHandler 回调
	 */
	public void get(Context context, final String url, final Map<String, String> params, final IResponseHandler responseHandler) {
	    //split join url
	    String get_url = url;
	    if(params != null && params.size() > 0) {
	        int i = 0;
	        for (Map.Entry<String, String> entry : params.entrySet()) {
	            if(i++ == 0) {
	                get_url = get_url + "?" + entry.getKey() + "=" + entry.getValue();
	            } else {
	                get_url = get_url + "&" + entry.getKey() + "=" + entry.getValue();
	            }
	        }
	    }
	
	    Request request;
	
	    if(context == null) {
	        request = new Request.Builder()
	                .url(get_url)
	                .build();
	    } else {
	        request = new Request.Builder()
	                .url(get_url)
	                .tag(context)
	                .build();
	    }
	
	    mClient.newCall(request).enqueue(new MyCallback(new Handler(), responseHandler, false));
	}
	
	/**
	 * 上传文件
	 * @param url url
	 * @param files 上传的文件files
	 * @param responseHandler 回调
	 */
	public void upload(String url, Map<String, File> files, final IResponseHandler responseHandler) {
	    upload(null, url, null, files, responseHandler);
	}
	
	/**
	 * 上传文件
	 * @param url url
	 * @param params 参数
	 * @param files 上传的文件files
	 * @param responseHandler 回调
	 */
	public void upload(String url, Map<String, String> params, Map<String, File> files, final IResponseHandler responseHandler) {
	    upload(null, url, params, files, responseHandler);
	}
	
	/**
	 * 上传文件
	 * @param context 发起请求的context，建议使用 用于取消请求
	 * @param url url
	 * @param files 上传的文件files
	 * @param responseHandler 回调
	 */
	public void upload(Context context, String url, Map<String, File> files, final IResponseHandler responseHandler) {
	    upload(context, url, null, files, responseHandler);
	}
	
	/**
	 * 上传文件
	 * @param context 发起请求的context，建议使用 用于取消请求
	 * @param url url
	 * @param params 参数
	 * @param files 上传的文件files
	 * @param responseHandler 回调
	 */
	public void upload(Context context, String url, Map<String, String> params, Map<String, File> files, final IResponseHandler responseHandler) {
	    MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
	
	    if (params != null && !params.isEmpty()) {
	        for (String key : params.keySet()) {
	            multipartBuilder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
	                    RequestBody.create(null, params.get(key)));
	        }
	    }
	
	    //add upload file
	    if (files != null && !files.isEmpty()) {
	        RequestBody fileBody;
	        for (String key : files.keySet()) {
	            File file = files.get(key);
	            String fileName = file.getName();
	            fileBody = RequestBody.create(MediaType.parse(guessMimeType(fileName)), file);
	            multipartBuilder.addPart(Headers.of("Content-Disposition",
	                    "form-data; name=\"" + key + "\"; filename=\"" + fileName + "\""),
	                    fileBody);
	        }
	    }
	
	    Request request;
	    if(context == null) {
	        request = new Request.Builder()
	                .url(url)
	                .post(new ProgressRequestBody(multipartBuilder.build(),responseHandler))
	                .build();
	    } else {
	        request = new Request.Builder()
	                .url(url)
	                .post(new ProgressRequestBody(multipartBuilder.build(),responseHandler))
	                .tag(context)
	                .build();
	    }
	
	    mClient.newCall(request).enqueue(new MyCallback(new Handler(), responseHandler, false));
	}
	
	/**
	 * 下载文件
	 * @param url 下载地址
	 * @param filedir 下载目的目录
	 * @param filename 下载目的文件名
	 * @param downloadResponseHandler 下载回调
	 */
	public void download(String url, String filedir,String filename, long breakPoint, final DownloadResponseHandler downloadResponseHandler) {
	    download(null, url, filedir, filename, breakPoint, downloadResponseHandler);
	}
	
	/**
	 * 下载文件
	 * @param context 发起请求的context，建议使用 用于取消请求
	 * @param url 下载地址
	 * @param filedir 下载目的目录
	 * @param filename 下载目的文件名
	 * @param breakPoint 断点续传的断点
	 * @param downloadResponseHandler 下载回调
	 */
	public void download(Context context, String url, String filedir, String filename,long breakPoint, final DownloadResponseHandler downloadResponseHandler) {
	    mStartPoint = breakPoint;
	    Request request;
	    if(context == null) {
	        request = new Request.Builder()
	                .url(url)
	                .build();
	    } else {
	        request = new Request.Builder()
	                .url(url)
	                .header("RANGE", "bytes=" + mStartPoint + "-")
	                .tag(context)
	                .build();
	    }
	    mClient.newBuilder()
	            .addNetworkInterceptor(new Interceptor() {
	                @Override
	                public Response intercept(Chain chain) throws IOException {
	                    Response originalResponse = chain.proceed(chain.request());
	                    return originalResponse.newBuilder()
	                            .body(new ResponseProgressBody(originalResponse.body(), downloadResponseHandler))
	                            .build();
	                }
	            })
	            .build()
	            .newCall(request)
	            .enqueue(new MyDownloadCallback(new Handler(), downloadResponseHandler, filedir, filename));
	}
	
	/**
	 * 取消当前context的所有请求
	 * @param context 发起请求的context
	 */
	public void cancel(Context context) {
	    if(mClient != null) {
	        for(Call call : mClient.dispatcher().queuedCalls()) {
	            if(call.request().tag().equals(context))
	                call.cancel();
	        }
	        for(Call call : mClient.dispatcher().runningCalls()) {
	            if(call.request().tag().equals(context))
	                call.cancel();
	        }
	    }
	}
	
	//download callback
	private class MyDownloadCallback implements Callback {
	
	    private Handler mHandler;
	    private DownloadResponseHandler mDownloadResponseHandler;
	    private String mFileDir;
	    private String mFilename;
	
	    public MyDownloadCallback(Handler handler, DownloadResponseHandler downloadResponseHandler,
	                              String filedir, String filename) {
	        mHandler = handler;
	        mDownloadResponseHandler = downloadResponseHandler;
	        mFileDir = filedir;
	        mFilename = filename;
	    }
	
	    @Override
	    public void onFailure(Call call, final IOException e) {
	        mHandler.post(new Runnable() {
	            @Override
	            public void run() {
	                mDownloadResponseHandler.onFailure(e.toString());
	            }
	        });
	    }
	
	    @Override
	    public void onResponse(Call call, final Response response) throws IOException {
	        if(response.isSuccessful()) {
	            File file = null;
	            try {
	                file = saveFile(response, mFileDir, mFilename);
	            } catch (final IOException e) {
	                mHandler.post(new Runnable() {
	                    @Override
	                    public void run() {
	                        mDownloadResponseHandler.onFailure("onResponse saveFile fail." + e.toString());
	                    }
	                });
	            }
	
	            final File newFile = file;
	            mHandler.post(new Runnable() {
	                @Override
	                public void run() {
	                    mDownloadResponseHandler.onFinish(newFile);
	                }
	            });
	        } else {
	            mHandler.post(new Runnable() {
	                @Override
	                public void run() {
	                    mDownloadResponseHandler.onFailure("fail status=" + response.code());
	                }
	            });
	        }
	    }
	}
	
	//callback
	private class MyCallback implements Callback {
		private boolean mNeedDecrypt = true;
	    private Handler mHandler;
	    private IResponseHandler mResponseHandler;
	
	    public MyCallback(Handler handler, IResponseHandler responseHandler, boolean need) {
	        mHandler = handler;
	        mResponseHandler = responseHandler;
	        mNeedDecrypt = need;
	    }
	
	    @Override
	    public void onFailure(Call call, final IOException e) {
	        mHandler.post(new Runnable() {
	            @Override
	            public void run() {
					Log.e(TAG,"Http request onfailure->"+e);
	                mResponseHandler.onFailure(CommonUtil.hasNetwork(mContext)
							?STATUS_CODE_NETWORK_ERROR:STATUS_CODE_SERVER_ERROR, e.toString());

	            }
	        });
	    }
	
	    @Override
	    public void onResponse(Call call, final Response response) throws IOException {
	        if(response.isSuccessful()) {
	        	final String response_body;
	        	if(mNeedDecrypt) {
	        		response_body = decrypt(response.body().string());
	        	}else {
	        		response_body = response.body().string();
	        	}
	
	            if(mResponseHandler instanceof JsonResponseHandler) {       //json callback
	                try {
	                    final JSONObject jsonBody = new JSONObject(response_body);
	                    mHandler.post(new Runnable() {
	                        @Override
	                        public void run() {
	                            ((JsonResponseHandler)mResponseHandler).onSuccess(response.code(), jsonBody);
	                        }
	                    });
	                } catch (JSONException e) {
	                    mHandler.post(new Runnable() {
	                        @Override
	                        public void run() {
	                            mResponseHandler.onFailure(response.code(), "fail parse jsonobject, body=" + response_body);
	                        }
	                    });
	                }
	            } else if(mResponseHandler instanceof FastJsonResponseHandler) {    //fastjson callback
	                mHandler.post(new Runnable() {
	                    @Override
	                    public void run() {
	                        try {
	                            ((FastJsonResponseHandler)mResponseHandler).onSuccess(response.code(),
	                            		JSON.parseObject(response_body, ((FastJsonResponseHandler)mResponseHandler).getType(), mFeature));
	                        } catch (Exception e) {
	                            mResponseHandler.onFailure(response.code(), "fail parse jsonobject, body=" + response_body);
	                        }
	                    }
	                });
	            } else if(mResponseHandler instanceof RawResponseHandler) {     //raw string callback
	                mHandler.post(new Runnable() {
	                    @Override
	                    public void run() {
	                        ((RawResponseHandler)mResponseHandler).onSuccess(response.code(), response_body);
	                    }
	                });
	            }
	        } else {
	            mHandler.post(new Runnable() {
	                @Override
	                public void run() {
	                    mResponseHandler.onFailure(response.code(), "fail status=" + response.code());
						Log.e(TAG,"Http request onfailure->"+" status Code->"+response.code());
	                }
	            });
	        }
	    }
	}
	
	private File saveFile(Response response, String filedir, String filename) throws IOException {
	    File dir = new File(filedir);
	    if (!dir.exists()) {
	        dir.mkdirs();
	    }
	    File destination = new File(filedir + "/" + filename);
	    ResponseBody body = response.body();
	    InputStream in = body.byteStream();
	    FileChannel channelOut = null;
	    // RandomAccessFile  can specify breakpoint
	    RandomAccessFile randomAccessFile = null;
	    try {
	        randomAccessFile = new RandomAccessFile(destination, "rwd");
	        //usage in Chanel NIO, because RandomAccessFile does not use caching policies, Direct use will slow down the download
	        channelOut = randomAccessFile.getChannel();
	        // Memory mapping， direct use RandomAccessFile，is to specify the starting location of the download using its Seek method, use the cache download, specify the download location here.
	        MappedByteBuffer mappedBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE, mStartPoint, body.contentLength());
	        byte[] buffer = new byte[1024];
	        int len;
	        while ((len = in.read(buffer)) != -1) {
	            mappedBuffer.put(buffer, 0, len);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }finally {
	        try {
	            in.close();
	            if (channelOut != null) {
	                channelOut.close();
	            }
	            if (randomAccessFile != null) {
	                randomAccessFile.close();
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return destination;
	}
	
	//获取mime type
	private String guessMimeType(String path) {
	    FileNameMap fileNameMap = URLConnection.getFileNameMap();
	    String contentTypeFor = fileNameMap.getContentTypeFor(path);
	    if (contentTypeFor == null) {
	        contentTypeFor = "application/octet-stream";
	    }
	    return contentTypeFor;
	}
	
	/**
     * 字符串加密
     * @param str 待加密字符串
     * @return 加密后的字符串
     */
    public static String encrypt(String str) {
        String rstStr = "";
        if (str != null && str.length()>0) {
            int j = 0;
            char[] charArray = str.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                charArray[i] = (char) (charArray[i] ^ (666 + j));
                if(j++ > 10000){
                    j = 0;
                }
            }
            rstStr = new String(charArray);
        }

        return rstStr;
    }

    /**
     * 字符串解密
     * @param str 待解密字符串
     * @return 解密后的字符串
     */
    public static String decrypt(String str) {
        return encrypt(str);
    }
}

