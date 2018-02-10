package com.hb.thememanager.http.response.adapter;

/**
 * 主题列表页面Item的数据接受类,主题包，壁纸，字体都可以使用该类
 */
public class ThemeResource {

    public String id;
    public String icon;
    public String name;
    public int isCharge;
    public String downloadUrl;
    public int putawayStatus;
    /**
     * 以分为单位
     */
    public int price;
    
    
	@Override
	public String toString() {
		return "ThemeResource [id=" + id + ", icon=" + icon + ", name=" + name
				+ ", isCharge=" + isCharge + ", downloadUrl=" + downloadUrl
				+ ", putawayStatus=" + putawayStatus + ", price=" + price + "]";
	}
}

