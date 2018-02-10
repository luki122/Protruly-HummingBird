
package com.android.launcher3.specialeffectpreview;



import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;


import com.android.launcher3.R;
import com.android.launcher3.pageindicators.PageIndicatorUnderline;

public class PreviewContainer extends LinearLayout {
    private static final String TAG = "PreviewContainer";
    

    private PageIndicatorUnderline mPageIndicator;

    EffectPreviewPagedView mPagedView;
	private View mContent;
	
    @SuppressLint("NewApi")
    public PreviewContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PreviewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewContainer(Context context) {
        this(context, null, 0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

	    mContent = findViewById(R.id.special_effect__content);;
		mPagedView = (EffectPreviewPagedView) mContent.findViewById(R.id.special_effect_paged_view_content);
		mPagedView.initParentViews(mContent);

    }


   public void initPagedView(){
           mPagedView.addEffectViews();
   }

}
