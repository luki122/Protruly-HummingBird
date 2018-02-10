package cn.com.protruly.filemanager.globalsearch;


import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HbSearchView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.BaseFragment;
import cn.com.protruly.filemanager.HomePage.HomeFragment;
import cn.com.protruly.filemanager.ui.ToolbarManager;
import hb.widget.HbListView;


public class GlobalSearchHistoryFragment extends BaseFragment implements HbSearchView.OnQueryTextListener,View.OnClickListener,View.OnKeyListener{

    protected HbSearchView mToolbarSearchView;

    private TextView tv_tip;
    private HbListView listView;
    private TextView bt_clear;
    private RelativeLayout emptyly;
    private GlobalSearchHisDbHelper helper;
    private SQLiteDatabase db;
    private BaseAdapter adapter;
    private LinearLayout listviewLayout;

    View clearhis;


    @Override
    public void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        helper = new GlobalSearchHisDbHelper(mContext);
        //mToolbar = ((HbActivity)getActivity()).getToolbar();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void initRootView(LayoutInflater inflater, ViewGroup contaner) {

        rootView = inflater.inflate(R.layout.global_search_hislist_layout, contaner, false);
        getToolbarManager().switchToStatus(ToolbarManager.STATUS_GLOBALSEARCH_HISTORY);
        InitSearchView();

        super.initRootView(inflater, contaner);

    }

    @Override
    protected void initViewOnCreateView() {
        super.initViewOnCreateView();

        tv_tip =(TextView) rootView.findViewById(R.id.tv_tip);
        listviewLayout=(LinearLayout)rootView.findViewById(R.id.listview_layout);
        listView = (HbListView)rootView.findViewById(R.id.listView);
        // bt_clear= (Button) rootView.findViewById(R.id.bt_clear);
        emptyly= (RelativeLayout)rootView.findViewById(R.id.emptyview);

        clearhis = (LinearLayout) LayoutInflater.from(mContext).inflate( R.layout.clear_history_item_layout, null);
        bt_clear= (TextView) clearhis.findViewById(R.id.bt_clear);
        listView.addFooterView(clearhis);

        bt_clear.setOnClickListener(this);

        queryData("");

    }

    public void InitSearchView() {
        Menu menu = mActivity.getOptionMenu();
        MenuItem item = menu.findItem(R.id.menu_search);
        if(item==null) return;
        mToolbarSearchView = (HbSearchView) item.getActionView();
        mToolbarSearchView.setIconifiedByDefault(true);
        mToolbarSearchView.setIconified(false);
        //mToolbarSearchView.clearFocus();
        mToolbarSearchView.setOnQueryTextListener(this);
        mToolbarSearchView.setOnKeyListener(this);
    }


    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER || event.getAction() == KeyEvent.ACTION_DOWN) {// 修改回车键功能
        TextView textView = findTextViewInSearchView(mToolbarSearchView);
        }
        return true;
    }


    @Override
    public boolean onQueryTextSubmit(String s) {

        goToGobalSearch(s.trim());

        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        //String tempName = s.trim();
        // 根据tempName去模糊查询数据库中有没有数据
        queryData(s.trim());
        return true;
    }

    private TextView findTextViewInSearchView(HbSearchView searchView) {
        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text",null,null);
        TextView textView = (TextView) searchView.findViewById(id);
        return textView;
    }


    @Override
    public void onToolbarNavigationIconClicked() {
        if(mToolbarSearchView!=null)mToolbarSearchView.clearFocus();
        startHomeFragment();
    }

    @Override
    public boolean onBackPressed() {

        startHomeFragment();
        return true;
    }

    private void goToGobalSearch(String keyword){

        Bundle bundle = new Bundle();
        bundle.putString("keyword",keyword);
        GlobalSearchResultFragment fragment = new GlobalSearchResultFragment();
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();
    }

    private void startHomeFragment(){

        HomeFragment fragment = new HomeFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container,fragment,fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();

    }


    @Override
    public int getFragmentId() {
        return 0;
    }

    @Override
    protected void getInitData() {

    }

    /**
     * 插入数据
     */
    private void insertData(String tempName) {
        db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", tempName);
        db.insert("records",null,values);
        //db.execSQL("insert into records(name) values('" + tempName + "')");
        //db.execSQL("records",null,values);
        db.close();
    }

    /**
     * 模糊查询数据
     */
    private void queryData(String tempName) {

        //Cursor cursor = helper.getReadableDatabase().rawQuery("select _id,name from records where name like '%" + tempName + "%' order by _id desc ", null);
          Cursor cursor = helper.getReadableDatabase().rawQuery("select * from records where name like ?", new String[]{"%"+tempName+"%"});
        //Cursor cursor = helper.getReadableDatabase().query("records",new String[]{"_id,name"},"name like ?",new Object[]{tempName},null,null,"_id desc",null);
        if(/*tempName.equals("") &&*/ !cursor.moveToNext()){
            tv_tip.setVisibility(View.GONE);
            bt_clear.setVisibility(View.GONE);
            emptyly.setVisibility(View.VISIBLE);
        }else{
            tv_tip.setVisibility(View.VISIBLE);
            bt_clear.setVisibility(View.VISIBLE);
            emptyly.setVisibility(View.GONE);
        }
        // 创建adapter适配器对象
        adapter = new DeletableCursorAdapter(mContext,cursor);
      /* adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor, new String[] { "name" },
                new int[] { android.R.id.text1 }, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);*/
        // 设置适配器
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
    /**
     * 检查数据库中是否已经有该条记录
     */
    private boolean hasData(String tempName) {
        // Cursor cursor = helper.getReadableDatabase().rawQuery(querycmd,new String[]{tempName});
        Cursor cursor = helper.getReadableDatabase().rawQuery(
                "select _id,name from records where name =?", new String[]{"%"+tempName+"%"});
        //判断是否有下一个
        Boolean hasdata = cursor.moveToNext();
        cursor.close();
        return hasdata;
    }

    /**
     * 清空数据
     */
    private void deleteData() {
        db = helper.getWritableDatabase();
        db.execSQL("delete from records");
        db.close();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id){
            case R.id.bt_clear:
                deleteData();
                queryData("");
                break;
        }
    }

    class DeletableCursorAdapter  extends SimpleCursorAdapter {

        public DeletableCursorAdapter(Context context, Cursor c) {
            super(context, R.layout.his_search_item_layout, c, new String[] {GlobalSearchHisDbHelper.VOLUMN_NAME}, new int[] {R.id.historywords});
        }


        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            final int id = cursor.getInt(cursor.getColumnIndex(GlobalSearchHisDbHelper.VOLUMN_ID));
            final String name = cursor.getString(cursor.getColumnIndex(GlobalSearchHisDbHelper.VOLUMN_NAME));
            ImageView delete = (ImageView) view.findViewById(R.id.delete_bt);
            TextView hiskey = (TextView) view.findViewById(R.id.historywords);
            // int position = cursor.getPosition();    //获取当前位置
            MyItemButtonClick hisItemButtonClick = new MyItemButtonClick(id,name);
            delete.setOnClickListener(hisItemButtonClick);
            hiskey.setOnClickListener(hisItemButtonClick);
        }
    }

    class MyItemButtonClick implements View.OnClickListener {
        private int mItemId;
        private String mItemSt;

        MyItemButtonClick(int id,String st) {
            mItemId = id;
            mItemSt = st;
        }

        @Override
        public void onClick(View v) {
            int viewId = v.getId();
            if (viewId == R.id.delete_bt) {
                SQLiteDatabase writableDB = helper.getWritableDatabase();
                writableDB.delete(GlobalSearchHisDbHelper.TABLE_NAME
                        , "_id =?" //添加"=？"
                        , new String[]{String.valueOf(mItemId)});
                writableDB.close();
                queryData("");
            }
            if (viewId == R.id.historywords) {
                goToGobalSearch(mItemSt);
            }
        }
    }




}
