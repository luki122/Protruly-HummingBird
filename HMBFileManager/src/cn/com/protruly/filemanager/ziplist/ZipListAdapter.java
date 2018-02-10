package cn.com.protruly.filemanager.ziplist;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-5-17.
 */

public class ZipListAdapter extends BaseAdapter {

    private Context mContext;
    private ZipPath mRootPath;
    private ZipPath mCurrentPath;
    private List<ZipPath> mChildrenUnderCurrent;
    public static final String TAG = "ZipListAdapter";

    private ExecutorService mExecutorService;

    public ZipListAdapter(Context context) {
        super();
        mContext = context;
        mRootPath = ZipPath.createRoot();
    }

    public void setCurrentZipPath(ZipPath zipPath) {
        mCurrentPath = zipPath;
        mChildrenUnderCurrent = zipPath.getSortedChildren();

        //TODO: delete log
        for(ZipPath tmp : mChildrenUnderCurrent) {
            LogUtil.i(TAG, "setCurrentZipPath-->  children under current zip path:" + tmp.toString());
        }
    }

    public ZipPath getRootPath() {
        return mRootPath;
    }

    public ZipPath getCurrentPath() {
        return mCurrentPath;
    }

    @Override
    public int getCount() {
        if(null == mChildrenUnderCurrent) return 0;
        return mChildrenUnderCurrent.size();
    }

    @Override
    public Object getItem(int position) {
        if(null == mChildrenUnderCurrent) return null;
        return mChildrenUnderCurrent.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if(null == convertView) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.zip_list_item_slider_view, null);
            viewHolder = new ViewHolder();
            viewHolder.iconImageView = (ImageView)convertView.findViewById(android.R.id.icon);
            viewHolder.fileNameTextView = (TextView)convertView.findViewById(R.id.file_name);
            viewHolder.dateTextView = (TextView)convertView.findViewById(R.id.file_date);
            viewHolder.sizeTextView = (TextView)convertView.findViewById(R.id.file_size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        ZipPath zipPath = (ZipPath) getItem(position);
        int iconResId = R.drawable.ic_file_icon_folder;
        if(!zipPath.isDirectory()) {
            iconResId = Util.getDefaultIconRes(zipPath.getName());
        }
        viewHolder.iconImageView.setImageResource(iconResId);
        viewHolder.fileNameTextView.setText(zipPath.getName());
        viewHolder.dateTextView.setText(Util.formatDateStringThird(zipPath.getTime()));
        viewHolder.sizeTextView.setText(Util.getFileSizeAndUnit(zipPath.getSize(), 2));
        viewHolder.zipPath = zipPath;
        loadZipPathInfo(viewHolder, zipPath);
        return convertView;
    }

    private class ViewHolder {
        public ImageView iconImageView;
        public TextView fileNameTextView;
        public TextView dateTextView;
        public TextView sizeTextView;
        public ZipPath zipPath;
    }

    public void loadZipPathInfo(ViewHolder viewHolder, ZipPath zipPath) {
        new LoadZipPathInfoTask(viewHolder, zipPath).execute();
    }

    private class LoadZipPathInfoTask extends AsyncTask<Void, Void, Long> {

        private ViewHolder mViewHolder;
        private ZipPath mZipPath;

        public LoadZipPathInfoTask(ViewHolder viewHolder, ZipPath zipPath) {
            mViewHolder = viewHolder;
            mZipPath = zipPath;
        }

        @Override
        protected Long doInBackground(Void... params) {
            return mZipPath.getAllDescendantsSize();
        }

        @Override
        protected void onPostExecute(Long size) {
            if(null == mViewHolder) return;
            if(mViewHolder.zipPath.equals(mZipPath)) {
                String sizeText = Util.getFileSizeAndUnit(size, 2);
                TextView sizeTextView = mViewHolder.sizeTextView;
                if(null == sizeTextView) return;
                mViewHolder.sizeTextView.setText(sizeText);
            }
        }

    }
}
