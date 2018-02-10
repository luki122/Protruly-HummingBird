package com.protruly.powermanager.purebackground.Info;

import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import java.util.ArrayList;
import java.util.List;

public class AutoStartInfo extends ItemInfo {
	private static final String TAG = AutoStartInfo.class.getSimpleName();

	/**
	 * flag - Boot Completed Auto Start.
	 */
	public static final int FLAG_BOOT_AUTO_START = 1<<0;

	/**
	 * flag - Background Auto Start.
	 */
	public static final int FLAG_BACKGROUND_AUTO_START = 2<<0;

	public int flags = 0;

	/**
	 * flag - App has the ability to AutoStart.
	 */
	private boolean hasAutoStart = false;

	/**
	 * flag - AutoStart is open
	 */
	private boolean isOpen = false;

	/**
	 * flag - AutoStart is open by user
	 */
	private boolean isOpenByUser = false;

	/**
	 * BootCompleted Receiver Information
	 */
	private ResolveInfo bootReceiveResolveInfo;

	/**
	 * Service Information
	 */
	private ServiceInfo[] serviceInfo;

	/**
	 * Broadcast Receiver Information
	 */
	private ActivityInfo[] receiveInfo;

	/**
	 * ResolveInfo list
	 */
	private List<ResolveInfo> resolveInfoList;

	public AutoStartInfo() {
		super(TAG);
	}

	public boolean getAutoStartOfUser() {
		return isOpenByUser;
	}

	public void setAutoStartOfUser(boolean b) {
		isOpenByUser = b;
	}

	public boolean getHasAutoStart() {
		return hasAutoStart;
	}

	public void setHasAutoStart(boolean b) {
		hasAutoStart = b;
	}
	
	public void setIsOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}
	
	public boolean getIsOpen(){
		return this.isOpen;
	}
	
	public void setBootReceiveResolveInfo(ResolveInfo bootReceiveResolveInfo) {
		this.bootReceiveResolveInfo = bootReceiveResolveInfo;
	}
	
	public ResolveInfo getBootReceiveResolveInfo() {
		return this.bootReceiveResolveInfo;
	} 
	
	public void setServiceInfo(ServiceInfo[] serviceInfo) {
		this.serviceInfo = serviceInfo;
	}
	
	public ServiceInfo[] getServiceInfo() {
		return this.serviceInfo;
	}
	
	public void setReceiveInfo(ActivityInfo[] receiveInfo) {
		this.receiveInfo = receiveInfo;
	}
	
	public ActivityInfo[] getReceiveInfo(){
		return this.receiveInfo;
	}

	public void AddResolveInfo(ResolveInfo resolveInfo) {
		if (resolveInfoList == null) {
			resolveInfoList = new ArrayList<ResolveInfo>();
		}
		
		for (int i = 0; i < resolveInfoList.size(); i++) {
			ResolveInfo tmp = resolveInfoList.get(i);
			if (tmp.activityInfo.name.equals(resolveInfo.activityInfo.name)) {
				return;
			}
		}
		
		resolveInfoList.add(resolveInfo);
	}
	
	public List<ResolveInfo> getResolveInfoList(){
		return this.resolveInfoList;
	}
	
	public void ClearResolveInfoList() {
		if (resolveInfoList != null) {
			resolveInfoList.clear();
		}
	}
}