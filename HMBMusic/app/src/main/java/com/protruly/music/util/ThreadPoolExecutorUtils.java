package com.protruly.music.util;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hujianwei on 17-8-30.
 */

public class ThreadPoolExecutorUtils {

    private ThreadPoolExecutor executor;
    public static final String TAG = "ThreadPoolExecutor";
    private static ThreadPoolExecutorUtils threadPoolExecutorUtils;

    public static ThreadPoolExecutorUtils getThreadPoolExecutor() {
        if (threadPoolExecutorUtils == null) {
            threadPoolExecutorUtils = new ThreadPoolExecutorUtils();
        }
        return threadPoolExecutorUtils;
    }

    public ThreadPoolExecutorUtils() {
        int processors = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(processors, 20, 2, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(20), new PriorityThreadFactory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private ExecutorService FULL_TASK_EXECUTOR;

    public ExecutorService getFULL_TASK_EXECUTOR() {
        if (FULL_TASK_EXECUTOR == null) {
            FULL_TASK_EXECUTOR = (ExecutorService) Executors.newCachedThreadPool();
        }
        return FULL_TASK_EXECUTOR;
    }

    public void shutdown() {
        executor.shutdown();
        executor = null;
        threadPoolExecutorUtils = null;
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    class PriorityThreadFactory implements ThreadFactory {
        final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(Thread.currentThread().getThreadGroup(), r, TAG + "music_thread" + (threadNumber.getAndIncrement()));
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            });
            return t;
        }
    }
}
