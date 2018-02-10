package com.protruly.powermanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.protruly.powermanager.powersave.PowerSaveActivity;
import com.protruly.powermanager.purebackground.activity.AppManagerActivity;

import hb.app.HbActivity;
import hb.widget.HbListView;


public class MainActivity extends HbActivity {

    private HbListView listView;

    private String[] itemStr = new String[]{"应用管理", "省电管理"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_main);
        getToolbar().setTitle("HMBPM");

        listView = (HbListView) findViewById(R.id.listView);
        listView.setAdapter(new ItemAdapter());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        openActivity(AppManagerActivity.class, null);
                        break;
                    case 1:
                        openActivity(PowerSaveActivity.class, null);
                        break;
                }
            }
        });
    }

    private class ItemAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return itemStr.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_1, null);
            }
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(itemStr[position]);
            return convertView;
        }
    }

    private void openActivity(Class<?> cls, Bundle data) {
        Intent i = new Intent(this, cls);
        if (data != null) {
            i.putExtras(data);
        }
        startActivity(i);
    }
}