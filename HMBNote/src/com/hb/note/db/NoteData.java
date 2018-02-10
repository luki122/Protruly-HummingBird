package com.hb.note.db;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class NoteData implements Parcelable, Serializable {

    private static final long serialVersionUID = -184302478425065055L;

    private int id;

    private String uuid;

    private String title;

    private String content;

    private String characters;

    private int imageCount;

    private long updateTime;

    private long stickTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCharacters() {
        return characters;
    }

    public void setCharacters(String characters) {
        this.characters = characters;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getStickTime() {
        return stickTime;
    }

    public void setStickTime(long stickTime) {
        this.stickTime = stickTime;
    }

    public NoteData() {

    }

    private NoteData(Parcel in) {
        id = in.readInt();
        uuid = in.readString();
        title = in.readString();
        content = in.readString();
        characters = in.readString();
        imageCount = in.readInt();
        updateTime = in.readLong();
        stickTime = in.readLong();
    }

    public static final Creator<NoteData> CREATOR = new Creator<NoteData>() {
        @Override
        public NoteData createFromParcel(Parcel in) {
            return new NoteData(in);
        }

        @Override
        public NoteData[] newArray(int size) {
            return new NoteData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(uuid);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(characters);
        dest.writeInt(imageCount);
        dest.writeLong(updateTime);
        dest.writeLong(stickTime);
    }
}
