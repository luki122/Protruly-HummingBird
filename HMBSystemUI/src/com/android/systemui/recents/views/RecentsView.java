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
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManagerGlobal;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsAppWidgetHostView;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.RecentsMemoryInfo;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.HbClearAllView.OnAnimEndListener;
import com.android.systemui.recents.views.HbTaskAdapter.OnItemListener;
import com.android.systemui.statusbar.FlingAnimationUtils;

import java.util.ArrayList;
import java.util.List;

import hb.widget.recycleview.RecyclerView;
import hb.widget.recycleview.RecyclerView.OnScrollListener;
import hb.widget.recycleview.LinearLayoutManager;
//import android.support.v7.widget.RecyclerView;
//import android.support.v7.widget.RecyclerView.OnScrollListener;
//import android.support.v7.widget.LinearLayoutManager;

/**
 * This view is the the top level layout that contains TaskStacks (which are laid out according
 * to their SpaceNode bounds.
 */
public class RecentsView extends FrameLayout implements TaskStackView.TaskStackViewCallbacks,
        RecentsPackageMonitor.PackageCallbacks {

    private static final String TAG = "RecentsView";

    /** The RecentsView callbacks */
    public interface RecentsViewCallbacks {
        public void onTaskViewClicked();
        public void onTaskLaunchFailed();
        public void onAllTaskViewsDismissed();
        public void onExitToHomeAnimationTriggered();
        public void onScreenPinningRequest();
        public void onTaskResize(Task t);
        public void runAfterPause(Runnable r);
    }

    RecentsConfiguration mConfig;
    LayoutInflater mInflater;
    DebugOverlayView mDebugOverlay;
    RecentsViewLayoutAlgorithm mLayoutAlgorithm;

    ArrayList<TaskStack> mStacks;
    List<TaskStackView> mTaskStackViews = new ArrayList<>();
    RecentsAppWidgetHostView mSearchBar;
    RecentsViewCallbacks mCb;
    
    /**hb: tangjun add to get tasks begin*/
    private ArrayList<Task> mHbTaskList = null;
    private HbTaskAdapter mAdapter;
    private HbTaskStackView hbTaskStackView;
    private int touchX;
    private int touchY;
    private boolean mIsTranslateY;
    private float mPagingTouchSlop;
    private VelocityTracker mVelocityTracker;
    private View mClearAllView;
    private HbClearAllView mClearAllImageView;
    private TextView mClearAllTextView;
    private AnimationDrawable mAnimationDrawable;
    private LinearLayoutManager mLinearLayoutManager;
    private boolean mSelfScroll;
    private int mItemWidth_0; 	//the width of item 0
    private int mItemWidth_normal; 	//the width of item 0
    private int mItemScroll_distance;
    private int totalDx = 0;; //RecylerView Distance
    private static final int MAX_DISTANCE = 1000;
    private Context mContext;
    private static final String LAUNCHER_PACKAGE_NAME = "com.android.dlauncher";
    private float mTotalMemSize;
    private float mTotalMemSizeGB;
    private boolean mClearAllAnimRun = false;
    private final FlingAnimationUtils mFlingAnimationUtils;
    
    private float mLastUnusedMem = 0;
    private float mCurrentUnusedMem = 0;
    /**hb: tangjun add to get tasks begin*/

    public RecentsView(Context context) {
        this(context, null);
    }

    public RecentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mConfig = RecentsConfiguration.getInstance();
        mInflater = LayoutInflater.from(context);
        mLayoutAlgorithm = new RecentsViewLayoutAlgorithm(mConfig);
        
        mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
        
        /**hb: tangjun add begin*/
        mPagingTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mContext = context;
        mTotalMemSize = RecentsMemoryInfo.getmem_total();
        mTotalMemSizeGB = mTotalMemSize / 1024 / 1024;
        mTotalMemSizeGB = (float)Math.ceil((double)mTotalMemSizeGB);
    	initDimens();
        /**hb: tangjun add end*/
    }
    
    private void initDimens() {
        /**hb: tangjun add begin*/
    	if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
	        mItemWidth_0 = this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_first_right_see_padding) + 
	        		this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_normal_paddingleft) + 
	        		this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_width);
	        mItemWidth_normal = this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_normal_paddingleft) + 
	        		this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_width);
	        mItemScroll_distance = mItemWidth_0 - this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_first_right_see_padding);
    	} else {
	        mItemWidth_0 = this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_first_right_see_padding_land) + 
	        		this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_normal_paddingleft_land) + 
	        		this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_width_land);
	        mItemWidth_normal = this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_normal_paddingleft_land) + 
	        		this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_width_land);
	        mItemScroll_distance = mItemWidth_0 - this.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_first_right_see_padding_land);
    	}
        /**hb: tangjun add end*/
    }

    /** Sets the callbacks */
    public void setCallbacks(RecentsViewCallbacks cb) {
        mCb = cb;
    }

    /** Sets the debug overlay */
    public void setDebugOverlay(DebugOverlayView overlay) {
        mDebugOverlay = overlay;
    }
    
    /**hb: tangjun add begin*/
    private void acquireVelocityTracker(final MotionEvent event) { 
        if(null == mVelocityTracker) { 
        	mVelocityTracker= VelocityTracker.obtain();
        } 
        mVelocityTracker.addMovement(event); 
    }
    
    private void releaseVelocityTracker() { 
        if(null != mVelocityTracker) {
        	mVelocityTracker.clear(); 
        	mVelocityTracker.recycle(); 
        	mVelocityTracker = null; 
        } 
    }
    
    private void checkOtherTask(Task t) {
    	 if(judgeIfRecentsWhiteList(t)) {
    		 return;
    	 }
    	int total = mHbTaskList.size();
    	for (int i = 0; i < total; i++) {
    		Task task = mHbTaskList.get(i);
    		if(task.key.baseIntent.getComponent().getPackageName().equals(t.key.baseIntent.getComponent().getPackageName())) {
    			task.isActive = false;
    		}
    	}
    }
    
    private boolean judgeIfRecentsWhiteList(Task t) {
    	String[] recentsWhiteList = RecentsTaskLoader.getInstance().getSystemServicesProxy().getRecentsWhiteList();
    	String packageName = t.key.baseIntent.getComponent().getPackageName();
    	if( packageName != null){
    		for(int i = 0; i < recentsWhiteList.length; i++) {
    			if(packageName.contains(recentsWhiteList[i])){
    				return true;
    			}
    		}
    	}
        return false;
    }
    
    private void startAnimationForDismiss(final View view, final int position) {
    	ObjectAnimator transAnim = ObjectAnimator.ofFloat(view, "TranslationY", view.getTranslationY(), -MAX_DISTANCE).setDuration(120);
    	ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(view, "Alpha", view.getAlpha(), 0).setDuration(120);
    	AnimatorSet animSet = new AnimatorSet();
    	animSet.addListener(new AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				// TODO Auto-generated method stub
				//Dissmiss the recent app
				int index = position;
				view.setTranslationY(0);
				//view.setAlpha(1);
				if(index >= mHbTaskList.size() && mHbTaskList.size() > 0) {
					index = mHbTaskList.size() - 1;
				}
		        
				Task t = mHbTaskList.get(index);
				mHbTaskList.remove(index);
				mAdapter.setRemoveIndex(index);
		        mAdapter.setTaskList(mHbTaskList);
		        
		        //hb: tangjun because we add another padding view in first of adapter, so we need to plus one begin 
		        mAdapter.notifyItemRemoved(index + 1);
		        mAdapter.notifyItemRangeChanged(0, mHbTaskList.size() + 1);
		        //hb: tangjun because we add another padding view in first of adapter, so we need to plus one end 
		        
		        if(mHbTaskList.size() == 0) {
		            // If there are no remaining tasks, then just close recents
		        	mCb.onAllTaskViewsDismissed();
		        }
		        
		        onTaskViewDismissed(t);
		        
		        checkOtherTask(t);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				// TODO Auto-generated method stub
			}
		});
    	animSet.play(transAnim).with(alphaAnim);
    	animSet.setInterpolator(new AccelerateInterpolator());
    	animSet.start();
    }
    
    private void startAnimationForBack(View view) {
    	ObjectAnimator transAnim = ObjectAnimator.ofFloat(view, "TranslationY", view.getTranslationY(), 0).setDuration(120);
    	ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(view, "Alpha", view.getAlpha(), 1).setDuration(120);
    	AnimatorSet animSet = new AnimatorSet();
    	animSet.addListener(new AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				// TODO Auto-generated method stub
			}
		});
    	animSet.play(transAnim).with(alphaAnim);
    	animSet.start();
    }
    
    private void startAnimationForLock(final View view, final int position) {
    	ObjectAnimator transAnim = ObjectAnimator.ofFloat(view, "TranslationY", view.getTranslationY(), 0).setDuration(120);
    	ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(view, "Alpha", 1, 1).setDuration(120);
    	AnimatorSet animSet = new AnimatorSet();
    	animSet.addListener(new AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animator animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				// TODO Auto-generated method stub
				lockOrUnlockRecents(view, position);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				// TODO Auto-generated method stub
			}
		});
    	animSet.play(transAnim).with(alphaAnim);
    	animSet.start();
    }
    
    /**
     * 
     * @param view
     * hb: tangjun lock or unlock recent app 
     */
    private void lockOrUnlockRecents(View view, int position) {
    	if(position >= mHbTaskList.size() && mHbTaskList.size() > 0) {
    		position = mHbTaskList.size() - 1;
    	}
    	Task t = mHbTaskList.get(position);
		View lockView = view.findViewById(R.id.lockicon);
		
     	String key = RecentsConfiguration.getRecentsAppsKey(t);
    	if(RecentsConfiguration.readRecentsAppsLockState(getContext(), key)){
    		lockView.setVisibility(View.GONE);
    		RecentsConfiguration.writeRecentsAppLockState(getContext(), key, false);
    	}else{
    		lockView.setVisibility(View.VISIBLE);
    		RecentsConfiguration.writeRecentsAppLockState(getContext(), key, true);
    	}
    	mAdapter.notifyDataSetChanged();
    }
    @Override
    protected void onFinishInflate() {
    	// TODO Auto-generated method stub
    	super.onFinishInflate();
    	Log.d("222222", "--initMstTaskStackView");
    	initMstTaskStackView();
    }
    
    private int getScrolledDistance() {
    	View firstVisibleItem = hbTaskStackView.getChildAt(0);
    	Log.d("222222", "---firstVisibleItem = " + firstVisibleItem);
    	if(firstVisibleItem != null) {
	    	int firstItemPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
	    	int firstItemRight = mLinearLayoutManager.getDecoratedRight(firstVisibleItem);
	    	Log.d("222222", "---firstItemPosition = " + firstItemPosition + ", firstItemRight = " + firstItemRight);
	    	if(firstItemPosition == 0) {
	    		return mItemWidth_0 - firstItemRight;
	    	} else {
	    		return mItemWidth_0 + firstItemPosition * mItemWidth_normal - firstItemRight;
	    	}
    	}
    	return 0;
    }
    
	private void initMstTaskStackView() {
    	hbTaskStackView = (HbTaskStackView)findViewById(R.id.id_recyclerview_horizontal);
    	hbTaskStackView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(mClearAllAnimRun) {
					return true;
				} else {
					return false;
				}
			}
		});
    	mLinearLayoutManager = new LinearLayoutManager(this.getContext());  
    	mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);  
        hbTaskStackView.setLayoutManager(mLinearLayoutManager);
        HbDefaultItemAnimator animator = new HbDefaultItemAnimator();
        animator.setChangeDuration(100);
        animator.setMoveDuration(100);
        hbTaskStackView.setItemAnimator(animator);
        hbTaskStackView.setOnScrollListener(new OnScrollListener() {
    		public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
    			//already stop
    			if(newState == 0) {
    				if(!mSelfScroll) {
//	    				totalDx = getScrolledDistance();
//	    				mSelfScroll = true;
//	    				Log.e("151515", "onScrollStateChanged totalDx = " + totalDx);
//	    				int index = totalDx / 630;
//	    				if(totalDx % 630 != 0) {
//		    				if(totalDx % 630 > 300) {
//		    					hbTaskStackView.smoothScrollBy((index + 1) * 630 - totalDx, 0);
//		    				} else {
//		    					hbTaskStackView.smoothScrollBy(index* 630 - totalDx, 0);
//		    				}
//	    				} else {
//	    					mSelfScroll = false;
//	    				}
    				} else {
    					mSelfScroll = false;
    				}
    			}
    		}

    		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    		}
		});
        
        mClearAllView = findViewById(R.id.clear_all_icon);
        mClearAllTextView = (TextView)findViewById(R.id.clear_recents_text);
        mClearAllImageView = (HbClearAllView)findViewById(R.id.clear_recents_image);
        mAnimationDrawable = (AnimationDrawable)mClearAllImageView.getDrawable();
        mClearAllImageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.e("181818", "-clearAllRecentApps ");
				clearAllRecentApps();
			}
		});
	}
	
	/**hb: tangjun add begin*/
	public void setHbTaskStackViewScrollWhenStart() {
		if(mHbTaskList != null && mHbTaskList.size() > 0) {
			if(hbTaskStackView != null) {
				totalDx = getScrolledDistance();
				//hbTaskStackView.scrollToPosition(0);
				
				if(RecentsTaskLoader.getInstance().getSystemServicesProxy().getSecondTopMostTask() != null &&
						"com.android.dlauncher".equals(RecentsTaskLoader.getInstance().getSystemServicesProxy().getSecondTopMostTask().topActivity.getPackageName())) {
					//hbTaskStackView.scrollBy(0 - totalDx, 0);
					hbTaskStackView.post(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							hbTaskStackView.scrollBy(0 - totalDx, 0);
						}
					});
				} else {
					Log.d("222222", "--setHbTaskStackViewScrollWhenStart totalDx =  " + totalDx);
					Log.d("222222", "--setHbTaskStackViewScrollWhenStart mItemScroll_distance =  " + mItemScroll_distance);
					//hbTaskStackView.scrollBy(mItemScroll_distance - totalDx, 0);
					hbTaskStackView.post(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							hbTaskStackView.scrollBy(mItemScroll_distance - totalDx, 0);
						}
					});
				}
			}
		}
	}
	/**hb: tangjun add end*/
	
    /**hb: tangjun add for orientation begin*/
    public void onConfigurationChanged() {

    	if(getVisibility() != View.VISIBLE) {
    		return;
    	}
    	
        /**hb: tangjun add begin*/
    	initDimens();
        /**hb: tangjun add end*/
        
    	LayoutParams layoutparam = (LayoutParams) hbTaskStackView.getLayoutParams();
    	if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
    		layoutparam.width = LayoutParams.MATCH_PARENT;
    	} else {
    		layoutparam.width = this.getResources().getDimensionPixelSize(R.dimen.hb_recents_task_view_width_lanscape);
    	}
    	Log.d("222222", "---111onConfigurationChanged setAdapter");
    	hbTaskStackView.setLayoutParams(layoutparam);
    	hbTaskStackView.setAdapter(mAdapter);
    	
    	layoutparam = (LayoutParams) mClearAllView.getLayoutParams();
    	if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
    		layoutparam.bottomMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginbottom_portait);
    		layoutparam.leftMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginleft_portait);
    	} else {
    		layoutparam.bottomMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginbottom_landscape);
    		layoutparam.leftMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginleft_landscape);
    	}
    	mClearAllView.setLayoutParams(layoutparam);
    }
    /**hb: tangjun add for orientation end*/
    
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
    	// TODO Auto-generated method stub
    	super.onConfigurationChanged(newConfig);

    	//Log.d("111111", "---RecentsView onConfigurationChanged orientation = " + newConfig.orientation + ", getVisibility = " + getVisibility());
    	Log.d("222222", "---333onConfigurationChanged setAdapter");
    	if(getVisibility() != View.VISIBLE) {
    		return;
    	}
    	
        /**hb: tangjun add begin*/
    	initDimens();
        /**hb: tangjun add end*/
        
    	LayoutParams layoutparam = (LayoutParams) hbTaskStackView.getLayoutParams();
    	if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
    		layoutparam.width = LayoutParams.MATCH_PARENT;
    	} else {
    		layoutparam.width = this.getResources().getDimensionPixelSize(R.dimen.hb_recents_task_view_width_lanscape);
    	}
    	hbTaskStackView.setLayoutParams(layoutparam);
    	hbTaskStackView.setAdapter(mAdapter);
    	
    	layoutparam = (LayoutParams) mClearAllView.getLayoutParams();
    	if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
    		layoutparam.bottomMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginbottom_portait);
    		layoutparam.leftMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginleft_portait);
    	} else {
    		layoutparam.bottomMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginbottom_landscape);
    		layoutparam.leftMargin = this.getResources().getDimensionPixelSize(R.dimen.hb_clear_all_icon_marginleft_landscape);
    	}
    	mClearAllView.setLayoutParams(layoutparam);
    }
    	
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasWindowFocus);
	}
	
	public void setViewClipPercent() {
		float unusedMem = RecentsMemoryInfo.getmem_unused(mContext, RecentsTaskLoader.getInstance().getSystemServicesProxy().getActivityManager());
		if(mClearAllImageView != null) {
			//float percent = 1 -  unusedMem / mTotalMemSize;
			float percent = 1 -  unusedMem /(float)(mTotalMemSizeGB * 1024 * 1024);
			mClearAllImageView.setViewClipPercent(percent);
		}
		setClearAllText(unusedMem);
	}
	
	private void setClearAllText(float unsedMem) {
		StringBuilder totalMemString = new StringBuilder(String.valueOf(mTotalMemSizeGB));
		totalMemString.append("GB");
		StringBuilder unusedMemString = new StringBuilder();
		//Log.d("111111", "---setClearAllText 11unsedMem" + unsedMem);
		if(unsedMem >= 1024.0f * 1024.0f) {
			unsedMem = unsedMem / 1024 / 1024;
			//Log.d("111111", "---setClearAllText 22unsedMem" + unsedMem);
			unsedMem = ((float)Math.round(unsedMem*10))/10;
			unusedMemString.append(unsedMem).append("GB");
		} else if(unsedMem >= 1024.0f) {
			unsedMem = unsedMem / 1024;
			unsedMem = (float)(Math.round(unsedMem*10)/10);
			unusedMemString.append(unsedMem).append("MB");
		} else {
			unusedMemString.append(unsedMem).append("KB");
		}
		mClearAllTextView.setText(mContext.getString(R.string.memory_info, unusedMemString, totalMemString));
	}
	
	private void showClearToast(float unsedMem) {
		float clearMem = unsedMem - mLastUnusedMem;
		if(clearMem < 1024.0f) {
			Toast.makeText(mContext, mContext.getString(R.string.clean_to_best), Toast.LENGTH_SHORT).show();
		} else {
			StringBuilder unusedMemString = new StringBuilder();
			if(clearMem >= 1024.0f * 1024.0f) {
				clearMem = clearMem / 1024 / 1024;
				clearMem = ((float)Math.round(clearMem*10))/10;
				unusedMemString.append(clearMem).append("GB");
			} else if(clearMem >= 1024.0f) {
				clearMem = clearMem / 1024;
				clearMem = (float)(Math.round(clearMem*10)/10);
				unusedMemString.append(clearMem).append("MB");
			} else {
				unusedMemString.append(clearMem).append("KB");
			}
			Toast.makeText(mContext, mContext.getString(R.string.memory_free, unusedMemString), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void clearAllRecentApps() {
		if(mClearAllAnimRun) {
			return;
		}
		mLastUnusedMem = RecentsMemoryInfo.getmem_unused(mContext, RecentsTaskLoader.getInstance().getSystemServicesProxy().getActivityManager());
    	int first = mLinearLayoutManager.findFirstVisibleItemPosition();
    	int last = mLinearLayoutManager.findLastVisibleItemPosition();
    	
    	mClearAllImageView.startCircleAnim();
    	mClearAllAnimRun = true;
    	mAnimationDrawable.stop();
    	mAnimationDrawable.start();
    	mClearAllImageView.setOnAnimEndListener(new OnAnimEndListener() {
			
			@Override
			public void onAnimEnd() {
				// TODO Auto-generated method stub
				//Log.d("tangjun222", "---onAnimEnd");
				mAnimationDrawable.stop();
	            // If there are no remaining tasks, then just close recents
				if(mHbTaskList != null && mHbTaskList.size() == 0) {
					mCb.onAllTaskViewsDismissed();
				} else {
					Task t = mHbTaskList.get(0);
					if(RecentsTaskLoader.getInstance().getSystemServicesProxy().getSecondTopMostTask() != null &&
							t.key.baseIntent.getComponent().getPackageName().equals(
							RecentsTaskLoader.getInstance().getSystemServicesProxy().getSecondTopMostTask().topActivity.getPackageName())) {
						//暂时不做全部清理进当前界面的功能,所以跑完动画先进桌面 tangjun
//			            EventBus.getDefault().send(new LaunchTaskEvent(mTaskStackView.getChildViewForTask(t),
//		                        t, null, INVALID_STACK_ID, false /* screenPinningRequested */));
						mCb.onAllTaskViewsDismissed();
					} else {
						mCb.onAllTaskViewsDismissed();
					}
				}
				
		        //hbTaskStackView.setAdapter(mAdapter);
		        
				float unusedMem = RecentsMemoryInfo.getmem_unused(mContext, RecentsTaskLoader.getInstance().getSystemServicesProxy().getActivityManager());
				setClearAllText(unusedMem);
				showClearToast(unusedMem);
				
				postDelayed(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mClearAllAnimRun = false;
					}
				}, 100);
			}
		});
    	//Log.e("181818", "-first = " + first + ", last = " + last);
    	
		int delay = 0;
    	for(int index = 0; index <= last - first && mHbTaskList.size() > 0; index++ ) {
    		//hb: tangjun because we add another padding view in first of adapter, so we need to minus one begin
    		if(first == 0 && index == 0) {
    			continue;
    		}
			float translate = 0;
			float alpha = 0;
			if(first+index - 1 < mHbTaskList.size()) {
	    		Task t = mHbTaskList.get(first+index - 1);
	    		//hb: tangjun mod for no anim if the task is locked begin 2016.11.2
				String key = RecentsConfiguration.getRecentsAppsKey(t);
				//暂时不做全部清理进当前界面的功能 tangjun 2017.4.21
				if (RecentsConfiguration.readRecentsAppsLockState(getContext(), key) 
						/*||t.key.baseIntent.getComponent().getPackageName().equals(RecentsTaskLoader.getInstance().getSystemServicesProxy().getSecondTopMostTask().topActivity.getPackageName())*/) {
					translate = 0;
					alpha = 1;
					//translate = -1000;
					//alpha = 0;
				} else {
					translate = -1000;
					alpha = 0;
				}
			}
			//hb: tangjun mod for no anim if the task is locked end 2016.11.2
    		final View view = hbTaskStackView.getChildAt(index);
    		Log.d("222222", "---view = " + view + ", index = " + index);
    		final boolean isLast = index == last - first;
	    	ObjectAnimator transAnim = ObjectAnimator.ofFloat(view, "TranslationY", 0, translate);
	    	ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(view, "Alpha", 1, alpha);
	    	AnimatorSet animSet = new AnimatorSet();
	    	
	    	animSet.setStartDelay(delay);
	    	animSet.setDuration(250 - delay);
	    	delay += 50;
	    	animSet.addListener(new AnimatorListener() {
				
				@Override
				public void onAnimationStart(Animator animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationRepeat(Animator animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationEnd(Animator animation) {
					// TODO Auto-generated method stub
					//clearAllRecentApps
					if(isLast) {
			            
						int count = mHbTaskList.size();
						int j = 0;
						for (int i = 0; i < count; i++) {
							Task t = mHbTaskList.get(j);
							String key = RecentsConfiguration.getRecentsAppsKey(t);
							if (!RecentsConfiguration.readRecentsAppsLockState(getContext(), key) 
									/*&& !t.key.baseIntent.getComponent().getPackageName().equals(
											RecentsTaskLoader.getInstance().getSystemServicesProxy().getSecondTopMostTask().topActivity.getPackageName())*/) {
								mHbTaskList.remove(j);
								mAdapter.setTaskList(mHbTaskList);
								//hb: tangjun because we add another padding view in first of adapter, so we need to plus one
								//mAdapter.notifyItemRemoved(j + 1);
								//mAdapter.notifyItemRangeChanged(0, mHbTaskList.size()+1);
								onTaskViewDismissed(t);
							} else {
								j++; 	//if not remove , then ++
							}
						}
						//Log.d("tangjun222", "---onAnimationEnd");
					}
					view.setTranslationY(0);
				}
				
				@Override
				public void onAnimationCancel(Animator animation) {
					// TODO Auto-generated method stub
				}
			});
	    	animSet.play(transAnim).with(alphaAnim);
	    	animSet.start();
    	}
    }
    
    private void updateHbTaskStackView(TaskStack stack) {
    	mHbTaskList = stack.getTasks();
        for(int i = 0; i < mHbTaskList.size(); i++) {
        	//Log.d("181818", "---RecentsView updateHbTaskStackView i = " + i);
        	//Log.d("181818", "---RecentsView updateHbTaskStackView thumbnail = " + mHbTaskList.get(i).thumbnail);
        	//Log.d("181818", "---RecentsView updateHbTaskStackView title = " + mHbTaskList.get(i).title);
        }
    	if(mAdapter == null) {
    		mAdapter = new HbTaskAdapter(this.getContext(),  mHbTaskList);
    		hbTaskStackView.setAdapter(mAdapter);
    	} else {
    		mAdapter.setTaskList(mHbTaskList);
    		mAdapter.notifyDataSetChanged();
    	}
    	mAdapter.setOnItemListener(new OnItemListener() {

			@Override
			public void onItemClick(View view, int position) {
				// TODO Auto-generated method stub
				//launch the recent app
				if(mHbTaskList.size() <= 0 || mClearAllAnimRun) {
					return;
				}
		        final SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
		        Log.d("181818", "----onItemClick position = " + position);
		    	if(position >= mHbTaskList.size() && mHbTaskList.size() > 0) {
		    		position = mHbTaskList.size() - 1;
		    	}
				Task task = mHbTaskList.get(position);
				ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view,  0,  0, view.getMeasuredWidth(), view.getMeasuredHeight());
                if (task.isActive) {
                    // Bring an active task to the foreground
                    ssp.moveTaskToFront(task.key.id, opts);
                } else {
                    if (ssp.startActivityFromRecents(getContext(), task.key.id,
                            task.activityLabel, opts)) {
                    } else {
                        // Dismiss the task and return the user to home if we fail to
                        // launch the task
                        onTaskViewDismissed(task);
                        if (mCb != null) {
                            mCb.onTaskLaunchFailed();
                        }

                        // Keep track of failed launches
                        MetricsLogger.count(getContext(), "overview_task_launch_failed", 1);
                    }
                }
			}

			@Override
			public void onItemDismiss(View view, int position) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAllItemDismiss() {
				// TODO Auto-generated method stub
			}

			@Override
			public boolean onItemTouchListener(View view, final int position,
					MotionEvent event) {
				// TODO Auto-generated method stub
				if(mHbTaskList.size() <= 0 || mClearAllAnimRun) {
					return false;
				}
				acquireVelocityTracker(event); 
				switch (event.getAction()) { 
		    	case MotionEvent.ACTION_DOWN: 
		    		touchX = (int) event.getRawX();
		    		touchY = (int) event.getRawY();
		    		hbTaskStackView.setTouchIntercept(true);
		    		mIsTranslateY = false;
		    		break;

		    	case MotionEvent.ACTION_MOVE: 
		    		if((Math.abs(event.getRawY() - touchY) > mPagingTouchSlop && Math.abs(event.getRawY() - touchY)> Math.abs(event.getRawX() - touchX)) 
		    				|| mIsTranslateY) {
		    			view.setTranslationY(Math.min(event.getRawY() - touchY, 300));
		    			if(event.getRawY() - touchY <= 0) {
		    				view.setAlpha((MAX_DISTANCE + event.getRawY() - touchY) / MAX_DISTANCE);
		    			}
		    			mIsTranslateY = true;
		    			hbTaskStackView.setTouchIntercept(false);
		    			return true;
		    		}
		    		break;

		    	case MotionEvent.ACTION_UP:
		    	case MotionEvent.ACTION_CANCEL:
		    		mVelocityTracker.computeCurrentVelocity(1000); 
		    		final float velocityX = mVelocityTracker.getXVelocity(); 
		    		final float velocityY = mVelocityTracker.getYVelocity();
		    		releaseVelocityTracker();
		    		if(hbTaskStackView.getTouchIntercept()) {
		    			return false;
		    		} else {
		    			Log.e("151515", "velocityX = " + velocityX + ", velocityY = " + velocityY + ", mFlingAnimationUtils.getMinVelocityPxPerSecond() = " + mFlingAnimationUtils.getMinVelocityPxPerSecond());
		    			if(event.getRawY() - touchY < 0) {
			    			if(event.getRawY() - touchY < - 300 || (velocityY < -mFlingAnimationUtils.getMinVelocityPxPerSecond() && event.getRawY() - touchY < - 100) ) {
			    				startAnimationForDismiss(view, position);
			    			} else {
			    				startAnimationForBack(view);
			    			}
		    			} else {
		    				if(event.getRawY() - touchY > 200 || (velocityY > mFlingAnimationUtils.getMinVelocityPxPerSecond() && event.getRawY() - touchY > 100) ) {
			    				startAnimationForLock(view, position);
			    			} else {
			    				startAnimationForBack(view);
			    			}
		    			}
		    			hbTaskStackView.setTouchIntercept(true);
		    			return true;
		    		}

		    	default: 
		    		break; 
		    	} 
				return false;
			}
		});
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
		//if we touch mClearAllImageView
		if (isInViewRect(mClearAllImageView, ev)) {
			return true;
		}
		if(ev.getPointerCount() > 1) {
			return true;
		}
		return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
		//if we touch mClearAllImageView
    	//if (isInViewRect(mClearAllImageView, ev)) {
		if (isInViewRect(mClearAllImageView, ev) && ev.getAction() != MotionEvent.ACTION_MOVE) {
			return  mClearAllImageView.onTouchEvent(ev) ;
		}
		return super.onTouchEvent(ev);
    }

	private boolean isInViewRect(View view, MotionEvent ev) {
		int[] location = new int[2];
		view.getLocationOnScreen(location);
		RectF rect = new RectF(location[0], location[1], location[0] + view.getWidth(),
				location[1] + view.getHeight());
		float x = ev.getRawX();
		float y = ev.getRawY();
		return rect.contains(x, y);
	}
    /**hb: tangjun add end*/

    /** Set/get the bsp root node */
    public void setTaskStacks(ArrayList<TaskStack> stacks) {
        int numStacks = stacks.size();

        // Remove all/extra stack views
        int numTaskStacksToKeep = 0; // Keep no tasks if we are recreating the layout
        if (mConfig.launchedReuseTaskStackViews) {
            numTaskStacksToKeep = Math.min(mTaskStackViews.size(), numStacks);
        }
        for (int i = mTaskStackViews.size() - 1; i >= numTaskStacksToKeep; i--) {
            removeView(mTaskStackViews.remove(i));
        }

        // Update the stack views that we are keeping
        for (int i = 0; i < numTaskStacksToKeep; i++) {
            TaskStackView tsv = mTaskStackViews.get(i);
            // If onRecentsHidden is not triggered, we need to the stack view again here
            tsv.reset();
            tsv.setStack(stacks.get(i));
        }

        // Add remaining/recreate stack views
        mStacks = stacks;
        /**hb tangjun mod begin*/
        /*
        for (int i = mTaskStackViews.size(); i < numStacks; i++) {
            TaskStack stack = stacks.get(i);
            TaskStackView stackView = new TaskStackView(getContext(), stack);
            stackView.setCallbacks(this);
            addView(stackView);
            mTaskStackViews.add(stackView);
        }
        */
        /**hb tangjun mod end*/
        
        /**hb tangjun add begin*/
        updateHbTaskStackView(mStacks.get(0));
        /**hb tangjun add end*/

        // Enable debug mode drawing on all the stacks if necessary
        if (mConfig.debugModeEnabled) {
            for (int i = mTaskStackViews.size() - 1; i >= 0; i--) {
                TaskStackView stackView = mTaskStackViews.get(i);
                stackView.setDebugOverlay(mDebugOverlay);
            }
        }

        // Trigger a new layout
        requestLayout();
    }

    /** Gets the list of task views */
    List<TaskStackView> getTaskStackViews() {
        return mTaskStackViews;
    }

    /** Gets the next task in the stack - or if the last - the top task */
    public Task getNextTaskOrTopTask(Task taskToSearch) {
        Task returnTask = null; 
        boolean found = false;
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = stackCount - 1; i >= 0; --i) {
            TaskStack stack = stackViews.get(i).getStack();
            ArrayList<Task> taskList = stack.getTasks();
            // Iterate the stack views and try and find the focused task
            for (int j = taskList.size() - 1; j >= 0; --j) {
                Task task = taskList.get(j);
                // Return the next task in the line.
                if (found)
                    return task;
                // Remember the first possible task as the top task.
                if (returnTask == null)
                    returnTask = task;
                if (task == taskToSearch)
                    found = true;
            }
        }
        return returnTask;
    }

    /** Launches the focused task from the first stack if possible */
    public boolean launchFocusedTask() {
        // Get the first stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            TaskStack stack = stackView.getStack();
            // Iterate the stack views and try and find the focused task
            List<TaskView> taskViews = stackView.getTaskViews();
            int taskViewCount = taskViews.size();
            for (int j = 0; j < taskViewCount; j++) {
                TaskView tv = taskViews.get(j);
                Task task = tv.getTask();
                if (tv.isFocusedTask()) {
                    onTaskViewClicked(stackView, tv, stack, task, false);
                    return true;
                }
            }
        }
        return false;
    }

    /** Launches a given task. */
    public boolean launchTask(Task task) {
        // Get the first stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            TaskStack stack = stackView.getStack();
            // Iterate the stack views and try and find the given task.
            List<TaskView> taskViews = stackView.getTaskViews();
            int taskViewCount = taskViews.size();
            for (int j = 0; j < taskViewCount; j++) {
                TaskView tv = taskViews.get(j);
                if (tv.getTask() == task) {
                    onTaskViewClicked(stackView, tv, stack, task, false);
                    return true;
                }
            }
        }
        return false;
    }

    /** Launches the task that Recents was launched from, if possible */
    public boolean launchPreviousTask() {
        // Get the first stack view
    	/**hb tangjun mod begin*/
    	/*
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            TaskStack stack = stackView.getStack();
            ArrayList<Task> tasks = stack.getTasks();

            // Find the launch task in the stack
            if (!tasks.isEmpty()) {
                int taskCount = tasks.size();
                for (int j = 0; j < taskCount; j++) {
                    if (tasks.get(j).isLaunchTarget) {
                        Task task = tasks.get(j);
                        TaskView tv = stackView.getChildViewForTask(task);
                        onTaskViewClicked(stackView, tv, stack, task, false);
                        return true;
                    }
                }
            }
        }
        return false;
        */

    	// Find the launch task in the stack
    	if (!mHbTaskList.isEmpty()) {
    		Log.d("tangjun111", "--launchPreviousTask");
    		int taskCount = mHbTaskList.size();
    		for (int j = 0; j < taskCount; j++) {
    			if (mHbTaskList.get(j).isLaunchTarget) {
    				Task task = mHbTaskList.get(j);
    				final SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
    				ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(hbTaskStackView.getChildAt(1),  0,  0, 
    						hbTaskStackView.getChildAt(1).getMeasuredWidth(), hbTaskStackView.getChildAt(1).getMeasuredHeight());
                    if (task.isActive) {
                        // Bring an active task to the foreground
                        ssp.moveTaskToFront(task.key.id, opts);
                    } else {
                        if (ssp.startActivityFromRecents(getContext(), task.key.id,
                                task.activityLabel, opts)) {
                        } else {
                            // Dismiss the task and return the user to home if we fail to
                            // launch the task
                            onTaskViewDismissed(task);
                            if (mCb != null) {
                                mCb.onTaskLaunchFailed();
                            }

                            // Keep track of failed launches
                            MetricsLogger.count(getContext(), "overview_task_launch_failed", 1);
                        }
                    }
    				return true;
    			}
    		}
    	}
        return false;
        /**hb tangjun mod end*/
    }

    /** Requests all task stacks to start their enter-recents animation */
    public void startEnterRecentsAnimation(ViewAnimation.TaskViewEnterContext ctx) {
        // We have to increment/decrement the post animation trigger in case there are no children
        // to ensure that it runs
        ctx.postAnimationTrigger.increment();

        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            stackView.startEnterRecentsAnimation(ctx);
        }
        ctx.postAnimationTrigger.decrement();
    }

    /** Requests all task stacks to start their exit-recents animation */
    public void startExitToHomeAnimation(ViewAnimation.TaskViewExitContext ctx) {
        // We have to increment/decrement the post animation trigger in case there are no children
        // to ensure that it runs
        ctx.postAnimationTrigger.increment();
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            stackView.startExitToHomeAnimation(ctx);
        }
        ctx.postAnimationTrigger.decrement();

        // Notify of the exit animation
        mCb.onExitToHomeAnimationTriggered();
    }

    /** Adds the search bar */
    public void setSearchBar(RecentsAppWidgetHostView searchBar) {
        // Remove the previous search bar if one exists
        if (mSearchBar != null && indexOfChild(mSearchBar) > -1) {
            removeView(mSearchBar);
        }
        /**hb tangjun mod begin*/
        // Add the new search bar
        /*
        if (searchBar != null) {
            mSearchBar = searchBar;
            addView(mSearchBar);
        }
        */
        /**hb tangjun mod end*/
    }

    /** Returns whether there is currently a search bar */
    public boolean hasValidSearchBar() {
        return mSearchBar != null && !mSearchBar.isReinflateRequired();
    }

    /** Sets the visibility of the search bar */
    public void setSearchBarVisibility(int visibility) {
        if (mSearchBar != null) {
            mSearchBar.setVisibility(visibility);
            // Always bring the search bar to the top
            mSearchBar.bringToFront();
        }
    }

    /**
     * This is called with the full size of the window since we are handling our own insets.
     */
    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Get the search bar bounds and measure the search bar layout
        Rect searchBarSpaceBounds = new Rect();
        if (mSearchBar != null) {
            mConfig.getSearchBarBounds(width, height, mConfig.systemInsets.top, searchBarSpaceBounds);
            mSearchBar.measure(
                    MeasureSpec.makeMeasureSpec(searchBarSpaceBounds.width(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(searchBarSpaceBounds.height(), MeasureSpec.EXACTLY));
        }

        Rect taskStackBounds = new Rect();
        mConfig.getAvailableTaskStackBounds(width, height, mConfig.systemInsets.top,
                mConfig.systemInsets.right, searchBarSpaceBounds, taskStackBounds);

        // Measure each TaskStackView with the full width and height of the window since the
        // transition view is a child of that stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        List<Rect> stackViewsBounds = mLayoutAlgorithm.computeStackRects(stackViews,
                taskStackBounds);
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            if (stackView.getVisibility() != GONE) {
                // We are going to measure the TaskStackView with the whole RecentsView dimensions,
                // but the actual stack is going to be inset to the bounds calculated by the layout
                // algorithm
                stackView.setStackInsetRect(stackViewsBounds.get(i));
                stackView.measure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        setMeasuredDimension(width, height);
    }
    */

    /**
     * This is called with the full size of the window since we are handling our own insets.
     */
    /*
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Get the search bar bounds so that we lay it out
        if (mSearchBar != null) {
            Rect searchBarSpaceBounds = new Rect();
            mConfig.getSearchBarBounds(getMeasuredWidth(), getMeasuredHeight(),
                    mConfig.systemInsets.top, searchBarSpaceBounds);
            mSearchBar.layout(searchBarSpaceBounds.left, searchBarSpaceBounds.top,
                    searchBarSpaceBounds.right, searchBarSpaceBounds.bottom);
        }

        // Layout each TaskStackView with the full width and height of the window since the 
        // transition view is a child of that stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            if (stackView.getVisibility() != GONE) {
                stackView.layout(left, top, left + stackView.getMeasuredWidth(),
                        top + stackView.getMeasuredHeight());
            }
        }
    }
    */

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Update the configuration with the latest system insets and trigger a relayout
        mConfig.updateSystemInsets(insets.getSystemWindowInsets());
        requestLayout();
        return insets.consumeSystemWindowInsets();
    }

    /** Notifies each task view of the user interaction. */
    public void onUserInteraction() {
        // Get the first stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            stackView.onUserInteraction();
        }
    }

    /** Focuses the next task in the first stack view */
    public void focusNextTask(boolean forward) {
        // Get the first stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        if (!stackViews.isEmpty()) {
            stackViews.get(0).focusNextTask(forward, true);
        }
    }

    /** Dismisses the focused task. */
    public void dismissFocusedTask() {
        // Get the first stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        if (!stackViews.isEmpty()) {
            stackViews.get(0).dismissFocusedTask();
        }
    }

    /** Unfilters any filtered stacks */
    public boolean unfilterFilteredStacks() {
        if (mStacks != null) {
            // Check if there are any filtered stacks and unfilter them before we back out of Recents
            boolean stacksUnfiltered = false;
            int numStacks = mStacks.size();
            for (int i = 0; i < numStacks; i++) {
                TaskStack stack = mStacks.get(i);
                if (stack.hasFilteredTasks()) {
                    stack.unfilterTasks();
                    stacksUnfiltered = true;
                }
            }
            return stacksUnfiltered;
        }
        return false;
    }

    public void disableLayersForOneFrame() {
        List<TaskStackView> stackViews = getTaskStackViews();
        for (int i = 0; i < stackViews.size(); i++) {
            stackViews.get(i).disableLayersForOneFrame();
        }
    }

    private void postDrawHeaderThumbnailTransitionRunnable(final TaskView tv, final int offsetX,
            final int offsetY, final TaskViewTransform transform,
            final ActivityOptions.OnAnimationStartedListener animStartedListener) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Disable any focused state before we draw the header
                if (tv.isFocusedTask()) {
                    tv.unsetFocusedTask();
                }

                float scale = tv.getScaleX();
                int fromHeaderWidth = (int) (tv.mHeaderView.getMeasuredWidth() * scale);
                int fromHeaderHeight = (int) (tv.mHeaderView.getMeasuredHeight() * scale);

                Bitmap b = Bitmap.createBitmap(fromHeaderWidth, fromHeaderHeight,
                        Bitmap.Config.ARGB_8888);
                if (Constants.DebugFlags.App.EnableTransitionThumbnailDebugMode) {
                    b.eraseColor(0xFFff0000);
                } else {
                    Canvas c = new Canvas(b);
                    c.scale(tv.getScaleX(), tv.getScaleY());
                    tv.mHeaderView.draw(c);
                    c.setBitmap(null);
                }
                b = b.createAshmemBitmap();
                int[] pts = new int[2];
                tv.getLocationOnScreen(pts);
                try {
                    WindowManagerGlobal.getWindowManagerService()
                            .overridePendingAppTransitionAspectScaledThumb(b,
                                    pts[0] + offsetX,
                                    pts[1] + offsetY,
                                    transform.rect.width(),
                                    transform.rect.height(),
                                    new IRemoteCallback.Stub() {
                                        @Override
                                        public void sendResult(Bundle data)
                                                throws RemoteException {
                                            post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (animStartedListener != null) {
                                                        animStartedListener.onAnimationStarted();
                                                    }
                                                }
                                            });
                                        }
                                    }, true);
                } catch (RemoteException e) {
                    Log.w(TAG, "Error overriding app transition", e);
                }
            }
        };
        mCb.runAfterPause(r);
    }
    /**** TaskStackView.TaskStackCallbacks Implementation ****/

    @Override
    public void onTaskViewClicked(final TaskStackView stackView, final TaskView tv,
                                  final TaskStack stack, final Task task, final boolean lockToTask) {

        // Notify any callbacks of the launching of a new task
        if (mCb != null) {
            mCb.onTaskViewClicked();
        }

        // Upfront the processing of the thumbnail
        TaskViewTransform transform = new TaskViewTransform();
        View sourceView;
        int offsetX = 0;
        int offsetY = 0;
        float stackScroll = stackView.getScroller().getStackScroll();
        if (tv == null) {
            // If there is no actual task view, then use the stack view as the source view
            // and then offset to the expected transform rect, but bound this to just
            // outside the display rect (to ensure we don't animate from too far away)
            sourceView = stackView;
            transform = stackView.getStackAlgorithm().getStackTransform(task, stackScroll, transform, null);
            offsetX = transform.rect.left;
            offsetY = mConfig.displayRect.height();
        } else {
            sourceView = tv.mThumbnailView;
            transform = stackView.getStackAlgorithm().getStackTransform(task, stackScroll, transform, null);
        }

        // Compute the thumbnail to scale up from
        final SystemServicesProxy ssp =
                RecentsTaskLoader.getInstance().getSystemServicesProxy();
        ActivityOptions opts = null;
        if (task.thumbnail != null && task.thumbnail.getWidth() > 0 &&
                task.thumbnail.getHeight() > 0) {
            ActivityOptions.OnAnimationStartedListener animStartedListener = null;
            if (lockToTask) {
                animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                    boolean mTriggered = false;
                    @Override
                    public void onAnimationStarted() {
                        if (!mTriggered) {
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mCb.onScreenPinningRequest();
                                }
                            }, 350);
                            mTriggered = true;
                        }
                    }
                };
            }
            if (tv != null) {
                postDrawHeaderThumbnailTransitionRunnable(tv, offsetX, offsetY, transform,
                        animStartedListener);
            }
            if (mConfig.multiStackEnabled) {
                opts = ActivityOptions.makeCustomAnimation(sourceView.getContext(),
                        R.anim.recents_from_unknown_enter,
                        R.anim.recents_from_unknown_exit,
                        sourceView.getHandler(), animStartedListener);
            } else {
                opts = ActivityOptions.makeThumbnailAspectScaleUpAnimation(sourceView,
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8).createAshmemBitmap(),
                        offsetX, offsetY, transform.rect.width(), transform.rect.height(),
                        sourceView.getHandler(), animStartedListener);
            }
        }

        final ActivityOptions launchOpts = opts;
        final Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                if (task.isActive) {
                    // Bring an active task to the foreground
                    ssp.moveTaskToFront(task.key.id, launchOpts);
                } else {
                    if (ssp.startActivityFromRecents(getContext(), task.key.id,
                            task.activityLabel, launchOpts)) {
                        if (launchOpts == null && lockToTask) {
                            mCb.onScreenPinningRequest();
                        }
                    } else {
                        // Dismiss the task and return the user to home if we fail to
                        // launch the task
                        onTaskViewDismissed(task);
                        if (mCb != null) {
                            mCb.onTaskLaunchFailed();
                        }

                        // Keep track of failed launches
                        MetricsLogger.count(getContext(), "overview_task_launch_failed", 1);
                    }
                }
            }
        };

        // Keep track of the index of the task launch
        int taskIndexFromFront = 0;
        int taskIndex = stack.indexOfTask(task);
        if (taskIndex > -1) {
            taskIndexFromFront = stack.getTaskCount() - taskIndex - 1;
        }
        MetricsLogger.histogram(getContext(), "overview_task_launch_index", taskIndexFromFront);

        // Launch the app right away if there is no task view, otherwise, animate the icon out first
        if (tv == null) {
            launchRunnable.run();
        } else {
            if (task.group != null && !task.group.isFrontMostTask(task)) {
                // For affiliated tasks that are behind other tasks, we must animate the front cards
                // out of view before starting the task transition
                stackView.startLaunchTaskAnimation(tv, launchRunnable, lockToTask);
            } else {
                // Otherwise, we can start the task transition immediately
                stackView.startLaunchTaskAnimation(tv, null, lockToTask);
                launchRunnable.run();
            }
        }
    }

    @Override
    public void onTaskViewAppInfoClicked(Task t) {
        // Create a new task stack with the application info details activity
        Intent baseIntent = t.key.baseIntent;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", baseIntent.getComponent().getPackageName(), null));
        intent.setComponent(intent.resolveActivity(getContext().getPackageManager()));
        TaskStackBuilder.create(getContext())
                .addNextIntentWithParentStack(intent).startActivities(null,
                new UserHandle(t.key.userId));
    }

    @Override
    public void onTaskViewDismissed(Task t) {
        // Remove any stored data from the loader.  We currently don't bother notifying the views
        // that the data has been unloaded because at the point we call onTaskViewDismissed(), the views
        // either don't need to be updated, or have already been removed.
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        /**hb tangjun add begin*/
        // Report that this tasks's data is no longer being used
        loader.unloadTaskData(t);
        /**hb tangjun add end*/
        loader.deleteTaskData(t, false);

        // Remove the old task from activity manager
        loader.getSystemServicesProxy().removeTask(t.key.id);
        /**hb tangjun add begin*/
        loader.getSystemServicesProxy().forceStopPackage(t.key.baseIntent.getComponent().getPackageName());
        /**hb tangjun add end*/
        /**hb tangjun add begin*/
        if(!mClearAllAnimRun) {
        	setViewClipPercent();
        }
        /**hb tangjun add end*/
    }

    @Override
    public void onAllTaskViewsDismissed(ArrayList<Task> removedTasks) {
        if (removedTasks != null) {
            int taskCount = removedTasks.size();
            for (int i = 0; i < taskCount; i++) {
                onTaskViewDismissed(removedTasks.get(i));
            }
        }

        mCb.onAllTaskViewsDismissed();

        // Keep track of all-deletions
        MetricsLogger.count(getContext(), "overview_task_all_dismissed", 1);
    }

    /** Final callback after Recents is finally hidden. */
    public void onRecentsHidden() {
        // Notify each task stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            stackView.onRecentsHidden();
        }
        
        /**hb tangjun add for unloadTaskData when quit RecentsActivity begin*/
        if(mHbTaskList != null) {
	        int count = mHbTaskList.size();
	        //Log.d("tangjun222", "--onRecentsHidden count = " + count);
	        for(int i = 0; i < count; i++) {
	        	Task t = mHbTaskList.get(i);
	            // Report that this tasks's data is no longer being used
	            RecentsTaskLoader.getInstance().unloadTaskData(t);
	        }
        }
        /**hb tangjun add for unloadTaskData when quit RecentsActivity end*/
    }

    @Override
    public void onTaskStackFilterTriggered() {
        // Hide the search bar
        if (mSearchBar != null) {
            mSearchBar.animate()
                    .alpha(0f)
                    .setStartDelay(0)
                    .setInterpolator(mConfig.fastOutSlowInInterpolator)
                    .setDuration(mConfig.filteringCurrentViewsAnimDuration)
                    .withLayer()
                    .start();
        }
    }

    @Override
    public void onTaskStackUnfilterTriggered() {
        // Show the search bar
        if (mSearchBar != null) {
            mSearchBar.animate()
                    .alpha(1f)
                    .setStartDelay(0)
                    .setInterpolator(mConfig.fastOutSlowInInterpolator)
                    .setDuration(mConfig.filteringNewViewsAnimDuration)
                    .withLayer()
                    .start();
        }
    }

    @Override
    public void onTaskResize(Task t) {
        if (mCb != null) {
            mCb.onTaskResize(t);
        }
    }

    /**** RecentsPackageMonitor.PackageCallbacks Implementation ****/

    @Override
    public void onPackagesChanged(RecentsPackageMonitor monitor, String packageName, int userId) {
        // Propagate this event down to each task stack view
        List<TaskStackView> stackViews = getTaskStackViews();
        int stackCount = stackViews.size();
        for (int i = 0; i < stackCount; i++) {
            TaskStackView stackView = stackViews.get(i);
            stackView.onPackagesChanged(monitor, packageName, userId);
        }
    }
}
