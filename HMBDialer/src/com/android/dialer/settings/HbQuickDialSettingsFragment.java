//add by liyang 2017-4-1
package com.android.dialer.settings;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.AlertDialog.Builder;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Data;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;



public class HbQuickDialSettingsFragment extends Fragment implements View.OnClickListener{

	private static final String TAG = "HbQuickDialSettingsFragment";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	

	}

	private ViewGroup[] items;
	private SharedPreferences sharedPreferences;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreateView(inflater, container, savedInstanceState);
		View view=initView(inflater,container);
		fillNameAndNumber();
		return view;
	}
	
	private View initView(LayoutInflater inflater,ViewGroup container){
		items=new ViewGroup[9];
		View view = inflater.inflate(R.layout.hb_quickdial_settings, container, false);
		for(int i=0;i<9;i++){
			items[i]=(ViewGroup) view.findViewById(ids[i]);
			items[i].setOnClickListener(this);
		}	
		
		return view;
	}


	private void fillNameAndNumber(){
		if(sharedPreferences==null){
			sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		}
		

		for(int i=0;i<9;i++){
			ViewGroup item=items[i];

			TextView mText=(TextView) item.getChildAt(TEXT_INDEX);
			mText.setText(i+1+"");

			String savedName=sharedPreferences.getString("hbQuickDialName"+(i+1), "");		
			TextView mName=(TextView) item.getChildAt(NAME_INDEX);
			if(!TextUtils.isEmpty(savedName)){				
				mName.setText(savedName);
			}else{
				mName.setText(null);
			}

			String savedNumber=sharedPreferences.getString("hbQuickDialNumber"+(i+1), "");
			TextView mNumber=(TextView) item.getChildAt(NUMBER_INDEX);
			if(!TextUtils.isEmpty(savedNumber)){	
				mNumber.setText(savedNumber);
			}else {
				mNumber.setText(null);
			}

		}
	}
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	private final int[] ids=new int[]{R.id.item1,R.id.item2,R.id.item3,R.id.item4,R.id.item5,R.id.item6,R.id.item7,R.id.item8,R.id.item9};
		
	private int getItemId(int id){
		for(int i=0;i<9;i++){
			if(id==ids[i]) return i+1;						
		}
		return 0;
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG,"onActivityResult,resultCode:"+resultCode+" data:"+data);
		if (resultCode == Activity.RESULT_OK) {
			final Uri dataUri = data.getData();			
			if (TextUtils.isEmpty(dataUri.toString())) {
				return;
			}

			final long dataId=ContentUris.parseId(dataUri);
			Log.d(TAG,"onActivityResult,dataIds:"+dataUri+dataUri+" dataId:"+dataId);
			Cursor cursor=null;
			try{
				cursor=getContext().getContentResolver().query(android.provider.ContactsContract.Data.CONTENT_URI, new String[]{"_id","display_name","data1"}, "_id="+dataId, null, null);
				ViewGroup view=items[requestCode-1];
				TextView mName=(TextView) view.getChildAt(NAME_INDEX);
				TextView mNumber=(TextView) view.getChildAt(NUMBER_INDEX);
				if(cursor!=null && cursor.moveToFirst()){
					String mNameString=cursor.getString(1);
					String mNumberString=cursor.getString(2);
					Log.d(TAG,"mNameString:"+mNameString+" mNumberString:"+mNumberString);
					mName.setText(mNameString);
					mNumber.setText(mNumberString);
					
					final SharedPreferences.Editor editor = sharedPreferences.edit();
					
					editor.putString("hbQuickDialName"+requestCode, mNameString);
					editor.putString("hbQuickDialNumber"+requestCode, mNumberString);
					editor.commit();
				}
			}catch(Exception e){
				Log.d(TAG,"e:"+e);
			}finally{
				if(cursor!=null){
					cursor.close();
					cursor=null;
				}
			}
		}
	}
	
	private void deleteQuickDialNumber(final int id){
		final SharedPreferences.Editor editor = sharedPreferences.edit();		
		editor.remove("hbQuickDialName"+id);
		editor.remove("hbQuickDialNumber"+id);
		editor.commit();
		
		ViewGroup view=items[id-1];
		TextView mName=(TextView) view.getChildAt(NAME_INDEX);
		TextView mNumber=(TextView) view.getChildAt(NUMBER_INDEX);
		mName.setText(null);
		mNumber.setText(null);
	}
	
	public static final int TEXT_INDEX=0;
	public static final int NAME_INDEX=1;
	public static final int NUMBER_INDEX=2;
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		final int id=getItemId(v.getId());
		if(id==1) {
			Toast.makeText(getContext(), "1号键不可设置", Toast.LENGTH_LONG).show();
			return;
		}
		String mName=sharedPreferences.getString("hbQuickDialName"+id, null);
		String mNumber=sharedPreferences.getString("hbQuickDialNumber"+id, null);
		Log.d(TAG,"onclick,mName:"+mName+" mNumber:"+mNumber);
		
		if(!TextUtils.isEmpty(mName) || !TextUtils.isEmpty(mNumber)){
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setMessage(mNumber);
			builder.setTitle(getContext().getString(R.string.hb_delete_number));
			builder.setNegativeButton(getContext().getString(R.string.hb_cancel), null);
			builder.setPositiveButton(getContext().getString(R.string.hb_delete), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
					deleteQuickDialNumber(id);
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			return;
		}
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(Phone.CONTENT_TYPE);
		startActivityForResult(intent, id);
	}



}
