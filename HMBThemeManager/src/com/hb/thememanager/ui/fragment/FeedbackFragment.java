package com.hb.thememanager.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hb.thememanager.R;
import com.hb.thememanager.exception.MvpViewNotAttachedException;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.request.FeedbackRequest;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.ui.SimpleRequestPresenter;
import com.hb.thememanager.utils.Config.HttpUrl;

public class FeedbackFragment extends AbsLocalThemeFragment {
	private View mContentView;
	private EditText mFeedbackEditText;
	private Button mCommitBtn;
	private Http mHttp;
	private FeedbackRequest mFbRequest;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContentView = inflater.inflate(R.layout.feedback_layout, container, false);
		initView();
		mFbRequest = new FeedbackRequest(this.getContext());
		mHttp = Http.getHttp(this.getContext());
		return mContentView;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void initView() {
		// TODO Auto-generated method stub
		mFeedbackEditText = (EditText)mContentView.findViewById(R.id.feedback);
		mCommitBtn = (Button) mContentView.findViewById(R.id.feedback_commit);
		mCommitBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (mFeedbackEditText.getText().toString().isEmpty()) {
					Toast.makeText(FeedbackFragment.this.getContext(), R.string.feedback_commit_no_input, Toast.LENGTH_SHORT).show();
					return;
				}
				mFbRequest.setContent(mFeedbackEditText.getText().toString());
				mHttp.post(HttpUrl.FEEDBACK_URL, mFbRequest.createJsonRequest(), new RawResponseHandler() {

					@Override
					public void onFailure(int statusCode, String error_msg) {
						// TODO Auto-generated method stub
						Toast.makeText(FeedbackFragment.this.getContext(), R.string.feedback_commit_fail, Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onSuccess(int statusCode, String response) {
						// TODO Auto-generated method stub
						Toast.makeText(FeedbackFragment.this.getContext(), R.string.feedback_commit_succ, Toast.LENGTH_SHORT).show();
						getActivity().onBackPressed();
					}
				});
			}

		});
	}


}
