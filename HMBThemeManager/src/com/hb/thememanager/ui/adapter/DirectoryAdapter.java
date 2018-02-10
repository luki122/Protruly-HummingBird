package com.hb.thememanager.ui.adapter;

import java.io.File;
import java.util.List;

import com.hb.thememanager.utils.FileTypeUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.hb.thememanager.R;
public class DirectoryAdapter extends BaseAdapter {

	private List<File> mFiles;
    private Context mContext;
    private OnItemClickListener mOnItemClickListener;

    public DirectoryAdapter(Context context, List<File> files) {
        mContext = context;
        mFiles = files;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }
    
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mFiles.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mFiles.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		Holder holder;
		if(convertView == null){
			convertView = LayoutInflater.from(mContext) .inflate(R.layout.theme_picker_item_file, null);
			holder = new Holder();
			holder.mFileImage = (ImageView) convertView.findViewById(R.id.item_file_image);
			holder.mFileTite = (TextView) convertView.findViewById(R.id.item_file_title);
			holder. mFileSubtitle = (TextView) convertView.findViewById(R.id.item_file_subtitle);
			convertView.setTag(holder);
		}else{
			holder = (Holder) convertView.getTag();
		}
		File currentFile = mFiles.get(position);
		FileTypeUtils.FileType fileType =  FileTypeUtils.getFileType(currentFile);
			holder.mFileImage.setImageResource(fileType.getIcon());
//			holder.mFileSubtitle.setText(fileType.getDescription());
			holder.mFileTite.setText(currentFile.getName());
			convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	mOnItemClickListener.onItemClick(null, null, position, 0);
                }
            });
		return convertView;
	}
	
	
	
	class Holder {
		ImageView mFileImage;
		TextView mFileTite;
		TextView mFileSubtitle;
	}

	   public File getModel(int index) {
	        return mFiles.get(index);
	    }
}
