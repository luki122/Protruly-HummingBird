package com.hb.interception.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import hb.app.HbActivity;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.ActionMode.Item;
import hb.widget.toolbar.Toolbar;
import hb.widget.HbListView;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Contacts.Entity;
import android.provider.ContactsContract;

import com.hb.interception.R;
import com.hb.interception.adapter.AddBlackByCallLogAdapter;
import com.hb.interception.database.BlackItem;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.ContactUtils;
import com.hb.interception.util.InterceptionUtils;
import com.hb.interception.util.YuloreUtil;

public class AddByNumberBase extends HbActivity implements OnItemClickListener {
	
	private static final String TAG = "AddByNumberBase";
    protected Context mContext;
    
    private static final int QUERY_TOKEN = 1;
    
    
    private QueryHandler mQueryHandler;
    protected AddBlackByCallLogAdapter mAdapter;
    private HbListView mList;
    private View mEmptyView;
    private String mBlackNumbers = null;
    
    public static HashMap<String, Integer> mCheckedItem = new HashMap<String, Integer>();
    protected boolean mIsAdding = false;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHbContentView(R.layout.add_black_from_call_log_frag);
		mEmptyView = findViewById(R.id.calllog_empty);
		mContext = this;
		
		mAdapter = new AddBlackByCallLogAdapter(mContext);
	    mList = (HbListView)findViewById(android.R.id.list);
	    mList.setItemsCanFocus(false);
	    mList.setOnItemClickListener(this);
	    mList.setAdapter(mAdapter);
	    mList.setFastScrollEnabled(false);
	    mList.setFastScrollAlwaysVisible(false);
	    
	    Bundle extras = getIntent().getExtras();
	    if (null != extras) {
	    	mBlackNumbers = extras.getString("blacknumbers");
	    }
		
	    mQueryHandler = new QueryHandler(this);
	    
		initBottomMenuAndActionbar();
		startQuery();
	}

	@Override
    protected void onResume() {
	    super.onResume();
	    
	}
	
	@Override
    protected void onDestroy() {
		mCheckedItem.clear();
		
		super.onDestroy();
		if(mAdapter != null) {
			mAdapter.changeCursor(null);
		}
	}

	private void startQuery() {
		String selection = " number != '' AND number is not null ";
		if (mBlackNumbers != null) {
			selection = "number not in(" + mBlackNumbers + ")" +" AND number != '' AND number is not null ";
		} 
		mQueryHandler.startQuery(QUERY_TOKEN, null, 
				Uri.parse("content://call_log/hbcallsjoindataview"), 
				AddBlackByCallLogAdapter.CALL_LOG_PROJECTION,
				selection, null, "_id DESC");
	}
	
	private final class QueryHandler extends AsyncQueryHandler {
		private final WeakReference<AddByNumberBase> mActivity;

		/**
		 * Simple handler that wraps background calls to catch
		 * {@link SQLiteException}, such as when the disk is full.
		 */
		protected class CatchingWorkerHandler extends
				AsyncQueryHandler.WorkerHandler {
			public CatchingWorkerHandler(Looper looper) {
				super(looper);
			}

			@Override
			public void handleMessage(Message msg) {
				try {
					// Perform same query while catching any exceptions
					super.handleMessage(msg);
				} catch (SQLiteDiskIOException e) {
					Log.w(TAG, "Exception on background worker thread", e);
				} catch (SQLiteFullException e) {
					Log.w(TAG, "Exception on background worker thread", e);
				} catch (SQLiteDatabaseCorruptException e) {
					Log.w(TAG, "Exception on background worker thread", e);
				}
			}
		}

		@Override
		protected Handler createHandler(Looper looper) {
			// Provide our special handler that catches exceptions
			return new CatchingWorkerHandler(looper);
		}

		public QueryHandler(Context context) {
			super(context.getContentResolver());
			mActivity = new WeakReference<AddByNumberBase>(
					(AddByNumberBase) context);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			final AddByNumberBase activity = mActivity.get();
			if (activity != null && !activity.isFinishing()) {
				final AddBlackByCallLogAdapter callsAdapter = activity.mAdapter;
				callsAdapter.clearCheckedItem();
				callsAdapter.changeCursor(cursor);
				
				if (cursor == null) {
					Log.e(TAG, "onQueryCompleted - cursor is null");
					mEmptyView.setVisibility(View.VISIBLE);
					return;
				}
                
				mCheckedItem.clear();
				setBottomMenuEnable(false);
				if (cursor.getCount() == 0) {
					mEmptyView.setVisibility(View.VISIBLE);
					initActionBar(true);
				} else {
				    if(!isInDeleteMode()) {
				        initActionBar(true);
				    }
					mEmptyView.setVisibility(View.GONE);			
				}
				
                Log.i(TAG, "onQueryCompleted - Count:" + cursor.getCount());
			} else {
				if(cursor != null) {
					cursor.close();
				}
			}
		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (mAdapter == null) {
			mEmptyView.setVisibility(View.VISIBLE);
			return;
		}
		
		final CheckBox checkBox = (CheckBox) view.findViewById(R.id.list_item_check_box);
		if (null != checkBox) {
            boolean checked = checkBox.isChecked();
            if (!checked &&mCheckedItem.size() >= MAX_ADD_NUM) {
            	Toast.makeText(mContext, mContext.getString(R.string.max_add_to_blacklist), Toast.LENGTH_SHORT).show();
            	return;
            }
            checkBox.setChecked(!checked);
            String name = mAdapter.getName(position);
            String number = mAdapter.getNumber(position);
            if (number == null) {
            	return;
            }
            Log.i(TAG, "checked="+checked);
            if (!checked) {
            	mCheckedItem.put(number, position);
            	mAdapter.setCheckedItem(number);
            } else {
            	mCheckedItem.remove(number);
            	mAdapter.removeCheckedItem(number);
            }
            
            updateActionMode();
        }
	}
	
	private class MyProgressDialog extends ProgressDialog {
        public MyProgressDialog(Context context) {
            super(context);
        }
        
        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (mIsAdding) {
                    return true;
                }
                break;
            }
            }
            
            return super.onKeyDown(keyCode, event);
        }
    };
    
    private MyProgressDialog mSaveProgressDialog = null;
    protected static final int START = 0;
    protected static final int END = 1;
    protected final Handler mHandler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            
            switch(msg.what) {
            case START: {
            	mIsAdding = true;
            	
                if (!isFinishing()) {
                    if (null == mSaveProgressDialog) {
                        mSaveProgressDialog = new MyProgressDialog(mContext);
                    }
                    mSaveProgressDialog.setTitle(R.string.save_title);
                    mSaveProgressDialog.setIndeterminate(false);
                    mSaveProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    try {
                        mSaveProgressDialog.show();
                    } catch (Exception e) {
                        
                    }
                }
                break;
            }
            
            case END: {
            	mIsAdding = false;
                if (!isFinishing() 
                        && null != mSaveProgressDialog && mSaveProgressDialog.isShowing()) {
                    try {
                        mSaveProgressDialog.dismiss();
                        mSaveProgressDialog = null;
                        finish();
                    } catch (Exception e) {
                        
                    }
                }
                break;
            }
            }
            
            super.handleMessage(msg);
        }
    
    };
    
    private void addContacts() {
    	int selectedCount = mCheckedItem.size();
        if (0 >= selectedCount) {
            return;
        }
        
        Set<String> numbers = mCheckedItem.keySet();
        ArrayList<String> numbersForBlack = new ArrayList<String>();
        ArrayList<String> namesForBlack = new ArrayList<String>();
        ArrayList<Integer> rawContactIds= new ArrayList<Integer>();
        ArrayList<Integer> dataIds = new ArrayList<Integer>();
        for (String number : numbers) {
        	numbersForBlack.add(number);
        	int position = mCheckedItem.get(number);
        	Cursor cursor = (Cursor) mAdapter.getItem(position);
	        if (cursor != null) {
	        	namesForBlack.add(cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME)));
	        	rawContactIds.add(cursor.getInt(cursor.getColumnIndex(Calls.RAW_CONTACT_ID)));
	        	dataIds.add(cursor.getInt(cursor.getColumnIndex(Calls.DATA_ID)));
	        }
        }
        
        if (mIsAdding) {
        	return;
        }
        
        new AddThread(namesForBlack, numbersForBlack, rawContactIds, dataIds).start();
    	
    }
    
    private class AddThread extends Thread {
        ArrayList<String> nameForBlackName = new ArrayList<String>();
        ArrayList<String> numberForBlackName = new ArrayList<String>();
        ArrayList<Integer> rawContactIds= new ArrayList<Integer>();
        ArrayList<Integer> dataIds = new ArrayList<Integer>();
        
        public AddThread(ArrayList<String> nameList, ArrayList<String> numberList, ArrayList<Integer> rawContactIds, ArrayList<Integer> dataIds) {
            this.nameForBlackName = nameList;
            this.numberForBlackName = numberList;
            this.rawContactIds = rawContactIds;
            this.dataIds = dataIds;
        }
        
        @Override
        public void run() {
//        	List<BlackItem> list = ContactUtils.getBlackItemListByNumber(mContext, numberForBlackName, null);
            if (numberForBlackName == null || numberForBlackName.size() < 1) {
                return;
            }
            
            mHandler.sendEmptyMessage(START);
            
			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            
            for (int i = 0; i < numberForBlackName.size(); i++) {
            	String number = numberForBlackName.get(i);
            	String name = nameForBlackName.get(i);
            	if(TextUtils.isEmpty(name)) {
            		name = getString(R.string.calllog_note);
            	}
            	int rawContactId = rawContactIds.get(i);
            	int dataId = dataIds.get(i);
            	
				ops.add(getOperation(number,name, rawContactId, dataId));	                
            }
            
			try {
				mContext.getContentResolver().applyBatch(BlackUtils.HB_CONTACT_AUTHORITY,
						ops);
			} catch (Exception e) {
				e.printStackTrace();
			}
            
            mHandler.sendEmptyMessageDelayed(END, InterceptionUtils.MIN_DIALOG_SHOW_TIME);
            mCheckedItem.clear();
        }
    }
    
	protected ContentProviderOperation getOperation(String number, String name,
			long rawContactId, long dataId) {
		return null;
	}
       
    private String mSelectAllStr;
	private String mUnSelectAllStr;
	private BottomNavigationView mBottomNavigationView;
	private ActionMode mActionMode;
	private ActionModeListener mActionModeListener = new ActionModeListener() {

		@Override
		public void onActionItemClicked(Item item) {
			// TODO Auto-generated method stub
			switch (item.getItemId()) {
			case ActionMode.POSITIVE_BUTTON:
				int checkedCount = mCheckedItem.size();
				int all = mAdapter.getCount();
				all = all < MAX_ADD_NUM ? all : MAX_ADD_NUM;
				selectAll(checkedCount < all);
				break;
			case ActionMode.NAGATIVE_BUTTON:
//				safeQuitDeleteMode();
			    finish();
				break;
			default:
			}

		}

		@Override
		public void onActionModeDismiss(ActionMode arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onActionModeShow(ActionMode arg0) {
			// TODO Auto-generated method stub
			updateActionMode();
		}

	};

	private static int MAX_ADD_NUM = 1000;
	private void selectAll(boolean checked) {
		int num =  mAdapter.getCount();
		if(checked && num > MAX_ADD_NUM) {
	    	Toast.makeText(mContext, mContext.getString(R.string.max_add_to_blacklist), Toast.LENGTH_SHORT).show();
			num = MAX_ADD_NUM;
		}
		for (int position = 0; position < num; ++position) {
			  String number = mAdapter.getNumber(position);
			if (checked) {				
			   	mAdapter.setCheckedItem(number);
				mCheckedItem.put(number, position);
			} else {
			   	mAdapter.removeCheckedItem(number);
				mCheckedItem.remove(number);
			}

			int realPos = position - mList.getFirstVisiblePosition();
			if (realPos >= 0) {
				View view = mList.getChildAt(realPos);
				if (view != null) {
					final CheckBox checkBox = (CheckBox) view
							.findViewById(R.id.list_item_check_box);
					if (null != checkBox) {
						checkBox.setChecked(checked);
					}
				}
			}
		}

		updateActionMode();
	}

	private boolean isInDeleteMode() {
		return getActionMode().isShowing();
	}


	public void updateActionMode() {
		if (mAdapter == null) {
			finish();
			return;
		}
		int checkedCount = mCheckedItem.size();
		int all = mAdapter.getCount();
		all = all < MAX_ADD_NUM ? all : MAX_ADD_NUM;
		if (all >0 && checkedCount >= all ) {
			mActionMode.setPositiveText(mUnSelectAllStr);
		} else {
			mActionMode.setPositiveText(mSelectAllStr);
		}
		//mActionMode.setNagativeText("");

		Log.d(TAG,"checkedCount:"+checkedCount);
		if (checkedCount > 0) {
			setBottomMenuEnable(true);
			updateActionModeTitle(mContext.getString(R.string.selected_total_num,
					checkedCount));
		} else {
			setBottomMenuEnable(false);
			updateActionModeTitle(mContext.getString(R.string.hb_select_calllogs));
		}
	}

	private void setBottomMenuEnable(boolean flag) {
//		mBottomNavigationView.setEnabled(flag);
		if(flag){
			if(mBottomNavigationView.getVisibility()!=View.VISIBLE) mBottomNavigationView.setVisibility(View.VISIBLE);
		}else if(mBottomNavigationView.getVisibility()!=View.GONE) mBottomNavigationView.setVisibility(View.GONE);
	}


	private void initActionBar(boolean flag) {
		showActionMode(flag);
//		mBottomNavigationView.setVisibility(flag ? View.VISIBLE : View.GONE);
	}

	private void initBottomMenuAndActionbar() {
		mSelectAllStr = mContext.getResources().getString(R.string.select_all);
        mUnSelectAllStr = mContext.getResources().getString(R.string.deselect_all);
		mActionMode = getActionMode();
		mActionMode.setNagativeText(mContext.getResources().getString(R.string.cancel));
		mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
		mBottomNavigationView
				.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.menu_add:
							addContacts();
							return true;
						default:
							return false;
						}
					}
				});
		setActionModeListener(mActionModeListener);
		updateActionMode();
	}
	
}
