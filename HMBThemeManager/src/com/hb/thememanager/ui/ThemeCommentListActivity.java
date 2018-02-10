package com.hb.thememanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import hb.app.HbActivity;

import com.hb.thememanager.R;
import com.hb.thememanager.http.request.ThemeCommentsRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.CommentsHeaderResponse;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeCommentsResponse;
import com.hb.thememanager.model.Comments;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;
import com.hb.thememanager.ui.mvpview.CommentsView;
import com.hb.thememanager.ui.persenter.ThemeCommentListPersenter;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.CommentListHeaderPercentBar;
import com.hb.thememanager.views.ExpandableTextView;
import com.hb.thememanager.views.ThemeListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.hb.thememanager.http.request.CommentsHeaderRequest;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

import hb.view.menu.bottomnavigation.BottomNavigationView;
/**
 * Created by alexluo on 17-7-14.
 */

public class ThemeCommentListActivity extends HbActivity implements CommentsView,
        BottomNavigationView.OnNavigationItemSelectedListener,AutoLoadListView.OnAutoLoadListener
        ,PullToRefreshBase.OnRefreshListener{

    private static final int PAGE_SIZE = 15;
    private ArrayList<Comments> mComments;
    private CommentsAdapter mAdapter;
    private ThemeListView mCommentList;
    private View mHeaderView;
    private RatingBar mTotalRating;
    private TextView mTotalGrade;
    private CommentListHeaderPercentBar mCommentRating5;
    private CommentListHeaderPercentBar mCommentRating4;
    private CommentListHeaderPercentBar mCommentRating3;
    private CommentListHeaderPercentBar mCommentRating2;
    private CommentListHeaderPercentBar mCommentRating1;
    private ThemeCommentListPersenter mPresenter;
    private BottomNavigationView mAddCommentsBar;
    private int mThemeType;
    private Theme mCurrentTheme;
    private CommentsHeaderRequest mHeaderRequest;
    private ThemeRequest mCommentsRequest;
    private int mCurrentPage = 1;
    private boolean mHasMore = true;
    private boolean mFromFresh = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_comment_list);
        mCommentList = (ThemeListView)findViewById(android.R.id.list);
        mAddCommentsBar = (BottomNavigationView)findViewById(R.id.bottom_nav_bar) ;
        mAddCommentsBar.setNavigationItemSelectedListener(this);
        mComments = getIntent().getParcelableArrayListExtra(Config.ActionKey.KEY_SHOW_COMMENTS);
        mCurrentTheme = getIntent().getParcelableExtra(Config.ActionKey.KEY_HANDLE_COMMENTS);
        mThemeType = mCurrentTheme.type;
        mHeaderView = getLayoutInflater().inflate(R.layout.comments_list_header_view,null);
        mTotalRating = (RatingBar)mHeaderView.findViewById(R.id.rating);
        mTotalGrade = (TextView)mHeaderView.findViewById(R.id.grade);
        mPresenter = new ThemeCommentListPersenter(this);
        mPresenter.attachView(this);

        mCommentRating5 = (CommentListHeaderPercentBar)mHeaderView.findViewById(R.id.rating5) ;
        mCommentRating4 = (CommentListHeaderPercentBar)mHeaderView.findViewById(R.id.rating4) ;
        mCommentRating3 = (CommentListHeaderPercentBar)mHeaderView.findViewById(R.id.rating3) ;
        mCommentRating2 = (CommentListHeaderPercentBar)mHeaderView.findViewById(R.id.rating2) ;
        mCommentRating1 = (CommentListHeaderPercentBar)mHeaderView.findViewById(R.id.rating1) ;
        if(mComments != null){
            mAdapter = new CommentsAdapter(mComments);
            mCommentList.setAdapter(mAdapter);
        }

        setTitle(R.string.activity_title_all_comments);

        mCommentList.setOnAutoLoadListener(this);
        mCommentList.setOnRefreshListener(this);
        mCommentList.addHeaderView(mHeaderView);

        mCommentRating5.setRatingNumber(5);
        mCommentRating4.setRatingNumber(4);
        mCommentRating3.setRatingNumber(3);
        mCommentRating2.setRatingNumber(2);
        mCommentRating1.setRatingNumber(1);
        mHeaderRequest = new CommentsHeaderRequest(this,mThemeType);
        createCommentsRequest();
        mHeaderRequest.setId(mCurrentTheme.id);
        mPresenter.requestTheme(mHeaderRequest);
        mPresenter.requestTheme(mCommentsRequest);
    }

    private void createCommentsRequest(){
        if(mCommentsRequest == null) {
            mCommentsRequest = new ThemeCommentsRequest(getApplicationContext(), mThemeType);
            mCommentsRequest.setId(mCurrentTheme.id);
        }
        mCommentsRequest.setPageNumber(mCurrentPage);
        mCommentsRequest.setPageSize(PAGE_SIZE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onDestory();
    }



    @Override
    public void onLoading(boolean hasMore) {
        if(hasMore){
            mFromFresh = false;
            mCurrentPage ++;
            createCommentsRequest();
            mPresenter.requestTheme(mCommentsRequest);
        }

    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
       refresh();
    }

    private void refresh(){
        mFromFresh = true;
        mPresenter.requestTheme(mHeaderRequest);
        mCurrentPage = 0;
        createCommentsRequest();
        mPresenter.requestTheme(mCommentsRequest);
    }


    @Override
    public void onNavigationClicked(View view) {
        onBackPressed();
    }

    @Override
    public void showToast(String msg) {

    }

    @Override
    public void showMyDialog(int dialogId) {

    }

    @Override
    public void showEmptyView(boolean show) {

    }

    @Override
    public void showNetworkErrorView(boolean show) {

    }

    @Override
    public void updateComments(Response response) {
        ThemeCommentsResponse comments = (ThemeCommentsResponse)response;
        if(comments.returnBody() == null){
            mHasMore = false;
        }else{
            mHasMore = mCurrentPage < comments.returnBody().getTotalNum() -1;
        }
        mCommentList.onRefreshComplete();
        mCommentList.onLoadingComplete(mHasMore);
        List<Comments> commentsList = comments.getComments();
        if(commentsList != null && commentsList.size() >0){
            if(mFromFresh){
                mAdapter.clearAll();
            }
            mAdapter.addComments(commentsList);
        }

    }

    @Override
    public void updateCommentsHeader(Response obj) {
        if(obj == null){
            return;
        }
        CommentsHeaderResponse chr = (CommentsHeaderResponse)obj;
        if(chr != null && chr.returnBody() != null) {
            mTotalGrade.setText(String.valueOf(chr.getScore()));
            mTotalRating.setRating(chr.getScore() * mTotalRating.getNumStars() / 10);
            mCommentRating5.setProgress(chr.getStart5());
            mCommentRating4.setProgress(chr.getStart4());
            mCommentRating3.setProgress(chr.getStart3());
            mCommentRating2.setProgress(chr.getStart2());
            mCommentRating1.setProgress(chr.getStart1());

            mCommentRating5.setPercent(chr.getStart5()+"%");
            mCommentRating4.setPercent(chr.getStart4()+"%");
            mCommentRating3.setPercent(chr.getStart3()+"%");
            mCommentRating2.setPercent(chr.getStart2()+"%");
            mCommentRating1.setPercent(chr.getStart1()+"%");
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.add_comments){
            int addCommentTips = CommonUtil.getAddCommentTips(this,mCurrentTheme);
            if(addCommentTips != 0){
                ToastUtils.showShortToast(this,addCommentTips);
                return false;
            }

            User user = CommonUtil.getUser(this);
            if(!user.isLogin()){
                user.jumpLogin(null,this);
                return false;
            }



            Intent intent = new Intent(Config.Action.ACTION_ADD_COMMENTS);
            intent.putExtra(Config.ActionKey.KEY_HANDLE_COMMENTS,mCurrentTheme);
            startActivityForResult(intent,AddCommentActivity.ADD_COMMENT_REQUEST_CODE);
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == AddCommentActivity.ADD_COMMENT_RESULT_CODE){
            refresh();
        }
    }

    static class CommentsAdapter extends BaseAdapter{

        List<Comments> mComments;
        public CommentsAdapter(List<Comments> comments){
            mComments = comments;
        }

        public void addComments(List<Comments> commentsList){
            synchronized (mComments){
                mComments.addAll(commentsList);
                notifyDataSetChanged();
            }
        }

        public void clearAll(){
            synchronized (mComments){
                mComments.clear();
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return mComments.size();
        }

        @Override
        public Object getItem(int position) {
            return mComments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            Holder holder;
            if (view == null) {
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_theme_comments, null);
                holder = new Holder();
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.time = (TextView) view.findViewById(R.id.time);
                holder.detail = (ExpandableTextView) view.findViewById(R.id.comment);
                holder.rating = (RatingBar) view.findViewById(R.id.rating);
                view.setTag(holder);
            } else {
                holder = (Holder) view.getTag();
            }

            Comments comment = mComments.get(position);
            if (comment != null) {
                holder.name.setText(comment.nickname);
                holder.time.setText(comment.getRealTime(viewGroup.getResources()));
                holder.detail.setText(comment.getContent());
                int grade = comment.starLevel;
                holder.rating.setRating(grade);

            }
            return view;
        }

        class Holder{
            TextView name ;
            TextView time;
            ExpandableTextView detail;
            RatingBar rating;
        }

    }


}
