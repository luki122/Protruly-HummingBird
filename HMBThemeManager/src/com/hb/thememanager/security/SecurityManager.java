package com.hb.thememanager.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;

import android.content.Context;
import android.text.TextUtils;

import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.job.parser.ThemeParser;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.ThemeZip;
import com.hb.thememanager.utils.Config;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
public class SecurityManager {
	
	/**
	 * 对指定路径的主题包进行完整性检查校验，检查分为两个部分
	 * <li>1、检查主题包的完整性
	 * <li>2、检查指定路径的文件是否是合乎规范的主题包
	 * @param themeFilePath 需要被检查的文件路径
	 * @return true表示指定的主题包正常，false表示非法或者不完整的主题包
	 */
	public static boolean checkThemePackage(String themeFilePath){
		if(TextUtils.isEmpty(themeFilePath)){
			return false;
		}
		try{
				ThemeZip themeZip = new ThemeZip(new File(themeFilePath));
				/*
				 * 1、检查是否有description.xml配置文件
				 */
				ZipEntry description = themeZip.getEntry(Config.LOCAL_THEME_DESCRIPTION_FILE_NAME);
				if(description == null){
					return false;
				}
				InputStream descStream = themeZip.getInputStream(description);
				XmlPullParser xmlParser = null;
				try {
					xmlParser = XmlPullParserFactory.newInstance().newPullParser();
					xmlParser.setInput(descStream, ThemeParser.ENCODING);
					int type;
					while ((type = xmlParser.next()) != XmlPullParser.START_TAG
							&& type != XmlPullParser.END_DOCUMENT) {
						// Seek parser to start tag.
					}
					/*
					 * 2、检查description.xml文件是否是正常的xml文件
					 */
					if (type != XmlPullParser.START_TAG) {
						return false;
					}
					/*
					 * 3、检查description.xml是否是按照主题相关元素进行配置的
					 */
					final String name = xmlParser.getName();
					return Config.ThemeDescription.ROOT.equals(name);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					if(descStream != null){
						try {
							descStream.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
		}catch(Exception e){
			
		}
		return false;
	}
	
	/**
	 * 检查指定主题的主题包是否正常，see{@link #checkThemePackage(String)}
	 * @param theme 需要被检查的主题
	 * @return true表示指定的主题包正常，false表示非法或者不完整的主题包
	 */
	public static boolean checkThemePackage(Theme theme){
		return checkThemePackage(theme.themeFilePath);
	}

	/**
	 * 检查指定主题是否已经存在，不允许同一个主题被加载多次
	 * @param theme
	 * @return
	 */
	public static boolean checkThemeExists(Theme theme,ThemeDatabaseController<Theme> dbController){
		List<Theme> themes = dbController.getThemes();
		if(themes == null || themes.size() == 0){
			return false;
		}
		return themes.contains(theme);
	}
	
	
}
