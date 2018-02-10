package com.hb.netmanage.observer;

import java.util.ArrayList;

/**
 * Created by zhaolaichao on 17-4-17.
 */

public class UpdateObserver implements DataObserver {
    private ArrayList<Observer> mObservers = new ArrayList<Observer>();
    @Override
    public void addObserver(Observer observer) {
        synchronized (mObservers) {

            mObservers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        synchronized (mObservers) {
            mObservers.remove(observer);
        }
    }

    @Override
    public void notifyObserver() {
        synchronized (mObservers) {
            for (Observer observer : mObservers) {
                observer.update();
            }
        }
    }

    /**
     * 开始更新
     */
    public void setUpdate() {
        notifyObserver();
    }
}
