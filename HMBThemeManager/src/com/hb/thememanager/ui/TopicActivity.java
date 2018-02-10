package com.hb.thememanager.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import hb.app.HbActivity;
import com.hb.thememanager.R;
import com.hb.thememanager.http.request.HomeThemeHeaderRequest;
import com.hb.thememanager.http.request.ThemeRankingTabRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.request.TopicRequest;
import com.hb.thememanager.http.request.WallpaperRankingTabRequest;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.TopicThemeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Topic;
import com.hb.thememanager.ui.adapter.TopicThemeListAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView.OnAutoLoadListener;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase.OnRefreshListener;

public class TopicActivity extends SimpleRequestActivity
		implements OnItemClickListener {
	private static final String TAG = "TopicActivity";
	private Intent mIntent;
	private TopicThemeListAdapter mAdapter;
	private int mType = 0;
	private TopicRequest mTopicRequest;
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		setTitle(R.string.text_topic);
		
		init();
		requestFirstPage();
	}
	
	private void init() {
		Intent intent = getIntent();
		mType = 0;
		if(intent != null) {
			mType = intent.getIntExtra(Config.ActionKey.KEY_FAST_ENTRY, 0);
		}

		mAdapter = new TopicThemeListAdapter(this);
		setAdapter(mAdapter);
		mList.setOnItemClickListener(this);
	}

	@Override
	protected ThemeRequest createHeaderRequest() {
		return null;
	}

	@Override
	protected ThemeRequest createBodyRequest() {
		if(mTopicRequest == null) {
			mTopicRequest = new TopicRequest(getApplicationContext(), mType);
		}
		return mTopicRequest;
	}



	@Override
	public void update(Response result) {
		TLog.d(TAG,"get topic response topic-->");
		super.update(result);
		TopicThemeResponse ttr = (TopicThemeResponse)result;
		if(ttr.body != null){
			List<Topic> topics = ttr.body.special;
			if(topics != null && topics.size() > 0){
				if(mFromRefresh){
					mAdapter.removeAll();
				}
				mAdapter.addTopics(topics);
			}else{
				if(mCurrentPage == 0){
					showEmptyView(true);
				}
			}
		}else{
			if(mCurrentPage == 0){
				showEmptyView(true);
			}
		}

	}



	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if(mAdapter.getCount() == (position-1))
			return;
		Topic topic = (Topic) mAdapter.getItem(position-1);

        mIntent = new Intent(this, TopicDetailActivity.class);
        mIntent.putExtra(Config.ActionKey.KEY_TOPIC_TITLE, topic.getName());
        mIntent.putExtra(Config.ActionKey.KEY_TOPIC_TYPE, mType);
		mIntent.putExtra(Config.ActionKey.KEY_TOPIC_ID,topic.getId());
		TLog.d(TAG,topic.toString());
        startActivity(mIntent);
	}


}


