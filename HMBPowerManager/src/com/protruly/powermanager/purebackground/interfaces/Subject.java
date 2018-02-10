package com.protruly.powermanager.purebackground.interfaces;

import java.util.ArrayList;
import java.util.List;

/**
 * The object who's state change is being observed, to be communicated to
 * the observers upon occurrence.
 */
public class Subject {
     private List<Observer> observers = new ArrayList<>();
     
     /**
      * Register observer
      * @param observer
      */
     public void attach(Observer observer) {
    	 for (Observer mObserver : observers) {
    		 if (mObserver != null && mObserver == observer) {
    			 return ;
    		 }
    	 }
    	 observers.add(observer);
     }
     
     /**
      * Unregister observer
      * @param observer
      */
     public void detach(Observer observer) {
    	 observers.remove(observer);
     }

     protected void notifyObserversOfInit() {
    	 for (Observer observer : observers) {
    		 observer.updateOfInit(this);
    	 }
     }

     protected void notifyObserversOfInStall(String pkgName) {
    	 for (Observer observer : observers) {
    		 observer.updateOfInStall(this, pkgName);
    	 }
     }

     protected void notifyObserversOfCoverInStall(String pkgName) {
    	 for (Observer observer : observers) {
    		 observer.updateOfCoverInStall(this, pkgName);
    	 }
     }

     protected void notifyObserversOfUnInstall(String pkgName) {
    	 for (Observer observer : observers) {
    		 observer.updateOfUnInstall(this, pkgName);
    	 }
     }

     protected void notifyObserversOfExternalAppAvailable(List<String> pkgList) {
    	 for (Observer observer : observers) {
    		 observer.updateOfExternalAppAvailable(this, pkgList);
    	 }
     }

     protected void notifyObserversOfExternalAppUnAvailable(List<String> pkgList) {
    	 for (Observer observer : observers) {
    		 observer.updateOfExternalAppUnAvailable(this, pkgList);
    	 }
     }
}