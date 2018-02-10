package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;

import com.android.launcher3.colors.ColorManager;
import com.android.launcher3.config.ProviderConfig;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.FolderPagedView;
import com.android.launcher3.theme.utils.PhotoUtils;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.ItemInfoMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by antino on 17-3-31.
 */
public class ArrangeNavigationBar extends HorizontalScrollView implements DragSource,DropTarget,DragController.DragListener, View.OnLongClickListener, View.OnClickListener {
    private static final String TAG ="ArrangeNavigationBar";
    private Launcher mLauncher;
    private LinkedHashMap<View,ItemInfo> mArrangeItemMap = new LinkedHashMap<View,ItemInfo>();
    private ArrangeNavLinearLayout mNav;
    private IconCache mIconCatch;
    private BubbleTextView mAddFolder;
    private ArrayList<FolderIcon> mAddedFolders;
    private int mCountX=5;

    private int mIconWidth;
    private int mIconHeight;
    private DeviceProfile mDeviceProfile;
    private final Canvas mCanvas = new Canvas();
    private View mCurrentDragView;
    private float mDownMotionX;
    private float mDownMotionY;
    private ArrangeInfo  mCurrentDragArrangeInfo;

    private int mTargetRank=1;
    public ArrangeNavigationBar(Context context) {
        this(context,null);
    }

    public ArrangeNavigationBar(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ArrangeNavigationBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher)context;
        mDeviceProfile=LauncherAppState.getInstance().getInvariantDeviceProfile().portraitProfile;
    }

    public boolean onBackPressed(boolean animate){
        if(mArrangeItemMap.isEmpty()){
           finishRestoreItemsCallback.run();
        }
        mLauncher.getDragController().removeDropTarget(this);
        return restoreItems(animate);
        //return mutilpleAdd2Other();
    }

    public boolean onBackPressed(){
        return onBackPressed(true);
        //return mutilpleAdd2Other();
    }

    public View getLayout(){
        return mNav;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNav = (ArrangeNavLinearLayout) findViewById(R.id.nav_linear);
        mIconCatch = LauncherAppState.getInstance().getIconCache();
        mNav.enableLayoutTransitions();
        mAddFolder = (BubbleTextView) findViewById(R.id.add_folder);
        mAddFolder.setOnClickListener(this);
//        ViewGroup.LayoutParams lp = mAddFolder.getLayoutParams();
//        lp.width=mIconWidth;
//        lp.height = LayoutParams.MATCH_PARENT;
        float fontScale = getResources().getConfiguration().fontScale;
        if(fontScale>=1.3f){
            mAddFolder.setPadding(getPaddingLeft(),
                    getResources().getDimensionPixelSize(R.dimen.add_folder_padding_top_large),
                    getPaddingRight(),
                    getPaddingBottom());
        }
        mAddFolder.setCompoundDrawablePadding(mDeviceProfile.iconDrawablePaddingPx);
        //mAddFolder.setLayoutParams(lp);
        mAddFolder.setTextColor(ColorManager.getInstance().getColors()[0]);
    }

    public  void setItemWidth(int width) {
        mIconWidth = width;
        ViewGroup.LayoutParams lp = mAddFolder.getLayoutParams();
        lp.width = mIconWidth;
        lp.height = LayoutParams.MATCH_PARENT;
        Log.i("YYYY", "mDeviceProfile.iconDrawablePaddingPx = " + mDeviceProfile.iconDrawablePaddingPx);
        mAddFolder.setCompoundDrawablePadding(mDeviceProfile.iconDrawablePaddingPx);
        mAddFolder.setLayoutParams(lp);
    }


    public void addViewToTheFirstPage(View view, ShortcutInfo info){
        addArrangeItem(info,view);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width=mIconWidth;
        lp.height = LayoutParams.MATCH_PARENT;
        if(view.getParent()!=null){
            Log.i(Launcher.TAG,"addViewToTheFirstPage :Something may be wrong here.view = "+view);
            ((ViewGroup)(view.getParent())).removeView(view);
        }
        mNav.addView(view,1);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        //view.setOnKeyListener(null);
    }
    public void addViewToThePage(View view, ShortcutInfo info,int rank){
        addArrangeItem(info,view);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width=mIconWidth;
        lp.height = LayoutParams.MATCH_PARENT;
        mNav.addView(view,rank);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        //view.setOnKeyListener(null);
    }
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

    }

    private void removeArrangeItem(ItemInfo info, View v){
        mArrangeItemMap.remove(v);

    }
    private void addArrangeItem(ItemInfo info,View v){
        mArrangeItemMap.put(v,info);
    }

    public boolean mutilpleAdd2Other(){
//        if(mLauncher.mWorkspace.isAddScreen()){
//            Toast.makeText(mLauncher, "此页无法操作.", Toast.LENGTH_SHORT).show();
//            return false;
//        }
        Log.i(TAG,"mutilpleAdd2Other");
        Folder folder = mLauncher.getOpenFolder();
        if(folder==null) {
            Set<View> keyset = mArrangeItemMap.keySet();
            ArrayList<View> views = new ArrayList<>();
            views.addAll(keyset);
            for (View startView : views) {
                ShortcutInfo info = (ShortcutInfo) startView.getTag();
                final int[] position = {info.cellX, info.cellY};
                Log.i("YYYYYYY","goback : "+info+" ,  ");
                boolean founded = mLauncher.getWorkspace().findCellForSpanInCurrentPage(position, info);
                Log.i("YYYYYYY","goback : "+info+" ,  "+founded);
                if (founded) {
                    View newView = mLauncher.createShortcut(info);
                    Bitmap b = getDragView(startView,info);
                    final DragView dragView = new DragView(mLauncher, b, 0, 0, 1.0f, 0.0f);
                    removeArrangeItem(info, startView);
                    mAnimationObjects.add(new AnimationObject(dragView, newView, startView, info));
                    mLauncher.getWorkspace().addViewAndUpdateData(position, newView, dragView, startView, false, false);
                    newView.setVisibility(View.INVISIBLE);
                }
            }
            int delay = -80;
            Collections.sort(mAnimationObjects);
            for (final AnimationObject object : mAnimationObjects) {
                delay += 80;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        object.startAnimation();
                    }
                }, delay);
            }
            mAnimationObjects.clear();
            views.clear();

        }else{
            int childCountBefore = folder.getItemCount();
            Set<View> keyset = mArrangeItemMap.keySet();
            ArrayList<View> views = new ArrayList<>();
            views.addAll(keyset);
            for(View startView:views){
                ShortcutInfo info = (ShortcutInfo) startView.getTag();
                ArrangeInfo rInfo = mArrangeInfoMap.get(info);
                info.container = folder.mInfo.id;
                Bitmap b = getDragView(startView,info);
                DragView dragView = new DragView(mLauncher, b, 0, 0, 1.0f, 0.0f);
                removeArrangeItem(info, startView);
                View newView = folder.addIcon(dragView, info, 0,false);
                newView.setVisibility(View.INVISIBLE);
                AnimationObject aInfo =new AnimationObject(dragView, newView, startView, info);
                ((ViewGroup)(startView.getParent())).removeView(startView);
                aInfo.folderItemIndex = childCountBefore;
                mAnimationObjects.add(aInfo);
            }
            int childCountCurrent = folder.getItemCount();
            int lastPageIconCount = folder.getContent().getLastPageIconCount();
            int delay = -80;
            int i = mAnimationObjects.size();
            for (final AnimationObject object : mAnimationObjects) {
                delay += 80;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        object.startAnimation();
                    }
                }, delay);
            }
            mAnimationObjects.clear();
            views.clear();
        }

        if (mArrangeItemMap.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    Runnable finishRestoreItemsCallback = new Runnable() {
        @Override
        public void run() {
            mLauncher.mWorkspace.cleanEmptyScreensAndFolders();//lijun add
        }
    };

    private boolean restoreItems(boolean animate){
        boolean returnV = true;
        Set<View> keyset = mArrangeItemMap.keySet();
        ArrayList<View> goBack = new ArrayList<View>();
        ArrayList<View> goEmptyPosition = new ArrayList<View>();
        ArrayList<View> goBackFoder=new ArrayList<>();
        for(View v:keyset){
            ShortcutInfo info = (ShortcutInfo) v.getTag();
            Log.i(TAG,"arrange info : "+mArrangeInfoMap.get(info));
            ArrangeInfo aInfo=mArrangeInfoMap.get(info);
            if(aInfo.root instanceof FolderPagedView){
                goBackFoder.add(v);
            }else if(!isOcupied(mArrangeItemMap.get(v))){
                goBack.add(v);
            }else{
                goEmptyPosition.add(v);
            }
            returnV = false;
        }
        for(final View startView:goBack){
            ShortcutInfo info = (ShortcutInfo) startView.getTag();
            if(isOcupied(mArrangeItemMap.get(startView))){
                goEmptyPosition.add(startView);
                continue;
            }
            final int[] position ={info.cellX,info.cellY};
            Bitmap b =  getDragView(startView,info);
            DragView dragView = null;
            removeArrangeItem(info,startView);
            //startView.setVisibility(View.GONE);
            View newView = mLauncher.createShortcut((ShortcutInfo) startView.getTag());
            if(animate) {
                dragView = new DragView(mLauncher,b,0,0,1.0f,0.0f);
                mAnimationObjects.add(new AnimationObject(dragView, newView, startView, info));
                newView.setVisibility(View.INVISIBLE);
            }
            mLauncher.getWorkspace().addViewAndUpdateData(position,newView,dragView,startView,true,false);

        }
        for(View startView:goEmptyPosition){
            ShortcutInfo info = (ShortcutInfo) startView.getTag();
            final int[] position ={info.cellX,info.cellY};
            boolean founded = mLauncher.getWorkspace().findSuitablePositionForView(position,info);
            if(!founded){
                long screenId = LauncherSettings.Settings.call(mLauncher.getContentResolver(),
                        LauncherSettings.Settings.METHOD_NEW_SCREEN_ID)
                        .getLong(LauncherSettings.Settings.EXTRA_VALUE);
                mLauncher.getWorkspace().insertNewWorkspaceScreen(screenId);
                info.cellX=0;
                info.cellY=0;
                info.screenId=screenId;
            }
            View newView = mLauncher.createShortcut(info);
            Bitmap b = getDragView(startView,info);
            DragView dragView =null;
            removeArrangeItem(info,startView);
            //startview.setvisibility(view.gone);
            if(animate) {
                dragView = new DragView(mLauncher,b,0,0,1.0f,0.0f);
                mAnimationObjects.add(new AnimationObject(dragView, newView, startView, info));
                newView.setVisibility(View.INVISIBLE);
            }
            mLauncher.getWorkspace().addViewAndUpdateData(position,newView,dragView,startView,true,false);


        }
        for(View startView:goBackFoder){
            ShortcutInfo info = (ShortcutInfo) startView.getTag();
            ArrangeInfo aInfo=mArrangeInfoMap.get(info);
            if(aInfo!=null&&aInfo.root instanceof FolderPagedView){
                Folder folder=((FolderPagedView) (aInfo.root)).mFolder;
                Bitmap b = getDragView(startView,info);
                DragView dragView = null;
                removeArrangeItem(info,startView);
                //folder.addIcon(dragView,info,0,false);
                if(animate) {
                    dragView = new DragView(mLauncher,b,0,0,1.0f,0.0f);
                    mAnimationObjects.add(new AnimationObject(dragView, folder.mFolderIcon, startView, info));
                }
                ((ViewGroup)startView.getParent()).removeView(startView);
                if(!animate) {
                    folder.mFolderIcon.addItem(info);
                }
            }else{
                Log.i(TAG,"ArrangeInfo root is null or is not FolderPagedView");
            }
        }

        if(!mAnimationObjects.isEmpty()){
            mAnimationObjects.get(mAnimationObjects.size()-1).callback = finishRestoreItemsCallback;
        }else if(!animate){
            finishRestoreItemsCallback.run();
        }
        mArrangeItemMap.clear();
        int delay = -100;
        Collections.sort(mAnimationObjects);
        for(final AnimationObject object:mAnimationObjects){
            delay+=100;
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    object.startAnimation();
                }
            },delay);
        }
        Log.i("compare","mAnimationObjects = "+mAnimationObjects.toString());
        mAnimationObjects.clear();
        return returnV;
    }

    private Comparator<AnimationObject> mAnimationObjectComparator = new Comparator<AnimationObject>() {
        @Override
        public int compare(AnimationObject o1, AnimationObject o2) {
            return 0;
        }
    };

    private boolean isOcupied(ItemInfo itemInfo) {
        return mLauncher.getWorkspace().isOcupied(itemInfo);
    }


    @Override
    public void onClick(View v) {
//        if(mLauncher.mWorkspace.isAddScreen()){
//            Toast.makeText(mLauncher, "此页无法操作.", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if(v.getTag() instanceof ShortcutInfo){
            addNavigationbarIconToOtherPage(v);
        }else if(v.getId()==R.id.add_folder){
            addEmptyFolder();
        }
    }

    private void addEmptyFolder() {
        FolderIcon folderIcon = mLauncher.mWorkspace.createEmptFolder();
        if(folderIcon == null){
            return;
        }
        folderIcon.getFolder().getContent().createAndAddNewPage();
        if(folderIcon!=null){
            if(mAddedFolders == null){
                mAddedFolders = new ArrayList<FolderIcon>();
            }
            mAddedFolders.add(folderIcon);
        }
    }

    public boolean addNavigationbarIconToOtherPage(View v,boolean animateImmedialty){
        Folder folder = mLauncher.getOpenFolder();
        if(folder==null){
            int[] position = new int[2];
            if(mLauncher.getWorkspace().findCellForSpanInCurrentPage(position)){
                ShortcutInfo info = (ShortcutInfo) v.getTag();
                mArrangeInfoMap.remove(info);
                Bitmap b = getDragView(v,info);
                DragView dragView = new DragView(mLauncher,b,0,0,1.0f,0.0f);
                removeArrangeItem(info,v);
                v.setVisibility(View.GONE);
                View  newView = mLauncher.createShortcut((ShortcutInfo) v.getTag());
                mLauncher.getWorkspace().addViewAndUpdateData(position,newView,dragView,v);
                return  true;
            }else{
                mLauncher.showOutOfSpaceMessage(false);
            }
        }else{
            ShortcutInfo info = (ShortcutInfo) v.getTag();
            ArrangeInfo rInfo = mArrangeInfoMap.get(info);
            Bitmap b =  getDragView(v,info);
            DragView dragView = new DragView(mLauncher, b, 0, 0, 1.0f, 0.0f);
            Rect r = new Rect();
            int[] cord = new int[]{0,0};
            Utilities.getDescendantCoordRelativeToAncestor(v, mLauncher.getDragLayer(),
                    cord, false);
            dragView.show(cord[0], cord[1]);
            removeArrangeItem(info, v);
            v.setVisibility(View.GONE);
            folder.addIcon(dragView, info, 0);
        }
        return false;
    }

    public  boolean addNavigationbarIconToOtherPage(View v){
        return addNavigationbarIconToOtherPage(v,false);
    }

    public void addIconIntoNavigationbar(final View v){
        ViewGroup group = (ViewGroup)(v.getParent().getParent());
        if(group instanceof CellLayout){
            ShortcutInfo info = (ShortcutInfo) v.getTag();
            int[] cord1 = new int[]{0,0};
            Bitmap b =  getDragView(v,info);
            float scale = mLauncher.getDragLayer().getLocationInDragLayer(v, cord1);
            cord1[0]= Math.round(cord1[0] - (b.getWidth() - scale * v.getWidth()) / 2);
            final DeviceProfile grid = mLauncher.getDeviceProfile();
            cord1[1]=Math.round(cord1[1]+scale*(((CellLayout)group).getCellHeight()-grid.cellHeightPx) / 2-((1-scale)*b.getWidth())/2);
            final DragView dragView = new DragView(mLauncher,b,0,0,scale,0);
            dragView.show((int)(cord1[0]),(int)(cord1[1]));
            ViewGroup root=null;
            if(group!=null&&group.getParent()!=null){
                root = (ViewGroup)group.getParent();
            }
            if(root!=null){
                mArrangeInfoMap.put(info,new ArrangeInfo(root,info));
            }else{
                Log.i(TAG,"addIconIntoNavigationbar : something maybe wrong here");
            }
            if(root!=null&&root.getParent().getParent() instanceof Folder){
                v.setVisibility(INVISIBLE);
                ((Folder)root.getParent().getParent()).animate(info);
            }else{
                v.setVisibility(INVISIBLE);
            }
            mLauncher.removeItem(v,info,false);
            addViewToTheFirstPage(v,info);
            measureChlid();
            mNav.invalidate();
            post(new Runnable() {
                @Override
                public void run() {
                mLauncher.getDragLayer().animateViewIntoPosition2(dragView,v,null,null);
                }
            });

        }
    }
    public void addIconDragToNavigationbar(final View v, final DragObject dragObject){
        ViewGroup group = null;
        if(v.getParent()!=null) {
            group = (ViewGroup) (v.getParent().getParent());
        }else if(dragObject.dragSource instanceof  Folder){
            Folder folder = (Folder) dragObject.dragSource;
            group= folder.getContent().getCurrentCellLayout();
        }
        ShortcutInfo info = (ShortcutInfo) v.getTag();
        if(group instanceof CellLayout){
            int[] cord1 = new int[]{0,0};
            Bitmap b =  getDragView(v,info);
            float scale = mLauncher.getDragLayer().getLocationInDragLayer(v, cord1);
            cord1[0]= Math.round(cord1[0] - (b.getWidth() - scale * v.getWidth()) / 2);
            final DeviceProfile grid = mLauncher.getDeviceProfile();
            cord1[1]=Math.round(cord1[1]+scale*(((CellLayout)group).getCellHeight()-grid.cellHeightPx) / 2-((1-scale)*b.getWidth())/2);
            //final DragView dragView = new DragView(mLauncher,b,0,0,scale,0);
            //dragView.show((int)(cord1[0]),(int)(cord1[1]));
            ViewGroup root=null;
            if(group!=null&&group.getParent()!=null){
                root = (ViewGroup)group.getParent();
            }
            if(root!=null){
                mArrangeInfoMap.put(info,new ArrangeInfo(root,info));
            }else{
                Log.i(TAG,"addIconIntoNavigationbar : something maybe wrong here");
            }
            Log.i(TAG,"dragObject.dragView =  "+dragObject.dragView);
            if(root!=null&&root.getParent().getParent() instanceof Folder){
                v.setVisibility(INVISIBLE);
               // ((Folder)root.getParent().getParent()).animate(info);
            }else{
                v.setVisibility(INVISIBLE);
            }
            mLauncher.removeItem(v,info,false);
            addViewToThePage(v,info,mTargetRank);
            measureChlid();
            mNav.invalidate();
/*            post(new Runnable() {
                @Override
                public void run() {*/
                    mLauncher.getDragLayer().animateViewIntoPosition3(dragObject.dragView,v,null,null);
/*                }
            });*/

        }else if(mCurrentDragArrangeInfo !=null){
            ArrangeInfo arrangeInfo = mCurrentDragArrangeInfo;
            ViewGroup root=null;
            root = arrangeInfo.root;
//            if(root!=null){
                mArrangeInfoMap.put(info,new ArrangeInfo(root,info));
//            }else{
//                Log.i(TAG,"addIconIntoNavigationbar : something maybe wrong here");
//            }
            Log.i(TAG,"dragObject.dragView =  "+dragObject.dragView);
            if(root!=null&&root.getParent().getParent() instanceof Folder){
                v.setVisibility(INVISIBLE);
                ((Folder)root.getParent().getParent()).animate(info);
            }else{
                v.setVisibility(INVISIBLE);
            }
            mLauncher.removeItem(v,info,false);
            addViewToThePage(v,info,mTargetRank);
            measureChlid();
            mNav.invalidate();
/*            post(new Runnable() {
                @Override
                public void run() {*/
            mLauncher.getDragLayer().animateViewIntoPosition3(dragObject.dragView,v,null,null);
/*                }
            });*/

        }
    }
    private ArrayList<AnimationObject> mAnimationObjects = new ArrayList<AnimationObject>();

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 1f ;
    }

    @Override
    public void onFlingToDeleteCompleted() {

    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete, boolean success) {
        if(!success&&mCurrentDragView!=null){
            addIconDragToNavigationbar(mCurrentDragView,d);
        }
        mCurrentDragView=null;
        mCurrentDragArrangeInfo=null;
        mTargetRank = 1;
    }

    @Override
    public void fillInLaunchSourceData(View v, ItemInfo info, LauncherLogProto.Target target, LauncherLogProto.Target targetParent) {

    }

    @Override
    public boolean isDropEnabled() {
        return mLauncher.isArrangeBarShowing()&&mLauncher.isLauncherArrangeMode();
    }

    @Override
    public void onDrop(DragObject dragObject) {
        addIconDragToNavigationbar(mLauncher.getWorkspace().getDragInfo().cell,dragObject);
        //dragObject.deferDragViewCleanupPostAnimation = false;
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        if(dragObject.dragSource instanceof  Folder){
            Folder folder=(Folder) dragObject.dragSource;
            folder.cancelExitAlarm();
        }
    }

    @Override
    public void onDragOver(DragObject d) {
        ItemInfo item = d.dragInfo;
        if (item == null) {
            if (ProviderConfig.IS_DOGFOOD_BUILD) {
                throw new NullPointerException("DragObject has null info");
            }
            return;
        }
        final float[] r = new float[2];
        mTargetRank = getTargetRank(d,r);
    }

    private int getTargetRank(DragObject d, float[] recycle) {
        d.getVisualCenter(recycle);
        int childCount = mNav.getChildCount();
        int width = mLauncher.getDeviceProfile().widthPx - getPaddingLeft() - getPaddingRight();
        float x = recycle[0] - getPaddingLeft() -1;
        int viewWidth = mNav.getChildAt(0).getMeasuredWidth();

        int round = Math.round(x / viewWidth);
        if(round==0){
            return 1;
        }/*else if( round>4){
            return childCount<round?childCount:round;
        }*/else {

            return childCount<round?childCount:round;
        }
    }

    @Override
    public void onDragExit(DragObject dragObject) {

    }

    @Override
    public void onFlingToDelete(DragObject dragObject, PointF vec) {

    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        return true;
    }

    @Override
    public void prepareAccessibilityDrop() {

    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
        outRect.bottom+=500;
        outRect.right+=200;
        outRect.left-=200;
    }

    @Override
    public void onDragStart(DragObject dragObject, DragOptions options) {
    Log.d(TAG,"onDragStart");
        if(mCurrentDragView instanceof  BubbleTextView) {
            ShortcutInfo info = (ShortcutInfo) mCurrentDragView.getTag();
            mNav.removeView(mCurrentDragView);
            mCurrentDragArrangeInfo = mArrangeInfoMap.remove(mCurrentDragView.getTag());
            removeArrangeItem(info, mCurrentDragView);
        }
    }

    @Override
    public void onDragEnd() {
        Log.d(TAG,"onDragEnd");
        mLauncher.getDragController().removeDragListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        return mLauncher.onLongClick(v);
    }

    class AnimationObject implements Comparable<AnimationObject>{
        public DragView dragView;
        public View targetView;
        public View startView;
        public int[] position = new int[2];
        public ItemInfo info;
        public int folderItemIndex;
        public Runnable callback;
        public AnimationObject(DragView dg,View newView,View startView,ItemInfo info){
            this.info = info;
            dragView =dg;
            targetView = newView;
            this.startView = startView;
            Utilities.getDescendantCoordRelativeToAncestor(startView, mLauncher.getDragLayer(),
                    position, false);
        }
        public void startAnimation(){
            Log.i("xxxxx","position[0] = "+position[0]+" , position[1] = "+position[1]+" info.rank = "+info.rank);
            dragView.show(position[0],position[1]);

            float scaleRelativeToDragLayer = 1.0f;
            if(targetView instanceof FolderIcon) {
                if (dragView != null) {
                    FolderIcon icon = (FolderIcon) targetView;
                    int index = icon.mInfo.contents.size();
                    info.cellX = -1;
                    info.cellY = -1;
                    DragLayer dragLayer = mLauncher.getDragLayer();
                    Rect from = new Rect();
                    dragLayer.getViewRectRelativeToSelf(dragView, from);
                    Rect to = null;
                    if (to == null) {
                        to = new Rect();
                        Workspace workspace = mLauncher.getWorkspace();
                        // Set cellLayout and this to it's final state to compute final animation locations
                        workspace.setFinalTransitionTransform((CellLayout) icon.getParent().getParent());
                        float scaleX = icon.getScaleX();
                        float scaleY = icon.getScaleY();
                        icon.setScaleX(1.0f);
                        icon.setScaleY(1.0f);
                        scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(icon, to);
                        // Finished computing final animation locations, restore current state
                        icon.setScaleX(scaleX);
                        icon.setScaleY(scaleY);
                        workspace.resetTransitionTransform((CellLayout) icon.getParent().getParent());
                    }
                    int[] center=new int[2];
                    float scale = icon.getLocalCenterForIndex(index, index + 1, center);
                    //center[0] = (int) Math.round(scaleRelativeToDragLayer * center[0]);
                    //center[1] = (int) Math.round(scaleRelativeToDragLayer * center[1]);


                    to.offset(center[0] - dragView.getMeasuredWidth() / 2,
                            center[1] - dragView.getMeasuredHeight() / 2);

                    float finalAlpha = index < icon.mPreviewLayoutRule.numItems() ? 0.5f : 0f;

                    float finalScale = scale * scaleRelativeToDragLayer;
                    dragLayer.animateView2(dragView, from, to, finalAlpha,
                            1, 1, finalScale, finalScale, 600,
                            new DecelerateInterpolator(2), new AccelerateInterpolator(2),
                            callback, DragLayer.ANIMATION_END_DISAPPEAR, null);
                    icon.addItem((ShortcutInfo) info);
                    icon.hideItem((ShortcutInfo) info,index);
                }
            }else {
                mLauncher.getDragLayer().animateViewIntoPosition2(dragView, targetView, callback, null);
            }
        }

        @Override
        public int compareTo(AnimationObject o) {
            if(getRank(o.info)<getRank(this.info)){
                return 1;
            }else{
                return -1;
            }
        }
        @Override
        public String toString() {
            return info.toString();
        }
    }

    public int getRank(ItemInfo info){
        int pageIndex = mLauncher.getWorkspace().getPageIndexForScreenId(info.screenId);
        int row = LauncherAppState.getInstance().getInvariantDeviceProfile().numRows;
        int column = LauncherAppState.getInstance().getInvariantDeviceProfile().numColumns;
        return pageIndex*row*column+info.cellY*column+info.cellX;
    }

    public HashMap<ShortcutInfo,ArrangeInfo> mArrangeInfoMap = new HashMap<ShortcutInfo,ArrangeInfo>();
    class ArrangeInfo{
        public ViewGroup root;
        public ShortcutInfo arrangeItem;
        public ArrangeInfo(ViewGroup vg,ShortcutInfo info){
            root = vg;
            arrangeItem = info;
        }

        @Override
        public String toString() {
            return arrangeItem.title+" : "+root.toString();
        }
    }

    public void removeItemsByMatcher(final ItemInfoMatcher matcher){
        final ArrayList<View> childrenToRemove = new ArrayList<>();
        final HashMap<ItemInfo, View> children = new HashMap<>();
        for (int j = 0; j < mNav.getChildCount(); j++) {
            final View view = mNav.getChildAt(j);
            if(view.getTag()!=null){
                children.put((ItemInfo) view.getTag(), view);
            }
        }
        LauncherModel.ItemInfoFilter filter = new LauncherModel.ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (matcher.matches(info, cn)) {
                    childrenToRemove.add(children.get(info));
                    return true;
                }
                return false;
            }
        };
        LauncherModel.filterItemInfos(children.keySet(), filter);
        for (View child : childrenToRemove) {
            ShortcutInfo info = (ShortcutInfo) child.getTag();
            mNav.removeView(child);
            mArrangeInfoMap.remove(child.getTag());
            removeArrangeItem(info,child);
        }

    }

    public void enableAddFolder(boolean enable){
        mAddFolder.setEnabled(enable);
        mAddFolder.setTextColor(mAddFolder.getTextColors().withAlpha(enable?255:51));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                if(!determineScrollingStart(ev)){
                    return false;
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                // Remember location of down touch
                mDownMotionX = x;
                mDownMotionY = y;
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                if(!determineScrollingStart(ev)){
                    return false;
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                // Remember location of down touch
                mDownMotionX = x;
                mDownMotionY = y;
            }
        }
        return super.onTouchEvent(ev);
    }

    private boolean determineScrollingStart(MotionEvent ev){
        float deltaX = ev.getX() - mDownMotionX;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(ev.getY() - mDownMotionY);

        if (Float.compare(absDeltaX, 0f) == 0) return false;

        float slope = absDeltaY / absDeltaX;
        float theta = (float) Math.atan(slope);

        if (theta > (float) Math.PI / 3) {
            return false;
        }
        return true;
    }

    public void onColorChanged(int[] colors){
        mAddFolder.setTextColor(colors[0]);
    }
    private Bitmap getDragView(View view, ShortcutInfo info){
        if(view instanceof  BubbleTextView){
            BubbleTextView textView = (BubbleTextView) view;
            if(textView.hasIDynamicIcon()){
                //boolean showUnread = mLauncher.isShowUnread();//liuzuo add

                return PhotoUtils.drawable2bitmap(textView.getIcon());//new DragPreviewProvider(view).createDragBitmap(mCanvas,showUnread)
            }
        }
        return info.getIcon(mIconCatch);
    }
    public boolean startDrag(View v, DragOptions options) {
        Log.d(TAG,"startDrag");
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            ShortcutInfo item = (ShortcutInfo) tag;
            if (!v.isInTouchMode()) {
                return false;
            }else if(tag instanceof  FolderInfo){
                return false;
            }

//            mEmptyCellRank = item.rank;
            mCurrentDragView = v;
            mLauncher.getDragController().addDragListener(this);
//
//            mDragController.addDragListener(this);
            if (options.isAccessibleDrag) {
            /*    mDragController.addDragListener(new AccessibileDragListenerAdapter(
                        mContent, CellLayout.FOLDER_ACCESSIBILITY_DRAG) {

                    @Override
                    protected void enableAccessibleDrag(boolean enable) {
                        super.enableAccessibleDrag(enable);
                        mFooter.setImportantForAccessibility(enable
                                ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                                 : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                    }
                });*/
            }

            mLauncher.getWorkspace().beginDragShared(v, this, options);
            //lijun add for pageindicator start
            CellLayout.CellInfo longClickCellInfo = null;
            if (v.getTag() instanceof ItemInfo) {
                ItemInfo info = (ItemInfo) v.getTag();
                longClickCellInfo = new CellLayout.CellInfo(v, info);
            }
            mLauncher.getWorkspace().startDragOutOfWorkspace(longClickCellInfo);
            if(mLauncher.getOpenFolder()!=null){
                mLauncher.getOpenFolder().beginExternalDrag();
            }
            //lijun add for pageindicator end
        }
        return true;
    }

    public int getTargetRank() {
        return mTargetRank;
    }
    public int getIconWidth() {
        return mIconWidth;
    }

    public void measureChlid() {
        if (mNav != null) {
            mNav.measureChlid();
        }
    }
}
