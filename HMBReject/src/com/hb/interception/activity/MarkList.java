package com.hb.interception.activity;

import hb.app.dialog.AlertDialog;
import hb.view.menu.BottomWidePopupMenu;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.HbListView;

import java.util.ArrayList;
import java.util.List;

import com.hb.interception.R;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.hb.interception.adapter.MarkAdapter;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.YuloreUtil;

public class MarkList extends InterceptionActivityBase {
	private static final String TAG = "MarkList";

	private List<String> mMarkStringList = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.mark_list);
		setHbContentView(R.layout.mark_list);
		mListUri = BlackUtils.MARK_URI;
		init();
	}
	
	protected void init() {
	    mList = (HbListView) findViewById(R.id.mark_list);
	    mEmpty =  findViewById(R.id.mark_empty);      
		super.init();
	}
	
	protected void startQuery() {
		mQueryHandler.startQuery(0, null, mListUri, null,
				"lable is not null and number is null", null, null);		
	}
	
	@Override
	protected void processQueryComplete(Context context, Cursor cursor) {
		if (cursor != null) {
			mMarkStringList.clear();
			if (cursor.moveToFirst()) {
				do {
					mMarkStringList.add(cursor.getString(cursor
							.getColumnIndex("lable")));
				} while (cursor.moveToNext());
			}

		}
		if (cursor != null) {
			cursor.moveToFirst();
			if (mAdapter == null) {
				mAdapter = new MarkAdapter(context, cursor);
				mAdapter.setListener(MarkList.this);
				mList.setAdapter(mAdapter);
			} else {
				mAdapter.changeCursor(cursor);
			}
		} else {
			if (mAdapter != null) {
				mAdapter.changeCursor(null);
			}
		}
	}
	
	@Override
	protected void showDialogMenu(final int pos) {
	    BottomWidePopupMenu menu = new BottomWidePopupMenu(this);
        menu.inflateMenu(R.menu.mark);
        menu.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onItemClicked(MenuItem item) {
                // TODO Auto-generated method stub
                int id = item.getItemId();
                switch (id) {
                    case R.id.edit:
                        showEditDialogMenu(pos);
                        break;
                    case R.id.del:
                        showDeleteDialog(pos);
                        break;
                    default:
                        break;
                    }

                return true;
            }
        });
        menu.show();
	}

	private void showEditDialogMenu(final int pos) {
		Cursor cursor = (Cursor) mList
				.getItemAtPosition(pos);
		final String mTarget = cursor.getString(cursor.getColumnIndex("lable"));
		final String mTargetId = cursor.getString(cursor.getColumnIndex("_id"));
		
		View view = LayoutInflater.from(mContext).inflate(
				R.layout.dialog_edittext, null);
		final EditText mark_content = (EditText) view
				.findViewById(R.id.mark_content);
		mark_content.setText(mTarget);
		mark_content.setSelection(mTarget.length());
		mark_content
				.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) });
		AlertDialog dialogs = new AlertDialog.Builder(mContext)
				.setTitle(
						mContext.getResources().getString(
								R.string.edit_mark))
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								String s = mark_content.getText().toString()
										.replace(" ", "");
								if (!s.equals("")) {
									s = mark_content.getText().toString();
									ContentResolver cr = getContentResolver();
									ContentValues cv = new ContentValues();
									cv.put("lable", s);
									int uri2 = cr.update(mListUri, cv, "lable=?",
											new String[] { mTarget });

									updateData(mTarget, s);
								} else {
									Toast.makeText(
											mContext,
											mContext
													.getResources().getString(
															R.string.no_marks),
											Toast.LENGTH_LONG).show();
									return;
								}
								dialog.dismiss();
							}
						})
				.setNegativeButton(android.R.string.cancel, null).show();
		dialogs.setCanceledOnTouchOutside(false);
		dialogs.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}

	private void updateData(String oldLable, String newLable) {
		final String oldMark = oldLable;
		final String newMark = newLable;
		Log.e(TAG, "oldMark = " + oldMark + " newMark = " + newMark);

		new Thread() {
			public void run() {
				Cursor callCursor = mContext.getContentResolver().query(
						Calls.CONTENT_URI,
						new String[] { "_id", "number" },
						"mark='" + oldMark
								+ "' and user_mark='-1' and reject in (0,1)",
						null, null, null);
				if (callCursor != null) {
					if (callCursor.moveToFirst()) {
						do {
							ContentValues cv = new ContentValues();
							int userMark = 0;
							String number = callCursor.getString(1);
							Log.e(TAG, "number ========== " + number);

							if (newMark != null) {
								YuloreUtil.insertUserMark(mContext, number,
										newMark);
								userMark = -1;
							} else {
								userMark = 0;
								YuloreUtil.deleteUserMark(mContext, number);
							}

							cv.put("mark", newMark);
							cv.put("user_mark", userMark);
							mContext.getContentResolver().update(
									Calls.CONTENT_URI, cv,
									"_id=" + callCursor.getString(0), null);
						} while (callCursor.moveToNext());
					}

					callCursor.close();
				}

				Uri blackUri = BlackUtils.BLACK_URI;
				System.out.println("oldMark=" + oldMark);
				Cursor blackCursor = mContext.getContentResolver().query(
						blackUri,
						new String[] { "_id", "number", "user_mark" },
						"lable='" + oldMark + "' and user_mark='-1'", null,
						null, null);
				if (blackCursor != null) {
					if (blackCursor.moveToFirst()) {
						try {
							do {
								ContentValues cv = new ContentValues();
								int userMark = 0;
								String number = blackCursor.getString(1);

								if (newMark != null) {
									YuloreUtil.insertUserMark(mContext, number,
											newMark);
									userMark = -1;
								} else {
									userMark = 0;
									YuloreUtil.deleteUserMark(mContext, number);
								}

								cv.put("lable", newMark);
								cv.put("user_mark", userMark);
								int i = mContext.getContentResolver()
										.update(blackUri,
												cv,
												"_id="
														+ blackCursor
																.getString(0),
												null);
								System.out.println("i=" + i);
							} while (blackCursor.moveToNext());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					blackCursor.close();
				}
			}
		}.start();
	}

	protected void initBottomMenuAndActionbar() {
		super.initBottomMenuAndActionbar();		
		mBottomNavigationView
				.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.delete:
							deleteSelectedMark();
							return true;
						default:
							return false;
						}
					}
				});
	}



	private void deleteSelectedMark() {
		// TODO Auto-generated method stub

		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(
						this.getResources().getString(
								R.string.del_mark))
				.setMessage(
						this.getResources().getString(
								R.string.is_confirm_del))
				.setPositiveButton(R.string.del_confirm,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								ContentResolver cr = getContentResolver();
								Cursor pcursor = (Cursor) mAdapter.getCursor();
								for (int pos : mAdapter.getCheckedItem()) {
									pcursor.moveToPosition(pos);
									final String lable = pcursor.getString(1);
									cr.delete(mListUri, "lable=?",
											new String[] { lable });

									updateData(lable, null);
								}
								changeToNormalMode(true);
							}
						})
				.setNegativeButton(android.R.string.cancel, null).show();
		dialog.setCanceledOnTouchOutside(false);
	}

	protected void initToolBar() {
		super.initToolBar();
		myToolbar.setTitle(R.string.marks_manage);
		myToolbar.inflateMenu(R.menu.toolbar_menu_add);
	}


	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onMenuItemClick--->" + item.getTitle());
		if (item.getItemId() == R.id.add) {
			View view = LayoutInflater.from(mContext).inflate(
					R.layout.dialog_edittext, null);
			final EditText mMarkText = (EditText) view
					.findViewById(R.id.mark_content);
			mMarkText
					.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
							15) });
			AlertDialog dialogs = new AlertDialog.Builder(
					mContext)
					.setTitle(
							mContext.getResources().getString(
									R.string.add_mark))
					.setView(view)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {

									String s = mMarkText.getText().toString();
									if (!s.equals("")) {
										if (s != null
												&& s.replaceAll(" ", "")
														.equals("")) {
											Toast.makeText(
													mContext,
													mContext.getResources()
															.getString(
																	R.string.mark_error),
													Toast.LENGTH_SHORT).show();
											return;
										}

										if (!mMarkStringList.contains(s)) {
											ContentResolver cr = getContentResolver();
											ContentValues cv = new ContentValues();
											cv.put("lable", s);
											try {
												Uri uri2 = cr.insert(mListUri, cv);
											} catch (Exception e) {
												e.printStackTrace();
											}
										} else {
											Toast.makeText(
													mContext,
													mContext
															.getResources()
															.getString(
																	R.string.mark_content_exist),
													Toast.LENGTH_LONG).show();
										}

									} else {
										Toast.makeText(
												mContext,
												mContext
														.getResources()
														.getString(
																R.string.no_content),
												Toast.LENGTH_LONG).show();
									}
									dialog.dismiss();
								}
							})
							.setNegativeButton(android.R.string.cancel, null)
							.show();
			dialogs.setCanceledOnTouchOutside(false);
			dialogs.getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		return false;
	}

	private void showDeleteDialog(int position) {
	    Cursor pcursor = (Cursor) mList.getItemAtPosition(position);
        if (pcursor == null) {
            return;
        }
        final String lable = pcursor.getString(1);

        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(
                        mContext.getResources().getString(
                                R.string.del_mark))
                .setMessage(
                        mContext.getResources().getString(
                                R.string.is_confirm_del))
                .setPositiveButton(R.string.del_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                dialog.dismiss();
                                ContentResolver cr = getContentResolver();
                                cr.delete(mListUri, "lable=?",
                                        new String[] { lable });

                                updateData(lable, null);
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null).show();
        dialog.setCanceledOnTouchOutside(false);
	}
}
