package cn.com.protruly.soundrecorder.recordlist;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.common.ActionModeManager;
import cn.com.protruly.soundrecorder.managerUtil.AudioPlayManager;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.GlobalUtil;
import cn.com.protruly.soundrecorder.util.RecordFileInfo;

/**
 * Created by wenwenchao on 17-8-28.
 */

public class ExpandableAdapter extends BaseAdapter {

    private Context mContex;
    private List<RecordFileInfo> mDataList;
    private AudioPlayManager mAudioPlayManager;
    private ActionModeManager mActionModeManager;
    private HashSet<RecordFileInfo> selectedFileSet = new HashSet<>();
    private int markGraidViewRow = 4;
    public Boolean firstopen = false;
    private final Executor FileOption_EXECUTOR = Executors.newFixedThreadPool(2);
    private final int UPDATE_ITEM_UI = 1;
    private OnMenuClickListenerInAll mOnMenuClickListenerInAll;

    public ExpandableAdapter(Context context, List datalist) {
        this.mContex = context;
        this.mDataList = datalist;
    }

    public AudioPlayManager getAudioPlayManager() {
        return mAudioPlayManager;
    }

    public void setAudioPlayManager(AudioPlayManager mAudioPlayManager) {
        this.mAudioPlayManager = mAudioPlayManager;
    }

    public void setActionModeManager(ActionModeManager mActionModeManager) {
        this.mActionModeManager = mActionModeManager;
    }

    public void setSelectedFileSet(HashSet<RecordFileInfo> selectedFileSet) {
        this.selectedFileSet = selectedFileSet;
    }

    public void setmOnMenuClickListenerInAll(OnMenuClickListenerInAll mOnMenuClickListenerInAll) {
        this.mOnMenuClickListenerInAll = mOnMenuClickListenerInAll;
    }

    @Override
    public int getCount() {
        return mDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContex).inflate(R.layout.expandable_list_item, parent, false);
            vh = new ViewHolder(convertView);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        RecordFileInfo fileinfo = mDataList.get(position);
        vh.fileName.setText(fileinfo.getNameLabel());
        if (fileinfo.getTimeLong() != -1 || fileinfo.getTimeLabel()!=null) {
            vh.fileTimeLong.setText(GlobalUtil.formatTime_m_s(fileinfo.getTimeLong()));
            vh.fileCreateTime.setText(fileinfo.getTimeLabel());
        } else {
            showAndGetExtraInfo(vh, fileinfo);    //异步加载时长
        }

        if (mActionModeManager.getCurrentActionMode())     //批量操作模式
        {
            vh.checkBox.setVisibility(View.VISIBLE);
            if (selectedFileSet.contains(fileinfo)) {
                vh.checkBox.setChecked(true);
            } else {
                vh.checkBox.setChecked(false);
            }
        } else {
            vh.checkBox.setVisibility(View.GONE);
        }


        Boolean isExpanding = animationedView(convertView, vh.expandLayout, position);
        if (position == lastOpenPosition && isExpanding || firstopen) {
            if(firstopen)setNowListMinIndex(0);
            firstopen = false;
            setCurrentViewHolder(vh);
            initUIforViewHolder(fileinfo, vh, parent);
            if(convertView.getBackground()==null)convertView.setBackgroundColor(Color.WHITE);
        }else{
            if(convertView.getBackground()!=null)convertView.setBackground(null);
        }
        return convertView;
    }

    private void initUIforViewHolder(RecordFileInfo fileinfo, ViewHolder vh, ViewGroup parent) {
        vh.timelong.setText(GlobalUtil.formatTime_m_s(fileinfo.getTimeLong()));
        // Toast.makeText(mContex, "convertView is null", Toast.LENGTH_SHORT).show();
        vh.seekBar.setOnSeekBarChangeListener(new MySeekBarListener(fileinfo, vh));
        MyGridViewItemClickListener gridViewItemClickListener = new MyGridViewItemClickListener(fileinfo, vh);
        MyGridViewItemLongClickListener gridViewItemLongClickListener = new MyGridViewItemLongClickListener(fileinfo, vh);
        MyMuneViewClickListener muneViewClickListener = new MyMuneViewClickListener(fileinfo, vh);
        TimeGridViewAdapter mTimeGridViewAdapter = new TimeGridViewAdapter(mContex, fileinfo.getMarkTimeList());
        Log.d("wwcwwc","path="+fileinfo.getName()+"    marklist="+fileinfo.getMarkTimeList());

        vh.gridView.setAdapter(mTimeGridViewAdapter);
        vh.gridView.setOnItemLongClickListener(gridViewItemLongClickListener);
        vh.gridView.setOnItemClickListener(gridViewItemClickListener);
        vh.playButton.setOnClickListener(muneViewClickListener);
        vh.markButton.setOnClickListener(muneViewClickListener);
        vh.editButton.setOnClickListener(muneViewClickListener);
        vh.deleteButton.setOnClickListener(muneViewClickListener);


        if (fileinfo.getMarkTimeList().size() > 4 * markGraidViewRow) {
            View v = LayoutInflater.from(mContex).inflate(R.layout.item_gridview_layout, null);
            v.measure(0, 0);
            int testhigh = v.getMeasuredHeight();
            // int width =View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
            // int height =View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
            //  menuViewHolder.gridView.measure(width,height);
            //  Log.d("wnewenchao","  "+testhigh+"    "+menuViewHolder.gridView.getVerticalSpacing()+"    "+menuViewHolder.gridView.getNumColumns());
            int scrollhight = (testhigh + vh.gridView.getVerticalSpacing()) * markGraidViewRow;
            ViewGroup.LayoutParams params = vh.scrollView.getLayoutParams();
            params.height = scrollhight;
            vh.scrollView.setLayoutParams(params);
            ((CustomView.MyListView) parent).setEnableGridScroll(true);
        } else {
            ViewGroup.LayoutParams params = vh.scrollView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            vh.scrollView.setLayoutParams(params);
            ((CustomView.MyListView) parent).setEnableGridScroll(false);
        }
    }

    public class ViewHolder {
        TextView fileName;
        TextView fileTimeLong;
        TextView fileCreateTime;
        public CheckBox checkBox;

        public LinearLayout expandLayout;
        TextView timecount;
        TextView timelong;
        ImageButton playButton;
        ScaleSeekBar seekBar;
        CustomView.TimeGridView gridView;
        ScrollView scrollView;
        public Button markButton;
        Button editButton;
        Button deleteButton;


        public ViewHolder(View v) {
            fileName = (TextView) v.findViewById(R.id.item_filename);
            fileTimeLong = (TextView) v.findViewById(R.id.item_file_time_long);
            fileCreateTime = (TextView) v.findViewById(R.id.item_file_create_time);
            checkBox = (CheckBox) v.findViewById(R.id.item_file_checkbox);

            expandLayout = (LinearLayout) v.findViewById(R.id.expandable);
            timecount = (TextView) v.findViewById(R.id.item_file_timecount1);
            timelong = (TextView) v.findViewById(R.id.item_file_timecount2);
            playButton = (ImageButton) v.findViewById(R.id.item_playview);
            seekBar = (ScaleSeekBar) v.findViewById(R.id.item_seekbar);
            gridView = (CustomView.TimeGridView) v.findViewById(R.id.item_gridview);
            scrollView = (ScrollView) v.findViewById(R.id.item_gridscrollview);
            markButton = (Button) v.findViewById(R.id.item_mark);
            editButton = (Button) v.findViewById(R.id.item_edit);
            deleteButton = (Button) v.findViewById(R.id.item_delete);
        }
    }

    private void showAndGetExtraInfo(final ViewHolder holder, final RecordFileInfo fileinfo) {
        holder.fileTimeLong.setText("Loading...");//GlobalUtil.formatLongTimeString(0)
        holder.fileCreateTime.setText("Loading...");
        holder.fileTimeLong.setTag(fileinfo.getPath());
        holder.fileCreateTime.setTag(fileinfo.getPath());
        FileOption_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                long timelong = GlobalUtil.getTimeLongForAudio(fileinfo);
                int daycount = -1;
                String createTimeLable = null;
                try {
                    daycount = GlobalUtil.getDayFromNow(fileinfo.getModifiedTime());

                    if (daycount == 0) {
                        if (fileinfo.getModifiedTime() > GlobalConstant.JUSTNOW * 1000) {
                            createTimeLable = mContex.getString(R.string.justnow);
                        } else {
                            createTimeLable = GlobalUtil.formatDate_H_m(fileinfo.getModifiedTime());
                        }
                    } else if (daycount == 1) {
                        createTimeLable = mContex.getString(R.string.yesturday);
                    } else if (daycount > 1 && daycount <= 3) {
                        switch (GlobalUtil.getWeekDay(fileinfo.getModifiedTime())-1){
                            case 1:createTimeLable = mContex.getString(R.string.Mon);break;
                            case 2:createTimeLable = mContex.getString(R.string.Thu);break;
                            case 3:createTimeLable = mContex.getString(R.string.Wed);break;
                            case 4:createTimeLable = mContex.getString(R.string.Thu);break;
                            case 5:createTimeLable = mContex.getString(R.string.Fri);break;
                            case 6:createTimeLable = mContex.getString(R.string.Sat);break;
                            case 7:createTimeLable = mContex.getString(R.string.Sun);break;
                        }
                    } else if(daycount == -1) {
                        createTimeLable = GlobalUtil.formatDate_Y_M_d(mContex,fileinfo.getModifiedTime());
                    } else{
                        createTimeLable = GlobalUtil.formatDate_M_d(mContex,fileinfo.getModifiedTime());
                    }
                } catch (Exception E) {
                    Log.d("wwc516", "时间读取出错！");
                }
                fileinfo.setTimeLabel(createTimeLable);
                GroupViewHolderExtraInfo folderextrainfo = new GroupViewHolderExtraInfo(fileinfo.getPath(), timelong,createTimeLable, holder);
                mUpDateUIHandler.obtainMessage(UPDATE_ITEM_UI, folderextrainfo).sendToTarget();
            }
        });
    }

    private Handler mUpDateUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_ITEM_UI:
                    GroupViewHolderExtraInfo folderextrainfo = (GroupViewHolderExtraInfo) msg.obj;
                    long timelong = folderextrainfo.timelong;
                    String createTimeLable = folderextrainfo.createTimeLable;
                    String path = folderextrainfo.path;

                    ViewHolder viewholder = folderextrainfo.gvh;
                    if (timelong != -1 && viewholder.fileTimeLong.getTag().equals(path)) {
                        viewholder.fileTimeLong.setText(GlobalUtil.formatTime_m_s(timelong));
                    }
                    if(createTimeLable!=null && viewholder.fileCreateTime.getTag().equals(path)){
                        viewholder.fileCreateTime.setText(createTimeLable);
                    }
                    break;
            }
        }
    };

    class GroupViewHolderExtraInfo {
        public String path;
        public long timelong;
        public String createTimeLable;
        public ViewHolder gvh;

        public GroupViewHolderExtraInfo(String path, long timelong, String createTimeLable, ViewHolder gvh) {
            this.path = path;
            this.timelong = timelong;
            this.createTimeLable = createTimeLable;
            this.gvh = gvh;
        }
    }


    public interface OnMenuClickListenerInAll {
        void onPlayButtomClick(RecordFileInfo fileInfo, ViewHolder mvh);

        void onMarkButtomClick(RecordFileInfo fileInfo, ViewHolder mvh);

        void onEditButtomClick(RecordFileInfo fileInfo, ViewHolder mvh);

        void onDeleteButtomClick(RecordFileInfo fileInfo, ViewHolder mvh, int index);

        void onMarkGridItemClick(RecordFileInfo fileInfo, ViewHolder mvh, int index);

        void onMarkGridItemLongClick(RecordFileInfo fileInfo, ViewHolder mvh, int index);
    }

    class MyGridViewItemClickListener implements GridView.OnItemClickListener {
        RecordFileInfo fileInfo;
        ViewHolder menuViewHolder;

        public MyGridViewItemClickListener(RecordFileInfo fileInfo, ViewHolder mvh) {
            this.fileInfo = fileInfo;
            this.menuViewHolder = mvh;
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mOnMenuClickListenerInAll.onMarkGridItemClick(fileInfo, menuViewHolder, i);
        }
    }

    class MyGridViewItemLongClickListener implements GridView.OnItemLongClickListener {
        RecordFileInfo fileInfo;
        ViewHolder menuViewHolder;

        public MyGridViewItemLongClickListener(RecordFileInfo fileInfo, ViewHolder mvh) {
            this.fileInfo = fileInfo;
            this.menuViewHolder = mvh;
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            mOnMenuClickListenerInAll.onMarkGridItemLongClick(fileInfo, menuViewHolder, i);
            return true;
        }
    }

    class MyMuneViewClickListener implements View.OnClickListener {
        RecordFileInfo fileInfo;
        ViewHolder menuViewHolder;

        public MyMuneViewClickListener(RecordFileInfo fileInfo, ViewHolder mvh) {
            this.fileInfo = fileInfo;
            this.menuViewHolder = mvh;
        }

        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.item_playview:
                    mOnMenuClickListenerInAll.onPlayButtomClick(fileInfo, menuViewHolder);
                    break;
                case R.id.item_mark:
                    mOnMenuClickListenerInAll.onMarkButtomClick(fileInfo, menuViewHolder);
                    break;
                case R.id.item_edit:
                    mOnMenuClickListenerInAll.onEditButtomClick(fileInfo, menuViewHolder);
                    break;
                case R.id.item_delete:
                    mOnMenuClickListenerInAll.onDeleteButtomClick(fileInfo, menuViewHolder, lastOpenPosition);
                    break;
                default:
                    break;
            }
        }
    }

    class MySeekBarListener implements SeekBar.OnSeekBarChangeListener {
        RecordFileInfo fileInfo;
        ViewHolder menuViewHolder;

        public MySeekBarListener(RecordFileInfo fileInfo, ViewHolder mvh) {
            this.fileInfo = fileInfo;
            this.menuViewHolder = mvh;
            initSeekBarUI();
        }

        private void initSeekBarUI() {
            int playstatus = getAudioPlayManager().GetCurrentPlayStats();
            menuViewHolder.seekBar.setMax((int) fileInfo.getTimeLong());
            menuViewHolder.seekBar.setScalelist(fileInfo.getMarkTimeList());
            menuViewHolder.seekBar.invalidate();
            if (playstatus != GlobalConstant.PLAY_START && playstatus != GlobalConstant.PLAY_GOING && playstatus != GlobalConstant.PLAY_PAUSE) {     //设置图标初始状态
                menuViewHolder.seekBar.setProgress(0);
                menuViewHolder.timecount.setText(GlobalUtil.formatTime_m_s(0));
                menuViewHolder.playButton.setImageResource(R.drawable.icon_list_play);
                getCurrentViewHolder().deleteButton.setEnabled(true);
                getCurrentViewHolder().editButton.setEnabled(true);
                getCurrentViewHolder().markButton.setEnabled(false);
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mAudioPlayManager.onProgressChanged(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mAudioPlayManager.onStartTrackingTouch(seekBar.getProgress());
            getCurrentViewHolder().deleteButton.setEnabled(false);
            getCurrentViewHolder().editButton.setEnabled(false);
            getCurrentViewHolder().markButton.setEnabled(false);

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mAudioPlayManager.onStopTrackingTouch(seekBar.getProgress());
            setNowListMinIndex(0);
            if(getLastOpenPosition()!=-1)new markableAsyncTask((long)(seekBar.getProgress()),mDataList.get(getLastOpenPosition()),getCurrentViewHolder()).execute();
        }
    }

    private ViewHolder mCurrentViewHolder;

    public ViewHolder getCurrentViewHolder() {
        return mCurrentViewHolder;
    }

    public void setCurrentViewHolder(ViewHolder mCurrentViewHolder) {
        this.mCurrentViewHolder = mCurrentViewHolder;
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("wwc516", "play status ->" + msg.what);
            if (msg.what == GlobalConstant.PLAY_START) {
                getCurrentViewHolder().playButton.setImageResource(R.drawable.icon_list_stop);
                getCurrentViewHolder().deleteButton.setEnabled(false);
                getCurrentViewHolder().editButton.setEnabled(false);
            } else if(msg.what == GlobalConstant.PLAY_GOING){
                if (msg.obj != null) {
                    getCurrentViewHolder().timecount.setText(GlobalUtil.formatTime_m_s((long) msg.obj));
                    getCurrentViewHolder().seekBar.setProgress((int)((long) (msg.obj)));
                    if(getLastOpenPosition()!=-1)new markableAsyncTask((long) (msg.obj),mDataList.get(getLastOpenPosition()),getCurrentViewHolder()).execute();
                }
            } else{
                getCurrentViewHolder().playButton.setImageResource(R.drawable.icon_list_play);
                getCurrentViewHolder().deleteButton.setEnabled(true);
                getCurrentViewHolder().editButton.setEnabled(true);
                if(msg.what == GlobalConstant.PLAY_STOP){
                    getCurrentViewHolder().markButton.setEnabled(false);
                }
            }

        }

    };



    public void setNowListMinIndex(int nowListMinIndex) {
        this.nowListMinIndex = nowListMinIndex;
    }
    private int nowListMinIndex = 0;

   class markableAsyncTask extends AsyncTask{
       private long timecount;
       private RecordFileInfo file;
       private ViewHolder vh;
       private List<Long> markList;
       private Boolean enableMenu = true;
       private int maxpos;

       public markableAsyncTask(long timecount,RecordFileInfo file,ViewHolder vh){
           this.timecount=timecount;
           this.file=file;
           this.vh =vh;
           this.markList = file.getMarkTimeList();
       }
       @Override
       protected Object doInBackground(Object[] objects) {

           if(markList==null || markList.size()==0){
               enableMenu = true;
               return null;
           }
           maxpos = -1;
           for(int index=nowListMinIndex;index<markList.size();index++) {
               if (markList.get(index) > timecount) {
                   maxpos = index;
                   break;
               }
               if(markList.get(index) == timecount || markList.get(index)<timecount && markList.get(index)+1000 >timecount){   //当前时间或前一刻在1秒内，禁止点击
                   enableMenu = false;
                   return null;
               }
           }
           if(maxpos==-1){
               enableMenu = true;
               return null;
           }else{
               nowListMinIndex=maxpos;
               if(markList.get(maxpos)-1200>timecount && (maxpos==0 || maxpos>=1 && timecount>markList.get(maxpos-1)+1200)){
                   enableMenu = true;
               } else{
                   enableMenu = false;
               }

           }
           return null;
       }

       @Override
       protected void onPostExecute(Object o) {
           super.onPostExecute(o);
           if(enableMenu){
               vh.markButton.setEnabled(true);
           }else{
               vh.markButton.setEnabled(false);
           }
       }
   }





    private View lastOpen = null;
    private View lastOpenItemView = null;

    private int lastOpenPosition = -1;

    private int animationDuration = 300;

    private BitSet openItems = new BitSet();

    private final SparseIntArray viewHeights = new SparseIntArray(10);

    private ViewGroup parent;

    private OnItemExpandCollapseListener expandCollapseListener;

    public void setParent(ViewGroup parent) {
        this.parent = parent;
    }

    public int getLastOpenPosition() {
        return lastOpenPosition;
    }

    Boolean animationedView(final View parent, final View expandView, final int position) {

        expandView.measure(parent.getWidth(), parent.getHeight());
        if (expandView == lastOpen && position != lastOpenPosition) {//wwc
            // lastOpen is recycled, so its reference is false
            lastOpen = null;
            lastOpenItemView=null;
        }
        if (position == lastOpenPosition) {
            // re reference to the last view
            // so when can animate it when collapsed
            lastOpen = expandView;
            lastOpenItemView = parent;
        }
        int height = viewHeights.get(position, -1);
        Boolean isExpanding = false;
        if (height == -1) {
            viewHeights.put(position, expandView.getMeasuredHeight());
            isExpanding = updateExpandable(expandView, position);
        } else {
            isExpanding = updateExpandable(expandView, position);
        }
        expandView.requestLayout();
        return isExpanding;
    }


    public void dealAnimationOnItemClick(final View itemView,final View expandView, final int position) {

        Animation a = expandView.getAnimation();

        if (a != null && a.hasStarted() && !a.hasEnded()) {

            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                        /*view.performClick();*/
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

        } else {

            expandView.setAnimation(null);

            int type = expandView.getVisibility() == View.VISIBLE
                    ? ExpandCollapseAnimation.COLLAPSE
                    : ExpandCollapseAnimation.EXPAND;

            // remember the state
            if (type == ExpandCollapseAnimation.EXPAND) {
                openItems.set(position, true);
            } else {
                openItems.set(position, false);
            }
            // check if we need to collapse a different view
            if (type == ExpandCollapseAnimation.EXPAND) {
                if (lastOpenPosition != -1 && lastOpenPosition != position) {
                    if (lastOpen != null) {
                        animateView(lastOpen, ExpandCollapseAnimation.COLLAPSE);
                        notifiyExpandCollapseListener(
                                ExpandCollapseAnimation.COLLAPSE,
                                lastOpen, lastOpenPosition);
                    }
                    openItems.set(lastOpenPosition, false);
                }
                lastOpen = expandView;
                lastOpenPosition = position;
                lastOpenItemView = itemView;
            } else if (lastOpenPosition == position) {
                lastOpenPosition = -1;
            }
            animateView(expandView, type);
            // notifyDataSetChanged();//wwc
            notifiyExpandCollapseListener(type, expandView, position);
        }
    }


    Boolean updateExpandable(View target, int position) {

        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) target.getLayoutParams();

        if (position == lastOpenPosition && openItems.get(position)) {   //wwc
            target.setVisibility(View.VISIBLE);
            target.setBackgroundColor(Color.WHITE);
            params.bottomMargin = 0;
            return true;
        } else {
            target.setVisibility(View.GONE);
            target.setBackground(null);
            params.bottomMargin = 0 - viewHeights.get(position);
        }
        return false;
    }

    private void animateView(final View target, final int type) {
        Animation anim = new ExpandCollapseAnimation(
                target,
                type
        );
        anim.setDuration(getAnimationDuration());
        anim.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (type == ExpandCollapseAnimation.EXPAND) {
                    if (parent instanceof ListView) {
                        ListView listView = (ListView) parent;
                        if(listView.getLastVisiblePosition()==getLastOpenPosition() || listView.getFirstVisiblePosition()==getLastOpenPosition()) {

                            int movement = target.getBottom();
                            int itemTop = lastOpenItemView!=null?lastOpenItemView.getTop():0;
                            int itemButtom = lastOpenItemView!=null?lastOpenItemView.getBottom():0;
                            int listButtom = listView.getBottom();


                            Log.w("onAnimationEnd", "ExpandCollapseAnimation.EXPAND->listTOP="+listView.getTop()+"   listbottom="+listView.getBottom()+
                            "    itemtop="+lastOpenItemView.getTop()+"   itembottom="+lastOpenItemView.getBottom()+
                            "    extratop="+target.getTop()+"    extrabottom="+target.getBottom());

                            if(itemTop<0){
                                listView.smoothScrollBy(itemTop-50, getAnimationDuration());
                            }else if(itemButtom>listButtom){
                                listView.smoothScrollBy(itemButtom-listButtom+50, getAnimationDuration());
                            }



                        /*    Log.w("onAnimationEnd", "ExpandCollapseAnimation.EXPAND->listTOPR="+r2.top+"   listbottomR="+r2.bottom+
                                    "    extratopR="+r.top+"    extrabottomR="+r.bottom);

                            Rect r = new Rect();
                            boolean visible = target.getGlobalVisibleRect(r);
                            Rect r2 = new Rect();
                            listView.getGlobalVisibleRect(r2);
                            if (!visible) {
                                listView.smoothScrollBy(movement, getAnimationDuration());
                            } else {
                                if (r2.bottom == r.bottom) {
                                    Log.w("demo", "r2.bottom == r.bottom");
                                    Log.w("demo", r.bottom + "");
                                    Log.w("demo", movement + "");
                                    listView.smoothScrollBy(movement, getAnimationDuration());
                                }else if(itemTop < 0){
                                    Log.w("demo", "itemTop <= r2.top");
                                    Log.w("demo", itemTop + "");
                                    Log.w("demo", r2.top + "");
                                    listView.smoothScrollBy(itemTop, getAnimationDuration());
                                }
                            }*/
                        }
                    }
                }
               notifyDataSetChanged();
            }
        });
        target.startAnimation(anim);
    }


    /**
     * Closes the current open item.
     * If it is current visible it will be closed with an animation.
     *
     * @return true if an item was closed, false otherwise
     */
    public boolean collapseLastOpen() {
        if (isAnyItemExpanded()) {
            // if visible animate it out
            if (lastOpen != null) {
                animateView(lastOpen, ExpandCollapseAnimation.COLLAPSE);
                if(expandCollapseListener!=null){
                    expandCollapseListener.onCollapse(lastOpen,getLastOpenPosition());
                }
            }
            openItems.set(lastOpenPosition, false);
            lastOpenPosition = -1;
            return true;
        }
        return false;
    }


    /**
     * Sets a listener which gets call on item expand or collapse
     *
     * @param listener the listener which will be called when an item is expanded or
     *                 collapsed
     */
    public void setItemExpandCollapseListener(
            OnItemExpandCollapseListener listener) {
        expandCollapseListener = listener;
    }

    public void removeItemExpandCollapseListener() {
        expandCollapseListener = null;
    }

    /**
     * Interface for callback to be invoked whenever an item is expanded or
     * collapsed in the list view.
     */
    public interface OnItemExpandCollapseListener {
        /**
         * Called when an item is expanded.
         *
         * @param itemView the view of the list item
         * @param position the position in the list view
         */
        public void onExpand(View itemView, int position);

        /**
         * Called when an item is collapsed.
         *
         * @param itemView the view of the list item
         * @param position the position in the list view
         */
        public void onCollapse(View itemView, int position);

    }

    private void notifiyExpandCollapseListener(int type, View view, int position) {
        if (expandCollapseListener != null) {
            Log.w("demo", "expandCollapseListener != null");
            if (type == ExpandCollapseAnimation.EXPAND) {
                expandCollapseListener.onExpand(view, position);
            } else if (type == ExpandCollapseAnimation.COLLAPSE) {
                expandCollapseListener.onCollapse(view, position);
            }
        }
    }


    /**
     * Gets the duration of the collapse animation in ms.
     * Default is 330ms. Override this method to change the default.
     *
     * @return the duration of the anim in ms
     */
    public int getAnimationDuration() {
        return animationDuration;
    }

    /**
     * Set's the Animation duration for the Expandable animation
     *
     * @param duration The duration as an integer in MS (duration > 0)
     * @throws IllegalArgumentException if parameter is less than zero
     */
    public void setAnimationDuration(int duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("Duration is less than zero");
        }

        animationDuration = duration;
    }

    /**
     * Check's if any position is currently Expanded
     * To collapse the open item @see collapseLastOpen
     *
     * @return boolean True if there is currently an item expanded, otherwise false
     */
    public boolean isAnyItemExpanded() {
        return (lastOpenPosition != -1) ? true : false;
    }


}
