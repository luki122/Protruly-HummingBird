package com.hb.thememanager.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.views.HomeThemeListItem;

import java.util.List;

/**
 * Created by alexluo on 17-8-9.
 */

public class DesignerFontsAdapter extends AbsLocalBaseAdapter<Theme>{

    private int mItemLayout;
    public DesignerFontsAdapter(Context context, int itemLayoutId) {
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
        private ImageView mCover;
        private TextView mTitle;
        private TextView mPrice;
        public Holder(Context context, ListAdapter adapter) {
            super(context, adapter);
        }

        @Override
        public void holdConvertView(View convertView) {
            mCover = (ImageView)convertView.findViewById(R.id.theme_cover);
            mTitle = (TextView)convertView.findViewById(R.id.theme_name);
            mPrice = (TextView)convertView.findViewById(R.id.theme_price);
        }

        @Override
        public void bindDatas(int position, List<Theme> themes) {
            Theme theme = themes.get(position);
            if(theme != null){
                Glide.with(getContext()).load(theme.coverUrl).into(mCover);
                mTitle.setText(theme.name);
                CommonUtil.getThemePrice(theme,getContext());
                if(!TextUtils.isEmpty(theme.getPrice())){
                    try{
                        double newPrice = Double.parseDouble(theme.getPrice());
                        mPrice.setText(getContext().getString(R.string.theme_price_suffix,theme.getPrice()));
                    }catch (Exception e){
                        mPrice.setText(theme.getPrice());
                    }

                }else{
                    mPrice.setText(getContext().getString(R.string.theme_price_free));
                }
            }
        }
    }


}
