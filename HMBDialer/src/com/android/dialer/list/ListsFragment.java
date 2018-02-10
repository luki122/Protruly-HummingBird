package com.android.dialer.list;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.AlertDialog.Builder;
import com.android.dialer.yellowpage.YellowPageFragment;
import java.util.ArrayList;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.calllog.CallLogFragment;
import com.android.dialer.calllog.CallLogQueryHandler;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.voicemail.VoicemailStatusHelper;
import com.android.dialer.voicemail.VoicemailStatusHelperImpl;
import com.android.dialer.widget.ActionBarController;
import com.android.dialer.yellowpage.YellowPageFragment;
import com.android.dialer.R;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Trace;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import hb.widget.FragmentPagerAdapter;
import hb.widget.ViewPager;
import hb.widget.ViewPager.OnPageChangeListener;
import hb.widget.tab.TabLayout;
import hb.widget.tab.TabLayout.OnTabSelectedListener;
/**
 * Fragment that is used as the main screen of the Dialer.
 *
 * Contains a ViewPager that contains various contact lists like the Speed Dial list and the
 * All Contacts list. This will also eventually contain the logic that allows sliding the
 * ViewPager containing the lists up above the search bar and pin it against the top of the
 * screen.
 */
public class ListsFragment extends Fragment
implements ViewPager.OnPageChangeListener, CallLogQueryHandler.Listener,
YellowPageFragment.IYellowPageNotification{
	private static final boolean DEBUG = DialtactsActivity.DEBUG;
	private static final String TAG = "ListsFragment";

	//    public static final int TAB_INDEX_SPEED_DIAL = 0;
	//    public static final int TAB_INDEX_RECENTS = 1;
	//    public static final int TAB_INDEX_ALL_CONTACTS = 2;
	//    public static final int TAB_INDEX_VOICEMAIL = 3;
	//
	//    public static final int TAB_COUNT_DEFAULT = 3;
	//    public static final int TAB_COUNT_WITH_VOICEMAIL = 4;

	//    public static final int TAB_INDEX_SPEED_DIAL = 0;
	public static final int TAB_INDEX_RECENTS = 0;
	public static final int TAB_INDEX_ALL_CONTACTS = 1;
	//    public static final int TAB_INDEX_VOICEMAIL = 2;
	public static final int TAB_INDEX_YELLOWPAGE= 2;

	public static final int TAB_COUNT_DEFAULT = 3;
	public static final int TAB_COUNT_WITH_VOICEMAIL = 3;

	private static final int MAX_RECENTS_ENTRIES = 1000;
	// Oldest recents entry to display is 1 year old.
	private static final long OLDEST_RECENTS_DATE = 1000L * 60 * 60 * 24 * 365*3;

	private static final String PREF_KEY_HAS_ACTIVE_VOICEMAIL_PROVIDER =
			"has_active_voicemail_provider";

	public interface HostInterface {
		public ActionBarController getActionBarController();
	}

	private ActionBar mActionBar;
	public HbViewPager mViewPager;
	//    public ViewPagerTabs mViewPagerTabs;
	//    private ImageView mSettingIcon;
	//    private View lists_pager_header_layout;
	public ViewPagerAdapter mViewPagerAdapter;
	private RemoveView mRemoveView;
	private View mRemoveViewContent;

	private SpeedDialFragment mSpeedDialFragment;
	public CallLogFragment mRecentsFragment;
	public CallLogFragment getRecentsFragment(){
		Log.d(TAG, "getRecentsFragment:"+mRecentsFragment);
		return mRecentsFragment;
	}
	public AllContactsFragment mAllContactsFragment;
	private CallLogFragment mVoicemailFragment;
	public YellowPageFragment mYellowPageFragment;

	private SharedPreferences mPrefs;
	private boolean mHasActiveVoicemailProvider;
	private boolean mHasFetchedVoicemailStatus;
	private boolean mShowVoicemailTabAfterVoicemailStatusIsFetched;

	private VoicemailStatusHelper mVoicemailStatusHelper;
	private ArrayList<OnPageChangeListener> mOnPageChangeListeners =
			new ArrayList<OnPageChangeListener>();

	private String[] mTabTitles;
	private int[] mTabIcons;

	/**
	 * The position of the currently selected tab.
	 */
	private int mTabIndex = TAB_INDEX_RECENTS;

	public class ViewPagerAdapter extends FragmentPagerAdapter {
		public ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public long getItemId(int position) {
			return getRtlPosition(position);
		}

		@Override
		public Fragment getItem(int position) {
			Log.d(TAG,"getItem,pos:"+position);
			switch (getRtlPosition(position)) {
			//                case TAB_INDEX_SPEED_DIAL:
			//                    mSpeedDialFragment = new SpeedDialFragment();
			//                    return mSpeedDialFragment;
			case TAB_INDEX_RECENTS:
				mRecentsFragment = new CallLogFragment(
						CallLogQueryHandler.CALL_TYPE_ALL, MAX_RECENTS_ENTRIES,
						System.currentTimeMillis() - OLDEST_RECENTS_DATE,mViewPager);
				/// M: [Call Log Account Filter] don't use Account Filter when
				// viewing recents log
				//                    mRecentsFragment.setAccountFilterState(false);
				return mRecentsFragment;
			case TAB_INDEX_ALL_CONTACTS:
				mAllContactsFragment = new AllContactsFragment();
				return mAllContactsFragment;
				//                case TAB_INDEX_VOICEMAIL:
				//                    mVoicemailFragment = new CallLogFragment(Calls.VOICEMAIL_TYPE);
				//                    /// M: [Call Log Account Filter] don't use Account Filter when
				//                    // viewing logs in main activity
				//                    mVoicemailFragment.setAccountFilterState(false);
				//                    return mVoicemailFragment;
			case TAB_INDEX_YELLOWPAGE:
				mYellowPageFragment=new YellowPageFragment();
				mYellowPageFragment.setYellowPageNotification(ListsFragment.this);
				return mYellowPageFragment;
				//                	return new Fragment();
			}
			throw new IllegalStateException("No fragment at position " + position);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// On rotation the FragmentManager handles rotation. Therefore getItem() isn't called.
			// Copy the fragments that the FragmentManager finds so that we can store them in
			// instance variables for later.
			final Fragment fragment =
					(Fragment) super.instantiateItem(container, position);
			if (fragment instanceof YellowPageFragment) {
				mYellowPageFragment = (YellowPageFragment) fragment;
			} else if (fragment instanceof CallLogFragment) {
				mRecentsFragment = (CallLogFragment) fragment;
			} else if (fragment instanceof AllContactsFragment) {
				mAllContactsFragment = (AllContactsFragment) fragment;
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return mHasActiveVoicemailProvider ? TAB_COUNT_WITH_VOICEMAIL : TAB_COUNT_DEFAULT;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Log.d(TAG,"getPageTitle,posi:"+position);
			//			return mTabTitles[position];
			if(position==2 && showRedPoint){
				String title = mTabTitles[position];
				SpannableString spanString = new SpannableString(title + " *");
				ImageSpan span = new ImageSpan(getActivity(), R.drawable.dot_red);
				spanString.setSpan(span,spanString.length() - 1,spanString.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				return spanString;
			}else return mTabTitles[position];
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Trace.beginSection(TAG + " onCreate");
		super.onCreate(savedInstanceState);

		Trace.beginSection(TAG + " getCurrentCountryIso");
		final String currentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());
		Trace.endSection();

		mVoicemailStatusHelper = new VoicemailStatusHelperImpl();
		mHasFetchedVoicemailStatus = false;

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mHasActiveVoicemailProvider = mPrefs.getBoolean(
				PREF_KEY_HAS_ACTIVE_VOICEMAIL_PROVIDER, false);
		Trace.endSection();
	}

	@Override
	public void onResume() {
		Trace.beginSection(TAG + " onResume");
		super.onResume();
		mActionBar = getActivity().getActionBar();
		if (getUserVisibleHint()) {
			sendScreenViewForCurrentPosition();
		}

		// Fetch voicemail status to determine if we should show the voicemail tab.
		CallLogQueryHandler callLogQueryHandler =
				new CallLogQueryHandler(getActivity(), getActivity().getContentResolver(), this);
		callLogQueryHandler.fetchVoicemailStatus();
		Trace.endSection();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Trace.beginSection(TAG + " onCreateView");
		Trace.beginSection(TAG + " inflate view");
		final View parentView = inflater.inflate(R.layout.hb_lists_fragment, container, false);
		Trace.endSection();
		Trace.beginSection(TAG + " setup views");
		mViewPager = (HbViewPager) parentView.findViewById(R.id.lists_pager);
		Log.d(TAG,"parentview:"+parentView+" mViewPager:"+mViewPager);
		mViewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setOffscreenPageLimit(TAB_COUNT_WITH_VOICEMAIL - 1);
		mViewPager.setOnPageChangeListener(this);
		showTab(TAB_INDEX_RECENTS);

		mTabTitles = new String[TAB_COUNT_WITH_VOICEMAIL];
		//        mTabTitles[TAB_INDEX_SPEED_DIAL] = getResources().getString(R.string.tab_speed_dial);
		mTabTitles[TAB_INDEX_RECENTS] = getResources().getString(R.string.tab_recents);
		mTabTitles[TAB_INDEX_ALL_CONTACTS] = getResources().getString(R.string.tab_all_contacts);
		//        mTabTitles[TAB_INDEX_VOICEMAIL] = getResources().getString(R.string.tab_voicemail);
		mTabTitles[TAB_INDEX_YELLOWPAGE] = getResources().getString(R.string.tab_yellowpage);

		mTabIcons = new int[TAB_COUNT_WITH_VOICEMAIL];
		//        mTabIcons[TAB_INDEX_SPEED_DIAL] = R.drawable.tab_speed_dial;
		mTabIcons[TAB_INDEX_RECENTS] = R.drawable.tab_recents;
		mTabIcons[TAB_INDEX_ALL_CONTACTS] = R.drawable.tab_contacts;
		//        mTabIcons[TAB_INDEX_VOICEMAIL] = R.drawable.tab_voicemail;
		mTabIcons[TAB_INDEX_YELLOWPAGE] = R.drawable.tab_recents;

		//        mViewPagerTabs = (ViewPagerTabs) parentView.findViewById(R.id.lists_pager_header);
		//        mViewPagerTabs.setTabIcons(mTabIcons);
		//        mViewPagerTabs.setViewPager(mViewPager);
		//        addOnPageChangeListener(mViewPagerTabs);

		mRemoveView = (RemoveView) parentView.findViewById(R.id.remove_view);
		mRemoveViewContent = parentView.findViewById(R.id.remove_view_content);

		//        mSettingIcon=(ImageView) parentView.findViewById(R.id.hb_setting_icon);
		//        mSettingIcon.setOnClickListener(new View.OnClickListener() {
		//			
		//			@Override
		//			public void onClick(View v) {
		//				// TODO Auto-generated method stub
		//				((DialtactsActivity) getActivity()).handleMenuSettings();
		//			}
		//		});
		//        lists_pager_header_layout=parentView.findViewById(R.id.lists_pager_header_layout);

		Trace.endSection();
		Trace.endSection();


		View tabView=LayoutInflater.from(getActivity()).inflate(R.layout.hb_dialtactsactivity_tab_layout,null);
		hb.widget.toolbar.Toolbar.LayoutParams params=new hb.widget.toolbar.Toolbar.LayoutParams(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
		params.setMargins(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.hb_tab_layout_margin_bottom));
		((DialtactsActivity)getActivity()).getToolBar().addView(tabView,params);

		tabLayout = (TabLayout) tabView.findViewById(R.id.tab_layout);
		tabLayout.addTab(tabLayout.newTab().setText(mTabTitles[0]));
		tabLayout.addTab(tabLayout.newTab().setText(mTabTitles[1]));
		tabLayout.addTab(tabLayout.newTab().setText(mTabTitles[2]));

		tabLayout.setupWithViewPager(mViewPager);
		tabLayout.setTabsFromPagerAdapter(mViewPagerAdapter);
		tabLayout.setOnTabSelectedListener(onTabSelectedListener);
		return parentView;
	}


	public TabLayout tabLayout;
	private OnTabSelectedListener onTabSelectedListener;
	public void setOnTabSelectedListener(OnTabSelectedListener onTabSelectedListener){
		this.onTabSelectedListener=onTabSelectedListener;
	}
	public void addOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
		if (!mOnPageChangeListeners.contains(onPageChangeListener)) {
			mOnPageChangeListeners.add(onPageChangeListener);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d(TAG, "onViewCreated,indexFrom:"+((DialtactsActivity)getActivity()).indexFrom);
		if(((DialtactsActivity)getActivity()).indexFrom==TAB_INDEX_RECENTS 
				&& ((DialtactsActivity)getActivity()).tabIndex==TAB_INDEX_RECENTS) ((DialtactsActivity)getActivity()).hbShowDialpad();
		Trace.endSection();
	}

	/**
	 * Shows the tab with the specified index. If the voicemail tab index is specified, but the
	 * voicemail status hasn't been fetched, it will try to show the tab after the voicemail status
	 * has been fetched.
	 */
	public void showTab(int index) {
		if (/*index == TAB_INDEX_VOICEMAIL*/false) {
			if (mHasActiveVoicemailProvider) {
				//                mViewPager.setCurrentItem(getRtlPosition(TAB_INDEX_VOICEMAIL));
			} else if (!mHasFetchedVoicemailStatus) {
				// Try to show the voicemail tab after the voicemail status returns.
				mShowVoicemailTabAfterVoicemailStatusIsFetched = true;
			}
		} else {
			mViewPager.setCurrentItem(getRtlPosition(index));
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		mTabIndex = getRtlPosition(position);

		final int count = mOnPageChangeListeners.size();
		for (int i = 0; i < count; i++) {
			mOnPageChangeListeners.get(i).onPageScrolled(position, positionOffset,
					positionOffsetPixels);
		}
	}

	@Override
	public void onPageSelected(int position) {
		mTabIndex = getRtlPosition(position);

		// Show the tab which has been selected instead.
		mShowVoicemailTabAfterVoicemailStatusIsFetched = false;

		final int count = mOnPageChangeListeners.size();
		for (int i = 0; i < count; i++) {
			mOnPageChangeListeners.get(i).onPageSelected(position);
		}
		sendScreenViewForCurrentPosition();
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		final int count = mOnPageChangeListeners.size();
		for (int i = 0; i < count; i++) {
			mOnPageChangeListeners.get(i).onPageScrollStateChanged(state);
		}
	}

	@Override
	public void onVoicemailStatusFetched(Cursor statusCursor) {
		mHasFetchedVoicemailStatus = true;

		if (getActivity() == null || getActivity().isFinishing()) {
			return;
		}

		// Update mHasActiveVoicemailProvider, which controls the number of tabs displayed.
		boolean hasActiveVoicemailProvider =
				mVoicemailStatusHelper.getNumberActivityVoicemailSources(statusCursor) > 0;
				if (hasActiveVoicemailProvider != mHasActiveVoicemailProvider) {
					mHasActiveVoicemailProvider = hasActiveVoicemailProvider;
					mViewPagerAdapter.notifyDataSetChanged();
					//            mViewPagerTabs.setViewPager(mViewPager);

					mPrefs.edit()
					.putBoolean(PREF_KEY_HAS_ACTIVE_VOICEMAIL_PROVIDER, hasActiveVoicemailProvider)
					.commit();
				}

				if (mHasActiveVoicemailProvider && mShowVoicemailTabAfterVoicemailStatusIsFetched) {
					mShowVoicemailTabAfterVoicemailStatusIsFetched = false;
					//            showTab(TAB_INDEX_VOICEMAIL);
				}
	}

	@Override
	public boolean onCallsFetched(Cursor statusCursor) {
		// Return false; did not take ownership of cursor
		return false;
	}

	public int getCurrentTabIndex() {
		return mTabIndex;
	}

	public void showRemoveView(boolean show) {
		mRemoveViewContent.setVisibility(show ? View.VISIBLE : View.GONE);
		mRemoveView.setAlpha(show ? 0 : 1);
		mRemoveView.animate().alpha(show ? 1 : 0).start();
	}

	public boolean shouldShowActionBar() {
		// TODO: Update this based on scroll state.
		return mActionBar != null;
	}

	public SpeedDialFragment getSpeedDialFragment() {
		return mSpeedDialFragment;
	}

	public RemoveView getRemoveView() {
		return mRemoveView;
	}

	public int getTabCount() {
		return mViewPagerAdapter.getCount();
	}

	private int getRtlPosition(int position) {
		if (DialerUtils.isRtl()) {
			return mViewPagerAdapter.getCount() - 1 - position;
		}
		return position;
	}

	public void sendScreenViewForCurrentPosition() {
		if (!isResumed()) {
			return;
		}

		String fragmentName;
		switch (getCurrentTabIndex()) {
		//            case TAB_INDEX_SPEED_DIAL:
		//                fragmentName = SpeedDialFragment.class.getSimpleName();
		//                break;
		case TAB_INDEX_RECENTS:
			fragmentName = CallLogFragment.class.getSimpleName() + "#Recents";
			break;
		case TAB_INDEX_ALL_CONTACTS:
			fragmentName = AllContactsFragment.class.getSimpleName();
			break;
			//            case TAB_INDEX_VOICEMAIL:
			//                fragmentName = CallLogFragment.class.getSimpleName() + "#Voicemail";
		default:
			return;
		}
		AnalyticsUtil.sendScreenView(fragmentName, getActivity(), null);
	}

	/// M: [Multi-Delete] For CallLog delete @{
	//    @Override
	public void onCallsDeleted() {
		// Do nothing
	}
	/// @}

	@Override
	public void onRedPointReceived() {
		// TODO Auto-generated method stub
		//		mViewPager.onRedPointReceived();
		//		new AlertDialog.Builder(getActivity())
		//		.setTitle(null) 
		//		.setMessage("onRedPointReceived")
		//		.setPositiveButton(getActivity().getString(com.hb.R.string.ok), null)
		//		.setNegativeButton(getActivity().getString(com.hb.R.string.cancel), null)
		//		.show();
		showRedPoint=true;
	}

	public boolean showRedPoint=false;
	@Override
	public void onSwitchToYPTab() {
		// TODO Auto-generated method stub
		//		Toast.makeText(getActivity(), "onSwitchToYPTab", Toast.LENGTH_LONG).show();
		mViewPager.setCurrentItem(TAB_INDEX_YELLOWPAGE);
	}
}
