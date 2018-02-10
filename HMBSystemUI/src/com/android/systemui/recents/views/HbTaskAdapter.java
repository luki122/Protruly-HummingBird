package com.android.systemui.recents.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;  
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;  
import android.view.MotionEvent;
import android.view.View;  
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;  
import android.widget.TextView; 

import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import hb.widget.recycleview.RecyclerView;
import hb.widget.recycleview.RecyclerView.OnScrollListener;
//import android.support.v7.widget.RecyclerView;
//import android.support.v7.widget.RecyclerView.OnScrollListener;
  
public class HbTaskAdapter extends  RecyclerView.Adapter<HbTaskAdapter.ViewHolder>  {  
	
    private LayoutInflater mInflater;  
    private ArrayList<Task> mMstTaskList = null;
    private Context mContext;
    private SystemServicesProxy mSsp;
    private ArrayList<String> mSplitScreenPackageList;
    
    private static final int RECENTS_ITEM = 1;
    private static final int PADDINGFIRST_ITEM = 2;
    private static final int PADDINGLAST_ITEM = 3;
    
    private int mRemoveIndex = -10;
    private int mBindViewHolderIndex = 0;

    public interface OnItemListener  {  
        void onItemClick(View view, int position); 
        void onItemDismiss(View view, int position); 
        void onAllItemDismiss(); 
        boolean onItemTouchListener(View view, int position, MotionEvent event);
    }
    private OnItemListener mOnItemLitener;  
    public void setOnItemListener(OnItemListener onItemLitener)  {  
        this.mOnItemLitener = onItemLitener;  
    }
  
    public HbTaskAdapter(Context context, ArrayList<Task> taskList)  {  
    	mContext = context;
        mInflater = LayoutInflater.from(context);  
        mMstTaskList = taskList;
        mSsp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        mRemoveIndex = -10;
    }  
  
    public static class ViewHolder extends RecyclerView.ViewHolder  {  
        public ViewHolder(View arg0, int viewType)  {  
            super(arg0); 
            if(viewType == RECENTS_ITEM) {
            	mMstTaskView = (HbTaskView) arg0;
            }
        }
        HbTaskView mMstTaskView; 
        int mTaskIndex;
        boolean mIsTaskLoad;
        
        public void setTaskIndex(int taskIndex) {
        	mTaskIndex = taskIndex;
        }
        
        public int getTaskIndex() {
        	return mTaskIndex;
        }
        
        public void setIsTaskLoad(boolean isTaskLoad) {
        	mIsTaskLoad = isTaskLoad;
        }
        
        public boolean getIsTaskLoad() {
        	return mIsTaskLoad;
        }
    }  
    
    @Override  
    public int getItemCount()  {
        return mMstTaskList.size() + 1 + 1;  
    } 
    
    @Override
    public int getItemViewType(int position) {
    	// TODO Auto-generated method stub
    	if(position == 0) {
    		return PADDINGFIRST_ITEM;
    	} else if(position == mMstTaskList.size()+1){
    		return PADDINGLAST_ITEM;
    	} else {
    		return RECENTS_ITEM;
    	}
    }
  
    @Override  
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)  {
    	View view;
    	//Log.d("tangjun222", "---onCreateViewHolder ");
    	if(viewType == RECENTS_ITEM) {
    		view = mInflater.inflate(R.layout.recyclerview_task_item, viewGroup, false);
    	} else {
    		view = mInflater.inflate(R.layout.recyclerview_paddingfirst_item, viewGroup, false);
    	}
        ViewHolder viewHolder = new ViewHolder(view, viewType);
        
        return viewHolder;  
    }  

    @Override  
    public void onBindViewHolder(final ViewHolder viewHolder, final int position)  {
    	if(position == 0 || position == mMstTaskList.size() + 1) {
    		return;
    	}
    	final int index = position - 1;
    	final Task task = mMstTaskList.get(index);
    	//Log.d("tangjun222", "onBindViewHolder--- index = " + index + "，onBindViewHolder task.title = " + task.activityLabel);
    	mBindViewHolderIndex = index;
    	viewHolder.setTaskIndex(index);
    	viewHolder.setIsTaskLoad(true);
    	viewHolder.mMstTaskView.onTaskBound(task);
    	// Load the task data
    	RecentsTaskLoader.getInstance().loadTaskData(task, mContext);
        if(index == mMstTaskList.size() - 1) {
        	viewHolder.itemView.setPadding(mContext.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_normal_paddingleft), viewHolder.itemView.getPaddingTop(), 
        			mContext.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_last_paddingright), viewHolder.itemView.getPaddingBottom());
        } else {
        	viewHolder.itemView.setPadding(mContext.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_normal_paddingleft), viewHolder.itemView.getPaddingTop(), 
        			mContext.getResources().getDimensionPixelSize(R.dimen.hb_recent_task_item_normal_paddingright), viewHolder.itemView.getPaddingBottom());
        }
        
        //如果设置了回调，则设置点击事件  
        if (mOnItemLitener != null)  {  
            viewHolder.itemView.setOnClickListener(new OnClickListener()  { 
                @Override  
                public void onClick(View v)  {
                	mOnItemLitener.onItemClick(viewHolder.itemView, index); 
                }  
            });
            viewHolder.itemView.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					return mOnItemLitener.onItemTouchListener(viewHolder.itemView, index, event);
				}
			});
        }  
    }
    
    @Override  
    public void onViewRecycled(final ViewHolder holder) {
    	if(holder.getTaskIndex() < mMstTaskList.size()) {
    		//防止删除item的时候也调用onViewRecycled导致错误
    		if(mRemoveIndex < mBindViewHolderIndex - 2 || mRemoveIndex > mBindViewHolderIndex + 2) {
		    	final Task task = mMstTaskList.get(holder.getTaskIndex());
		    	if(holder.getIsTaskLoad()) {
		    		//Log.d("tangjun222", "---onViewRecycled task = " + task.activityLabel + ", index = " + holder.getTaskIndex());
		    		// Report that this tasks's data is no longer being used
		    		RecentsTaskLoader.getInstance().unloadTaskData(task);
		    		holder.setIsTaskLoad(false);
		    	}
		    	mRemoveIndex = -10;
    		} else {
    			//Log.d("tangjun222", "---onViewRecycled not  index = " + holder.getTaskIndex());
    		}
    	}
    }
    
    @Override 
    public void onViewAttachedToWindow(final ViewHolder holder) {
    	/*
    	if(holder.getTaskIndex() < mMstTaskList.size()) {
	    	final Task task = mMstTaskList.get(holder.getTaskIndex());
	    	Log.d("tangjun222", "---onViewAttachedToWindow task = " + task.activityLabel + ", index = " + holder.getTaskIndex());
    	}
    	*/
    }

    @Override 
    public void onViewDetachedFromWindow(final ViewHolder holder) {
    	/*
    	if(holder.getTaskIndex() < mMstTaskList.size()) {
	    	final Task task = mMstTaskList.get(holder.getTaskIndex());
	    	Log.d("tangjun222", "---onViewDetachedFromWindow task = " + task.activityLabel + ", index = " + holder.getTaskIndex());
    	}
    	*/
    }
    
    public void setTaskList(ArrayList<Task> taskList) {
    	mMstTaskList = taskList;
    }
    
    public void setSplitScreenPackageNames(ArrayList<String> splitScreenPackageList) {
    	mSplitScreenPackageList = splitScreenPackageList;
    }
    
    public void setRemoveIndex(int index) {
    	mRemoveIndex = index;
    }
}  
