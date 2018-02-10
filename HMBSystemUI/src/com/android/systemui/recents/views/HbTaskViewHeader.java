/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;


/* The task bar view */
public class HbTaskViewHeader extends RelativeLayout {

    RecentsConfiguration mConfig;
    private SystemServicesProxy mSsp;

    // Header views
    ImageView mApplicationIcon;
    TextView mActivityDescription;

    // Header drawables
    boolean mCurrentPrimaryColorIsDark;
    int mCurrentPrimaryColor;
    int mBackgroundColor;
    Drawable mLightDismissDrawable;
    Drawable mDarkDismissDrawable;
    RippleDrawable mBackground;
    GradientDrawable mBackgroundColorDrawable;
    AnimatorSet mFocusAnimator;
    String mDismissContentDescription;

    // Static highlight that we draw at the top of each view
    static Paint sHighlightPaint;

    // Header dim, which is only used when task view hardware layers are not used
    Paint mDimLayerPaint = new Paint();
    PorterDuffColorFilter mDimColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);

    boolean mLayersDisabled;

    public HbTaskViewHeader(Context context) {
        this(context, null);
    }

    public HbTaskViewHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HbTaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HbTaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mConfig = RecentsConfiguration.getInstance();
        mSsp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        });

        // Load the dismiss resources
        mLightDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_light);
        mDarkDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_dark);
        mDismissContentDescription =
                context.getString(R.string.accessibility_recents_item_will_be_dismissed);

        // Configure the highlight paint
        if (sHighlightPaint == null) {
            sHighlightPaint = new Paint();
            sHighlightPaint.setStyle(Paint.Style.STROKE);
            sHighlightPaint.setStrokeWidth(mConfig.taskViewHighlightPx);
            sHighlightPaint.setColor(mConfig.taskBarViewHighlightColor);
            sHighlightPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            sHighlightPaint.setAntiAlias(true);
        }
    }

    @Override
    protected void onFinishInflate() {
        // Initialize the icon and description views
        mApplicationIcon = (ImageView) findViewById(R.id.application_icon);
        mActivityDescription = (TextView) findViewById(R.id.activity_description);

        // Hide the backgrounds if they are ripple drawables
        if (!Constants.DebugFlags.App.EnableTaskFiltering) {
            if (mApplicationIcon.getBackground() instanceof RippleDrawable) {
                mApplicationIcon.setBackground(null);
            }
        }

        mBackgroundColorDrawable = (GradientDrawable) getContext().getDrawable(R.drawable
                .recents_task_view_header_bg_color);
        // Copy the ripple drawable since we are going to be manipulating it
        mBackground = (RippleDrawable)
                getContext().getDrawable(R.drawable.recents_task_view_header_bg);
        mBackground = (RippleDrawable) mBackground.mutate().getConstantState().newDrawable();
        mBackground.setColor(ColorStateList.valueOf(0));
        mBackground.setDrawableByLayerId(mBackground.getId(0), mBackgroundColorDrawable);
        //setBackground(mBackground);
    }

    /** Binds the bar view to the task */
    public void rebindToTask(Task t) {
        // If an activity icon is defined, then we use that as the primary icon to show in the bar,
        // otherwise, we fall back to the application icon
        if (t.activityIcon != null) {
            mApplicationIcon.setImageDrawable(t.activityIcon);
        } else if (t.applicationIcon != null) {
            mApplicationIcon.setImageDrawable(t.applicationIcon);
        }
        if (!mActivityDescription.getText().toString().equals(t.activityLabel)) {
            mActivityDescription.setText(t.activityLabel);
        }
        mActivityDescription.setContentDescription(t.contentDescription);

        // Try and apply the system ui tint
        int existingBgColor = (getBackground() instanceof ColorDrawable) ?
                ((ColorDrawable) getBackground()).getColor() : 0;
        if (existingBgColor != t.colorPrimary) {
            mBackgroundColorDrawable.setColor(t.colorPrimary);
            mBackgroundColor = t.colorPrimary;
        }
    }

    /** Unbinds the bar view from the task */
    void unbindFromTask() {
        mApplicationIcon.setImageDrawable(null);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {

        // Don't forward our state to the drawable - we do it manually in onTaskViewFocusChanged.
        // This is to prevent layer trashing when the view is pressed.
        return new int[] {};
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mLayersDisabled) {
            mLayersDisabled = false;
            postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    mLayersDisabled = false;
                    setLayerType(LAYER_TYPE_HARDWARE, mDimLayerPaint);
                }
            });
        }
    }
}
