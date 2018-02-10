package com.hb.thememanager.ui.fragment.ringtone;

import java.io.File;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import hb.app.HbActivity;
import hb.app.dialog.ProgressDialog;

import com.hb.thememanager.R;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.model.Ringtone;
import com.hb.thememanager.model.Theme;
import com.mediatek.audioprofile.AudioProfileManager;

/**
 * set to alarm or incall ...... 
 */
public class setTo extends Activity implements OnClickListener,  Downloadinfo{

	private TextView tvIncall;
	private TextView tvSms;
	private TextView tvAlarm;
	private TextView tvCancel;
	
	private LinearLayout mChoose;
	private ProgressDialog mProgressDialog;
	
	private String  mUri ;
	private String  mName ;
	private String  mNo ;
	private Ringtone mRingtone;
	private int clickWhat;
	private RingtoneDownload mDownload;
	
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		Intent it = this.getIntent();
		mUri = (String) it.getExtra("uri");
		mName = (String) it.getExtra("wname");
		mNo = (String) it.getExtra("wno");
		
		setContentView(R.layout.ringtone_setto_layout);
		tvIncall =  (TextView)findViewById(R.id.incallringtone);
		tvIncall.setOnClickListener(this);
		tvSms =  (TextView)findViewById(R.id.smsringtone);
		tvSms.setOnClickListener(this);
		tvAlarm =  (TextView)findViewById(R.id.alarmringtone);
		tvAlarm.setOnClickListener(this);
		tvCancel = (TextView)findViewById(R.id.cancel);
		tvCancel.setOnClickListener(this);
		
		mChoose = (LinearLayout)findViewById(R.id.choose);
		mRingtone = new Ringtone(Theme.RINGTONE,  mNo, mName, mUri);
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		clickWhat = view.getId();
        switch (clickWhat) {
        case R.id.incallringtone :
		case R.id.smsringtone :
		case R.id.alarmringtone :
			mChoose.setVisibility(View.GONE);
			mDownload = new RingtoneDownload(this);
			if (mDownload.hasDownloaded(mRingtone)) {
				onDownloadSuccess();
			} else {
				mDownload.setDownloadinfo(this);
				mDownload.setTheme(mRingtone);
				mDownload.startDownload();

				mProgressDialog  = new ProgressDialog(this);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgressDialog.setCanceledOnTouchOutside(false);
				mProgressDialog.setTitle(R.string.downloadringtone);
				mProgressDialog.show();
			}
			break;
		case R.id.cancel :
			finish();
			break;
		default:
			finish();
			break;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub设置通知铃声成功
		finish();
		return super.onTouchEvent(event);
	}
	
	@Override
	public void onDownloadSuccess() {
		Log.e("huliang", "!!!setTo download success");
		Uri ringtoneUri = queryUriforAudio(mRingtone.loadedPath);
		if (null != mProgressDialog) {
			mProgressDialog.dismiss();
		}
		switch(clickWhat) {
		case R.id.incallringtone :
			RingtoneManager.setActualDefaultRingtoneUri(this, AudioProfileManager.TYPE_RINGTONE, ringtoneUri);
	        Toast.makeText(this, R.string.setincallringtonesucc, Toast.LENGTH_SHORT).show();
			break;
		case R.id.smsringtone :
			RingtoneManager.setActualDefaultRingtoneUri(this, AudioProfileManager.TYPE_NOTIFICATION, ringtoneUri);
			Toast.makeText(this, R.string.setinforingtonesucc, Toast.LENGTH_SHORT).show();
			break;
		case R.id.alarmringtone :
			Intent intent = new Intent();
	        intent.setComponent( new ComponentName("com.android.deskclock",
	                "com.android.deskclock.SetRingtoneActivity"));
	        intent.setData(ringtoneUri);
			this.startActivity(intent);
			break;
		}
		finish();
	}

	@Override
	public void onDownloadFailed(DownloadException e) {
		// TODO Auto-generated method stub
		Toast.makeText(this, R.string.downloadringtonefail, Toast.LENGTH_LONG).show();
	}
	
	/**
	 * 查找在于SDcard中的Audio文件对应于MediaStore  的uri
	 * @param path 音频文件
	 * @return Uri
	 */
	public Uri queryUriforAudio(String path)
	{
		String filepath = path.replace("sdcard", "/storage/emulated/0");
		File file = new File(filepath);
	    final String where = MediaStore.Audio.Media.DATA + "='"+file.getAbsolutePath()+"'";
	    Cursor cursor = null;
	    int id = -1;
	    int i = 0;
	    for ( ; i < 10 ; i++) {
			try {
				cursor = this.getContentResolver().query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
						where, null, null);
				if (cursor == null) {
					// return null;
					Thread.sleep(100);
				}
				id = -1;
				if (cursor != null) {
					cursor.moveToFirst();
					if (!cursor.isAfterLast()) {
						id = cursor.getInt(0);
					}
					cursor.close();
				}
				if (id == -1) {
					Log.d("huliang", "queryUriforAudio: uri为空 2");
					// return null;
					Thread.sleep(100);
				} else {
					break;
				}
			} catch (Exception e) {
				Log.e("huliang", " ", e);
			}
	    } 
	   
	   if (i >= 10) {
		   return null;
	   }
	   
	    return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
	}
}
