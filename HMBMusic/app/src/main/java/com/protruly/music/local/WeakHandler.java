package com.protruly.music.local;

import java.lang.ref.WeakReference;
import android.os.Handler;
import android.os.Looper;

public abstract class WeakHandler<T> extends Handler {
    private WeakReference<T> mOwner;

    public WeakHandler(T owner, Looper loop) {
        super(loop);
        mOwner = new WeakReference<T>(owner);
    }

    public T getOwner() {
        return mOwner.get();
    }
}
