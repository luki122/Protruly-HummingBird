package com.android.mms.ui;

/*tangyisen*/
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.LinearGradient;
import android.graphics.Rect;
//import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.ui.ZzzRecipientsAutoCompleteEditor.OnDropDownListener;
import com.android.mms.util.MmsLog;
import hb.app.dialog.AlertDialog;
import com.zzz.provider.Telephony.Mms;

public class ZzzRecipientsFlowLayout extends ViewGroup implements OnEditorActionListener {

    private static final String TAG = "ZzzRecipientsFlowLayout";

    private static final int RECIPIENTS_LIMIT_FOR_SMS = MmsConfig.getSmsRecipientLimit();
    /**
     * 存储所有的View，按行记录
     */
    private List<List<View>> mAllViews = new ArrayList<List<View>>();

    private int mChildMargin;
    private int mChildHeight;//linearlayout
    private int mChildEditorMinWidth;
    private int mChildEditorMaxWidth;
    private int mDropDownVerticalOffset;
    /**
     * 记录每一行的最大高度
     */
    private List<Integer> mLineHeight = new ArrayList<Integer>();
    private ZzzRecipientsAutoCompleteEditor mRecipientsEditor;

    private ZzzRecipientsAdapter mZzzRecipientsAdapter;

    LayoutInflater mLayoutInflater;

    private ContactList mContactList = new ContactList();
    Context mContext;

    /*private ArrayList<TextView> mTextView = new ArrayList<>();
    private ArrayList<>*/
    public ZzzRecipientsFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mChildMargin = (int)context.getResources().getDimension(R.dimen.recipients_editor_item_margin);
        mChildHeight = (int)context.getResources().getDimension(R.dimen.recipients_editor_item_height);
        mChildEditorMinWidth = (int)context.getResources().getDimension(R.dimen.recipients_editor_min_width);
        mChildEditorMaxWidth = (int)context.getResources().getDimension(R.dimen.recipients_editor_max_width);
        mDropDownVerticalOffset = (int)context.getResources().getDimension(R.dimen.recipients_editor_popupwindow_vertical_offset);
        mLayoutInflater = LayoutInflater.from( context );
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(mContext, attrs);
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
        if(modeWidth == 0) {//beacuse I know its MATCH_PARENT,so it must be AT_MOST
            return;
        }

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
        //记录总共行数
        int lines = 1;

        int cCount = getChildCount();

        // 遍历每个子元素
        View lastView = null;
        for (int i = 0; i < cCount; i++) {
            View child = getChildAt(i);
            if (child instanceof AutoCompleteTextView) {
                lastView = child;
                continue;
            }
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
            if (lineWidth != 0){//说明这一行有至少一个child,其margin是这行的child个数-1
                lineWidth += mChildMargin;
            }
            if (lineWidth + childWidth > sizeWidth) {
                width = Math.max(lineWidth, childWidth);// 取最大的
                lineWidth = childWidth; // 重新开启新行，开始记录
                // 叠加当前高度，
                height += lineHeight;
                // 开启记录下一行的高度
                lineHeight = childHeight;
                lines ++;
                if(lines == 2) {
                    mOnMultiLineListener.onMultiLine(true);
                }
            } else {
                // 否则累加值lineWidth,lineHeight取最大高度
                lineWidth += childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }
            // 如果是最后一个，则将当前记录的最大宽度和当前lineWidth做比较
            /*if (i == cCount - 1) {
                width = Math.max(width, lineWidth);
                height += lineHeight;
                if(lines > 1) {
                    height += (lines - 1) * mChildMargin;
                }
            }*/
        }
        if(lastView != null) {
            //if(getChildCount() == 1) {
            //measureLastChild(lastView, widthMeasureSpec, heightMeasureSpec, mChildEditorMaxWidth);
            measureChild(lastView, widthMeasureSpec, heightMeasureSpec);
            MarginLayoutParams lp = (MarginLayoutParams)lastView.getLayoutParams();
            int childWidth = lastView.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            // 当前子空间实际占据的高度
            int childHeight = lastView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            if (lineWidth != 0){//说明这一行有至少一个child,其margin是这行的child个数-1
                lineWidth += mChildMargin;
            }
            if (lineWidth + childWidth > sizeWidth) {
                //width = Math.max(lineWidth, childWidth);// 取最大的
                lineWidth = childWidth; // 重新开启新行，开始记录
                // 叠加当前高度，
                height += lineHeight;
                // 开启记录下一行的高度
                lineHeight = childHeight;
                lines ++;
                if(lines == 2) {
                    mOnMultiLineListener.onMultiLine(true);
                }
                //tangtang
                if(mIsInput) {
                    lastView.setMinimumWidth( sizeWidth - lp.leftMargin - lp.rightMargin);
                    mIsInput = false;
                }
                //tangtang
            } else {
                // 否则累加值lineWidth,lineHeight取最大高度
                //tangtang
                if(mIsInput) {
                    lastView.setMinimumWidth( sizeWidth - lineWidth - lp.leftMargin - lp.rightMargin);
                    mIsInput = false;
                }
                //tangtang
                lineWidth += childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }
            width = Math.max(width, lineWidth);
            height += lineHeight;
            if(lines > 1) {
                height += (lines - 1) * mChildMargin;
            }
            /*} else {
                measureLastChild(lastView, widthMeasureSpec, heightMeasureSpec, LayoutParams.WRAP_CONTENT);
                MarginLayoutParams lp2 = (MarginLayoutParams)lastView.getLayoutParams();
                int childWidth = lastView.getMeasuredWidth() + lp2.leftMargin + lp2.rightMargin;
                // 当前子空间实际占据的高度
                int childHeight2 = lastView.getMeasuredHeight() + lp2.topMargin + lp2.bottomMargin;

                if (lineWidth != 0){//说明这一行有至少一个child,其margin是这行的child个数-1
                    lineWidth += mChildMargin;
                }
                int ccwidth = Math.max(mChildEditorMinWidth, childWidth); 
                if (lineWidth + ccwidth > sizeWidth) {
                    //width = Math.max(lineWidth, childWidth);// 取最大的
                    //lineWidth = childWidth; // 重新开启新行，开始记录
                    // 叠加当前高度，
                    height += lineHeight;
                    // 开启记录下一行的高度
                    lineHeight = childHeight2;
                    lines ++;
                    if(lines == 2) {
                        mOnMultiLineListener.onMultiLine(true);
                    }
                    lastView.setMinimumWidth(mChildEditorMinWidth);
                    measureLastChild(lastView, widthMeasureSpec, heightMeasureSpec, mChildEditorMaxWidth);
                } else {
                    // 否则累加值lineWidth,lineHeight取最大高度
                    //lineWidth += childWidth;
                    lineHeight = Math.max(lineHeight, childHeight2);
                    lastView.setMinimumWidth( sizeWidth - lineWidth - lp2.leftMargin - lp2.rightMargin);
                    //WARP_CONTENT
                    measureLastChild(lastView, widthMeasureSpec, heightMeasureSpec, LayoutParams.WRAP_CONTENT);
                }
                width = sizeWidth;
                height += lineHeight;
                if(lines > 1) {
                    height += (lines - 1) * mChildMargin;
                }
            }*/

        }
        if(lines == 1) {
            mOnMultiLineListener.onMultiLine(false);
        }
        setMeasuredDimension((modeWidth == MeasureSpec.EXACTLY) ? sizeWidth : width,
                (modeHeight == MeasureSpec.EXACTLY) ? sizeHeight : height);
    }

    private void measureLastChild(View child, int parentWidthMeasureSpec,
                                  int parentHeightMeasureSpec, int childWidth) {
        final LayoutParams lp = child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight, childWidth);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private boolean mIsInput = true;
    public void setIsEidtorInput(boolean flag) {
        mIsInput = flag;
    }
    public boolean getIsEditorInput() {
        return mIsInput;
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
            if (child instanceof AutoCompleteTextView) {
                continue;
            }
            MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (lineWidth != 0){//说明这一行有至少一个child,其margin是这行的child个数-1
                lineWidth += mChildMargin;
            }
            // 如果已经需要换行
            if (childWidth + lp.leftMargin + lp.rightMargin + lineWidth > width) {
                if(!mLineHeight.isEmpty()) {//说明这个不是第一行，则都需要添加一个margin
                    mLineHeight.add(lineHeight + mChildMargin);
                } else {
                    mLineHeight.add(lineHeight);
                }
                // 记录这一行所有的View以及最大高度
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
        if(mRecipientsEditor != null) {
            MarginLayoutParams lp = (MarginLayoutParams)mRecipientsEditor.getLayoutParams();
            int childWidth = mRecipientsEditor.getMeasuredWidth();
            int childHeight = mRecipientsEditor.getMeasuredHeight();

            // 如果已经需要换行
            if (childWidth + lp.leftMargin + lp.rightMargin + lineWidth > width) {
                // 记录这一行所有的View以及最大高度
                if(!mLineHeight.isEmpty()) {//说明这个不是第一行，则都需要添加一个margin
                    mLineHeight.add(lineHeight + mChildMargin);
                } else {
                    mLineHeight.add(lineHeight);
                }
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
            lineViews.add(mRecipientsEditor);
        }
        // 记录最后一行
        if(!mLineHeight.isEmpty()) {//说明这个不是第一行，则都需要添加一个margin
            mLineHeight.add(lineHeight + mChildMargin);
        } else {
            mLineHeight.add(lineHeight);
        }
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
                /*if (j > 0) {
                    lc += mChildMargin;
                    rc += mChildMargin;
                }*/
                if (i > 0) {
                    tc += mChildMargin;
                    bc += mChildMargin;
                }
                Log.e(TAG, child + " , l = " + lc + " , t = " + t + " , r =" + rc + " , b = " + bc);
                child.layout(lc, tc, rc, bc);
                left += child.getMeasuredWidth() + lp.rightMargin + lp.leftMargin + mChildMargin;
            }
            left = 0;
            top = top + lineHeight/* + (i - 1) * mChildMargin*/;
        }
    }

    private ZzzMmsToolbar mToolbar;
    public void setToolBar(ZzzMmsToolbar toolbar) {
        mToolbar = toolbar;
        mRecipientsEditor.setDropDownAnchor(mToolbar.getId());
        mRecipientsEditor.setDropDownVerticalOffset(mDropDownVerticalOffset);
    }
    @Override
    protected void onFinishInflate() {
        // TODO Auto-generated method stub
        super.onFinishInflate();
        mRecipientsEditor = (ZzzRecipientsAutoCompleteEditor) findViewById(R.id.recipients_editor);
        mRecipientsEditor.setFlowLayout( this );
        mRecipientsEditor.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mRecipientsEditor.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        //int screenHeight = getResources().getDisplayMetrics().heightPixels;
        mRecipientsEditor.setDropDownWidth(screenWidth);
        mRecipientsEditor.setDropDownBackgroundResource(R.drawable.zzz_recipients_editor_dropdown_bg);
        //mRecipientsEditor.setDropDownHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        //mRecipientsEditor.setDropDownAnchor(((View) getParent()).getId());
        //mRecipientsEditor.setDropDownVerticalOffset((int)getResources().getDimension(R.dimen.compose_message_edit_vertical_offset));

        mRecipientsEditor.setThreshold(1);
        mRecipientsEditor.setOnEditorActionListener(this);
        //fix bug642
        if(mZzzRecipientsAdapter == null) {
            mZzzRecipientsAdapter = new ZzzRecipientsAdapter(mContext);
            mZzzRecipientsAdapter.setAccount( new Account( MessageUtils.ACCOUNT_NAME_LOCAL_PHONE, MessageUtils.ACCOUNT_TYPE_LOCAL_PHONE ) );
        }
        mRecipientsEditor.setAdapter(mZzzRecipientsAdapter);
        mRecipientsEditor.setOnFocusChangeListener( new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TODO Auto-generated method stub
                if(mFoucsChangeListener != null) {
                    mFoucsChangeListener.onFocusChange( ZzzRecipientsFlowLayout.this, hasFocus );
                }
            }
        } );

        mRecipientsEditor.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (isCanRemoveChildView && keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_UP) {
                    removeLastChildView();
                    return true;
                }
                if(!isCanRemoveChildView && TextUtils.isEmpty( mRecipientsEditor.getText() )) {
                    isCanRemoveChildView = true;
                }
                return false;
            }
        });
    }

    private boolean isCanRemoveChildView = true;
    public void setRemoveFlag(boolean flag) {
        isCanRemoveChildView = flag;
    }

    private int mMmsRecipientCount = 0;
    //return: if true,means add success,fals,means add failed
    public boolean addFlowChild(String name, String number/*, boolean isContact*/) {
        Context context = mContext;
        //tv.setTextAppearance(context, R.style.recipients_editor_tv_style);
        if(TextUtils.isEmpty( number )) {
            return false;
        }
        if (mContactList.size() >= RECIPIENTS_LIMIT_FOR_SMS) {
            Toast.makeText(mContext, R.string.cannot_add_recipient, Toast.LENGTH_SHORT).show();
            mRecipientsEditor.setText( null );
            mRecipientsEditor.setHint(null);
            return false;
        }
        mRecipientsEditor.setText( null );
        Contact add = addContact(number);
        if (add == null) {
            //it means it is invalid or duplicated
            if(mContactList.isEmpty()) {
                mRecipientsEditor.setHint(R.string.to_hint);
            }
            return false;
        }
        //tangtang
        mRecipientsEditor.setMinimumWidth( mChildEditorMinWidth );
        mIsInput = true;
        //mRecipientsEditor.setMinimumWidth( mChildEditorMinWidth );
        //tangtang
        LinearLayout parent = (LinearLayout)mLayoutInflater.inflate( R.layout.zzz_recipients_editor_flowlayout_child, null );
        TextView childTv = (TextView) parent.findViewById( R.id.recipient_editor_item_content );
        ImageView childImg = (ImageView) parent.findViewById( R.id.recipient_editor_item_img );
        MarginLayoutParams lp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, mChildHeight);//(MarginLayoutParams)tv.getLayoutParams();
        /*lp.width = LayoutParams.WRAP_CONTENT;
        lp.height = LayoutParams.WRAP_CONTENT;*/
        /*lp.bottomMargin = tvChildTextMargin;
        lp.leftMargin = tvChildTextMargin;
        lp.topMargin = tvChildTextMargin;
        lp.rightMargin = tvChildTextMargin;*/
        parent.setLayoutParams(lp);
        //String contactName = add.getRealName()
        childTv.setText(add.getName());
        parent.setTag( add );
        //childTv.setTag(number);
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                removeZzzView(view);
            }
        });
        addView(parent);
        if(null != mOnChildAddedListener) {
            mOnChildAddedListener.onAdded( parent, add );
        }
        if(Mms.isEmailAddress(number)) {
            mMmsRecipientCount ++;
            mOnChildAddedListener.onMmsAdded(true);
        }
        if(mRecipientsEditor != null && mRecipientsEditor.getHint() != null) {
            mRecipientsEditor.setHint(null);
        }
        return true;
    }

    public Contact addContact(String number) {
        if(TextUtils.isEmpty( number )) {
            return null;
        }
        Contact contact = findDuplicatePhone(number);
        if (contact == null) {
            contact = Contact.get(number, true);
            if(contact.existsInDatabase() || !isInvalidRecipient( true, number ) ) {
                mContactList.add(contact);
                return contact;
            }
        } /*else {
            //Toast.makeText(mContext, R.string.remove_duplicate_contact, Toast.LENGTH_SHORT).show();
            return null;
        }*/
        return null;
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if(hasFocus) {
            mRecipientsEditor.requestFocus();
        }/* else {
            ensureAddContact();
        }*/
    }

    public ContactList constructContactsFromInput(boolean blocking) {
        ContactList newList = new ContactList();
        /*for (String number : mContactList.getNumbers()) {
            Contact contact = Contact.get(number, blocking);
            contact.setNumber(number);
            newList.add(contact);
        }*/
        newList.addAll(mContactList);
        mContactList = newList;
        return mContactList;
    }

    private Contact findDuplicatePhone(String number) {
        if(TextUtils.isEmpty(number)) {
            return null;
        }
        for(Contact tmp : mContactList) {
            if (getRealNumber(tmp.getNumber()).equals(getRealNumber(number))) {
                return tmp;
            }
        }
        return null;
    }

    private void findInvalidatedContact(String str) {

    }

    public boolean isInvalidRecipient(boolean isMms, String str) {
        if (!isValidAddress(str, isMms)) {
            if (MmsConfig.getEmailGateway() == null) {
                return true;
            } else if (!MessageUtils.isAlias(str)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidAddress(String number, boolean isMms) {
        boolean rtn;
        if (isMms) {
            rtn = MessageUtils.isValidMmsAddress(number);
        } else {
            // TODO: PhoneNumberUtils.isWellFormedSmsAddress() only check if the number is a valid
            // GSM SMS address. If the address contains a dialable char, it considers it a well
            // formed SMS addr. CDMA doesn't work that way and has a different parser for SMS
            // address (see CdmaSmsAddress.parse(String address)). We should definitely fix this!!!
            boolean commonValidValue = MessageUtils.isWellFormedSmsAddress(number.replaceAll(" |-", ""))
                    || Mms.isEmailAddress(number);//tangyisen add 
            rtn = commonValidValue;
        }
        rtn = rtn && canAddToContacts(number);
        return rtn;
    }

    private boolean canAddToContacts(String number) {
        // There are some kind of automated messages, like STK messages, that we don't want
        // to add to contacts. These names begin with special characters, like, "*Info".
        if (!TextUtils.isEmpty(number)) {
            char c = number.charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!TextUtils.isEmpty(number)) {
            char c = number.charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!(Mms.isEmailAddress(number) ||
                Mms.isPhoneNumber(number))) {
            return false;
        }
        return true;
    }

    private boolean isSpecialChar(char c) {
        return c == '*' || c == '%' || c == '$';
    }

    public void removeZzzView(View view) {
        mRecipientsEditor.setMinimumWidth( mChildEditorMinWidth );
        mIsInput = true;
        removeView(view);
        /*mContactList.remove( index )*/
        Contact tag = (Contact)view.getTag();
        mContactList.remove( tag );
        if(tag.isEmail()) {
            mMmsRecipientCount --;
            if(mMmsRecipientCount < 1) {
                Log.v(TAG, "mMmsRecipientCount = " + mMmsRecipientCount);
                mMmsRecipientCount = 0;
                mOnChildAddedListener.onMmsAdded( false );
            }
        }
        if(mContactList.isEmpty()) {
            mRecipientsEditor.setHint( R.string.to_hint );
        } else {
            mRecipientsEditor.setHint( null );
        }
        if(mOnClickChildListener != null) {
            Contact contact = (Contact)view.getTag();
            mOnClickChildListener.onClick(view, contact);
        }
    }

    public void requestContentFocus() {
        mRecipientsEditor.requestFocus();
        showInputMethod(mRecipientsEditor);
    }

    public boolean hasValidRecipient(boolean isMms) {
        if(mContactList.size() != 0) {
            return true;
        }
        return !isInvalidRecipient(isMms, mRecipientsEditor.getText().toString());
    }

    public boolean hasInvalidRecipient(boolean isMms) {
        return false;
    }

    private void showInputMethod(View view) {
        //View focusView = this.getCurrentFocus();
        final InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            //imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            imm.showSoftInput(view, 0);
        }
    }

    public void clearContentFocus() {
        mRecipientsEditor.clearFocus();
    }

    public void setIsTouchable(boolean touchable) {

    }

    public static final int LIMIT_RECIPIENTS_HINT = 20;
    public static final int DELAY_LOAD_TIME = 500;
    private AlertDialog mPickContactsProgressDialog;

    private Runnable mShowPickingContactsProgressDialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPickContactsProgressDialog == null) {
                mPickContactsProgressDialog = createPickingContactsProgressDialog();
            }
            mPickContactsProgressDialog.show();
        }
    };

    private void dismissPickingContactsProgressDialog() {
        if (mPickContactsProgressDialog != null && mPickContactsProgressDialog.isShowing()) {
            mPickContactsProgressDialog.dismiss();
        }
    }

    private AlertDialog createPickingContactsProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mContext.getText(R.string.populate_recipients_too_more_to_load_hint));
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
    }

    public void populate(final ContactList list) {
        if(list != null && list.size() > LIMIT_RECIPIENTS_HINT) {
            if(mShowPickingContactsProgressDialogRunnable != null) {
                mShowPickingContactsProgressDialogRunnable.run();
            }
            postDelayed( new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    populateInner(list);
                    dismissPickingContactsProgressDialog();
                }
            }, DELAY_LOAD_TIME );
        } else {
            populateInner(list);
        }
    }

    public void populate(final ContactList list, final Runnable r) {
        if(list != null && list.size() > LIMIT_RECIPIENTS_HINT) {
            if(mShowPickingContactsProgressDialogRunnable != null) {
                mShowPickingContactsProgressDialogRunnable.run();
            }
            postDelayed( new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    populateInner(list);
                    dismissPickingContactsProgressDialog();
                    if (r != null) {
                        r.run();
                    }
                }
            }, DELAY_LOAD_TIME );
        } else {
            populateInner(list);
            if (r != null) {
                r.run();
            }
        }
    }

    private void populateInner(ContactList list) {
        mContactList.clear();
        removeAllViewsExcludeRecipientsEditor();
        if (list.size() == 0) {
            if(mContactList.isEmpty()) {
                mRecipientsEditor.setHint(R.string.to_hint);
            } else {
                mRecipientsEditor.setText( null );
                mRecipientsEditor.setHint(null);
            }
        } else {
            for (Contact c : list) {
                String displayName = c.getName();
                String number = c.getNumber();
                /*if (!TextUtils.isEmpty(displayName) && displayName.equals(c.getNumber())) {
                    displayName = "";
                }*/
                //mContactList.add( c );
                if(TextUtils.isEmpty( displayName )) {
                    displayName = number;
                }
                addFlowChild( displayName, number );
            }
        }
        if(mOnChildAddedListener != null) {
            mOnChildAddedListener.onPopulateDone();
        }
    }

    public void populate(ContactList list, boolean clear) {
        if(clear) {
            mContactList.clear();
            removeAllViewsExcludeRecipientsEditor();
        }
        if (list.size() == 0) {
            if(mContactList.isEmpty()) {
                mRecipientsEditor.setHint(R.string.to_hint);
            } else {
                mRecipientsEditor.setText( null );
                mRecipientsEditor.setHint(null);
            }
        } else {
            for (Contact c : list) {
                String displayName = c.getName();
                String number = c.getNumber();
                /*if (!TextUtils.isEmpty(displayName) && displayName.equals(c.getNumber())) {
                    displayName = "";
                }*/
                //mContactList.add( c );
                if(TextUtils.isEmpty( displayName )) {
                    displayName = number;
                }
                addFlowChild( displayName, number );
            }
        }
        if(mOnChildAddedListener != null) {
            mOnChildAddedListener.onPopulateDone();
        }
    }

    public void removeLastChildView() {
        for(int i = getChildCount() - 1; i > -1; i--) {
            View child = getChildAt(i);
            if(child instanceof AutoCompleteTextView) {
                continue;
            } else {
                removeZzzView(child);
                break;
            }
        }
    }

    private void removeAllViewsExcludeRecipientsEditor() {
        mMmsRecipientCount = 0;
        ArrayList<View> removeView = new ArrayList<View>();
        for(int i = 0;i < getChildCount(); i++) {
            View child = getChildAt( i );
            if(!(child instanceof ZzzRecipientsAutoCompleteEditor)) {
                removeView.add( child );
            }
        }
        for(View view : removeView) {
            removeView(view);
        }
    }

    public int getRecipientCount() {
        return mContactList.size();
    }

    public boolean hasRecipients() {
        return getRecipientCount() > 0 || !TextUtils.isEmpty( mRecipientsEditor.getText() );
    }

    public boolean needAddChild() {
        return !TextUtils.isEmpty( mRecipientsEditor.getText());
    }

    public List<String> getNumbers() {
        return mContactList.getNumbersList();
    }

    private Contact findDuplicatePhone(String phone, String name) {
        if(TextUtils.isEmpty(phone)) {
            return null;
        }
        for(Contact tmp : mContactList) {
            if (getRealNumber(tmp.getNumber()).equals(getRealNumber(phone))
                    && tmp.getName().equals(name)) {
                return tmp;
            }
        }
        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // TODO Auto-generated method stub
        super.setEnabled( enabled );
        mRecipientsEditor.setEnabled( enabled );
    }

    @Override
    public void setFocusable(boolean focusable) {
        // TODO Auto-generated method stub
        super.setFocusable( focusable );
        mRecipientsEditor.setFocusable( focusable );
    }

    @Override
    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        // TODO Auto-generated method stub
        super.setFocusableInTouchMode( focusableInTouchMode );
        mRecipientsEditor.setFocusableInTouchMode( focusableInTouchMode );
    }

    private String getRealNumber(String number) {
        return new String(number).replace("-", "").replace(" ", "");
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // TODO Auto-generated method stub
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            /*dismissDropDown();
            ensureAddContact();
            if(mOnEditorOperateListener != null) {
                mOnEditorOperateListener.onEditorDoneClick(getText().toString());
            }*/
            addFlowChild( mRecipientsEditor.getText().toString(), mRecipientsEditor.getText().toString() );
            mRecipientsEditor.requestFocus();
            return true;
        }
        return false;
    }

    public boolean addChild() {
        return addFlowChild( mRecipientsEditor.getText().toString(), mRecipientsEditor.getText().toString() );
    }
    /*public int getValidRecipientsCount(boolean isMms) {
        int validNum = 0;
        int invalidNum = 0;
        for (String number : mContactList.getNumbers()) {//mTokenizer.getNumbers()) {
            if (isValidAddress(number, isMms)) {
                validNum++;
            } else {
                invalidNum++;
            }
        }
        int count = mContactList.size();//mTokenizer.getNumbers().size();
        if (validNum == count) {
            return MessageUtils.ALL_RECIPIENTS_VALID;
        } else if (invalidNum == count) {
            return MessageUtils.ALL_RECIPIENTS_INVALID;
        }
        return invalidNum;

    }
    
    public boolean hasInvalidRecipient(boolean isMms) {
        for (String number : mContactList.getNumbers()) {
            if (!isValidAddress(number, isMms)) {
                if (MmsConfig.getEmailGateway() == null) {
                    return true;
                } else if (!MessageUtils.isAlias(number)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public String formatInvalidNumbers(boolean isMms) {
        StringBuilder sb = new StringBuilder();
        for (String number : mContactList.getNumbers()) {//mTokenizer.getNumbers()) {
            if (!isValidAddress(number, isMms)) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(number);
            }
        }
        return sb.toString();
    }

    public boolean containsEmail() {
        if (TextUtils.indexOf(getText(), '@') == -1)
            return false;

        List<String> numbers = mContactList.getNumbersList();//mTokenizer.getNumbers();
        for (String number : numbers) {
            if (Mms.isEmailAddress(number))
                return true;
        }
        return false;
    }*/
    private OnClickChildListener mOnClickChildListener;
    public interface OnClickChildListener {
        void onClick(View view, Contact contact);
    }

    public void setOnClickChildListener(OnClickChildListener listener) {
        mOnClickChildListener = listener;
    }

    private OnChildAddedListener mOnChildAddedListener;
    public interface OnChildAddedListener {
        void onAdded(View view, Contact contact);
        void onMmsAdded(boolean isMms);
        void onPopulateDone();
    }

    public void setOnChildAddedListener(OnChildAddedListener listener) {
        mOnChildAddedListener = listener;
    }

    private OnMultiLineListener mOnMultiLineListener;
    public interface OnMultiLineListener {
        void onMultiLine(boolean isMultiLine);
    }

    public void setOnMultiLineListener(OnMultiLineListener listener) {
        mOnMultiLineListener = listener;
    }

    private FoucsChangeListener mFoucsChangeListener;
    public interface FoucsChangeListener {
        void onFocusChange(View v, boolean hasFocus);
    }

    public void setFoucsChangeListener(FoucsChangeListener listener) {
        mFoucsChangeListener = listener;
    }

    public void addRecipientsTextChangedListener(TextWatcher watcher) {
        mRecipientsEditor.addTextChangedListener( watcher );
    }

    public boolean containsEmail() {
        /*if (TextUtils.indexOf(getText(), '@') == -1)
            return false;

        List<String> numbers = mContactList.getNumbersList();//mTokenizer.getNumbers();
        for (String number : numbers) {
            if (Mms.isEmailAddress(number))
                return true;
        }*/
        return false;
    }

    public void setOnDropDownListener(ZzzRecipientsAutoCompleteEditor.OnDropDownListener listener) {
        mRecipientsEditor.setOnDropDownListener( listener );
    }
}