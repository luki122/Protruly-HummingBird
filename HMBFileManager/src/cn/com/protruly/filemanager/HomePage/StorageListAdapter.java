package cn.com.protruly.filemanager.HomePage;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.com.protruly.filemanager.StorageVolumeManager;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.R;

/**
 * Created by liushitao on 17-4-18.
 */

public class StorageListAdapter extends BaseAdapter{
    private List<FileInfo> list;
    private ListView mListView;
    private Context mContext;
    private int i=0;

    public StorageListAdapter(Context context, ArrayList<String> storageList) {
        mContext = context;
        if(storageList!=null){
            list = new ArrayList<>(storageList.size());
        }
        constructFileList(storageList);
    }

    public StorageListAdapter(Context context, ArrayList<String> storageList, ListView listView) {
        mContext = context;
        mListView = listView;
        if(storageList!=null){
            list = new ArrayList<>(storageList.size());
        }
        constructFileList(storageList);
    }

    private void constructFileList(List<String> storagePathList) {
        for(String path : storagePathList) {
            list.add(new FileInfo(path));
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView==null){
            convertView = View.inflate(mContext,R.layout.storage_list_item,null);
            holder = new ViewHolder(this,convertView);
            convertView.setTag(holder);
        } else{
            holder = (ViewHolder) convertView.getTag();
        }
        if(position<=list.size()-1){
            showStorageInfo(holder, list.get(position));
        }
        return convertView;
    }

    private void setProgressBarColor(ProgressBar progressBar,int percent){
        if(percent>=90){
            progressBar.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.progressbar_red));
        }else{
            progressBar.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.progressbar_green));
        }
    }

    private void doProgressBarAnimation(final ProgressBar progressBar, final int toProgress) {
        ValueAnimator animator = ValueAnimator.ofInt(0, toProgress);
        animator.setDuration(250);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer progress = (Integer)animation.getAnimatedValue();
                progressBar.setProgress(progress.intValue());
            }
        });
        animator.start();
    }


    private void showStorageInfo(ViewHolder holder,FileInfo fileInfo){
        String storagePath = fileInfo.getPath();
        /*if((TextUtils.equals(Util.getPhoneStoragePath(mContext), storagePath)) || (TextUtils.equals("/storage/emulated/0", storagePath))) {
            showPhoneStorageInfo(holder, fileInfo);
            return;
        }
        if(TextUtils.equals(Util.getOtgStoragePath(mContext), storagePath)) {
            showOTGStorageInfo(holder, fileInfo);
            return;
        }
        if(TextUtils.equals(Util.getSdStoragePath(mContext), storagePath)) {
            showSdStrorageInfo(holder, fileInfo);
            return;
        }*/
        ArrayList<Map> otgMap = StorageVolumeManager.getOTGPathList();
        String sdPath = StorageVolumeManager.getSDPath();
        if((TextUtils.equals(StorageVolumeManager.getPhoneStoragePath(mContext), storagePath)) || (TextUtils.equals("/storage/emulated/0", storagePath))) {
            showPhoneStorageInfo(holder, fileInfo);
            return;
        }
        if(null != sdPath && TextUtils.equals(sdPath, storagePath)) {
            showSdStrorageInfo(holder, fileInfo);
            return;
        }
        if(null != otgMap && otgMap.size()==1 &&
                TextUtils.equals(String.valueOf(otgMap.get(0).get(StorageVolumeManager.PATH)), storagePath)) {
            showOTGStorageInfo(holder, fileInfo);
        }else if(null != otgMap && otgMap.size()>1) {
            showOtgListStorageInfo(holder, fileInfo, otgMap);
        }
        //showOtherStorageInfo(holder, fileInfo);
    }

    private void showOtherStorageInfo(StorageListAdapter.ViewHolder holder, FileInfo info) {
        String storagePath = info.getPath();
        holder.storageName.setText(info.getName());
        if (Util.isStorageMounted(mContext, storagePath)) {
            showStorageCapacity(holder, storagePath);
            return;
        }
        holder.storageCapacity.setText(mContext.getResources().getString(R.string.exception_storage_info));
        setProgressBarColor(holder.progressBar, 0);
    }

    private void showOtgListStorageInfo(StorageListAdapter.ViewHolder holder, FileInfo info,ArrayList<Map> mapArrayList) {
        String storagePath = info.getPath();
        for(int i=0;i<mapArrayList.size();i++){
            info.fileName = String.valueOf(mapArrayList.get(i).get(StorageVolumeManager.PATH));
            if(TextUtils.equals(info.fileName, info.getPath())) {
                String lable = String.valueOf(mapArrayList.get(i).get(StorageVolumeManager.LABEL));
                holder.storageName.setText(lable);
                if (Util.isStorageMounted(mContext, storagePath)) {
                    showStorageCapacity(holder, storagePath);
                    return;
                }
                holder.storageCapacity.setText(mContext.getResources().getString(R.string.exception_storage_info));
                setProgressBarColor(holder.progressBar, 0);
                break;
            }
        }
    }

    private void showPhoneStorageInfo(StorageListAdapter.ViewHolder holder, FileInfo info) {
        info.fileName = mContext.getString(R.string.phone_storage);
        holder.storageName.setText(info.fileName);
        Long freeCapacity = Util.getFreeSizeWithFile(info.getPath());
        Map<String, Object> freeFilesizeAndUnit = Util.getFileSizeAndUnits(mContext, freeCapacity);
        String freeCapacityStr = Util.formatFreePhoneSizeSecond((Float)freeFilesizeAndUnit.get("file.size")) + freeFilesizeAndUnit.get("file.units");
        String availStr = mContext.getString(R.string.available_capacity, new Object[] {freeCapacityStr});
        Long totalCapacity = Util.getPhoneRomSpace(mContext);
        Map<String, Object> totalFilesizeAndUnit = Util.getTotalFileSizeAndUnits(mContext, totalCapacity);
        String totalCapacityStr = Util.formatFileSizeSecond(Double.valueOf(totalFilesizeAndUnit.get("file.size").toString())) + totalFilesizeAndUnit.get("file.units");
        String totalStr = mContext.getString(R.string.total_capacity, new Object[] {totalCapacityStr});
        if(Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US")){
            availStr = freeCapacityStr;
            totalStr = totalCapacityStr;
        }
        holder.storageCapacity.setText(availStr + " / " + totalStr);
        double percent = (double)freeCapacity/(double)totalCapacity;
        int usedStorage = 100-(int)(percent*100);
        setProgressBarColor(holder.progressBar, usedStorage);
        doProgressBarAnimation(holder.progressBar, usedStorage);
        holder.storageName.setEnabled(false);
        holder.storageCapacity.setEnabled(false);
        holder.progressBar.setEnabled(false);
    }

    private void showSdStrorageInfo(StorageListAdapter.ViewHolder holder, FileInfo info) {
        String storagePath = info.getPath();
        info.fileName = mContext.getString(R.string.sd_storage);
        holder.storageName.setText(info.fileName);
        if(Util.isStorageMounted(mContext, storagePath)) {
            showStorageCapacity(holder, storagePath);
            return;
        }
        holder.storageCapacity.setText(mContext.getResources().getString(R.string.exception_storage_info));
        setProgressBarColor(holder.progressBar, 0);
    }

    private void showStorageCapacity(final ViewHolder viewHolder, final String paramString) {

        Long freeCapacity = Util.getFreeSizeWithFile(paramString);
        Map localMap1 = Util.getFileSizeAndUnits(StorageListAdapter.this.mContext, freeCapacity);
        String freeCapacityStr = Util.formatFreePhoneSizeSecond(((Float)localMap1.get("file.size"))) + localMap1.get("file.units");
        String availStr = StorageListAdapter.this.mContext.getString(R.string.available_capacity, new Object[] { freeCapacityStr });
        Long totalCapacity = Util.getTotalSize(paramString);
        Map<String, Object> totalFilesizeAndUnit = Util.getFileSizeAndUnits(StorageListAdapter.this.mContext, totalCapacity);
        String totalCapacityStr = Util.formatFreePhoneSizeSecond(Float.valueOf(totalFilesizeAndUnit.get("file.size").toString())) + totalFilesizeAndUnit.get("file.units");
        String totalStr = StorageListAdapter.this.mContext.getString(R.string.total_capacity, new Object[] { totalCapacityStr });
        if(Locale.getDefault().toString().equals("en_GB")||Locale.getDefault().toString().equals("en_US")){
            availStr = freeCapacityStr;
            totalStr = totalCapacityStr;
        }
        viewHolder.storageCapacity.setText( availStr + " / " + totalStr);
        double percent = (double)freeCapacity/(double)totalCapacity;
        int usedStorage = 100-(int)(percent*100);
        setProgressBarColor(viewHolder.progressBar, usedStorage);
        doProgressBarAnimation(viewHolder.progressBar, usedStorage);
        //showToastOnUI(paramViewHolder, str2 + " / " + str4);
        /*new Thread(new Runnable()
        {
            public void run()
            {
                Long localLong1 = Long.valueOf(Util.getFreeSize(paramString));
                Map localMap1 = Util.getFileSizeAndUnits(StorageListAdapter.this.mContext, localLong1.longValue());
                String str1 = Util.formatFreePhoneSize(((Float)localMap1.get("file.size")).floatValue()) + localMap1.get("file.units");
                String str2 = StorageListAdapter.this.mContext.getString(R.string.available_capacity, new Object[] { str1 });
                Long localLong2 = Long.valueOf(Util.getTotalSize(paramString));
                Map localMap2 = Util.getFileSizeAndUnits(StorageListAdapter.this.mContext, localLong2.longValue());
                String str3 = Util.formatFreePhoneSize(((Float)localMap2.get("file.size")).floatValue()) + localMap2.get("file.units");
                String str4 = StorageListAdapter.this.mContext.getString(R.string.total_capacity, new Object[] { str3 });
                showToastOnUI(paramViewHolder, str2 + " / " + str4);
            }
        }).start();*/
    }

    private void showToastOnUI(final ViewHolder paramViewHolder, final String paramString)
    {
        ((Activity)this.mContext).runOnUiThread(new Runnable()
        {
            public void run()
            {
                paramViewHolder.storageCapacity.setText(paramString);
            }
        });
    }

    private void showOTGStorageInfo(StorageListAdapter.ViewHolder holder, FileInfo info) {
        String storagePath = info.getPath();
        info.fileName = mContext.getString(R.string.otg_storage);
        holder.storageName.setText(info.fileName);
        if(Util.isStorageMounted(mContext, storagePath)) {
            showStorageCapacity(holder, storagePath);
            return;
        }
        holder.storageCapacity.setText(mContext.getResources().getString(R.string.exception_storage_info));
        setProgressBarColor(holder.progressBar, 0);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        if(position<=list.size()-1){
            return list.get(position);
        }
        return null;
    }

    public View getItemView(int position){
        return getView(position,null,mListView);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    class ViewHolder{
        public TextView storageName;
        public TextView storageCapacity;
        public ProgressBar progressBar;

        public ViewHolder(StorageListAdapter storageListAdapter,View convertView){
            storageName = (TextView) convertView.findViewById(R.id.storage_name);
            storageCapacity = (TextView) convertView.findViewById(R.id.storage_capacity);
            progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
        }


    }
}
