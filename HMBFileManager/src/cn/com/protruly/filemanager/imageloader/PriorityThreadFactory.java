package cn.com.protruly.filemanager.imageloader;

import java.util.concurrent.ThreadFactory;

public class PriorityThreadFactory implements ThreadFactory {

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setPriority(Thread.NORM_PRIORITY - 1);
		return t;
	}

}
