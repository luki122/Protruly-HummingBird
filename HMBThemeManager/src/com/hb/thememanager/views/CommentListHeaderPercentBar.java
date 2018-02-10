package com.hb.thememanager.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.hb.thememanager.R;

/**
 * Created by alexluo on 17-7-14.
 */

public class CommentListHeaderPercentBar extends RelativeLayout {


    private LinearLayout mRating;
    private ProgressBar mProgressBar;
    private TextView mPercent;

    public CommentListHeaderPercentBar(Context context) {
        super(context);
    }

    public CommentListHeaderPercentBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(getContext()).inflate(R.layout.list_item_comment_progress_bar,this,true);
        mRating = (LinearLayout) findViewById(R.id.rating_bar);
        mProgressBar = (ProgressBar)findViewById(R.id.progress);
        mPercent = (TextView)findViewById(R.id.percent);
    }


    public CommentListHeaderPercentBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CommentListHeaderPercentBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }



    public void setPercent(String percent){
        mPercent.setText(percent);
    }

    public void setRatingNumber(int number){
        for(int i = 0;i<number;i++){
            ImageView start = new ImageView(getContext());
            start.setImageResource(R.drawable.ic_start_selected_16);
            mRating.addView(start,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    public void setProgress(int progress){
        mProgressBar.setProgress(progress);
    }










}
