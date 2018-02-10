package com.hmb.manager.rubbishclean;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;
import hb.app.HbActivity;
import tmsdk.fg.creator.ManagerCreatorF;
import tmsdk.fg.module.cleanV2.CleanManager;
import tmsdk.fg.module.cleanV2.ICleanTaskCallBack;
import tmsdk.fg.module.cleanV2.IScanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdk.fg.module.cleanV2.RubbishHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hmb.manager.Constant;
import com.hmb.manager.HMBManagerApplication;
import com.hmb.manager.R;
import com.hmb.manager.adapter.RubblishExpandableListAdapter;
import com.hmb.manager.bean.APKFile;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.bean.RubblishInfo;
import com.hmb.manager.rubbishclean.BackGroundAppActivity.KillAppThread;
import com.hmb.manager.rubbishclean.CleanSpeedActivity.ScanTaskCallBack;
import com.hmb.manager.utils.ApkSearchUtils;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;
import com.hmb.manager.utils.TransUtils;
import hb.app.dialog.AlertDialog;

public class RubbishCleanActivity extends HbActivity {
	private ExpandableListView expandableListView = null;
	private RubblishExpandableListAdapter adapter;
	private Context mContext = null;
	private CleanManager mCleanV2Manager;
	private ArrayList<RubblishInfo> resultInfoList;
	private List<RubblishInfo> mSystemRubblishList, mAPKRubblishList, mUnApkRubblishList, mUnInstallRubList;
	private List<APKFile> mSdkAPKs;
	private List<APKFile> aPKs;
	private Button rubblishCleanBtn = null;
	private static final String TAG = "RubbishCleanActivity";
	private RubbishHolder mCurrentRubbish;
	private ArrayList<RubblishInfo> rubblishResultInfoList = new ArrayList<RubblishInfo>();
	private LinearLayout rb_listLayout;
	private LinearLayout rb_main_null;
	private Dialog dialog = null;
	private static final int MSG_RUBBLISH_CLEAN_START = 0x01;
	private static final int MSG_RUBBLISH_CLEAN_END = 0x02;
	private static final int MSG_SDSCANNER_END = 0x03;
	private static final int MSG_SDSCANNER_START = 0x04;
	private static final int MSG_PROGRESS_HIDE = 0x05;
	private HMBManagerApplication application = null;
	private long mRubblishTotalSize = 0;
	private LinearLayout progressLin, bv;
	Map<String, APKFile> sdFiles = null;
	private List<String> rubblishListSize = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_rubblish_clean);
		mContext = this.getApplicationContext();
		mCleanV2Manager = ManagerCreatorF.getManager(CleanManager.class);
		rubblishCleanBtn = (Button) findViewById(R.id.rubblishCleanBtn);
		rb_listLayout = (LinearLayout) findViewById(R.id.rb_listLayout);
		rb_main_null = (LinearLayout) findViewById(R.id.rb_main_null);
		progressLin = (LinearLayout) findViewById(R.id.progressLin);
		bv = (LinearLayout) findViewById(R.id.bv);
		application = (HMBManagerApplication) getApplication();
		new Thread(new ScandCardAPKThread()).start();
		initRubbishHolder();
		setExpandListView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		setupActionModeWithDecor(getToolbar());
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (null != mCleanV2Manager) {
			mCleanV2Manager.onDestroy();
		}
		mCleanV2Manager = null;
	}

	private void initRubbishHolder() {
		mCleanV2Manager.scanDisk(mScanTaskCallBack, null);
	}
	
	class ScandCardAPKThread implements Runnable {

		@Override
		public void run() {
			sCandUnInstallAPK();
		}
	}

	private void setExpandListView() {

		expandableListView = (ExpandableListView) findViewById(R.id.rubblishList);
		expandableListView.setGroupIndicator(null);

		adapter = new RubblishExpandableListAdapter(mContext);
		adapter.setRubblishCleanBtn(rubblishCleanBtn);
		expandableListView.setAdapter(adapter);
		expandableListView.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				((BaseExpandableListAdapter) adapter).notifyDataSetChanged();
				return false;
			}
		});

		expandableListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
					long id) {
				adapter.setChild_groupId(groupPosition);
				adapter.setChild_childId(childPosition);
				((BaseExpandableListAdapter) adapter).notifyDataSetChanged();
				return false;
			}
		});

		rubblishCleanBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				dialog = new AlertDialog.Builder(RubbishCleanActivity.this).setTitle(R.string.app_kill_notification)
						.setMessage(R.string.rubblish_clean_message)
						.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (null != mCurrentRubbish) {
									rubblishSelectToClean();
									if (mCleanV2Manager.cleanRubbish(mCurrentRubbish, mCleanCallback)) {
										rubblishCleanBtn.setEnabled(false);
									}
									application.setRubblishCleanStatus(1);
								}
							}
						}).create();
				dialog.show();

			}
		});

	}

	private void rubblishSelectToClean() {
		boolean[][] child_checkbox;
		Map<String, RubbishEntity> rubbishes;
		List<RubbishEntity> rubblishList = null;
		String mDescription = null;
		String mPackageName = null;
		child_checkbox = adapter.getChild_checkbox();
		for (int i = 0; i < 4; ++i) {
			if (child_checkbox[i] != null && child_checkbox[i].length > 0) {
				if (i == 0) {
					rubbishes = mCurrentRubbish.getmSystemRubbishes();
					if (rubbishes != null) {
						for (int j = 0; j < child_checkbox[i].length; ++j) {
							if (child_checkbox[i][j]) {
								if (mSystemRubblishList.get(j).getaRubbish() != null) {
									mSystemRubblishList.get(j).getaRubbish()
											.setStatus(RubbishEntity.MODEL_TYPE_SELECTED);
								}
							}
						}
					}
				} else if (i == 1) {
					rubbishes = mCurrentRubbish.getmUnInstallRubbishes();
					if (rubbishes != null) {
						for (int j = 0; j < child_checkbox[i].length; ++j) {
							if (child_checkbox[i][j]) {
								if (mUnInstallRubList.get(j).getaRubbish() != null) {
									mUnInstallRubList.get(j).getaRubbish().setStatus(RubbishEntity.MODEL_TYPE_SELECTED);
								}
							}
						}
					}
				} else if (i == 2) {
					for (int j = 0; j < child_checkbox[i].length; ++j) {
						rubblishList = mCurrentRubbish.getmApkRubbishes();
						if (rubblishList != null) {
							if (child_checkbox[i][j]) {
								if (mAPKRubblishList.get(j).getaRubbish() != null) {
									mAPKRubblishList.get(j).getaRubbish().setStatus(RubbishEntity.MODEL_TYPE_SELECTED);
								}
							}
						}
					}
				} else if (i == 3) {
					for (int j = 0; j < child_checkbox[i].length; ++j) {
						rubblishList = mCurrentRubbish.getmApkRubbishes();
						if (rubblishList != null) {
							if (child_checkbox[i][j]) {
								if (mUnApkRubblishList.get(j).getaRubbish() != null) {
									mUnApkRubblishList.get(j).getaRubbish()
											.setStatus(RubbishEntity.MODEL_TYPE_SELECTED);
								}
							}
						}
					}
				}
			}
		}
	}

	private void adapterArrayInit(List<RubblishInfo> list) {
		RubblishInfo info = null;
		AppInfo appInfo = null;
		if (list != null && list.size() > 0) {
			info = list.get(0);
			int size = list.size();
			adapter.getChild_text_array()[info.getmLine()] = new String[size];
			adapter.getChild_text_size()[info.getmLine()] = new String[size];
			adapter.getChild_checkbox()[info.getmLine()] = new boolean[size];
			adapter.getChild_icon()[info.getmLine()] = new Drawable[size];
			int mLine;
			for (int j = 0; j < list.size(); ++j) {
				info = list.get(j);
				mLine = info.getmLine();
				adapter.getChild_text_size()[mLine][j] = TransUtils.transformShortType(info.getmSize(), true);
				adapter.getChild_checkbox()[info.getmLine()][j] = (info.getIsSuggest() == 1 ? true : false);
				if (mLine == 2) {
					adapter.getChild_text_array()[2][j] = info.getmAppName();
					adapter.getChild_checkbox()[2][j] = true;
					appInfo = ManagerUtils.getAppInfoByPackageName(mContext, info.getmPackageName());
					if (appInfo != null && appInfo.getAppIcon() != null) {
						adapter.getChild_icon()[2][j] = appInfo.getAppIcon();
					}
				} else if (mLine == 3) {
					adapter.getChild_text_array()[3][j] = info.getmAppName();
					adapter.getChild_checkbox()[3][j] = true;
					while(sdFiles==null){
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(sdFiles!=null&&sdFiles.size()>0){
					APKFile apkFile = sdFiles.get(info.getmPackageName());
					if (apkFile != null) {
						adapter.getChild_icon()[3][j] = apkFile.getIcon();
					}
					}
				} else {
					adapter.getChild_text_array()[mLine][j] = info.getmDescription();
				}
			}
		}
	}

	private void parseResultList(List<RubblishInfo> resultInfoList) {
		mSystemRubblishList = new ArrayList<RubblishInfo>();
		mAPKRubblishList = new ArrayList<RubblishInfo>();
		mUnApkRubblishList = new ArrayList<RubblishInfo>();
		mUnInstallRubList = new ArrayList<RubblishInfo>();
		int index;
		if (resultInfoList != null && resultInfoList.size() > 0) {
			for (RubblishInfo info : resultInfoList) {
				if(info.getmSize()>0){
				index = info.getmLine();
				switch (index) {
				case 0:
					mSystemRubblishList.add(info);
					break;
				case 1:
					mUnInstallRubList.add(info);
					break;
				case 2:
					// if (info.getmStatus() == 1) {
					String des = info.getmDescription();
					if (des != null) {
						if (des.equals(Constant.APK_INSTALLED) || des.equals(Constant.APK_OLDVERSION)) {
							mAPKRubblishList.add(info);
						} else if (des.equals(Constant.APK_UNINSTALLED)||des.equals(Constant.APK_NEWVERSION)) {
							info.setmLine(3);
							mUnApkRubblishList.add(info);
						}
					}
					// }
					break;
				}
				}
			}
		}
		adapterArrayInit(mSystemRubblishList);
		adapterArrayInit(mUnInstallRubList);
		adapterArrayInit(mAPKRubblishList);
		adapterArrayInit(mUnApkRubblishList);
		// initUnInstallAPK();
		((BaseExpandableListAdapter) adapter).notifyDataSetChanged();
		Message msg = mUIHandler.obtainMessage(MSG_PROGRESS_HIDE);
		msg.sendToTarget();
	}

	private void sCandUnInstallAPK() {
		ApkSearchUtils searchUtils = new ApkSearchUtils(mContext);
		sdFiles = new HashMap<String, APKFile>();
		List<String> pathsList = ManagerUtils.getStoragePathList(mContext);
		if (pathsList != null && pathsList.size() > 0) {
			for (String path : pathsList) {
				File file = new File(path);
				searchUtils.findAllAPKFile(file);
				sdFiles.putAll(searchUtils.getSdFiles());
			}
		}
		// Log.d(TAG, "--------------"+sdFiles.values().size());
		// mSdkAPKs = new ArrayList<APKFile>(sdFiles.values());
		// APKFile apk = null;
		// aPKs = new ArrayList<APKFile>();
		// for (APKFile a : mSdkAPKs) {
		// Log.d(TAG, a.getmInstalled()+"--------------"+a.getmPackageName());
		// if (a.getmInstalled() == ApkSearchUtils.UNINSTALLED) {
		// aPKs.add(a);
		// }
		// }
		// sdcardAPKInit(aPKs);
	};

	private void sdcardAPKInit(List<APKFile> mSdkAPKs) {
		APKFile apkFile = null;
		if (mSdkAPKs != null && mSdkAPKs.size() > 0) {
			int size = mSdkAPKs.size();
			adapter.getChild_text_array()[3] = new String[size];
			adapter.getChild_text_size()[3] = new String[size];
			adapter.getChild_checkbox()[3] = new boolean[size];
			adapter.getChild_icon()[3] = new Drawable[size];
			int mLine;
			for (int j = 0; j < mSdkAPKs.size(); ++j) {
				apkFile = mSdkAPKs.get(j);
				adapter.getChild_text_size()[3][j] = TransUtils.transformShortType(apkFile.getmSize(), true);
				adapter.getChild_checkbox()[3][j] = true;
				adapter.getChild_text_array()[3][j] = apkFile.getAppLabel();
				adapter.getChild_icon()[3][j] = apkFile.getIcon();
			}
		}

	}

	CleanCallback mCleanCallback = new CleanCallback();

	class CleanCallback implements ICleanTaskCallBack {

		@Override
		public void onCleanStarted() {
			Log.i(TAG, "onCleanStarted : ");
		}

		@Override
		public void onCleanProcessChange(int nowPercent, String aCleanPath) {
			Log.i(TAG, "onCleanProcessChange : " + nowPercent + "% ::" + aCleanPath);
		}

		@Override
		public void onCleanCanceled() {
			Message msg = mUIHandler.obtainMessage(MSG_RUBBLISH_CLEAN_END);
			msg.sendToTarget();
			Log.i(TAG, "onCleanCanceled : ");
		}

		@Override
		public void onCleanFinished() {
			Message msg = mUIHandler.obtainMessage(MSG_RUBBLISH_CLEAN_END);
			msg.sendToTarget();
			Log.i(TAG, "onCleanFinish : ");
		}

		@Override
		public void onCleanError(int error) {
			Message msg = mUIHandler.obtainMessage(MSG_RUBBLISH_CLEAN_END);
			msg.sendToTarget();
			Log.i(TAG, "onCleanError : ");
		}

	}

	ScanTaskCallBack mScanTaskCallBack = new ScanTaskCallBack();

	class ScanTaskCallBack implements IScanTaskCallBack {

		@Override
		public void onScanStarted() {
			Log.i(TAG, "onScanStarted");
			Message msg = mUIHandler.obtainMessage(MSG_SDSCANNER_START);
			msg.sendToTarget();
		}

		public void onRubbishFound(RubbishEntity aRubbish) {
			// Log.i(TAG, "onRubbishFound");
		}

		@Override
		public void onScanCanceled(RubbishHolder aRubbishHolder) {
			Message msg = mUIHandler.obtainMessage(MSG_SDSCANNER_END);
			msg.sendToTarget();
		}

		@Override
		public void onScanFinished(RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanFinished : ");
			mCurrentRubbish = aRubbishHolder;
			rubblishResultInfoList.clear();
			List<RubbishEntity> rubblishList = null;
			int mSize = 0;
			if (null != aRubbishHolder) {
				if (null != aRubbishHolder.getmApkRubbishes()) {
					rubblishList = aRubbishHolder.getmApkRubbishes();
					handlerRubbishes(rubblishList, 2);
					collectRubblish(rubblishList);
				}
				if (null != aRubbishHolder.getmSystemRubbishes()) {
					rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmSystemRubbishes().values());
					handlerRubbishes(rubblishList, 0);
					collectRubblish(rubblishList);
				}
				if (null != aRubbishHolder.getmInstallRubbishes()) {
					// sbtips.append("——————————————————————【软件缓存】——————————————————————\n
					// ");
					// mSize =
					// aRubbishHolder.getmInstallRubbishes().entrySet().size();
					// adapterArrayInit(4, mSize);
					//
					rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmInstallRubbishes().values());
					for (RubbishEntity entity : rubblishList) {
						Log.i(TAG, entity.getSize() + "install rubbish" + entity.getDescription() + " size "
								+ entity.getAppName());
					}
					// handlerRubbishes(rubblishList, 4);
					Log.i(TAG, "install rubbish size " + aRubbishHolder.getmInstallRubbishes().size());
				}
				if (null != aRubbishHolder.getmUnInstallRubbishes()) {
					rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmUnInstallRubbishes().values());
					handlerRubbishes(rubblishList, 1);
					collectRubblish(rubblishList);
				}
				// mRubblishTotalSize = aRubbishHolder.getAllRubbishFileSize();
				mRubblishTotalSize = addTotalRubblish();
			}
			Message msg = mUIHandler.obtainMessage(MSG_SDSCANNER_END);
			msg.sendToTarget();
		}

		@Override
		public void onScanError(int error, RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanError : " + error);
		}

		@Override
		public void onDirectoryChange(String dirPath, int fileCnt) {

		}

	}

	private void collectRubblish(List<RubbishEntity> rubblishList) {
		for (RubbishEntity entity : rubblishList) {
			rubblishListSize.add(TransUtils.transformShortType(entity.getSize(), true));
		}

	}

	private long addTotalRubblish() {
		long mTotalSize = 0;
		if (rubblishListSize != null && rubblishListSize.size() > 0) {
			for (String str : rubblishListSize) {
				mTotalSize = mTotalSize + TransUtils.unTransformShortType(str);
			}
		}
		return mTotalSize;
	}

	private void handlerRubbishes(List<RubbishEntity> rubblishList, int i) {
		RubbishEntity aRubbish = null;
		AppInfo info = null;
		RubblishInfo rubbInfo = null;
		String des = null;
		for (int j = 0; j < rubblishList.size(); ++j) {
			rubbInfo = new RubblishInfo();
			rubbInfo.setmLine(i);
			aRubbish = rubblishList.get(j);
			if (aRubbish != null) {
				if (i == 2) {
					rubbInfo.setmAppName(aRubbish.getAppName());
					rubbInfo.setmPackageName(aRubbish.getPackageName());
					Log.d(TAG, aRubbish.getAppName() + "----apk--" + aRubbish.getDescription() + "-------"
							+ aRubbish.getPackageName());
				}
				des = aRubbish.getDescription();
				if (des != null) {
					rubbInfo.setmDescription(aRubbish.getDescription());
				} else {
					rubbInfo.setmDescription(mContext.getString(R.string.rubblish_unknow));
				}

				// Log.d(TAG, aRubbish.getStatus() + "handlerRubbishes " + i +
				// "rubbish description "
				// + aRubbish.getDescription());
				rubbInfo.setmSize(aRubbish.getSize());
				int suggest = aRubbish.isSuggest() ? 1 : 0;
				rubbInfo.setIsSuggest(suggest);
				rubbInfo.setaRubbish(aRubbish);
			}
			rubbInfo.setmStatus(aRubbish.getRubbishType());
			rubblishResultInfoList.add(rubbInfo);
		}
	}

	private Handler mUIHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SDSCANNER_END:
				resultInfoList = rubblishResultInfoList;
				Log.d(TAG, TransUtils.transformShortType(mRubblishTotalSize, true) + "mRubblishTotalSize size "
						+ mRubblishTotalSize);
				SPUtils.instance(mContext).setLongValue(Constant.RUBBLISH_CLEANUP_SIZE, -1, mRubblishTotalSize);
				application.setRubblishCleanStatus(2);
				if (resultInfoList.size() == 0) {
					progressLin.setVisibility(View.GONE);
					rubblishCleanBtn.setEnabled(false);
					rubblishCleanBtn.setText(R.string.clean_btn_message);
					rubblishCleanBtn.setVisibility(View.GONE);
					rb_listLayout.setVisibility(View.GONE);
					rb_main_null.setVisibility(View.VISIBLE);
				} else {
					parseResultList(resultInfoList);
				}
				break;
			case MSG_PROGRESS_HIDE:
				progressLin.setVisibility(View.GONE);
				rb_listLayout.setVisibility(View.VISIBLE);
				bv.setVisibility(View.VISIBLE);
				break;
			case MSG_SDSCANNER_START:
				progressLin.setVisibility(View.VISIBLE);
				rb_listLayout.setVisibility(View.GONE);
				bv.setVisibility(View.GONE);
				break;
			// case MSG_REFRESH_HEAD_PROGRESS:
			// mScanResultStateView.setText("pathtips::"+mPathTips+"\n"+mRubbishTips);
			// mProgressbar.setProgress(mPercentTip);
			// break;
			// case MSG_REFRESH_SPACE_DETAIL:
			// break;
			case MSG_RUBBLISH_CLEAN_END:
				rubblishCleanBtn.setEnabled(false);
				Toast.makeText(mContext, mContext.getString(R.string.rubblish_clean_result,
						TransUtils.transformShortType(adapter.getCleanSize(), true)), Toast.LENGTH_LONG).show();
				SPUtils.instance(mContext).setLongValue(Constant.RUBBLISH_CLEANUP_SIZE, -1, adapter.getCleanSize());
				Log.d(TAG, TransUtils.transformShortType(adapter.getCleanSize(), true) + "mCleanSize size "
						+ adapter.getCleanSize());
				refreshList();
				SPUtils.instance(mContext).setLongValue(Constant.RUBBLISH_CLEANUP_TIME, -1, System.currentTimeMillis());
				break;
			}
		}

	};

	@Override
	public void onBackPressed() {

		super.onBackPressed();
	}

	@Override
	public void onNavigationClicked(View view) {

		onBackPressed();
	}

	private void refreshList() {
		String[][] new_child_text_array = new String[4][];
		String[][] new_child_text_size = new String[4][];
		boolean[][] new_child_checkbox = new boolean[4][];
		Drawable[][] new_child_icon = new Drawable[4][];
		int size = 0;
		int index = 0;
		String[][] child_text_array = adapter.getChild_text_array();

		String[][] child_text_size = adapter.getChild_text_size();

		boolean[][] child_checkbox = adapter.getChild_checkbox();

		Drawable[][] child_icon = adapter.getChild_icon();

		for (int i = 0; i < 4; ++i) {
			if (child_checkbox[i] != null && child_checkbox[i].length > 0) {
				size = getIntLength(child_checkbox[i]);
				new_child_text_array[i] = new String[size];
				new_child_text_size[i] = new String[size];
				new_child_checkbox[i] = new boolean[size];
				new_child_icon[i] = new Drawable[size];

				for (int j = 0; j < child_checkbox[i].length; ++j) {
					if (!child_checkbox[i][j] && index < size) {
						new_child_text_array[i][index] = child_text_array[i][j];
						new_child_text_size[i][index] = child_text_size[i][j];
						new_child_checkbox[i][index] = child_checkbox[i][j];
						if (i == 2 || i == 3) {
							new_child_icon[i][index] = child_icon[i][j];
						}
						++index;
					}

				}
				index = 0;
			}
		}
		adapter.setGroup_checked(new boolean[] { false, false, false, false });
		adapter.setChild_text_array(new_child_text_array);
		adapter.setChild_text_size(new_child_text_size);
		adapter.setChild_checkbox(new_child_checkbox);
		adapter.setChild_icon(new_child_icon);

		((BaseExpandableListAdapter) adapter).notifyDataSetChanged();

	}

	private int getIntLength(boolean[] child_checkbox) {
		int size = 0;
		if (child_checkbox != null && child_checkbox.length > 0) {
			for (int i = 0; i < child_checkbox.length; ++i) {
				if (!child_checkbox[i]) {
					++size;
				}
			}
		}
		return size;
	}

}
