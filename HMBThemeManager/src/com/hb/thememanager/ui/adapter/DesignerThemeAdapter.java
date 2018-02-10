package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.views.HomeThemeListItem;

import java.util.List;

/**
 * Created by alexluo on 17-8-9.
 */

public class DesignerThemeAdapter extends AbsLocalBaseAdapter<Theme>{

    private int mItemLayout;
    public DesignerThemeAdapter(Context context,int itemLayoutId) {
        super(context);
        mItemLayout = itemLayoutId;
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Holder holder;
        if(view == null){
            holder = new Holder(getContext(),this);
            view = getInflater().inflate(mItemLayout,viewGroup,false);
            holder.holdConvertView(view);
            view.setTag(holder);
        }else {
            holder = (Holder) view.getTag();
        }
        holder.bindDatas(i,getThemes());
        return view;
    }

    class Holder extends  AbsViewHolder<Theme>{
        private HomeThemeListItem mItem;
        public Holder(Context context, ListAdapter adapter) {
            super(context, adapter);
        }

        @Override
        public void holdConvertView(View convertView) {
            mItem = (HomeThemeListItem)convertView.findViewById(R.id.designer_theme_list_item);
        }

        @Override
        public void bindDatas(int position, List<Theme> themes) {
            Theme theme = themes.get(position);
            if(theme != null){
                CommonUtil.getThemePrice(theme,getContext());
                mItem.setIcon(theme.coverUrl);
                mItem.setPrice(theme.getPrice());
                mItem.setTitle(theme.name);
            }
        }
    }


}
