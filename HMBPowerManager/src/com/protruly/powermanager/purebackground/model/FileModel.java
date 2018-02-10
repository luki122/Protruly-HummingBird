package com.protruly.powermanager.purebackground.model;

import android.content.Context;

import com.protruly.powermanager.utils.FileUtils;
import com.protruly.powermanager.utils.StringUtils;
import com.protruly.powermanager.utils.Utils;

/**
 * 负责指定文件名的读取与写入
 */
public class FileModel {

	private String dataFilePath = "";
	private static FileModel instance;
	
	private FileModel(Context context) {
		dataFilePath = Utils.getApplicationFilesPath(context)+"/";
	}

	public static FileModel getInstance(Context context) {
		if (instance == null) {
			instance = new FileModel(context);
		}
		return instance;
	}

	public static void resetInstance(){
		instance = null;
	}
	
	/**
	 * 读取文件
	 * @param fileName 文件名
	 * @return
	 */
	public String readFile(String fileName){
		String contentStr = null;
		if (StringUtils.isEmpty(fileName)) {
			return contentStr;
		}
		if(isExistsInSys(fileName)){
			contentStr = FileUtils.readFile(dataFilePath+fileName);
		}	
		return contentStr;
	} 
	
	/**
	 * 写入文件
	 * @param fileName 文件名
	 * @param contentStr 写入内容
	 */
	public void writeFile(String fileName , String contentStr){
		if (StringUtils.isEmpty(fileName)) {
			return ;
		}
		String patnOfSys = dataFilePath + fileName;
		FileUtils.writeFile(patnOfSys,contentStr);
	}
	
	/**
	 * 判断系统文件夹中是否存在这个文件
	 * @param fileName
	 * @return
	 */
	private boolean isExistsInSys(String fileName){
		String filePath = dataFilePath+fileName;
		if (FileUtils.fileIsExists(filePath)) {
			return true;
	    }else{
	    	return false;
	    }
	}	
}
