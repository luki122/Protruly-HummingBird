package com.android.mms.ui;

/*tangyisen*/
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import android.widget.Toast;

public class ZzzFlowLayout extends ViewGroup {

    private static final String TAG = "ZzzFlowLayout";

    private int mChildWidth;
    private int mChildHeight;
    private int mChildMargin;
    private int mChildPadding;
    /**
     * 存储所有的View，按行记录
     */
    private List<List<View>> mAllViews = new ArrayList<List<View>>();
    LayoutInflater mLayoutInflater;

    private ContactList mContactList = new ContactList();
    /**
     * 记录每一行的最大高度
     */
    private List<Integer> mLineHeight = new ArrayList<Integer>();
    private ComposeMessageActivity mActivity;
    private static final int RECIPIENTS_LIMIT_FOR_SMS     = MmsConfig.getSmsRecipientLimit();

    public ZzzFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mChildWidth = (int)context.getResources().getDimension(R.dimen.recommend_contacts_item_width);
        mChildHeight = (int)context.getResources().getDimension(R.dimen.recommend_contacts_item_height);
        mChildMargin = (int)context.getResources().getDimension(R.dimen.recommend_contacts_item_margin);
        mChildPadding = (int)context.getResources().getDimension(R.dimen.recommend_contacts_item_padding);
        mLayoutInflater = LayoutInflater.from( context );
    }

    public void setActivity(ComposeMessageActivity activity) {
        mActivity = activity;
    }

    public int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    /**
     * 负责设置子控件的测量模式和大小 根据所有子控件设置自己的宽和高
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 获得它的父容器为它设置的测量模式和大小
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        Log.e(TAG, sizeWidth + "," + sizeHeight);

        // 如果是warp_content情况下，记录宽和高
        int width = 0;
        int height = 0;
        /**
         * 记录每一行的宽度，width不断取最大宽度
         */
        int lineWidth = 0;
        /**
         * 每一行的高度，累加至height
         */
        int lineHeight = 0;

        int cCount = getChildCount();

        // 遍历每个子元素
        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);
            // 测量每一个child的宽和高
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            // 得到child的lp
            MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
            // 当前子空间实际占据的宽度
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            // 当前子空间实际占据的高度
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            /**
             * 如果加入当前child，则超出最大宽度，则的到目前最大宽度给width，类加height 然后开启新行
             */
            if (lineWidth + childWidth > sizeWidth) {
                width = Math.max(lineWidth, childWidth);// 取最大的
                lineWidth = childWidth; // 重新开启新行，开始记录
                // 叠加当前高度，
                height += lineHeight;
                // 开启记录下一行的高度
                lineHeight = childHeight;
            } else {
                // 否则累加值lineWidth,lineHeight取最大高度
                lineWidth += childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }
            // 如果是最后一个，则将当前记录的最大宽度和当前lineWidth做比较
            if (i == cCount - 1) {
                width = Math.max(width, lineWidth);
                height += lineHeight;
            }
        }
        setMeasuredDimension((modeWidth == MeasureSpec.EXACTLY) ? sizeWidth : width,
                (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight : height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mAllViews.clear();
        mLineHeight.clear();

        int width = getWidth();

        int lineWidth = 0;
        int lineHeight = 0;
        // 存储每一行所有的childView
        List<View> lineViews = new ArrayList<View>();
        int cCount = getChildCount();
        // 遍历所有的孩子，mRecipientsEditor放在最后面
        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            // 如果已经需要换行
            if (childWidth + lp.leftMargin + lp.rightMargin + lineWidth > width) {
                // 记录这一行所有的View以及最大高度
                mLineHeight.add(lineHeight);
                // 将当前行的childView保存，然后开启新的ArrayList保存下一行的childView
                mAllViews.add(lineViews);
                lineWidth = 0;// 重置行宽
                lineViews = new ArrayList<View>();
            }
            /**
             * 如果不需要换行，则累加
             */
            lineWidth += childWidth + lp.leftMargin + lp.rightMargin;
            lineHeight = Math.max(lineHeight, childHeight + lp.topMargin + lp.bottomMargin);
            lineViews.add(child);
        }
        // 记录最后一行
        mLineHeight.add(lineHeight);
        mAllViews.add(lineViews);

        int left = 0;
        int top = 0;
        // 得到总行数
        int lineNums = mAllViews.size();
        for (int i = 0; i < lineNums; i++) {
            // 每一行的所有的views
            lineViews = mAllViews.get(i);
            // 当前行的最大高度
            lineHeight = mLineHeight.get(i);

            Log.e(TAG, "第" + i + "行 ：" + lineViews.size() + " , " + lineViews);
            Log.e(TAG, "第" + i + "行， ：" + lineHeight);

            // 遍历当前行所有的View
            for (int j = 0; j < lineViews.size(); j++) {
                View child = lineViews.get(j);
                if (child.getVisibility() == View.GONE) {
                    continue;
                }
                MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();

                // 计算childView的left,top,right,bottom
                int lc = left + lp.leftMargin;
                int tc = top + lp.topMargin;
                int rc = lc + child.getMeasuredWidth();
                int bc = tc + child.getMeasuredHeight();

                Log.e(TAG, child + " , l = " + lc + " , t = " + t + " , r =" + rc + " , b = " + bc);

                child.layout(lc, tc, rc, bc);

                left += child.getMeasuredWidth() + lp.rightMargin + lp.leftMargin;
            }
            left = 0;
            top += lineHeight;
        }
    }

    @Override
    protected void onFinishInflate() {
        // TODO Auto-generated method stub
        super.onFinishInflate();
    }

    public void addTextViewChild(String name, String number, boolean isContact) {
        Context context = getContext();
        /*LayoutInflater inflater = LayoutInflater.from(context);
        TextView tv = (TextView)inflater.inflate(R.layout.recipients_editor_flowlayout_child, null);*/
        //tv.setTextAppearance(context, R.style.recipients_editor_tv_style);
        /*TextView tv = new TextView(context);
        //MarginLayoutParams lp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);//(MarginLayoutParams)tv.getLayoutParams();
        MarginLayoutParams lp = new MarginLayoutParams(mChildWidth, mChildHeight);
        lp.width = LayoutParams.WRAP_CONTENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        //lp.bottomMargin = mChildMargin;
        lp.leftMargin = mChildMargin;
        lp.topMargin = mChildMargin;
        //lp.rightMargin = mChildMargin;
        tv.setLayoutParams(lp);
        tv.setPadding( mChildPadding, mChildPadding, mChildPadding, mChildPadding );
        tv.setBackgroundResource(R.color.recommend_contacts_item_enable_bg);
        tv.setTextColor(R.color.recommend_contacts_item_text_color);
        tv.setTextSize( mChildTextSize );
        tv.setSingleLine(true);
        if(isContact) {
            tv.setEllipsize(TruncateAt.END);
        }else {
            tv.setEllipsize(TruncateAt.MIDDLE);
        }
        ChildInfo childInfo = new ChildInfo( name, number, false, isContact ) ;
        tv.setText(name);
        tv.setTag(childInfo);
        tv.setEnabled( true );
        tv.setGravity( Gravity.CENTER );*/
        TextView tv = (TextView) mLayoutInflater.inflate( R.layout.zzz_recommend_child_textview, null );
        MarginLayoutParams lp = new MarginLayoutParams(mChildWidth, mChildHeight);
        //lp.bottomMargin = mChildMargin;
        lp.leftMargin = mChildMargin;
        lp.topMargin = mChildMargin;
        //lp.rightMargin = mChildMargin;
        tv.setLayoutParams(lp);
        if(isContact) {
            tv.setEllipsize(TruncateAt.END);
        }else {
            tv.setEllipsize(TruncateAt.MIDDLE);
        }
        Contact contact = Contact.get( number, false );
        mContactList.add( contact );
        ChildInfo childInfo = new ChildInfo(  false, contact) ;
        tv.setText(name);
        tv.setTag(childInfo);
        tv.setEnabled( true );
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if(!(view instanceof TextView)) {
                    return;
                }
                //TextView child = (TextView)view;
                if(mActivity != null) {
                    if (mActivity.recipientCount() >= RECIPIENTS_LIMIT_FOR_SMS) {
                        Toast.makeText(mActivity, R.string.cannot_add_recipient, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                ChildInfo childInfo = (ChildInfo)view.getTag();
                if(childInfo.mIsAddToRecipient) {
                    return;
                } else {
                    childInfo.mIsAddToRecipient = true;
                    view.setBackgroundResource(R.drawable.zzz_recommend_item_disable_background);
                    view.setEnabled( false );
                }
                if(mOnChildListener != null) {
                    mOnChildListener.onClick(view, childInfo.mContact);
                }
                //removeView(view);
            }
        });
        addView(tv);
    }

    public void addTextViewChild(Contact contact) {
        Context context = getContext();
        TextView tv = (TextView) mLayoutInflater.inflate( R.layout.zzz_recommend_child_textview, null );
        MarginLayoutParams lp = new MarginLayoutParams(mChildWidth, mChildHeight);
        //lp.bottomMargin = mChildMargin;
        lp.leftMargin = mChildMargin;
        lp.topMargin = mChildMargin;
        //lp.rightMargin = mChildMargin;
        tv.setLayoutParams(lp);
        if(contact.existsInDatabase()) {
            tv.setEllipsize(TruncateAt.END);
        }else {
            tv.setEllipsize(TruncateAt.MIDDLE);
        }
        mContactList.add( contact );
        ChildInfo childInfo = new ChildInfo(  false, contact ) ;
        tv.setText(contact.getName());
        tv.setTag(childInfo);
        tv.setEnabled( true );
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                if(!(view instanceof TextView)) {
                    return;
                }
                //TextView child = (TextView)view;
                if(mActivity != null) {
                    if (mActivity.recipientCount() >= RECIPIENTS_LIMIT_FOR_SMS) {
                        Toast.makeText(mActivity, R.string.cannot_add_recipient, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                ChildInfo childInfo = (ChildInfo)view.getTag();
                if(childInfo.mIsAddToRecipient) {
                    return;
                } else {
                    childInfo.mIsAddToRecipient = true;
                    view.setBackgroundResource(R.drawable.zzz_recommend_item_disable_background);
                    view.setEnabled( false );
                }
                if(mOnChildListener != null) {
                    mOnChildListener.onClick(view, childInfo.mContact);
                }
                //removeView(view);
            }
        });
        addView(tv);
    }

    public boolean isChildContactExisted(Contact contact, boolean makeDisable) {
        if(contact == null) {
            return false;
        }
        String number = contact.getNumber();
        if (TextUtils.isEmpty( number )) {
            return false;
        }
        /*for(Contact c : mContactList) {
            if( number.equals( c.getNumber() )) {
                if(makeDisable) {
                    for(int i = 0;i < getChildCount();i++) {
                        View view = getChildAt( i );
                        ChildInfo info = (ChildInfo) view.getTag();
                        if(number.equals( info.mContact.getNumber() )) {
                            view.setBackgroundResource(R.drawable.zzz_recommend_item_disable_background);
                            view.setEnabled( false );
                        }
                    }
                }
                return true;
            }
        }*/
        for(int i = 0;i < getChildCount();i++) {
            View view = getChildAt( i );
            ChildInfo info = (ChildInfo) view.getTag();
            if(number.equals( info.mContact.getNumber() )) {
                if(makeDisable) {
                    info.mIsAddToRecipient = true;
                    view.setBackgroundResource(R.drawable.zzz_recommend_item_disable_background);
                    view.setEnabled( false );
                } else {
                    info.mIsAddToRecipient = false;
                    view.setBackgroundResource(R.drawable.zzz_recommend_item_enable_background);
                    view.setEnabled( true );
                }
                return true;
            }
        }
        return false;
    }

    private OnChildListener mOnChildListener;
    public interface OnChildListener {
        void onClick(View view, Contact contact);
        //when recipient remove the recommend contact,it will call back
        //void onRemoveCallback();
    }

    public void setOnChildListener(OnChildListener listener) {
        mOnChildListener = listener;
    }

    //number will be only
    public void removeCallBack(String number) {
        if (TextUtils.isEmpty( number )) {
            return;
        }
       /* for(int i = 0; i < getChildCount(); i++) {
            (number.equals( object )) {
                break;
            }
        }*/
    }
    /*public String removeLastChildView() {
        String number = null;
        for(int i = getChildCount() - 1; i > -1; i--) {
            View child = getChildAt(i);
            if(child instanceof RecipientsEditor) {
                continue;
            } else {
                number = (String)child.getTag();
                if(mOnClickTVChildListener != null) {
                    mOnClickTVChildListener.onClick(child);
                }
                removeView(child);
                break;
            }
        }
        if(getChildCount() == 1) {
            mRecipientsEditor.setHint(R.string.to_hint);
        }
        return number;
    }*/

    private class ChildInfo {
        //if not contact,mname is equal mNumber
        /*public String mName;
        public String mNumber;*/
        public boolean mIsAddToRecipient;
        public Contact mContact;
        //public boolean mIsContact;
        public ChildInfo(/*String name, String number, */boolean isAddToRecipient/*, boolean isContact*/, Contact contact) {
            /*this.mName = name;
            this.mNumber = number;*/
            this.mIsAddToRecipient = isAddToRecipient;
            mContact = contact;
            //this.mIsContact = isContact;
        }
    }
}