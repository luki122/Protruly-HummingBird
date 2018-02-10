package com.protruly.music.model;

import android.text.TextUtils;

/**
 * Created by hujianwei on 17-9-1.
 */

public class Playlist {

    /**
     * The unique Id of the playlist
     */
    public long mPlaylistId;

    /**
     * The playlist name
     */
    public String mPlaylistName;

    /**
     * The playlist icon
     */
    public int mPlaylistIcon;

    /**
     * @param playlistId The Id of the playlist
     * @param playlistName The playlist name
     */
    public Playlist(final long playlistId, final String playlistName) {
        mPlaylistId = playlistId;
        mPlaylistName = playlistName;
    }

    public Playlist(final long playlistId, final String playlistName,int bitmapId) {
        mPlaylistId = playlistId;
        mPlaylistName = playlistName;
        mPlaylistIcon = bitmapId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) mPlaylistId;
        result = prime * result + (mPlaylistName == null ? 0 : mPlaylistName.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Playlist other = (Playlist)obj;
        if (mPlaylistId != other.mPlaylistId) {
            return false;
        }
        return TextUtils.equals(mPlaylistName, other.mPlaylistName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mPlaylistName;
    }
}
