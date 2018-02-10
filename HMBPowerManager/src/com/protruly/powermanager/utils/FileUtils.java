package com.protruly.powermanager.utils;

import android.app.Activity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
	// 常量，为编码格式
	public static final String ENCODING = "UTF-8";

	/**
	 * func:判断文件 或 文件夹 是否存在
	 * @param file
	 * @return
	 */
	public static boolean fileIsExists(String file) {
		try {
			File f = new File(file);
			if (!f.exists()) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * func:文件夹 是否存在
	 * @param file
	 * @return
	 */
	public static boolean dirIsExists(String dirName) {
		try {
			File f = new File(dirName);
			if (f.exists() && f.isDirectory()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * fun:判断该文件目录是否存在，如果不存在，就创建一个目录
	 * @param dirPath
	 */
	public static void sureDirExists(String dirPath){
		try {
			File f = new File(dirPath);
			if (!f.exists()) {
				f.mkdir();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 向指定的文件中写入指定的数据
	public static void writeFileData(Activity activity, String filename,
									 String message) {
		if(!fileIsOpt(filename)){
			return;
		}

		try {
			FileOutputStream fout = activity.openFileOutput(filename,
					Activity.MODE_PRIVATE);// 获得FileOutputStream
			// 将要写入的字符串转换为byte数组
			byte[] bytes = message.getBytes();
			fout.write(bytes);// 将byte数组写入文件
			fout.close();// 关闭文件输出流
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 打开指定文件，读取其数据，返回字符串对象
	public static String readFileData(Activity activity, String fileName) {
		String result = "";
		if(!fileIsOpt(fileName)){
			return result;
		}
		try {
			FileInputStream fin = activity.openFileInput(fileName);
			// 获取文件长度
			int lenght = fin.available();
			byte[] buffer = new byte[lenght];
			fin.read(buffer);
			// 将byte数组转换成指定格式的字符串
			result = new String(buffer, ENCODING);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	//文件夹的复制
	public static boolean copyFolder(String fromFile, String toFile) {
		// 要复制的文件目录
		if(!fileIsOpt(fromFile)||!fileIsOpt(toFile)){
			return false;
		}

		try {
			File[] currentFiles;
			File root = new File(fromFile);
			// 如同判断SD卡是否存在或者文件是否存在
						// 如果不存在则 return出去
			if (!root.exists()) {
				return false;
			}
			
			// 如果存在则获取当前目录下的全部文件 填充数组
			currentFiles = root.listFiles();

			// 目标目录
			File targetDir = new File(toFile);
			// 创建目录
			if (!targetDir.exists()) {
				targetDir.mkdirs();
			}
			
			// 遍历要复制该目录下的全部文件
			for (int i = 0; i < currentFiles.length; i++) {
				if (currentFiles[i].isDirectory())// 如果当前项为子目录 进行递归
				{
					copyFolder(currentFiles[i].getPath() + "/", toFile+ "/"
							+ currentFiles[i].getName() + "/");

				} else// 如果当前项为文件则进行文件拷贝
				{
					CopyFile(currentFiles[i].getPath(), toFile
							+ "/"+ currentFiles[i].getName());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 文件拷贝
	 * @param fromFile 文件路径+文件名
	 * @param toFile 文件路径+文件名
	 * @return
	 */
	public static boolean CopyFile(String fromFile, String toFile) {

		if(!fileIsOpt(fromFile)||!fileIsOpt(toFile)){
			return false;
		}
		
		try {
			InputStream fosfrom = new FileInputStream(fromFile);
			OutputStream fosto = new FileOutputStream(toFile);
			byte bt[] = new byte[1024];
			int c;
			while ((c = fosfrom.read(bt)) > 0) {
				fosto.write(bt, 0, c);
			}
			fosfrom.close();
			fosto.close();
			return true;

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * func:创建一个文件夹
	 * @param file
	 * @return
	 */
	public static boolean makeDir(String file) {
		
		if(!fileIsOpt(file)){
			return false;
		}
		
		if(fileIsExists(file)){
			return true;
		}
		
		try {
			boolean success = (new File(file)).mkdirs();
			return success;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void writeFile(String fileName, String message) {
		
		if(!fileIsOpt(fileName)){
			return;
		}
		
		try {
			FileOutputStream fout = new FileOutputStream(fileName);//存在则不创建，不存在则创建
			byte[] bytes = message.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除文件夹
	 * 
	 * @param filePathAndName
	 *            String 文件夹路径及名称 如c:/fqf
	 * @param fileContent
	 *            String
	 * @return boolean
	 */
	public static void delFolder(String folderPath) {
		
		if(!fileIsOpt(folderPath)){
			return;
		}
		
		try {
			delAllFile(folderPath); // 删除完里面所有内容
			String filePath = folderPath;
			filePath = filePath.toString();
			java.io.File myFilePath = new java.io.File(filePath);
			myFilePath.delete(); // 删除空文件夹

		} catch (Exception e) {
			System.out.println("删除文件夹操作出错");
			e.printStackTrace();

		}
	}

	/**
	 * 删除文件夹里面的所有文件
	 * 
	 * @param path
	 *            String 文件夹路径 如 c:/fqf
	 */
	public static void delAllFile(String path) {
		
		if(!fileIsOpt(path)){
			return;
		}
		
		try {
			File file = new File(path);
			if (!file.exists()) {
				return;
			}
			if (!file.isDirectory()) {
				return;
			}
			String[] tempList = file.list();
			File temp = null;
			for (int i = 0; i < tempList.length; i++) {
				if (path.endsWith(File.separator)) {
					temp = new File(path + tempList[i]);
				} else {
					temp = new File(path + File.separator + tempList[i]);
				}
				if (temp.isFile()) {
					temp.delete();
				}
				if (temp.isDirectory()) {
					delAllFile(path + "/" + tempList[i]);// 先删除文件夹里面的文件
					delFolder(path + "/" + tempList[i]);// 再删除空文件夹
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除 指定 文件
	 * @param path
	 * @return
	 */
	public static boolean delFile(String path) {
		
		if(!fileIsOpt(path)){
			return false;
		}
		
		try {
			if(fileIsExists(path)){
			File file = new File(path);
			return file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static long getFileSize(String path) {
		
		if(!fileIsOpt(path)){
			return 0;
		}
		try {
			File file = new File(path);
			return file.length();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static boolean renameFile(String oldPath, String newPath) {
		
		if(!fileIsOpt(oldPath)||!fileIsOpt(newPath)){
			return false;
		}
		
		try {
			File file = new File(oldPath);
			return file.renameTo(new File(newPath));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public static String readFile(String fileName) {
		String res = "";
		
		if(!fileIsOpt(fileName)){
			return res;
		}
		
		if (fileIsExists(fileName)) {
			try {
				FileInputStream fin = new FileInputStream(fileName);
				int length = fin.available();
				byte[] buffer = new byte[length];
				fin.read(buffer);
				res = new String(buffer, "UTF-8");
				fin.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	public static void copyInputToFile(InputStream in, String path) {

		if(!fileIsOpt(path)){
			return;
		}
		
		BufferedInputStream bis = null;
		FileOutputStream fos = null;
		try {
			byte[] buffer = new byte[10 * 1024];
			bis = new BufferedInputStream(in);
			fos = new FileOutputStream(path);
			int a = bis.read(buffer, 0, buffer.length);
			while (a != -1) {
				fos.write(buffer, 0, a);
				fos.flush();
				a = bis.read(buffer, 0, buffer.length);
			}
		} catch (Exception e) {
			// new IOTool().write("FileTool:" + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				fos.close();
				bis.close();
				in.close();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
	}
	
	public static boolean fileIsOpt(String file){
		
		if(file.toLowerCase().indexOf("sdcard/")>-1){
			if(!Utils.isSDCardReady()){
				return false;
			}
		}
		
		return true;
	}
}
