package com.protruly.music.downloadex;

import android.os.Parcel;
import android.os.Parcelable;

import com.protruly.music.downloadex.DownloadTask.State;

public class DownloadInfo implements Parcelable {

	/**
	 * 
	 */
	private long id = -1;
	private DownloadTask downloadTask;// 下载task
	private DownloadTask.State state;// 下载状态
	private String downloadUrl;// 下载地址
	private String fileName;// 下载后保存的文件名，带后缀
	private String fileSavePath;// 下载保存路径
	private long progress;// 当前下载进度
	private long fileLength;// 文件总长度
	private boolean autoResume;
	private boolean autoRename;

	private String title;// 下载后保存的文件名,不带后缀
	private String artist;// 艺术家
	private String album;// 专辑
	private String imgUrl;// 专辑图片URL
	private String bitrate;// 比特率
	private long createtime = 0;// 文件创建时间
	private String lrcUrl;// 歌词URL

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(downloadUrl);
		dest.writeString(fileName);
		dest.writeString(fileSavePath);
		dest.writeLong(progress);
		dest.writeLong(fileLength);
		dest.writeInt(autoResume ? 1 : 0);
		dest.writeInt(autoRename ? 1 : 0);
		dest.writeString(title);
		dest.writeString(artist);
		dest.writeString(album);
		dest.writeString(imgUrl);
		dest.writeString(bitrate);
		dest.writeLong(createtime);
		dest.writeString(lrcUrl);
	}

	private DownloadInfo(Parcel source) {
		id = source.readLong();
		downloadUrl = source.readString();
		fileName = source.readString();
		fileSavePath = source.readString();
		progress = source.readLong();
		fileLength = source.readLong();
		autoResume = (source.readInt() == 0) ? false : true;
		autoRename = (source.readInt() == 0) ? false : true;
		title = source.readString();
		artist = source.readString();
		album = source.readString();
		imgUrl = source.readString();
		bitrate = source.readString();
		createtime = source.readLong();
		lrcUrl = source.readString();
	}

	public static final Parcelable.Creator<DownloadInfo> CREATOR = new Creator<DownloadInfo>() {

		@Override
		public DownloadInfo[] newArray(int size) {
			return new DownloadInfo[size];
		}

		@Override
		public DownloadInfo createFromParcel(Parcel source) {
			return new DownloadInfo(source);
		}
	};

	public DownloadInfo() {
		super();
	}

	public DownloadInfo(long id, int state, String downloadUrl, String title, String fileName, String fileSavePath, long progress, long fileLength, String artist, String album, String bitrate,
			long createtime) {//
		this.id = id;
		this.state = DownloadTask.State.valueOf(state);
		this.downloadUrl = downloadUrl;
		this.fileName = fileName;
		this.title = title;
		this.fileSavePath = fileSavePath;
		this.progress = progress;
		this.fileLength = fileLength;
		this.autoResume = false;
		this.autoRename = false;
		this.artist = artist;
		this.album = album;
		this.bitrate = bitrate;
		this.createtime = createtime;
	}

	public DownloadInfo(long id, String title, String artist, String album, String lrcUrl, String imgUrl) {//
		this.id = id;
		this.title = title;
		this.autoResume = false;
		this.autoRename = false;
		this.artist = artist;
		this.album = album;
		this.lrcUrl = lrcUrl;
		this.imgUrl = imgUrl;
	}

	public String getLrcUrl() {
		return lrcUrl;
	}

	public void setLrcUrl(String lrcUrl) {
		this.lrcUrl = lrcUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	public long getCreatetime() {
		return createtime;
	}

	public void setCreatetime(long createtime) {
		this.createtime = createtime;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getBitrate() {
		return bitrate;
	}

	public void setBitrate(String bitrate) {
		this.bitrate = bitrate;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public DownloadTask getTask() {
		return downloadTask;
	}

	public void setTask(DownloadTask downloadTask) {
		this.downloadTask = downloadTask;
	}

	public DownloadTask.State getState() {
		return state;
	}

	public void setState(DownloadTask.State state) {
		this.state = state;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileSavePath() {
		return fileSavePath;
	}

	public void setFileSavePath(String fileSavePath) {
		this.fileSavePath = fileSavePath;
	}

	public long getProgress() {
		return progress;
	}

	public void setProgress(long progress) {
		this.progress = progress;
	}

	public long getFileLength() {
		if (fileLength < 0) {
			fileLength = 0;
		}
		return fileLength;
	}

	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}

	public boolean isAutoResume() {
		return autoResume;
	}

	public void setAutoResume(boolean autoResume) {
		this.autoResume = autoResume;
	}

	public boolean isAutoRename() {
		return autoRename;
	}

	public void setAutoRename(boolean autoRename) {
		this.autoRename = autoRename;
	}
	

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DownloadInfo))
			return false;

		DownloadInfo that = (DownloadInfo) o;

		if (id != that.id)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return (int) (id ^ (id >>> 32));
	}

	
	@Override
	public String toString() {
		return "DownloadInfo [id=" + id + ", downloadTask=" + downloadTask + ", state=" + state + ", downloadUrl=" + downloadUrl + ", fileName=" + fileName + ", fileSavePath=" + fileSavePath
				+ ", progress=" + progress + ", fileLength=" + fileLength + ", autoResume=" + autoResume + ", autoRename=" + autoRename + ", title=" + title + ", artist=" + artist + ", album="
				+ album + ", imgUrl=" + imgUrl + ", bitrate=" + bitrate + ", createtime=" + createtime + ", lrcUrl=" + lrcUrl +"]";
	}

	public boolean isDownloading() {
		return state == State.STARTED || state == State.LOADING;
	}

	public boolean isWaiting() {
		return state == State.WAITING;
	}

	public boolean isDownloadOver() {
		return state == State.SUCCESS || state == State.CANCELLED;
	}

	public boolean isDownloadFinished() {
		return state == State.SUCCESS;
	}


	public boolean isDownloadPause() {
		return state == State.PAUSE;
	}

}
