package com.hb.thememanager.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.content.Intent;
import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.IntentUtils;
/**
 *主题首页头部的快速入口Icon，例如排行榜，分类，状态图标 
 *
 */
public class IconFastEntry extends LinearLayout implements OnClickListener{

	private static final String TAG = "IconFastEntry";
	private ImageView mIcon;
	private TextView mTitle;
	private int mThemeType = Theme.THEME_NULL;
	
	public IconFastEntry(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.IconFastEntry);
        int srcHeight = typedArray.getDimensionPixelSize(R.styleable.IconFastEntry_srcHeight, getResources().getDimensionPixelSize(R.dimen.size_icon_fast_entry));
        int srcWidth = typedArray.getDimensionPixelSize(R.styleable.IconFastEntry_srcWidth, getResources().getDimensionPixelSize(R.dimen.size_icon_fast_entry));
        typedArray.recycle();
		
		LayoutInflater.from(context).inflate(R.layout.icon_fast_entry, this, true);
		mIcon = (ImageView)findViewById(R.id.icon);
		mTitle = (TextView)findViewById(R.id.title);
		
		LayoutParams param = (LayoutParams) mIcon.getLayoutParams();
		param.height = srcHeight;  
        param.width = srcWidth;  
        mIcon.setLayoutParams(param);
		
		TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ImageView);
		Drawable iconDr = a.getDrawable(android.R.styleable.ImageView_src);
		if(iconDr != null){
			mIcon.setImageDrawable(iconDr);
		}
		a.recycle();
		a = context.obtainStyledAttributes(attrs, android.R.styleable.TextView);
		CharSequence title = a.getText(android.R.styleable.TextView_text);
		if(title != null){
			mTitle.setText(title);
		}
		int color = a.getColor(android.R.styleable.TextView_textColor, getResources().getColor(R.color.topic_item_text_color));
		mTitle.setTextColor(color);
	//	float size = a.getDimension(android.R.styleable.TextView_textSize, getResources().getDimension(R.dimen.topic_detail_page_bannerdes_content_size));
	//	mTitle.setTextSize(size);
		a.recycle();
		setClickable(true);
		setOnClickListener(this);
	}

	public void setIconDrawable(Drawable icon){
		mIcon.setImageDrawable(icon);
	}
	
	public void setIconUrl(String url){
		//load icon drawable from internet if has
	}
	
	public void setTitle(CharSequence title){
		mTitle.setText(title);
	}
	
	/**
	 * 调用该接口设置对应Icon的主题类别，通过
	 * 该主题类别可以决定进入下一级页面后的加载数据
	 * 的类别
	 * @param themeType one of {@link Theme#THEME_PKG},{@link Theme#WALLPAPER},
	 * {@link Theme#RINGTONE},{@link Theme#FONTS}
	 */
	public void setThemeType(int themeType){
		mThemeType = themeType;
	}
	
	public int getThemeType(){
		return mThemeType;
	}

	@Override
	public void onClick(View view) {

		int id = view.getId();
		Intent intent = null;
		if(id == R.id.icon_rank){
			intent = IntentUtils.buildFastEntryIntent(getThemeType(), Config.Action.ACTION_RANK);
		}else if(id == R.id.icon_category){
			intent = IntentUtils.buildFastEntryIntent(getThemeType(), Config.Action.ACTION_CATEGORY);
		}else if(id == R.id.icon_topic){
			intent = IntentUtils.buildFastEntryIntent(getThemeType(), Config.Action.ACTION_TOPIC);
		}
		if(intent != null){
			getContext().startActivity(intent);
		}
	}
	
	
	
	
}
