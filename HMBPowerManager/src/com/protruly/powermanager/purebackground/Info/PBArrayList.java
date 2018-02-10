package com.protruly.powermanager.purebackground.Info;

import java.util.ArrayList;
import java.util.List;

/**
 * PBArrayList is an implementation of ArrayList, This class is equivalent to
 * ArrayList with synchronized operations.
 */

public class PBArrayList<T> {
    private long mLastModifyTime = 0;
    private final Object mLocked = new Object();
    private List<T> dataList = new ArrayList<T>();

    @Override
    public String toString() {
        return dataList.toString();
    }

    public long getLastModifyTime() {
        return mLastModifyTime;
    }

    public List<T> getDataList() {
        return this.dataList;
    }

    public int size() {
        synchronized (mLocked) {
            return dataList.size();
        }
    }

    public void add(T t) {
        synchronized (mLocked) {
            dataList.add(t);
            mLastModifyTime = System.currentTimeMillis();
        }
    }

    public void add(int position, T t) {
        if (position >= 0 && position <= size()) {
            synchronized (mLocked) {
                try {
                    dataList.add(position,t);
                    mLastModifyTime = System.currentTimeMillis();
                } catch(IndexOutOfBoundsException e){
                    //
                }
            }
        }
    }

    public T get(int index) {
        if (size() > 0 && size() > index) {
            synchronized (mLocked) {
                try {
                    return dataList.get(index);
                } catch (IndexOutOfBoundsException e){
                    //
                }
            }
        }
        return null;
    }

    public void clear() {
        synchronized (mLocked) {
            dataList.clear();
            mLastModifyTime = System.currentTimeMillis();
        }
    }

    public void remove(int index) {
        if (size() > 0 && size() > index) {
            synchronized (mLocked) {
                try {
                    dataList.remove(index);
                    mLastModifyTime = System.currentTimeMillis();
                } catch (IndexOutOfBoundsException e){
                    //
                }
            }
        }
    }

    public void remove(T t) {
        if(t != null) {
            try {
                synchronized (mLocked) {
                    dataList.remove(t);
                    mLastModifyTime = System.currentTimeMillis();
                }
            } catch (Exception e){
                //
            }
        }
    }

    public boolean contains(T t) {
        synchronized (mLocked) {
            return dataList.contains(t);
        }
    }

    public int indexOf(T t) {
        synchronized (mLocked) {
            return dataList.indexOf(t);
        }
    }

    public void releaseObject() {
        synchronized (mLocked) {
            dataList.clear();
            mLastModifyTime = System.currentTimeMillis();
        }
    }
}