package com.protruly.powermanager.purebackground.interfaces;

import java.util.List;

/**
 * Observer is the interface to be implemented by objects that receive
 * notification of updates on an Subject object.
 */
public interface Observer {

	/**
	 * Initialization completed.
	 * @param subject
     */
	public void updateOfInit(Subject subject);
	
	/**
	 * This method is called if a app installed.
	 * @param subject
	 * @param pkgName
	 */
	public void updateOfInStall(Subject subject, String pkgName);
	
	/**
	 * This method is called if a app cover inStalled.
	 * @param subject
	 * @param pkgName
	 */
	public void updateOfCoverInStall(Subject subject, String pkgName);
	
	/**
	 * This method is called if a app uninstalled.
	 * @param subject
	 * @param pkgName
	 */
	public void updateOfUnInstall(Subject subject, String pkgName);

	/**
	 * This method is called if a external app be availabled.
	 * @param subject
	 * @param pkgList
	 */
	public void updateOfExternalAppAvailable(Subject subject, List<String> pkgList);
		
	/**
	 * This method is called if a external app be UnAvailable.
	 * @param subject
	 * @param pkgList
	 */
	public void updateOfExternalAppUnAvailable(Subject subject, List<String> pkgList);
}