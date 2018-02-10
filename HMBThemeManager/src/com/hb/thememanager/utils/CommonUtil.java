package com.hb.thememanager.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.MediaStore.Images;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;

public class CommonUtil {

	private static final String CHMOD_TAG = "chmod";
	
	public static enum NetWorkType {
		NO_NET, MOBILE_ONLY, WIFI
	};

	public static boolean hasSDCard() {
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			return false;
		}
		return true;
	}

	public static String getRootFilePath() {
		if (hasSDCard()) {
			return Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/";// filePath:/sdcard/
		} else {
			return Environment.getDataDirectory().getAbsolutePath() + "/data/"; // filePath:
																				// /data/data/
		}
	}


	/**
	 * 字节转换为M或者KB
	 * @param bytes
	 * @return
	 */
	public static String getReadableFileSize(long bytes) {
		BigDecimal filesize = new BigDecimal(bytes);
		BigDecimal megabyte = new BigDecimal(1024 * 1024);
		float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		if (returnValue > 1)
			return (returnValue + "MB");
		BigDecimal kilobyte = new BigDecimal(1024);
		returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		return (returnValue + "KB");
	}

	public static boolean hasNetwork(Context context) {
		boolean netstate = false;
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						netstate = true;
						break;
					}
				}
			}
		}
		return netstate;
	}

	
	   public static  Intent getHomeIntent() {
	        Intent intent = new Intent(Intent.ACTION_MAIN);
	        ComponentName componentName = new ComponentName(Config.LAUNCHER_PKG_NAME, Config.LAUNCHER_COMPONENT_NAME);
	        intent.setComponent(componentName);
	        intent.addCategory(Intent.CATEGORY_HOME);
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		   
	        return intent;
	    }
	   
	   
	
	
	public static void showToast(Context context, String tip) {
		Toast.makeText(context, tip, Toast.LENGTH_SHORT).show();
	}

	public static int getScreenWidth(Context context) {
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		return display.getWidth();
	}

	public static int getScreenHeight(Context context) {
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		return display.getHeight();
	}

	public static boolean chmodFile(String path) {
		if (Build.VERSION.SDK_INT > 16) {
			Process p = null;
			try {
				p = Runtime.getRuntime().exec("chmod -R 755 " + path);
				int status = p.waitFor();
				if (status == 0) {
					// chmod succeed
					return true;
				} else {
					// chmod failed
					Log.d(CHMOD_TAG, "chmodFile status = : "
							+ status);
					return false;
				}
			} catch (IOException e) {
				Log.d(CHMOD_TAG,
						"chmodFile IOException: " + e.toString());
				try {
					p = Runtime.getRuntime().exec("chmod -R 755 " + path);
					int status = p.waitFor();
					if (status == 0) {
						// chmod succeed
						return true;
					} else {
						// chmod failed
						Log.d(CHMOD_TAG,
								"2-->chmodFile status = : " + status);
						return false;
					}
				} catch (IOException e2) {
					Log.d(CHMOD_TAG,
							"2-->chmodFile IOException: " + e2.toString());
					return false;
				} catch (InterruptedException e3) {
					Log.d(CHMOD_TAG,
							"2-->chmodFile InterruptedException: "
									+ e3.toString());
					return false;
				} catch (Exception e4) {
					Log.d(CHMOD_TAG, "2-->chmodFile Exception: "
							+ e4.toString());
					return false;
				}
			} catch (InterruptedException e) {
				Log.d(CHMOD_TAG,
						"chmodFile InterruptedException: " + e.toString());
				return false;
			} catch (Exception e) {
				Log.d(CHMOD_TAG,
						"chmodFile Exception: " + e.toString());
				return false;
			} finally {
				if (p != null)
					p.destroy();
			}
		} else {
			return true;
		}
	}

	public static String getCurrentTime() {
		String tempDate = new SimpleDateFormat("yyyyMMdd HH:mm").format(System
				.currentTimeMillis());
		return tempDate;
	}

	public static String getDateFromCurrent(int delta) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, delta);
		return new SimpleDateFormat("yyyyMMdd").format(calendar
				.getTimeInMillis());
	}

	public static String messageDiestBuilder(String str) {
		MessageDigest md5;
		StringBuffer resultBuffer = new StringBuffer();
		try {
			md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(str.getBytes());
			byte[] byteArray = md5.digest();

			for (int i = 0; i < byteArray.length; i++) {
				if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) {
					resultBuffer.append("0").append(
							Integer.toHexString(0xFF & byteArray[i]));
				} else {
					resultBuffer.append(Integer
							.toHexString(0xFF & byteArray[i]));
				}
			}

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
		}

		return resultBuffer.toString();
	}


	public static int getNetWorkType(Context context) {
		final ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connMgr.getActiveNetworkInfo();
		if(info == null){
			return  ConnectivityManager.TYPE_NONE;
		}
		if(info.getType() == ConnectivityManager.TYPE_WIFI && info.isConnected()){
			return ConnectivityManager.TYPE_WIFI;
		}else if(info.getType() == ConnectivityManager.TYPE_MOBILE && info.isConnected()){
			return ConnectivityManager.TYPE_MOBILE;
		}else{
			return ConnectivityManager.TYPE_NONE;
		}
	}

	/**
	 * 当前网络是移动网络
	 * @param context
	 * @return
	 */
	public static boolean isMobileNetwork(Context context){
		return getNetWorkType(context) == ConnectivityManager.TYPE_MOBILE;
	}

	/**
	 * 当前网络是WiFi
	 * @param context
	 * @return
	 */
	public static boolean isWifiNetwork(Context context){
		return getNetWorkType(context) == ConnectivityManager.TYPE_WIFI;
	}



	public static ContentValues getContentValues(Context context,
			Uri sourceUri, File file, long time, int width, int height) {
		final ContentValues values = new ContentValues();

		// time /= 1000;
		values.put(Images.Media.TITLE, file.getName());
		values.put(Images.Media.DISPLAY_NAME, file.getName());
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.DATE_TAKEN, time);
		values.put(Images.Media.DATE_MODIFIED, time);
		values.put(Images.Media.DATE_ADDED, time);
		values.put(Images.Media.ORIENTATION, 0);
		values.put(Images.Media.DATA, file.getAbsolutePath());
		values.put(Images.Media.SIZE, file.length());

		if (width != 0)
			values.put(Images.Media.WIDTH, width);
		if (height != 0)
			values.put(Images.Media.HEIGHT, height);

		// This is a workaround to trigger the MediaProvider to re-generate the
		// thumbnail.
		values.put(Images.Media.MINI_THUMB_MAGIC, 0);

		/*
		 * final String[] projection = new String[] { ImageColumns.DATE_TAKEN,
		 * ImageColumns.LATITUDE, ImageColumns.LONGITUDE, };
		 * 
		 * querySource(context, sourceUri, projection, new
		 * ContentResolverQueryCallback() {
		 * 
		 * @Override public void onCursorResult(Cursor cursor) { //paul del
		 * //values.put(Images.Media.DATE_TAKEN, cursor.getLong(0));
		 * 
		 * double latitude = cursor.getDouble(1); double longitude =
		 * cursor.getDouble(2); Log.d("Wallpaper_DEBUG",
		 * "NextDayLoadingPictureTask---------------latitude1 = "+latitude);
		 * Log.d("Wallpaper_DEBUG",
		 * "NextDayLoadingPictureTask---------------longitude2 = "+longitude);
		 * // TODO: Change || to && after the default location // issue is
		 * fixed. if ((latitude != 0f) || (longitude != 0f)) {
		 * values.put(Images.Media.LATITUDE, latitude);
		 * values.put(Images.Media.LONGITUDE, longitude);
		 * Log.d("Wallpaper_DEBUG",
		 * "NextDayLoadingPictureTask---------------latitude2 = "+latitude);
		 * Log.d("Wallpaper_DEBUG",
		 * "NextDayLoadingPictureTask---------------longitude2 = "+longitude); }
		 * } });
		 */
		return values;
	}

	public static void querySource(Context context, Uri sourceUri,
			String[] projection, ContentResolverQueryCallback callback) {
		ContentResolver contentResolver = context.getContentResolver();
		querySourceFromContentResolver(contentResolver, sourceUri, projection,
				callback);
	}

	private static void querySourceFromContentResolver(
			ContentResolver contentResolver, Uri sourceUri,
			String[] projection, ContentResolverQueryCallback callback) {
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(sourceUri, projection, null, null,
					null);
			if ((cursor != null) && cursor.moveToNext()) {
				callback.onCursorResult(cursor);
			}
		} catch (Exception e) {
			// Ignore error for lacking the data column from the source.
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public interface ContentResolverQueryCallback {
		void onCursorResult(Cursor cursor);
	}

	
	/**
	 * @Title: intstallApp @Description: 安装应用 @param @param
	 * context @param @param apkFile @param @param observer @return void @throws
	 */
	public static int intstallApp(Context context, String packageName, File apkFile,
			IPackageInstallObserver.Stub observer) {
		PackageManager pm = context.getPackageManager();
		if (TextUtils.isEmpty(packageName)) {
			PackageParser.Package parsed = getPackageInfo(apkFile);
			packageName = parsed.packageName;
		}
		int result = 0;
		int installFlags = 0;
		try {
			PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
			if (pi != null) {
				installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
				result = 1;
			}
		} catch (NameNotFoundException e) {
			// e.printStackTrace();
		}

		Uri mPackageURI = Uri.fromFile(apkFile);
		String filepath = mPackageURI.getPath();
		pm.installPackage(mPackageURI, observer, installFlags, null);
		return result;
	}
	
	public static void deletePackage(Context context,String packageName, IPackageDeleteObserver observer){
		PackageManager pm = context.getPackageManager();
		if(TextUtils.isEmpty(packageName)){
			return;
		}
		PackageInfo appInfo;
		try {
			appInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			if(appInfo != null){
				pm.deletePackage(packageName, observer, 0);
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void restartApplication(Context context,String packageName){
		PackageManager pm = context.getPackageManager();
		
	}
	
	public static <T> List<T> compare(List<T> t1, List<T> t2) {    
	      List<T> list2 = new ArrayList<T>();    
	      for (T t : t2) {    
	          if (!t1.contains(t)) {    
	              list2.add(t);    
	          }    
	      }    
	      return list2;    
	  }   
	
	public static <T> List<T> compareArray(T[] t1, T[] t2) {    
		List<T> list1 = Arrays.asList(t1);    
	      List<T> list2 = new ArrayList<T>();    
	      for (T t : t2) {    
	          if (!list1.contains(t)) {    
	              list2.add(t);    
	          }    
	      }    
	      return list2;    
	  } 
	  
	
	
	
	private static PackageParser.Package getPackageInfo(File sourceFile) {

		DisplayMetrics metrics = new DisplayMetrics();
		metrics.setToDefaults();
		Object pkg = null;
		final String archiveFilePath = sourceFile.getAbsolutePath();
		try {
			Class<?> clazz = Class.forName("android.content.pm.PackageParser");
			Object instance = getParserObject(archiveFilePath);
			if (Build.VERSION.SDK_INT >= 21) {
				Method method = clazz.getMethod("parsePackage", File.class, int.class);
				pkg = method.invoke(instance, sourceFile, 0);
			} else {
				Method method = clazz.getMethod("parsePackage", File.class, String.class, DisplayMetrics.class,
						int.class);
				pkg = method.invoke(instance, sourceFile, archiveFilePath, metrics, 0);
			}
			instance = null;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (PackageParser.Package) pkg;
	}
	
	private static Object getParserObject(String archiveFilePath) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {
		Class<?> clazz = Class.forName("android.content.pm.PackageParser");
		return Build.VERSION.SDK_INT >= 21 ? clazz.getConstructor()
				.newInstance() : clazz.getConstructor(String.class)
				.newInstance(archiveFilePath);
	}
	
	public static boolean copyFile(String fromPath, String toPath) {
		File fromFile = new File(fromPath);
		if (!fromFile.exists())
			return false;

		File toFile = new File(toPath);
		if (toFile.exists())
			return false;

		try {
			FileInputStream inputStream = new FileInputStream(fromFile);
			FileOutputStream outputStream = new FileOutputStream(toFile);

			byte[] bt = new byte[1024];
			int c;
			while ((c = inputStream.read(bt)) > 0) {
				outputStream.write(bt, 0, c);
			}
			// close input and output stream
			inputStream.close();
			outputStream.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		return true;
	}


	/**
	 * 获取手机IMEI号
	 * @param context
	 * @return
	 */
	public static String getIMEI(Context context){
		if(Config.DEBUG){
			return "1986511651001510";
		}else {
			TelephonyManager tpm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			return tpm.getDeviceId();
		}
	}


	/**
	 * 获取手机Model
	 * @param context
	 * @return
	 */
	public static String getModel(Context context){
		return Build.MODEL;
	}

	/**
	 * 获取主题商店VersionCode
	 * @param context
	 * @return
	 */
	public static long getThemeAppVersion(Context context){
		return context.getApplicationContext().getApplicationInfo().versionCode;
	}


	/**
	 * 获取ＲＯＭ版本号
	 * @return
	 */
	public static String getRomVersion(){
		return SystemProperties.get("ro.fota.version");
	}




	public static int getAddCommentTips(Context context,Theme theme){
		boolean themeDownloaded = ((ThemeManagerApplication)context.getApplicationContext()).getThemeManager()
				.themeExists(theme);
		if(!theme.isPaid() && !theme.isFree()){
			return R.string.add_comment_pay_tip;
		}


		if((theme.isPaid() || theme.isFree()) && !themeDownloaded){
			return R.string.add_comment_download_tip;
		}
		return  0;
	}


	public static User getUser(Context context){
		return User.getInstance(context.getApplicationContext());
	}


	public static void getThemePrice(Theme theme,Context context){
		ThemeManager tm = ThemeManagerImpl.getInstance(context);
		User user = User.getInstance(context);
		boolean themeExits = tm.themeExists(theme);
		boolean isBuyForUser = user.isLogin()?
				tm.themeIsBuyByUser(theme,Integer.parseInt(user.getId())):false;
		Resources res = context.getResources();

		String newPrice = null;

		if(themeExits) {
			if(theme.isFree()){
				newPrice = res.getString(R.string.downloaded);
			}else{
				if(isBuyForUser){
					newPrice = res.getString(R.string.downloaded);
				}
			}
		}else{
			if(!theme.isFree() && isBuyForUser){
				newPrice = res.getString(R.string.paid);
			}
		}

		TLog.d("newPrice","exists->"+themeExits+" newPirce->"+newPrice);
		
		theme.downloadStateStr = newPrice;
	}


}
