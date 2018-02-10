package com.android.quicksearchbox.hotsearch;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchActivity;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by lijun on 17-8-16.
 */

public class HotSearchView extends RelativeLayout {

    public static final String TAG = "HotSearch";
    public static final int SHOW_COUNT = 6;

    private int showIndex = 0;
    private TextView hotsearchText1;
    private TextView hotsearchText2;
    private TextView hotsearchText3;
    private TextView hotsearchText4;
    private TextView hotsearchText5;
    private TextView hotsearchText6;

    private View mRefreshButton;

    SearchActivity mSearchActivity;

    ArrayList<HotSearchInfo> hotInfos = new ArrayList<HotSearchInfo>();

    public HotSearchView(Context context) {
        super(context);
    }

    public HotSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HotSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addHotSearchInfo(HotSearchInfo info) {
        hotInfos.add(info);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        mSearchActivity = (SearchActivity) getContext();
        hotsearchText1 = (TextView) findViewById(R.id.hotsearch_text1);
        hotsearchText2 = (TextView) findViewById(R.id.hotsearch_text2);
        hotsearchText3 = (TextView) findViewById(R.id.hotsearch_text3);
        hotsearchText4 = (TextView) findViewById(R.id.hotsearch_text4);
        hotsearchText5 = (TextView) findViewById(R.id.hotsearch_text5);
        hotsearchText6 = (TextView) findViewById(R.id.hotsearch_text6);
        hotsearchText1.setOnClickListener(hotsearchClickListener);
        hotsearchText2.setOnClickListener(hotsearchClickListener);
        hotsearchText3.setOnClickListener(hotsearchClickListener);
        hotsearchText4.setOnClickListener(hotsearchClickListener);
        hotsearchText5.setOnClickListener(hotsearchClickListener);
        hotsearchText6.setOnClickListener(hotsearchClickListener);
        mRefreshButton = findViewById(R.id.hotsearch_refrush);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNext();
            }
        });
//        loadData();
    }

    private void showNext() {
        showIndex = showIndex + SHOW_COUNT;
        if (showIndex >= hotInfos.size()) {
            showIndex = 0;
            try {
                new HotSearchHttpGet(this).execute(HotSearchInfo.getHotSearchUrl());
            } catch (Exception e) {
                Log.d(TAG, "showNext() loadHotsearch error : " + e.toString());
                e.printStackTrace();
            }
        } else {
            if (showIndex + 5 > hotInfos.size()) {
                showIndex = hotInfos.size() - SHOW_COUNT;
            }
            if (showIndex < 0) showIndex = 0;
            updateViews();
        }
    }

    public void clear() {
        hotInfos.clear();
    }

    public boolean isEmpty() {
        return hotInfos == null || hotInfos.size() < SHOW_COUNT;
    }

    public void loadData() {
        hotInfos.clear();
        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i, "95岁杨振宁恢复中国国籍", 773688, 2,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            info.app_query_link = "sinaweibo://searchall?q=95%E5%B2%81%E6%9D%A8%E6%8C%AF%E5%AE%81%E6%81%A2%E5%A4%8D%E4%B8%AD%E5%9B%BD%E5%9B%BD%E7%B1%8D&luicode=10000493&lfid=daling_quanjusousuo_resoubang_170821&extparam=c_type%3D36";
            info.h5_query_link = "http://s.weibo.com/weibo/95%E5%B2%81%E6%9D%A8%E6%8C%AF%E5%AE%81%E6%81%A2%E5%A4%8D%E4%B8%AD%E5%9B%BD%E5%9B%BD%E7%B1%8D&luicode=10000493&lfid=daling_quanjusousuo_resoubang_170821&extparam=c_type%3D36";
            hotInfos.add(info);
        }

        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 4, "刘备", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }
        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 8, "狄仁杰", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }

        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 12, "王昭君", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }
        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 16, "诸葛亮", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }

        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 20, "张飞", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }
        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 24, "赵云", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }

        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 28, "关羽", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }

        for (int i = 1; i <= 4; i++) {
            HotSearchInfo info = new HotSearchInfo(i + 32, "小胖", 10000, 1,
                    "sinaweibo://searchall?q=%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://s.weibo.com/weibo/%E8%B5%B5%E5%8F%88%E5%BB%B7&luicode=100",
                    "http://h5.sinaimg.cn/upload/2015/08/07/20/img_hot.png");
            hotInfos.add(info);
        }
        sortById();
        updateViews();
    }

    public void updateData(ArrayList<HotSearchInfo> datas) {
        Log.d("HotSearch", "updateData size : " + datas.size());
//        for (HotSearchInfo info : datas) {
//            Log.d("HotSearch", " id : " + info.s_id + ", word : " + info.word);
//        }
        hotInfos.clear();
        showIndex = 0;
        hotInfos.addAll(datas);
        sortById();
        updateViews();
    }

    public void loadHotSearchSuccess(boolean success){
        mSearchActivity.loadHotSearchSuccess(success);
    }

    public void updateViews() {
        if (isEmpty()) {
            this.setVisibility(View.GONE);
        } else {
            this.setVisibility(View.VISIBLE);
        }
        hotsearchText1.setText(hotInfos.get(showIndex).word);
        hotsearchText1.setTag(hotInfos.get(showIndex));
        hotsearchText2.setText(hotInfos.get(showIndex + 1).word);
        hotsearchText2.setTag(hotInfos.get(showIndex + 1));
        hotsearchText3.setText(hotInfos.get(showIndex + 2).word);
        hotsearchText3.setTag(hotInfos.get(showIndex + 2));
        hotsearchText4.setText(hotInfos.get(showIndex + 3).word);
        hotsearchText4.setTag(hotInfos.get(showIndex + 3));
        hotsearchText5.setText(hotInfos.get(showIndex + 4).word);
        hotsearchText5.setTag(hotInfos.get(showIndex + 4));
        hotsearchText6.setText(hotInfos.get(showIndex + 5).word);
        hotsearchText6.setTag(hotInfos.get(showIndex + 5));
    }

    private void sortById() {//按热搜排名排序
        Collections.sort(hotInfos, new HotSearchComparator());
    }


    private static class HotSearchComparator implements Comparator<HotSearchInfo> {
        @Override
        public int compare(HotSearchInfo o1, HotSearchInfo o2) {
            return (o1.s_id - o2.s_id);
        }
    }

    private View.OnClickListener hotsearchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            HotSearchInfo info = (HotSearchInfo) v.getTag();
            if (info != null) {
                try {
                    Log.d(TAG, "loadweibo by app");
                    Uri uri = Uri.parse(info.app_query_link);
                    Intent intent = new Intent();
                    intent.setClassName("com.sina.weibo", "com.sina.weibo.page.SearchResultActivity");
                    intent.setData(uri);
                    getContext().startActivity(intent);
                } catch (Exception e) {
                    Log.d(TAG, "loadweibo by h5");
                    mSearchActivity.loadWeibo(info.h5_query_link);
                }
                mSearchActivity.hideInputMethod();
            }
        }
    };

    private ResolveInfo getWeiboResolveInfo() {
        Intent weiboIntent = new Intent(Intent.ACTION_SEND);
        weiboIntent.setType("text/plain");
        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> matches = pm.queryIntentActivities(weiboIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        String packageName = "com.sina.weibo";

        ResolveInfo info = null;
        for (ResolveInfo each : matches) {
            String pkgName = each.activityInfo.applicationInfo.packageName;
            if (packageName.equals(pkgName)) {
                info = each;
                break;
            }
        }
        return info;
    }
}
