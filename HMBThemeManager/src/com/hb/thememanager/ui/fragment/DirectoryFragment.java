package com.hb.thememanager.ui.fragment;

import java.io.File;

import com.hb.thememanager.ui.adapter.DirectoryAdapter;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.hb.thememanager.R;
import com.hb.thememanager.utils.FileUtils;
public class DirectoryFragment extends Fragment {
    public interface FileClickListener {
        void onFileClicked(File clickedFile);
    }

    private static final String ARG_FILE_PATH = "arg_file_path";

    private View mEmptyView;
    private String mPath;
    private ListView mDirectoryView;
    private DirectoryAdapter mDirectoryAdapter;
    private FileClickListener mFileClickListener;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFileClickListener = (FileClickListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFileClickListener = null;
    }

    public static DirectoryFragment getInstance(String path) {
        DirectoryFragment instance = new DirectoryFragment();

        Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, path);
        instance.setArguments(args);

        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_directory, container, false);
        mDirectoryView = (ListView) view.findViewById(R.id.directory_recycler_view);
        mEmptyView = view.findViewById(R.id.directory_empty_view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initArgs();
        initFilesList();
    }

    private void initFilesList() {
    	
        mDirectoryAdapter = new DirectoryAdapter(getActivity(), FileUtils.getFileListByDirPath(mPath));

        mDirectoryAdapter.setOnItemClickListener(new OnItemClickListener() {
           
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				 if (mFileClickListener != null) {
	                    mFileClickListener.onFileClicked(mDirectoryAdapter.getModel(position));
	                }
			}
        });

        mDirectoryView.setAdapter(mDirectoryAdapter);
        mDirectoryView.setEmptyView(mEmptyView);
    }

    private void initArgs() {
        if (getArguments().getString(ARG_FILE_PATH) != null) {
            mPath = getArguments().getString(ARG_FILE_PATH);
        }
    }
}
