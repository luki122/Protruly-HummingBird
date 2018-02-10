package com.hb.thememanager.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hb.thememanager.http.response.ThemeResponse;

/**
 *主题首页分为几个部分，最顶上的banner广告，快速入口部分，
 *还有对应的主题分类部分，每一个部分对应不同HomeThemeCategory
 *对象来实现不同的布局逻辑和业务逻辑，其中banner和快速入口
 *划分为同一个部分进行处理，其他部分各自单独处理
 *
 */
public class HomeThemeCategory implements Comparable<HomeThemeCategory>{
	
	public static final int TYPE_NONE= -1;
	/**
	 * 用于标识列表头部banner条和快速入口类型
	 */
	public static final int TYPE_HEADER = 0;
	/**
	 * 用于标识列表中的广告布局
	 */
	public static final int TYPE_ADVERTISIN = 1;
	/**
	 * 用于标识列表中的主题分类列表
	 */
	public static final int TYPE_CATEGORY = 2;
	
	/**
	 * 根据这个数据给首页ListView返回不同的布局文件
	 */
	private int type;
	
	/**
	 * 如果是广告，name为空
	 */
	private String name="";
	
	/**
	 * 该分类下的主题资源数量，如果数量大于
	 * 界面最多可以呈现的大小就在该类别的右上角
	 * 显示更多按钮
	 */
	private int childCount;


	private List<Theme> themes = new ArrayList<Theme>();


	public int getType() {
		return type;
	}


	public void setType(int type) {
		this.type = type;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public int getChildCount() {
		return childCount;
	}


	public void setChildCount(int count) {
		this.childCount = count;
	}


	public List<Theme> getThemes() {
		return themes;
	}


	public void setThemes(List<Theme> themes) {
		this.themes = themes;
	}


	public int id;

	@Override
	public String toString() {
		return "HomeThemeCategory [type=" + type + ", name=" + name
				+ ", childCount=" + childCount + ", themes=" + themes + "]";
	}


	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		HomeThemeCategory other = (HomeThemeCategory)obj;
		return this.name.equals(other.name) && this.type == other.type && this.childCount == other.childCount;
	}


	@Override
	public int compareTo(HomeThemeCategory homeThemeCategory) {
		return this.type - homeThemeCategory.type;
	}
}
