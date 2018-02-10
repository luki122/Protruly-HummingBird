package com.hb.thememanager.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Theme;

/**
 * Created by alexluo on 17-8-2.
 */

public class HomeThemeListItem extends LinearLayout {


    private ImageView mImage;
    private TextView mTitle;
    private TextView mPrice;
    public HomeThemeListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(getContext()).inflate(R.layout.list_item_home_theme_child,this,true);
        setOrientation(LinearLayout.VERTICAL);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mImage = (ImageView)findViewById(R.id.theme_cover);
        mTitle = (TextView)findViewById(R.id.theme_name);
        mPrice = (TextView)findViewById(R.id.theme_price);

    }


    public void setTitle(String title){
        mTitle.setVisibility(View.VISIBLE);
        mTitle.setText(title);
    }

    public void setPrice(String price){
        mPrice.setVisibility(View.VISIBLE);
        if(!TextUtils.isEmpty(price)){
                try{
                    double newPrice = Double.parseDouble(price);
                    mPrice.setText(getResources().getString(R.string.theme_price_suffix,price));
                }catch (Exception e){
                    mPrice.setText(price);
                }


        }else{
            mPrice.setText(getResources().getString(R.string.theme_price_free));
        }
    }

    public void setIcon(String url){
        Glide.with(getContext()).load(url).into(mImage);
    }



}
