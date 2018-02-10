package com.android.downloadui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Downloads;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
/**
 * @author wxue
 */
public class HbCommonUtil {
	public static final String AUTHORITY = "downloads";
	public static final int HB_SORT_ORDER_NAME = 1;
	public static final int HB_SORT_ORDER_TIME = 2;
	public static final int HB_SORT_ORDER_SIZE_BIG_SMALL= 3;
	public static final int HB_SORT_ORDER_SIZE_SMALL_BIG = 4;
	public static final String HB_ACTION_SPEEDS = "com.android.providers.downloads.action.DOWNLOAD_SPEEDS";
	public static final String HB_IDS = "down_ids";
	public static final String HB_SPEEDS = "down_speeds";
	
	private static final String[][] MIME_MapTable={ 
            {".3gp",    "video/3gpp"}, 
            {".apk",    "application/vnd.android.package-archive"}, 
            {".asf",    "video/x-ms-asf"}, 
            {".avi",    "video/x-msvideo"}, 
            {".bin",    "application/octet-stream"}, 
            {".bmp",    "image/bmp"}, 
            {".c",  "text/plain"}, 
            {".class",  "application/octet-stream"}, 
            {".conf",   "text/plain"}, 
            {".cpp",    "text/plain"}, 
            {".doc",    "application/msword"}, 
            {".docx",   "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}, 
            {".xls",    "application/vnd.ms-excel"},  
            {".xlsx",   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}, 
            {".exe",    "application/octet-stream"}, 
            {".gif",    "image/gif"}, 
            {".gtar",   "application/x-gtar"}, 
            {".gz", "application/x-gzip"}, 
            {".h",  "text/plain"}, 
            {".htm",    "text/html"}, 
            {".html",   "text/html"}, 
            {".jar",    "application/java-archive"}, 
            {".java",   "text/plain"}, 
            {".jpeg",   "image/jpeg"}, 
            {".jpg",    "image/jpeg"}, 
            {".js", "application/x-javascript"}, 
            {".log",    "text/plain"}, 
            {".m3u",    "audio/x-mpegurl"}, 
            {".m4a",    "audio/mp4a-latm"}, 
            {".m4b",    "audio/mp4a-latm"}, 
            {".m4p",    "audio/mp4a-latm"}, 
            {".m4u",    "video/vnd.mpegurl"}, 
            {".m4v",    "video/x-m4v"},  
            {".mov",    "video/quicktime"}, 
            {".mp2",    "audio/x-mpeg"}, 
            {".mp3",    "audio/x-mpeg"}, 
            {".mp4",    "video/mp4"}, 
            {".mpc",    "application/vnd.mpohun.certificate"},        
            {".mpe",    "video/mpeg"},   
            {".mpeg",   "video/mpeg"},   
            {".mpg",    "video/mpeg"},   
            {".mpg4",   "video/mp4"},    
            {".mpga",   "audio/mpeg"}, 
            {".msg",    "application/vnd.ms-outlook"}, 
            {".ogg",    "audio/ogg"}, 
            {".pdf",    "application/pdf"}, 
            {".png",    "image/png"}, 
            {".pps",    "application/vnd.ms-powerpoint"}, 
            {".ppt",    "application/vnd.ms-powerpoint"}, 
            {".pptx",   "application/vnd.openxmlformats-officedocument.presentationml.presentation"}, 
            {".prop",   "text/plain"}, 
            {".rc", "text/plain"}, 
            {".rmvb",   "audio/x-pn-realaudio"}, 
            {".rtf",    "application/rtf"}, 
            {".sh", "text/plain"}, 
            {".tar",    "application/x-tar"},    
            {".tgz",    "application/x-compressed"},  
            {".txt",    "text/plain"}, 
            {".wav",    "audio/x-wav"}, 
            {".wma",    "audio/x-ms-wma"}, 
            {".wmv",    "audio/x-ms-wmv"}, 
            {".wps",    "application/vnd.ms-works"}, 
            {".xml",    "text/plain"}, 
            {".z",  "application/x-compress"}, 
            {".zip",    "application/x-zip-compressed"}, 
            {"",        "*/*"}   
        }; 

	public static String getMIMEType(String filePath) { 
	    String type="*/*"; 
	    int dotIndex = filePath.lastIndexOf("."); 
	    if(dotIndex < 0){ 
	        return type; 
	    } 
	    String end=filePath.substring(dotIndex,filePath.length()).toLowerCase(); 
	    if(end=="")return type; 
	    for(int i=0;i<MIME_MapTable.length;i++){
	        if(end.equals(MIME_MapTable[i][0])) 
	            type = MIME_MapTable[i][1]; 
	    }        
	    return type; 
	} 
	
	public static void openFile(Context context, String filePath, String mimeType){ 
	    Intent intent = new Intent(Intent.ACTION_VIEW); 
	    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	    if(TextUtils.isEmpty(mimeType)){
	    	mimeType = getMIMEType(filePath); 
	    }
	    intent.setDataAndType(Uri.fromFile(new File(filePath)), mimeType); 
	    context.startActivity(intent); 
	} 
	
	
	public static Uri  getDocumentUri(int docId){
		return ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, docId);
	}
	
	public static String getWhereClauseForIds(long[] ids) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                whereClause.append("OR ");
            }
            whereClause.append(Downloads.Impl._ID);
            whereClause.append(" = ? ");
        }
        whereClause.append(")");
        return whereClause.toString();
    }

    /**
     * Get the selection args for a clause returned by {@link #getWhereClauseForIds(long[])}.
     */
    public static String[] getWhereArgsForIds(long[] ids) {
        String[] whereArgs = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            whereArgs[i] = Long.toString(ids[i]);
        }
        return whereArgs;
    }
    
    public static String getDownloadingFileName(String uri){
    	String name = "";
    	if(!TextUtils.isEmpty(uri)){
    		int index = uri.lastIndexOf('/') + 1;
            if (index > 0) {
            	name = uri.substring(index);
            } 
    	}
    	return name;
    }
    
    public static boolean isValidUrl(String downUrl){
    	boolean isValid = false; 
    	try {
			URL url = new URL(downUrl);
			isValid = url.getProtocol().equals("http") || url.getProtocol().equals("https");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			isValid = false; 
		}
    	return isValid;
    }
    
    public static String formatFileSize(long sizeBytes) {
        float result = sizeBytes;
        String suffix = "B";
        if (result > 900) {
            suffix = "KB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = "MB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = "GB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = "TB";
            result = result / 1024;
        }
        if (result > 900) {
            suffix = "PB";
            result = result / 1024;
        }
        final String roundedString = String.format("%.2f", result);
        return roundedString + " " + suffix;
    }
    
    /**
     * NetState
     * NET_NO：no network 
     * NET_2G :  2g 
     * NET_3G：3g 
     * NET_4G：4g
     * NET_WIFI：wifi
     * NET_UNKNOWN：unknown net
     */
    public enum NetState {
        NET_NO, NET_2G, NET_3G, NET_4G, NET_WIFI, NET_UNKNOWN
    }

    /**
     * get currently netstate
     * @param context 
     * @return NetState
     */
    public static NetState getNetState(Context context) {
        NetState stateCode = NetState.NET_NO;
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.isConnectedOrConnecting()) {
            switch (ni.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    stateCode = NetState.NET_WIFI;
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    switch (ni.getSubtype()) {
                        case TelephonyManager.NETWORK_TYPE_GPRS: // 联通2g
                        case TelephonyManager.NETWORK_TYPE_CDMA: // 电信2g
                        case TelephonyManager.NETWORK_TYPE_EDGE: // 移动2g
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                            stateCode = NetState.NET_2G;
                            break;
                        case TelephonyManager.NETWORK_TYPE_EVDO_A: // 电信3g
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        case TelephonyManager.NETWORK_TYPE_EHRPD:
                        case TelephonyManager.NETWORK_TYPE_HSPAP:
                            stateCode = NetState.NET_3G;
                            break;
                        case TelephonyManager.NETWORK_TYPE_LTE:
                            stateCode = NetState.NET_4G;
                            break;
                        default:
                            stateCode = NetState.NET_UNKNOWN;
                    }
                    break;
                default:
                    stateCode = NetState.NET_UNKNOWN;
            }

        }
        return stateCode;
}
}
