package com.protruly.music.downloadex.db;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.protruly.music.db.HBDbData;
import com.protruly.music.downloadex.DownloadInfo;
import com.protruly.music.downloadex.DownloadTask.State;
import com.protruly.music.util.LogUtil;

public class Dao extends SQLiteOpenHelper {

	private static final String TAG = "Dao";
	private SQLiteDatabase mDatabase = null;
	private static final String DATABASE_NAME = "mydownload.db";
	private static final String TABLE_DOWNLOAD = "download";
	private static Dao mInstance = null;
	private Context mContext = null;

	private boolean allowTransaction = false;
	private Lock writeLock = new ReentrantLock();
	private volatile boolean writeLocked = false;

	public static final String matchUri = "hb_download";
	public static final String mUri = "content://hb_download/download";
	public static final Uri mUpUri = Uri.parse(mUri);
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(matchUri, "download", 1);
		URI_MATCHER.addURI(matchUri, "download/#", 2);
	}

	public static final class DownloadItemColumns implements BaseColumns {
		public static final String DB_ID = "_id"; //1
		public static final String SONG_ID = "song_id";//2
		public static final String SONG_URL = "song_url";//3
		public static final String LYRIC_URL = "lyric_url";//4
		public static final String IMG_URL = "img_url";//5
		public static final String TITLE = "title";//6
		public static final String ARTIST = "artist";//7
		public static final String ALBUM = "album";//8
		public static final String TOTAL_BYTES = "total_bytes";//9
		public static final String CURRENT_BYTES = "current_bytes";//10
		public static final String SAVE_PATH = "save_path";//11
		public static final String LYRIC_PATH = "lyric_path";//12
		public static final String IMG_PATH = "img_path";//13
		public static final String SAVE_NAME = "save_name";//14
		public static final String FILE_NAME = "file_name";//15
		public static final String ADDED_TIME = "added_time";//16
		public static final String LAST_MOD = "last_mod";//17
		public static final String STATUS = "status";//18
		public static final String BITRATE = "bitrate";//19

		public static Uri getContentUri() {
			return Uri.parse(mUri);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTable(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		dropTable(db);
		createTable(db);
	}

	private void createTable(SQLiteDatabase db) {
		try {
			db.execSQL("CREATE TABLE IF NOT EXISTS download(_id INTEGER PRIMARY KEY, song_id INTEGER, song_url TEXT , lyric_url TEXT, img_url TEXT,"
					+ "title TEXT, artist TEXT, album TEXT, total_bytes INTEGER, current_bytes INTEGER, save_path TEXT, lyric_path TEXT, img_path TEXT ,"
					+ "save_name TEXT, file_name TEXT, status INTEGER, added_time INTEGER, file_type INTEGER , bitrate TEXT);");
		} catch (SQLException ex) {
			throw ex;
		}
	}

	private void dropTable(SQLiteDatabase db) {
		try {
			LogUtil.iv(TAG, "dropTable start");
			db.execSQL("DROP TABLE IF EXISTS download");
		} catch (SQLException ex) {
			LogUtil.iv(TAG, "dropTable fail");
			throw ex;
		}
	}

	public static synchronized Dao getInstance(Context context) {
		if (mInstance == null)
			mInstance = new Dao(context, DATABASE_NAME, HBDbData.VERSION);

		return mInstance;
	}

	private Dao(Context context, String name, int version) {
		super(context, name, null, version);
		this.mContext = context.getApplicationContext();
		openDataBase();
	}

	private void openDataBase() {
		try {
			LogUtil.iv(TAG, "dao openDataBase !!!");

			this.mDatabase = getWritableDatabase();
		} catch (IllegalStateException e) {
			this.mDatabase = null;
		} catch (SQLiteException e) {
			this.mDatabase = null;
		}
	}

	// private long time = 0;
	public void savaOrUpdate(DownloadInfo entity) {
		if (entity == null) {
			return;
		}

		try {
			// beginTransaction();
			savaOrUpdateEx(entity);
			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}
	}

	public void updateAll(List<DownloadInfo> list) {
		if (list == null || (list != null && list.size() == 0)) {
			return;
		}

		try {
			mDatabase.beginTransaction();
			for (DownloadInfo downloadInfo : list) {
				savaOrUpdate(downloadInfo);
			}
			mDatabase.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mDatabase.endTransaction();
		}

		return;
	}

	public void insert(DownloadInfo entity) throws Exception {
		if (entity == null) {
			return;
		}

		try {
			// beginTransaction();
			insertEx(entity);
			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}
	}

	public void updateFileSize(long id, long size) {
		if (id < 0) {
			return;
		}

		try {
			// beginTransaction();
			updateFileSizeEx(id, size);
			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}

		return;
	}

	public void updateStatus(long id, int status) {
		if (id < 0) {
			return;
		}

		try {
			// beginTransaction();

			if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
				LogUtil.iv(TAG, "updateStatus --- fail 0");
				return;
			}

			String sql = "update download set status=? where song_id=?";
			Object[] bindArgs = { status, String.valueOf(id) };
			this.mDatabase.execSQL(sql, bindArgs);
			LogUtil.iv(TAG, "updateStatus --- success");

			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}

		return;
	}

	public void updateDownloadSize(long id, long size) {
		if (id < 0) {
			return;
		}

		try {
			// beginTransaction();

			if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
				LogUtil.iv(TAG, "updateDownloadSize --- fail 0");
				return;
			}

			String sql = "update download set current_bytes=? where song_id=?";
			Object[] bindArgs = { size, String.valueOf(id) };
			this.mDatabase.execSQL(sql, bindArgs);
			// LogUtil.iv(TAG, "updateDownloadSize ccc --- success");

			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}

		return;
	}

	public void updateFileName(long id, String name) {
		if (id < 0) {
			return;
		}

		try {
			// beginTransaction();

			if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
				LogUtil.iv(TAG, "updateFileName --- fail 0");
				return;
			}

			String sql = "update download set save_name=? where song_id=?";
			Object[] bindArgs = { name, String.valueOf(id) };
			this.mDatabase.execSQL(sql, bindArgs);
			LogUtil.iv(TAG, "updateFileName --- success");

			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}

		return;
	}

	/**
	 * @param id
	 * @param name
	 *            歌名
	 * @param path
	 *            下载路径
	 * @param lrcPath
	 *            歌词下载路径
	 */
	public void updateFileInfo(long id, String name, String path, String lrcPath) {
		if (id < 0) {
			return;
		}
		try {
			if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
				LogUtil.d(TAG, "HBListItemHBListItem-updateFileInfo fail");
				return;
			}
			ContentValues values = new ContentValues();
			values.put("save_name", name);
			values.put("song_url", path);
			values.put("lyric_url", lrcPath);
			mDatabase.update("download", values, "song_id=?", new String[] { String.valueOf(id) });
			LogUtil.d(TAG, "HBListItemHBListItem-updateFileInfo success");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getDownloadStatusById(long id) {
		if (id < 0) {
			return -1;
		}

		Cursor cursor = null;
		int status = -1;
		try {
			StringBuilder where = new StringBuilder();
			where.append(DownloadItemColumns.SONG_ID + "=" + id);

			cursor = this.mDatabase.query(TABLE_DOWNLOAD, null, where.toString(), null, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
				if (!cursor.isAfterLast()) {
					status = cursor.getInt(15);
					LogUtil.iv(TAG, "getDownloadStatusById --- 1 success ,status:" + status);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.iv(TAG, "getDownloadStatusById --- fail 0");
		} finally {
			DbUtils.closeQuietly(cursor);
		}

		return status;
	}

	public boolean isDownloadFinished(long id) {

		if (id < 0) {
			return false;
		}

		int num = 0;
		Cursor cursor = null;
		try {
			StringBuilder where = new StringBuilder();
			where.append(DownloadItemColumns.STATUS + "=7");
			where.append(" AND " + DownloadItemColumns.SONG_ID + " = " + id);

			cursor = this.mDatabase.query(TABLE_DOWNLOAD, null, where.toString(), null, null, null, null);
			if (cursor != null) {
				num = cursor.getCount();
				LogUtil.iv(TAG, "isDownloadFinished --- 1 num:" + num);
			}

		} catch (Exception e) {
			e.printStackTrace();
			num = 0;
			LogUtil.iv(TAG, "isDownloadFinished --- fail 0");
		} finally {
			DbUtils.closeQuietly(cursor);
		}

		return ((num > 0) ? true : false);
	}

	public boolean findDownLoadingOrFinishedByUrl(long id) {

		if (id < 0) {
			return false;
		}

		int num = 0;
		Cursor cursor = null;
		try {
			StringBuilder where = new StringBuilder();
			where.append(DownloadItemColumns.STATUS + " IN (1, 2)");
			where.append(" AND " + DownloadItemColumns.SONG_ID + "=" + id);

			cursor = this.mDatabase.query(TABLE_DOWNLOAD, null, where.toString(), null, null, null, null);
			if (cursor != null) {
				num = cursor.getCount();
				LogUtil.iv(TAG, "findDownLoadingOrFinishedByUrl --- 1 num:" + num);
			}

		} catch (Exception e) {
			e.printStackTrace();
			num = 0;
			LogUtil.iv(TAG, "findDownLoadingOrFinishedByUrl --- fail 0");
		} finally {
			DbUtils.closeQuietly(cursor);
		}

		return ((num > 0) ? true : false);
	}

	public LinkedHashMap<Long, DownloadInfo> getAllDownLoadData() {

		LinkedHashMap<Long, DownloadInfo> result = new LinkedHashMap<Long, DownloadInfo>();

		Cursor cursor = null;
		try {
			cursor = this.mDatabase.query(TABLE_DOWNLOAD, null, null, null, null, null, null);
			if (cursor != null) {
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					DownloadInfo entity = new DownloadInfo(cursor.getLong(1), cursor.getInt(15), cursor.getString(2), cursor.getString(5), cursor.getString(14), cursor.getString(10),
							cursor.getLong(9), cursor.getLong(8), cursor.getString(6), cursor.getString(7), cursor.getString(18), cursor.getLong(16));

					entity.setImgUrl(cursor.getString(4));
					if (entity.isDownloading() || entity.isWaiting()) {
						entity.setState(State.PAUSE);
					}
					result.put(cursor.getLong(1), entity);
					cursor.moveToNext();
				}
			}

			LogUtil.iv(TAG, "getAllDownLoadData   2 success:");
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.iv(TAG, "getAllDownLoadData  fail 0");
		} finally {
			DbUtils.closeQuietly(cursor);
		}

		return result;
	}

	public void update(DownloadInfo entity) throws Exception {
		if (entity == null) {
			return;
		}

		try {
			// beginTransaction();
			updateEx(entity);
			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}

		return;
	}

	public DownloadInfo queryItem(String urlpath, long songid) throws Exception {

		try {
			// beginTransaction();
			DownloadInfo info = queryItemEx(urlpath, songid);
			// setTransactionSuccessful();

			return info;
		} finally {
			// endTransaction();
		}
	}

	public boolean isItemDownloaded(long id) throws Exception {
		boolean isExist = false;

		if (id >= 0) {
			Cursor cursor = null;
			try {
				String sql = "select * from download where song_id=?";
				cursor = this.mDatabase.rawQuery(sql, new String[] { String.valueOf(id) });
				if (cursor != null) {
					cursor.moveToFirst();
					while (!cursor.isAfterLast()) {
						String downurl = cursor.getString(2);
						LogUtil.iv(TAG, "isItemDownloaded --- downurl:" + downurl);
						if (!TextUtils.isEmpty(downurl)) {
							isExist = true;
							break;
						}
						cursor.moveToNext();
					}
				}

				LogUtil.iv(TAG, "isItemDownloaded --- isExist:" + isExist);
			} catch (Exception e) {
				LogUtil.iv(TAG, "isItemDownloaded --- fail 0");
			} finally {
				DbUtils.closeQuietly(cursor);
			}
		}

		return isExist;
	}

	public boolean isItemExist(long id) throws Exception {
		boolean isExist = false;

		if (id >= 0) {
			Cursor cursor = null;
			try {
				String sql = "select * from download where song_id=?";
				cursor = this.mDatabase.rawQuery(sql, new String[] { String.valueOf(id) });
				if (cursor != null && cursor.getCount() > 0) {
					isExist = true;
				}

				LogUtil.iv(TAG, "isItemExist --- isExist:" + isExist);
			} catch (Exception e) {
				LogUtil.iv(TAG, "queryItemEx --- fail 0");
			} finally {
				DbUtils.closeQuietly(cursor);
			}
		}

		return isExist;
	}

	public void delete(String urlpath, long songid) throws Exception {
		if (urlpath == null && songid < 0) {
			return;
		}

		try {
			// beginTransaction();
			deleteEx(urlpath, songid);
			// setTransactionSuccessful();
		} finally {
			// endTransaction();
		}

		return;
	}

	private void updateFileSizeEx(long id, long size) {
		if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
			LogUtil.iv(TAG, "updateFileSizeEx --- fail 0");
			return;
		}

		try {
			String sql = "update download set total_bytes=? where song_id=?";
			Object[] bindArgs = { size, String.valueOf(id) };
			this.mDatabase.execSQL(sql, bindArgs);
			LogUtil.iv(TAG, "updateFileSizeEx --- success");
		} catch (Exception e) {
			// TODO: handle exception
		}

		return;
	}

	public DownloadInfo queryDownloadSong(String title, String artist) {
		if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
			LogUtil.iv(TAG, "queryDownloadSong --- fail 0");
			return null;
		}
		DownloadInfo info = null;
		Cursor cursor = null;
		try {
			cursor = mDatabase.rawQuery("select * from download where title=? AND artist=?", new String[] { title, artist});

			if (cursor != null && cursor.moveToFirst()) {
				info = new DownloadInfo(cursor.getLong(2), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(4),
						cursor.getString(5));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return info;
	}

	private DownloadInfo queryItemEx(String urlpath, long songid) {
		if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
			LogUtil.iv(TAG, "queryItemEx --- fail 0");
			return null;
		}

		Cursor cursor = null;
		try {
			DownloadInfo info = null;
			String sql = null;

			LogUtil.iv(TAG, "queryItemEx --- 1 songid:" + songid + ",urlpath:" + urlpath);

			if (songid > 0 && urlpath != null) {
				sql = "select * from download where song_id=? AND song_url=?";
				cursor = this.mDatabase.rawQuery(sql, new String[] { String.valueOf(songid), urlpath });
			} else if (songid >= 0) {
				sql = "select * from download where song_id=?";
				cursor = this.mDatabase.rawQuery(sql, new String[] { String.valueOf(songid) });
			} else if (urlpath != null) {
				sql = "select * from download where song_url=?";
				cursor = this.mDatabase.rawQuery(sql, new String[] { urlpath });
			}

			if (cursor != null) {
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					info = new DownloadInfo(cursor.getLong(1), cursor.getInt(15), cursor.getString(2), cursor.getString(5), cursor.getString(14), cursor.getString(10), cursor.getLong(9),
							cursor.getLong(8), cursor.getString(6), cursor.getString(7), cursor.getString(18), cursor.getLong(16));
					info.setImgUrl(cursor.getString(4));
					cursor.moveToNext();
					LogUtil.iv(TAG, "queryItemEx --- success --- info:" + info.toString());
				}
			}

			return info;
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.iv(TAG, "queryItemEx --- fail 1");
		} finally {
			DbUtils.closeQuietly(cursor);
		}

		return null;
	}

	private void deleteEx(String urlpath, long songid) {
		if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
			LogUtil.iv(TAG, "deleteEx fail --- 0");
			return;
		}

		try {
			int ret = -1;

			if (urlpath != null && songid > 0) {
				ret = this.mDatabase.delete("download", "song_url=? AND song_id=?", new String[] { urlpath, String.valueOf(songid) });
			} else if (songid > 0) {
				ret = this.mDatabase.delete("download", "song_id=?", new String[] { String.valueOf(songid) });
			} else if (urlpath != null) {
				ret = this.mDatabase.delete("download", "song_url=?", new String[] { urlpath });
			}

			LogUtil.iv(TAG, "deleteEx 1 success ret:" + ret + ",urlpath:" + urlpath + ",songid:" + songid);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.iv(TAG, "deleteEx failed --- 1");
		}
	}

	private void savaOrUpdateEx(DownloadInfo infos) {
		if (infos == null) {
			return;
		}

		LogUtil.iv(TAG, "savaOrUpdateEx  --- 1");
		long id = infos.getId();
		if (id < 0) {
			return;
		}

		// time = System.currentTimeMillis();

		try {
			boolean has = isItemExist(id);
			LogUtil.iv(TAG, "savaOrUpdateEx  --- 2 has:" + has);

			// Log.i(TAG, "---- bbb 1 time:"+(System.currentTimeMillis() -
			// time)+",has:"+has);
			// time = System.currentTimeMillis();

			if (has) {
				// update
				updateEx(infos);
			} else {
				// insert
				insertEx(infos);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		// Log.i(TAG, "---- bbb 2 time:"+(System.currentTimeMillis() -
		// time));
		return;
	}

	private void insertEx(DownloadInfo infos) {

		boolean ret = insertInternal(infos);
		if (ret) {
			this.mContext.getContentResolver().notifyChange(mUpUri, null);
		}
		return;
	}

	private boolean insertInternal(DownloadInfo infos) {
		try {
			if ((this.mDatabase == null) || (!this.mDatabase.isOpen()) || (infos == null))
				return false;

			LogUtil.iv(TAG, "insertInternal 1 infos:" + infos.getDownloadUrl());

			String sql = "insert into download(song_id, song_url, title, artist, album, total_bytes,save_path, file_name, status, added_time, bitrate, img_url) values (?,?,?,?,?,?,?,?,?,?,?,?)";

			Object[] bindArgs = { infos.getId(), infos.getDownloadUrl(), infos.getTitle(), infos.getArtist(), infos.getAlbum(), infos.getFileLength(), infos.getFileSavePath(), infos.getFileName(),
					infos.getState().value(), infos.getCreatetime(), 128, infos.getImgUrl() };

			this.mDatabase.execSQL(sql, bindArgs);

			LogUtil.iv(TAG, "insertInternal success");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.iv(TAG, "insertInternal fail ---");
		}

		return false;
	}

	private void updateEx(DownloadInfo infos) {
		if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
			LogUtil.iv(TAG, "updateEx fail --- 0");
			return;
		}

		try {
			/*
			 * String sql =
			 * "update download set current_bytes=?, status=? where song_url=?";
			 */
			String sql = "update download set song_url=?, title=?, artist=?, album=?, total_bytes=?, save_path=?," + "file_name=?, current_bytes=?, status=? where song_id=?";

			Object[] bindArgs = { infos.getDownloadUrl(), infos.getTitle(), infos.getArtist(), infos.getAlbum(), infos.getFileLength(), infos.getFileSavePath(), infos.getFileName(),
					infos.getProgress(), infos.getState().value(), infos.getId() };
			this.mDatabase.execSQL(sql, bindArgs);

			LogUtil.iv(TAG, "updateEx success ---");
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.iv(TAG, "updateEx fail --- 1");
		}
		return;
	}

	// /////////////////////////////// utils //////////////////////////////////
	public void setAllowTransaction(boolean allowTransaction) {
		this.allowTransaction = allowTransaction;
		return;
	}

	public SQLiteDatabase getDatabase() {
		return mDatabase;
	}

	private void beginTransaction() {
		if (allowTransaction) {
			mDatabase.beginTransaction();
		} else {
			writeLock.lock();
			writeLocked = true;
		}
	}

	private void setTransactionSuccessful() {
		if (allowTransaction) {
			mDatabase.setTransactionSuccessful();
		}
	}

	private void endTransaction() {
		if (allowTransaction) {
			mDatabase.endTransaction();
		}

		if (writeLocked) {
			writeLock.unlock();
			writeLocked = false;
		}
	}


	public long deleteItemByFilePath(String filePath) {
		if ((this.mDatabase == null) || (!this.mDatabase.isOpen())) {
			LogUtil.iv(TAG, "deleteEx fail --- 0");
			return 0;
		}

		try {
			int ret = -1;
			long id = -1;
			if (!TextUtils.isEmpty(filePath)) {
				Cursor cursor = this.mDatabase.rawQuery("select song_id from download WHERE save_path=?", new String[] { filePath });
				if (cursor != null) {
					if (cursor.getCount() == 0) {
						cursor.moveToFirst();
						id = cursor.getLong(0);
					}
					cursor.close();
				}
				ret = this.mDatabase.delete("download", "save_path=?", new String[] { filePath });
				if (ret > 0) {
					return id;
				}
			}
			LogUtil.iv(TAG, "deleteItem 1 success ret:" + ret + ",filePath:" + filePath);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.iv(TAG, "deleteItem failed --- 1");
		}
		return 0;
	}


}
