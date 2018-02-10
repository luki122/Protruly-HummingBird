package com.protruly.music.downloadex;

import java.io.File;
import android.os.Environment;



public class DownloadFileUtil {

	/**
	 * 检测SDcard是否存在
	 * 
	 * @return
	 */
	public static boolean isExistSDcard() {
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 删除文件
	 * 
	 * @param file
	 * @return
	 */
	public static boolean deleteFile(File file) {
		if (file != null && file.exists()) {
			return file.delete();
		}
		return false;
	}

	/**
	 * 删除文件夹或者文件
	 * 
	 * @param dirOrFile
	 */
	public static void deleteDirOrFile(File dirOrFile) {
		if (dirOrFile.isDirectory()) {
			File[] files = dirOrFile.listFiles();
			if (files != null) {
				for (File file : files) {
					deleteDirOrFile(file);
				}
			}
		}
		dirOrFile.delete();
	}
}
