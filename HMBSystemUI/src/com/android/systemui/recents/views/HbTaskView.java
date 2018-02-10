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
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityManager;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

/* A task view */
public class HbTaskView extends FrameLayout implements Task.TaskCallbacks,
        View.OnClickListener, View.OnLongClickListener {

    Task mTask;
    boolean mTaskDataLoaded;
    boolean mIsFocused;
    boolean mClipViewInStack;

    View mContent;
    HbTaskViewThumbnail mThumbnailView;
    HbTaskViewHeader mHeaderView;
    View mLockView;

    public HbTaskView(Context context) {
        this(context, null);
    }

    public HbTaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HbTaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HbTaskView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mClipViewInStack = true;
    }

    /** Resets this TaskView for reuse. */
    void reset() {
        resetViewProperties();
    }

    /** Gets the task */
    Task getTask() {
        return mTask;
    }

    @Override
    protected void onFinishInflate() {
        // Bind the views
        mContent = findViewById(R.id.task_view_content);
        mHeaderView = (HbTaskViewHeader) findViewById(R.id.task_view_bar);
        mThumbnailView = (HbTaskViewThumbnail) findViewById(R.id.task_view_thumbnail);
        //mThumbnailView.updateClipToTaskBar(mHeaderView);
        mLockView = findViewById(R.id.lockicon);
    }

    /** Resets this view's properties */
    void resetViewProperties() {
        setLayerType(View.LAYER_TYPE_NONE, null);
        TaskViewTransform.reset(this);
    }

    /** Enables/disables handling touch on this task view. */
    void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    /**** View focus state ****/

    /**** TaskCallbacks Implementation ****/

    /** Binds this task view to the task */
    public void onTaskBound(Task t) {
        mTask = t;
        mTask.setCallbacks(this);
        
    	/**Mst: tangjun add for lockView begin*/
     	String key = RecentsConfiguration.getRecentsAppsKey(mTask);
    	if(RecentsConfiguration.readRecentsAppsLockState(getContext(), key)){
    		mLockView.setVisibility(View.VISIBLE);
    	}else{
    		mLockView.setVisibility(View.GONE);
    	}
    	/**Mst: tangjun add for lockView end*/
    }

    @Override
    public void onTaskDataLoaded() {
        if (mThumbnailView != null && mHeaderView != null) {
            // Bind each of the views to the new task data
            mThumbnailView.rebindToTask(mTask);
            mHeaderView.rebindToTask(mTask);
        }
        mTaskDataLoaded = true;
    }

    @Override
    public void onTaskDataUnloaded() {
        if (mThumbnailView != null && mHeaderView != null) {
            // Unbind each of the views from the task data and remove the task callback
            mTask.setCallbacks(null);
            mThumbnailView.unbindFromTask();
            mHeaderView.unbindFromTask();
        }
        mTaskDataLoaded = false;
    }

    @Override
    public void onMultiStackDebugTaskStackIdChanged() {
        mHeaderView.rebindToTask(mTask);
    }

    /**** View.OnClickListener Implementation ****/

    @Override
     public void onClick(final View v) {
    }

    /**** View.OnLongClickListener Implementation ****/

    @Override
    public boolean onLongClick(View v) {
        return false;
    }
}
