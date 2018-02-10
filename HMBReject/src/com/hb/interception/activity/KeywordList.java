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
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import com.hb.interception.adapter.KeywordAdapter;
import com.hb.interception.util.BlackUtils;

public class KeywordList extends InterceptionActivityBase {
	private static final String TAG = "KeywordList";

	private List<String> mKeywordStringList = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.mark_list);
		mListUri = BlackUtils.KEYWORD_URI;
		init();
	}

	protected void init() {
		mList = (HbListView) findViewById(R.id.mark_list);
		mEmpty = findViewById(R.id.mark_empty);
		super.init();
	}

	protected void startQuery() {
		mQueryHandler.startQuery(0, null, mListUri, null, null, null, "_id desc");
	}

	@Override
	protected void processQueryComplete(Context context, Cursor cursor) {
		if (cursor != null) {
			mKeywordStringList.clear();
			if (cursor.moveToFirst()) {
				do {
					mKeywordStringList.add(cursor.getString(cursor
							.getColumnIndex("word")));
				} while (cursor.moveToNext());
			}

		}
		if (cursor != null) {
			cursor.moveToFirst();
			if (mAdapter == null) {
				mAdapter = new KeywordAdapter(context, cursor);
				mAdapter.setListener(KeywordList.this);
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
//		BottomWidePopupMenu menu = new BottomWidePopupMenu(this);
//		menu.inflateMenu(R.menu.mark);
//		menu.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
//			@Override
//			public boolean onItemClicked(MenuItem item) {
//				// TODO Auto-generated method stub
//				int id = item.getItemId();
//				switch (id) {
//				case R.id.edit:
//					showEditDialogMenu(pos);
//					break;
//				case R.id.del:
//					showDeleteDialog(pos);
//					break;
//				default:
//					break;
//				}
//
//				return true;
//			}
//		});
//		menu.show();
		showEditDialogMenu(pos);
	}

	private void showEditDialogMenu(final int pos) {
		Cursor cursor = (Cursor) mList.getItemAtPosition(pos);
		final String mTarget = cursor.getString(cursor.getColumnIndex("word"));
		final String mTargetId = cursor.getString(cursor.getColumnIndex("_id"));

		View view = LayoutInflater.from(mContext).inflate(
				R.layout.dialog_edittext, null);
		final EditText editContent = (EditText) view
				.findViewById(R.id.mark_content);
		editContent.setText(mTarget);
		editContent.setSelection(mTarget.length());
		editContent
				.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) });
		AlertDialog dialogs = new AlertDialog.Builder(mContext)
				.setTitle(
						mContext.getResources()
								.getString(R.string.edit_keyword))
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								String s = editContent.getText().toString()
										.replace(" ", "");
								if (!s.equals("")) {
									s = editContent.getText().toString();
									ContentResolver cr = getContentResolver();
									ContentValues cv = new ContentValues();
									cv.put("word", s);
									int uri2 = cr.update(mListUri, cv,
											"word=?", new String[] { mTarget });

								} else {
									Toast.makeText(
											mContext,
											mContext.getResources().getString(
													R.string.no_keyword),
											Toast.LENGTH_LONG).show();
									return;
								}
								dialog.dismiss();
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dialogs.setCanceledOnTouchOutside(false);
		dialogs.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}

	protected void initBottomMenuAndActionbar() {
		super.initBottomMenuAndActionbar();
		mBottomNavigationView
				.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.delete:
							deleteSelectedWord();
							return true;
						default:
							return false;
						}
					}
				});
	}

	private void deleteSelectedWord() {
		// TODO Auto-generated method stub

		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(this.getResources().getString(R.string.del_keyword))
				.setMessage(
						this.getResources().getString(R.string.is_confirm_keyword_del))
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
									final String word = pcursor.getString(1);
									cr.delete(mListUri, "word=?",
											new String[] { word });
								}
								changeToNormalMode(true);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dialog.setCanceledOnTouchOutside(false);
	}

	protected void initToolBar() {
		super.initToolBar();
		myToolbar.setTitle(R.string.keywords_manage);
		myToolbar.inflateMenu(R.menu.toolbar_menu_add);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onMenuItemClick--->" + item.getTitle());
		if (item.getItemId() == R.id.add) {
			View view = LayoutInflater.from(mContext).inflate(
					R.layout.dialog_edittext, null);
			final EditText mKeywordText = (EditText) view
					.findViewById(R.id.mark_content);
			mKeywordText
					.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
							15) });
			AlertDialog dialogs = new AlertDialog.Builder(mContext)
					.setTitle(
							mContext.getResources().getString(
									R.string.add_keyword))
					.setView(view)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {

									String s = mKeywordText.getText()
											.toString();
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

										if (!mKeywordStringList.contains(s)) {
											ContentResolver cr = getContentResolver();
											ContentValues cv = new ContentValues();
											cv.put("word", s);
											try {
												Uri uri2 = cr.insert(mListUri,
														cv);
											} catch (Exception e) {
												e.printStackTrace();
											}
										} else {
											Toast.makeText(
													mContext,
													mContext.getResources()
															.getString(
																	R.string.keyword_content_exist),
													Toast.LENGTH_LONG).show();
										}

									} else {
										Toast.makeText(
												mContext,
												mContext.getResources()
														.getString(
																R.string.no_content),
												Toast.LENGTH_LONG).show();
									}
									dialog.dismiss();
								}
							}).setNegativeButton(android.R.string.cancel, null)
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
		final String word = pcursor.getString(1);

		AlertDialog dialog = new AlertDialog.Builder(mContext)
				.setTitle(
						mContext.getResources().getString(R.string.del_keyword))
				.setMessage(
						mContext.getResources().getString(
								R.string.is_confirm_keyword_del))
				.setPositiveButton(R.string.del_confirm,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								ContentResolver cr = getContentResolver();
								cr.delete(mListUri, "word=?",
										new String[] { word });

							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dialog.setCanceledOnTouchOutside(false);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		if (isInDeleteMode()) {
			selectItem(view, position);
		} else {
			showDialogMenu(position);
		}

	}
}
