package com.hb.thememanager.model;

/**
 * 专题详情头部类
 */
public class TopicDetailHeader{

	/**
	 * id
	 */
	private long id;
	
	/**
	 * 标题
	 */
	private String name;
	
	/**
	 * URL
	 */
	private String banner;

	/**
	 * 标题描述
	 */
	private String discription;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBanner() {
		return banner;
	}

	public void setBanner(String banner) {
		this.banner = banner;
	}

	public String getDiscription() {
		return discription;
	}

	public void setDiscription(String description) {
		this.discription = description;
	}

	@Override
	public String toString() {
		return "TopicDetailHeader [id=" + id + ", name=" + name + ", banner="
				+ banner + ", description=" + discription + "]";
	}

}

