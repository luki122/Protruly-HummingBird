package com.hmb.manager.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hmb.manager.Constant;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.update.UpdateReceiver;
import com.hb.themeicon.theme.IconManager;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.util.Log;
import android.os.storage.VolumeInfo;
public class ManagerUtils {
	public static AppInfo getAppInfoByPackageName(Context context, String packageName) {

		PackageManager packageManager = context.getPackageManager();
        IconManager iconManager = IconManager.getInstance(context, true, false);

		ApplicationInfo application;
		AppInfo appInfo = null;
		try {
			application = packageManager.getPackageInfo(packageName, 0).applicationInfo;
			appInfo = new AppInfo();

            Drawable icon = iconManager.getIconDrawable(packageName, UserHandle.CURRENT);
            if (icon == null) {
                icon = application.loadIcon(packageManager);
            }

			appInfo.setPkgName(packageName);
			appInfo.setAppIcon(icon);
			appInfo.setAppLabel(application.loadLabel(packageManager).toString());
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return appInfo;
	}

	public static AppInfo getApkInfoByAbsPath(Context context, String absPath) {
		AppInfo appInfo = new AppInfo();
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = pm.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES);
		if (pkgInfo != null) {
			ApplicationInfo applicationInfo = pkgInfo.applicationInfo;
			applicationInfo.sourceDir = absPath;
			applicationInfo.publicSourceDir = absPath;
			appInfo.setAppLabel(pm.getApplicationLabel(applicationInfo).toString());
			appInfo.setPkgName(applicationInfo.packageName);
			appInfo.setAppIcon(pm.getApplicationIcon(applicationInfo));
		}
		return appInfo;
	}
	
	public static  List<ApplicationInfo>  filterSystemAPP(List<ApplicationInfo> packageInfos){
		List<ApplicationInfo> infoList=new ArrayList<ApplicationInfo>();
		for (ApplicationInfo info : packageInfos) {
			if ((info.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
				infoList.add(info);
			}
		}
	return infoList;
	}
	
	public static  List<PackageStats>  filterSpecialAPP(List<PackageStats> stats,Context mContext){
		List<PackageStats> installedAppLists = new ArrayList<PackageStats>();
		for(PackageStats s:stats){
			if(!isFilterPackage(s.packageName,mContext)){
				installedAppLists.add(s);
			}
		}
		return installedAppLists;
	}

	public static AppInfo getAppInfo(Context context, ApplicationInfo app, int pid, String processName) {
		AppInfo appInfo = new AppInfo();
		PackageManager pm = context.getPackageManager();
		
        IconManager iconManager = IconManager.getInstance(context, true, false);
        Drawable icon = iconManager.getIconDrawable(app.packageName, UserHandle.CURRENT);
        if (icon == null) {
            icon = app.loadIcon(pm);
        }
		
		appInfo.setAppLabel((String) app.loadLabel(pm));
		appInfo.setAppIcon(icon);
		appInfo.setPkgName(app.packageName);

		appInfo.setPid(pid);
		appInfo.setProcessName(processName);

		return appInfo;
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {

		Bitmap bitmap = Bitmap.createBitmap(

				drawable.getIntrinsicWidth(),

				drawable.getIntrinsicHeight(),

				drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888

						: Bitmap.Config.RGB_565);

		Canvas canvas = new Canvas(bitmap);

		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

		drawable.draw(canvas);

		return bitmap;

	}

	public static byte[] bitmapToByte(Bitmap bitmap) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		return baos.toByteArray();
	}

	public static Bitmap byteToBitmap(byte[] data) {
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	public static Drawable bitmapToDrawable(Bitmap bitmap) {
		Drawable drawable = new BitmapDrawable(bitmap);
		return drawable;
	}

	public static Drawable getAppIcon(Context context, String apkFilepath) {
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = getPackageInfo(context, apkFilepath);
		if (pkgInfo == null) {
			return null;
		}

		ApplicationInfo appInfo = pkgInfo.applicationInfo;
		if (Build.VERSION.SDK_INT >= 8) {
			appInfo.sourceDir = apkFilepath;
			appInfo.publicSourceDir = apkFilepath;
		}
		return pm.getApplicationIcon(appInfo);
	}

	public static PackageInfo getPackageInfo(Context context, String apkFilepath) {
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = null;
		try {
			pkgInfo = pm.getPackageArchiveInfo(apkFilepath,
					PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
		} catch (Exception e) {
			// should be something wrong with parse
			e.printStackTrace();
		}
		return pkgInfo;
	}

	public static CharSequence getAppLabel(Context context, String apkFilepath) {
		PackageManager pm = context.getPackageManager();
		PackageInfo pkgInfo = getPackageInfo(context, apkFilepath);
		if (pkgInfo == null) {
			return null;
		}
		ApplicationInfo appInfo = pkgInfo.applicationInfo;
		if (Build.VERSION.SDK_INT >= 8) {
			appInfo.sourceDir = apkFilepath;
			appInfo.publicSourceDir = apkFilepath;
		}

		return pm.getApplicationLabel(appInfo);
	}

	public static long getUnUseMem(Context mContext) {
		long unUseMem = 0;
		// 得到ActivityManager
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE); // 创建ActivityManager.MemoryInfo对象
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(mi);
		// 取得剩余的内存空间
		unUseMem = mi.availMem;
		return unUseMem;
	}

	public static long getTotalMem() {
		long mTotal = 0;
		// /proc/meminfo读出的内核信息进行解释
		String path = "/proc/meminfo";
		String content = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(path), 8);
			String line;
			if ((line = br.readLine()) != null) {
				content = line;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// beginIndex
		int begin = content.indexOf(':');
		// endIndex
		int end = content.indexOf('k');
		// 截取字符串信息
		content = content.substring(begin + 1, end).trim();
		mTotal = Integer.parseInt(content);
		return mTotal * 1024;
	}

	public static long getInternalMemorySize(Context context) {
		File file = Environment.getDataDirectory();
		StatFs statFs = new StatFs(file.getPath());
		long blockSizeLong = statFs.getBlockSizeLong();
		long blockCountLong = statFs.getBlockCountLong();
		long size = blockCountLong * blockSizeLong;
		return size;
	}

	public static long getAvailableInternalMemorySize(Context context) {
		File file = Environment.getDataDirectory();
		StatFs statFs = new StatFs(file.getPath());
		long availableBlocksLong = statFs.getAvailableBlocksLong();
		long blockSizeLong = statFs.getBlockSizeLong();
		return availableBlocksLong * blockSizeLong;
	}

	public static long getFlashSize(boolean isInternal) {
		long totalSize = 0;
		try {
			Process p = Runtime.getRuntime().exec("mount");
			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			String[] spitItem;
			List<String> pathList = new ArrayList<>();
			boolean isTmpfsAdded = false;
			while ((line = br.readLine()) != null) {
				spitItem = line.split(" ", 4);
				if (spitItem.length > 1
						&& (spitItem[0].startsWith("/dev/block/") || (!isTmpfsAdded && spitItem[0].equals("tmpfs")))) {
					if (!isTmpfsAdded && spitItem[0].equals("tmpfs")) {
						isTmpfsAdded = true;
					}
					if (isInternal && !spitItem[2].startsWith("ext") && !spitItem[2].equals("tmpfs")) {
						continue;
					} else if (!isInternal && !spitItem[2].equals("vfat")) {
						continue;
					}
					pathList.add(spitItem[1]);
					totalSize += getPathSize(spitItem[1]);
				}
			}
			br.close();
			is.close();
			p.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (totalSize > 31000000000l && totalSize <= 34359738368l) {
			totalSize = 34359738368l;
		}
		if (totalSize > 15000000000l && totalSize <= 17179869184l) {
			totalSize = 17179869184l;
		}
		if(totalSize>61203283968l&&totalSize <= 68719476736l){
			totalSize = 68719476736l;
		}
		if(totalSize>122406567936l&&totalSize <= 137438953472l){
			totalSize = 137438953472l;
		}
		return totalSize;
	}

	public static long getPathSize(String path) {
		File file = new File(path);
		return file.exists() ? file.getTotalSpace() : 0;

	}

	public static long getUsedInternalMemorySize(Context context) {
		long size = 0;
		size = getFlashSize(true) - getAvailableInternalMemorySize(context);
		return size;
	}

	public static Map<String, ActivityManager.RunningAppProcessInfo> thirdApplicationFilter(
			Map<String, ActivityManager.RunningAppProcessInfo> map,Context mContext) {
		ApplicationInfo app = null;
		PackageManager pm=mContext.getPackageManager();
		Map<String, ActivityManager.RunningAppProcessInfo> pkgMap = new HashMap<String, ActivityManager.RunningAppProcessInfo>();
		for (Map.Entry<String, ActivityManager.RunningAppProcessInfo> entry : map.entrySet()) {
			if (isFilterCachePackage(entry.getKey(),mContext))
				continue;
			try {
				app = pm.getApplicationInfo(entry.getKey(), 0);
				if ((app.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
					pkgMap.put(entry.getKey(), entry.getValue());
				}
			} catch (PackageManager.NameNotFoundException e) {
				return null;
			}
		}
		return pkgMap;
	}
	
	public static Map<String, ActivityManager.RunningAppProcessInfo> thirdBgApplicationFilter(
			Map<String, ActivityManager.RunningAppProcessInfo> map,Context mContext) {
		ApplicationInfo app = null;
		PackageManager pm=mContext.getPackageManager();
		Map<String, ActivityManager.RunningAppProcessInfo> pkgMap = new HashMap<String, ActivityManager.RunningAppProcessInfo>();
		for (Map.Entry<String, ActivityManager.RunningAppProcessInfo> entry : map.entrySet()) {
			if (isBgFilterPackage(entry.getKey(),mContext))
				continue;
			try {
				app = pm.getApplicationInfo(entry.getKey(), 0);
				if ((app.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
					pkgMap.put(entry.getKey(), entry.getValue());
				}
			} catch (PackageManager.NameNotFoundException e) {
				return null;
			}
		}
		return pkgMap;
	}
	
	public static List<ApplicationInfo> thirdAppApplicationFilter(
			List<ApplicationInfo> packageInfos,Context mContext) {
		PackageManager pm=mContext.getPackageManager();
		List<ApplicationInfo> mList=new ArrayList<ApplicationInfo>();
		for(ApplicationInfo info:packageInfos){
			if ((info.flags & ApplicationInfo.FLAG_SYSTEM) <= 0&& !ManagerUtils.isFilterPackage(info.packageName,mContext)) {
				mList.add(info);
			}
		}
		return mList;
	}
	
	public static boolean isFilterPackage(String packName, Context mContext) {
		if (packName.equals(mContext.getPackageName()) || packName.equals("com.baidu.map.location")
				|| packName.equals("com.tencent.android.location") || packName.equals("cn.protruly.spiderman")
				|| packName.equals("com.tencent.mm") || packName.equals("com.tencent.mobileqq")) {
			return true;
		}
		return false;
	}

	public static boolean isFilterCachePackage(String packName, Context mContext) {
		if (packName.equals(mContext.getPackageName()) || packName.equals("com.baidu.map.location")
				|| packName.equals("com.tencent.android.location") || packName.equals("cn.protruly.spiderman")
				|| packName.equals("com.tencent.mm") || packName.equals("com.tencent.mobileqq")) {
			return true;
		}
		return false;
	}
	
	public static boolean isBgFilterPackage(String packName, Context mContext) {
		if (packName.equals(mContext.getPackageName()) || packName.equals("com.baidu.map.location")
				|| packName.equals("com.tencent.android.location") || packName.equals("cn.protruly.spiderman")) {
			return true;
		}
		return false;
	}
	
	public static boolean isPhoneHasLock(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
	}
	
	public static boolean isVirtusScanned(Context context){
		long mScanVirusTime=SPUtils.instance(context).getLongValue(Constant.SHARED_PREFERENCES_KEY_QSCAN_TIME, System.currentTimeMillis());
		double virusScantimeDay=countDays(System.currentTimeMillis(),mScanVirusTime);
		if(virusScantimeDay<1){
			return true;
		}
		return false;
	}
	
	public static double countDays(long now,long start){
		double timeDay=0;
		if(now>=start){
			timeDay=(double)(now-start)/(1000*3600*24);
		}
		return timeDay;
	}
	
	public static int getPhoneScore(Context context){
		long availMem=ManagerUtils.getAvailableInternalMemorySize(context);
		long tatalMem=ManagerUtils.getFlashSize(true);
		double percentD=0;
		percentD=(double)availMem/tatalMem;
		int totalScore=100;
		long mScanVirusTime=SPUtils.instance(context).getLongValue(Constant.SHARED_PREFERENCES_KEY_QSCAN_TIME, System.currentTimeMillis());
		double virusScantimeDay=countDays(System.currentTimeMillis(),mScanVirusTime);
		totalScore=totalScore-dayToScore(virusScantimeDay);
		if(!isPhoneHasLock(context)){
			totalScore=totalScore-15;
		}
		if(percentD<=0.1){
			totalScore=totalScore-5;
		}
		long onekeyCleanUpTime=SPUtils.instance(context).getLongValue(Constant.ONEKEY_CLEANUP_TIME, System.currentTimeMillis());
		double onekeyCleanUpDay=countDays(System.currentTimeMillis(),onekeyCleanUpTime);
		long cacheCleanUpTime=SPUtils.instance(context).getLongValue(Constant.CACHE_CLEANUP_TIME, System.currentTimeMillis());
		double cacheCleanUpDay=countDays(System.currentTimeMillis(),cacheCleanUpTime);
		long rubblishCleanUpTime=SPUtils.instance(context).getLongValue(Constant.RUBBLISH_CLEANUP_TIME, System.currentTimeMillis());
		double rubblishCleanUpDay=countDays(System.currentTimeMillis(),rubblishCleanUpTime);
		long memoryCleanUpTime=SPUtils.instance(context).getLongValue(Constant.MEMORY_CLEAN_TIME, System.currentTimeMillis());
		double memoryCleanUpDay=countDays(System.currentTimeMillis(),memoryCleanUpTime);
		if(onekeyCleanUpDay>=1){
			if(cacheCleanUpDay>=1){
				totalScore=totalScore-dayToScore(cacheCleanUpDay);
			}
			if(rubblishCleanUpDay>=1){
				totalScore=totalScore-dayToScore(rubblishCleanUpDay);
			}
			if(memoryCleanUpDay>=1){
				totalScore=totalScore-dayToScore(memoryCleanUpDay);
			}
		}
		return totalScore;
	}
	
	public static int dayToScore(double dayCon){
		int num=0;
		if(dayCon>=20){
			num=20;
		}else if(dayCon>=1&&dayCon<20){
			num=(int)dayCon;
		}
		return num;
	}
	
	public static Set<String> getWhiteListAPP(){
		Set<String> apkSets=new HashSet<String>();
		apkSets.add("/data/data/cn.protruly.spiderman/spiderman");
		return apkSets;
	}

	public static void scheduleUpdateService(Context context) {
		Intent intent = new Intent(context, UpdateReceiver.class);
		intent.setAction(UpdateReceiver.ACTION_AUTO_CHECK);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent autoCheckIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(autoCheckIntent);
		// Load the required settings from preferences
		long lastCheck = SPUtils.instance(context)
				.getLongValue(Constant.SHARED_PREFERENCES_LAST_CHECK_UPDATE_TIME, 0);
		Log.d("Update", "scheduleUpdateService -> lastCheck = " + lastCheck + ", updateFrequency = "
				+ Constant.AUTO_CHECK_INTERVAL + ", currentTime = " + System.currentTimeMillis());
		am.setRepeating(AlarmManager.RTC_WAKEUP, lastCheck + Constant.AUTO_CHECK_INTERVAL,
				Constant.AUTO_CHECK_INTERVAL, autoCheckIntent);
	}
	
	public static int selectAll(boolean[][] child_checkbox){
		int selectNum=0;
		int arraySize=0;
		if(child_checkbox!=null&&child_checkbox.length>0){
			for(int i=0;i<child_checkbox.length;++i){
				  if(child_checkbox[i]!=null&&child_checkbox.length>0){
					  for(int j=0;j<child_checkbox[i].length;++j){
						   if(child_checkbox[i][j]){
							   ++selectNum;
						   }
						   ++arraySize;
					  }
				  }
			}
		}
		if(selectNum==arraySize&&selectNum>0){
			return 1;
		}
		if(selectNum==0){
			return 0;
		}
		return 2;
	}
	
	public static List<String> getStoragePathList(Context context){
        List<String> pathList = new ArrayList<String>();
        String firstPath = Environment.getExternalStorageDirectory().getPath();
        pathList.add(firstPath);
        StorageManager mStorageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> vols = mStorageManager.getVolumes();
        for(VolumeInfo vol : vols){
            if(vol.getType()==VolumeInfo.TYPE_PUBLIC)
                if(vol.isMountedReadable()) {
                    pathList.add(vol.getPath().toString());
                }
        }
        return pathList;
    }
}