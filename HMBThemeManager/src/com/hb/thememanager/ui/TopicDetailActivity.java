package com.hb.thememanager.ui;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.request.TopicDetailBodyRequest;
import com.hb.thememanager.http.request.TopicDetailHeaderRequest;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.TopicDetailBodyResponse;
import com.hb.thememanager.http.response.TopicDetailHeaderResponse;
import com.hb.thememanager.model.Advertising;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.TopicDetail;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.ui.adapter.TopicDetailListAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.views.ExpandableTextView;
import com.hb.thememanager.views.ThemeListView;

public class TopicDetailActivity extends SimpleRequestActivity {
	private Intent mIntent;
	private int mType;
	/**
	 * 专题ID
	 */
	private int mId;
	private ImageView mBanner;
	private ExpandableTextView mTopicDes;
	private Intent mItent;
	private Theme mTheme;
	private TopicDetailListAdapter mAdapter;
	private ArrayList<Wallpaper> mWallpapers = new ArrayList<>();
	private TopicDetailHeaderRequest mHeaderRequest;
	private TopicDetailBodyRequest mBodyRequest;
	private View mHeaderView;
	private boolean mHasHeaderView = false;
	@Override
	protected void onCreate(Bundle arg0) {

		super.onCreate(arg0);
		mPageSize = 9;
		initIntent();
		mAdapter = new TopicDetailListAdapter(this, mId);
		mAdapter.setType(mType);
		setAdapter(mAdapter);
		requestFirstPage();

	}


	private void initIntent() {
		mIntent = getIntent();
		if(mIntent != null) {
			String topicTitle = mIntent.getStringExtra(Config.ActionKey.KEY_TOPIC_TITLE);
			mType = mIntent.getIntExtra(Config.ActionKey.KEY_TOPIC_TYPE, 0);
			mId = mIntent.getIntExtra(Config.ActionKey.KEY_TOPIC_ID,-1);
			Advertising adv = mIntent.getParcelableExtra(Config.ActionKey.KEY_ADV_DETAIL);
			if(adv != null){
				mType = adv.getWaresType();
				try {
					mId = Integer.parseInt(adv.getParameter());
				}catch (Exception e){
					mId = -1;
				}
				topicTitle = adv.getName();
			}
			setTitle(topicTitle);
		}
	}

	@Override
	protected ThemeRequest createHeaderRequest() {
		if(mHeaderRequest == null){
			mHeaderRequest = new TopicDetailHeaderRequest(this,mType);
		}
		mHeaderRequest.setId(String.valueOf(mId));
		return mHeaderRequest;
	}

	@Override
	protected ThemeRequest createBodyRequest() {
		if(mBodyRequest == null){
			mBodyRequest = new TopicDetailBodyRequest(this,mType);
		}
		mBodyRequest.setId(String.valueOf(mId));
		return mBodyRequest;
	}

	private void showHeaderView(boolean show){

	}

	@Override
	public void update(Response result) {
		super.update(result);
		if(result instanceof TopicDetailHeaderResponse){
			TopicDetailHeaderResponse tdhr = (TopicDetailHeaderResponse) result;
			if(tdhr.body != null){
				if(mHeaderView == null){
					mHeaderView = getLayoutInflater().inflate(R.layout.topic_detail_header,null);
					mBanner = (ImageView) mHeaderView.findViewById(R.id.topic_detail_banner);
					mTopicDes = (ExpandableTextView) mHeaderView.findViewById(R.id.topic_detail_topic_des);
				}
				Glide.with(this).load(tdhr.body.getBanner()).into(mBanner);
				mTopicDes.setText(tdhr.body.getDiscription());
				if(!mHasHeaderView) {
					mList.addHeaderView(mHeaderView);
					mHasHeaderView = true;
				}
			}

		}else if(result instanceof TopicDetailBodyResponse){
			TopicDetailBodyResponse tdbr = (TopicDetailBodyResponse) result;
			List<TopicDetail> topicDetail = tdbr.body.resource;
			if(topicDetail != null && topicDetail.size() > 0){
				showList();
				mAdapter.setData(topicDetail);
			}else{
				if(mCurrentPage == 0){
					showEmptyView(true);
				}
			}

		}
	}


	private void topicToWallpaper(List<TopicDetail> topics){
		for(TopicDetail t : topics){
			Wallpaper w = new Wallpaper();
			w.id = t.getId();
			w.downloadUrl = t.getIcon();
		}
	}







}



