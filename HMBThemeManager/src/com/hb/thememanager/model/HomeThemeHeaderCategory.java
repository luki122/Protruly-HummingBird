package com.hb.thememanager.model;

import java.util.ArrayList;

import com.hb.thememanager.http.response.HomeThemeHeaderResponse;

public class HomeThemeHeaderCategory extends HomeThemeCategory {


	private ArrayList<Advertising> banners;

	public HomeThemeHeaderCategory(HomeThemeHeaderResponse header){
		banners = header.body.banner;
	}
	

	public ArrayList<Advertising> getBanners() {
		return banners;
	}

	public void setBanners(ArrayList<Advertising> banners) {
		this.banners = banners;
	}


}
