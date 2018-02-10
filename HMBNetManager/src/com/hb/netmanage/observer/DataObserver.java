package com.hb.netmanage.observer;

/**
 * Created by zhaolaichao on 17-4-17.
 */

public interface DataObserver {
    /**
     * add observer
     * @param observer
     */
    public void addObserver(Observer observer);

    /**
     * remove observer
     * @param observer
     */
    public void removeObserver(Observer observer);

    /**
     * nofity observer
     */
    public void notifyObserver();
}
