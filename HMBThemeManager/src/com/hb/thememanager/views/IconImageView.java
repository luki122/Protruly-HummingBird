package com.hb.thememanager.views;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import com.hb.thememanager.R;
public class IconImageView extends ImageView {
	
	private int mWidth, mHeight;
	
	private final ViewOutlineProvider OVAL_OUTLINE_PROVIDER = new ViewOutlineProvider() {
		@Override
		public void getOutline(View view, Outline outline) {
			outline.setOval(0, 0, mWidth, mHeight);
		}
	};
	public IconImageView(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
		init();
	}

	public IconImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		init();
	}

	public IconImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init();
	}

	private void init(){
		mWidth = mHeight = getResources().getDimensionPixelOffset(R.dimen.theme_designer_icon_size);
	}
	
	public IconImageView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init();
	}
	
	
	
	/**
	 * clip this view to cycle by set outline
	 */
	private void clip() {
        setOutlineProvider(OVAL_OUTLINE_PROVIDER);
		setClipToOutline(true);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		setMeasuredDimension(mWidth, mWidth);
		clip();
	}
	
	
	
	
}
