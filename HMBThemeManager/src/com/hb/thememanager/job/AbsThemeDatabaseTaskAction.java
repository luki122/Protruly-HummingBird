package com.hb.thememanager.job;

import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;

public abstract class AbsThemeDatabaseTaskAction implements Runnable {

	public static final int FLAG_WRITE = 0x00;
	private static final int FLAG_NONE = 0x01;
	public static final int FLAG_READ = ~FLAG_WRITE;
	private int mActionFlag = FLAG_NONE;
	private OnThemeLoadedListener<Theme> mThemeLoadedListener;
	private ThemeDatabaseController<Theme> mDbController;
	private boolean isBusy = false;
	private boolean mSetupTransactionFirst = false;
	private String mFilePath;
	private boolean mUserImport = false;
	private Theme mTheme;
	public  AbsThemeDatabaseTaskAction(OnThemeLoadedListener<Theme> loadListener
			,ThemeDatabaseController<Theme> dbController) {
		// TODO Auto-generated constructor stub
		this(loadListener, dbController, "");
	}
	
	public  AbsThemeDatabaseTaskAction(OnThemeLoadedListener<Theme> loadListener
			,ThemeDatabaseController<Theme> dbController,String themeFilePath) {
		// TODO Auto-generated constructor stub
		mThemeLoadedListener = loadListener;
		mDbController = dbController;
		mFilePath = themeFilePath;
	}

	public  AbsThemeDatabaseTaskAction(OnThemeLoadedListener<Theme> loadListener
			,ThemeDatabaseController<Theme> dbController,Theme theme) {
		// TODO Auto-generated constructor stub
		mThemeLoadedListener = loadListener;
		mDbController = dbController;
		mTheme = theme;
		mFilePath = theme.themeFilePath;
	}

	public void setUserImport(boolean isUserImport){
		mUserImport = isUserImport;
	}

	public boolean isUserImport(){
		return mUserImport;
	}


	public Theme getTheme(){
		return mTheme;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(isBusy){
			return;
		}
		if(mSetupTransactionFirst && mDbController != null){
			mDbController.beginTransaction();
		}
		isBusy = true;
		doJob();
		
		if(mSetupTransactionFirst && mDbController != null){
			mDbController.endTransaction();
		}
		if(mDbController != null){
			mDbController.close();
		}
		isBusy = false;
	}
	
	/**
	 * Sets flag for this action to decide the database option is write or read.
	 * <p>
	 * <li>Flag {@link #FLAG_WRITE} means current action is write data into database.
	 * <li>Flag {@link #FLAG_READ} means current action is read data from database.
	 * <p>
	 * Important : Flag must be one of {@link #FLAG_WRITE} and {@link #FLAG_READ}
	 * 
	 * @param writeOrRead read data from database or write data into it
	 */
	public void setActionFlag(int writeOrRead){
		mActionFlag = writeOrRead;
	}
	
	protected int getActionFlag(){
		return mActionFlag;
	}
	
	public void openTransactionIfNeed(boolean open){
		mSetupTransactionFirst = open;
	}
	
	protected OnThemeLoadedListener<Theme> getListener(){
		return mThemeLoadedListener;
	}
	
	protected ThemeDatabaseController<Theme> getDatabaseController(){
		return mDbController;
	}
	
	protected String getFilePath(){
		
		return mFilePath;
	}
	protected abstract void doJob();
	
	
	

}
