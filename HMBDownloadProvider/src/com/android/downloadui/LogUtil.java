package com.android.downloadui;

/**
 * @author wxue
 */
public class LogUtil
{
	public final static String LOGTAG = "DocumentsUI";
	private final static boolean DEBUG = true;

	public static void v(String log)
	{
		if (log == null)
		{
			return;
		}
		
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.v(LOGTAG, log);
	}

	public static void d(String log)
	{
		if (log == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.d(LOGTAG, log);
	}

	public static void d(String tag, String log)
	{
		if (log == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.d(tag, log); 
	}

	public static void i(String log)
	{
		if (log == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.i(LOGTAG, log);
	}

	public static void e(String log)
	{
		if (log == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.e(LOGTAG, log); 
	} 

	public static void v(String tag, String log)
	{
		if (log == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.v(tag, log);
	}

	public static void i(String tag, String log)
	{
		if (log == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.i(tag, log); 
	}

	public static void e(String tag, String log)
	{
		if (log == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		
		android.util.Log.e(tag, log); 
	} 
	
	public static void e(String tag,String msg,Throwable e){
		if (msg == null && e == null)
		{
			return;
		}
		if (!DEBUG)
		{
			return;
		}
		android.util.Log.e(tag, msg, e);
	}
}