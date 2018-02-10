package com.protruly.music.util;

import android.app.Activity;
import android.content.Intent;

import com.protruly.music.ui.album.AlbumDetailActivity;
import com.protruly.music.ui.album.AlbumListActivity;

/**
 * Created by hujianwei on 17-9-1.
 */

public class IntentFactory {

    public IntentFactory() {
        // TODO Auto-generated constructor stub
    }

    public static Intent newAlbumListIntent(Activity mActivity,
                                            String artistid, String artistname, int artistofalbums,
                                            int artistofsongs) {
        Intent intent;
        if (artistofalbums <= 3) {
            intent = new Intent(mActivity, AlbumDetailActivity.class);
            intent.putExtra(Globals.KEY_ARTIST_ID, artistid);
            intent.putExtra(Globals.KEY_ARTIST_NAME, artistname);
            intent.putExtra("artistofalbum", artistofalbums);
            intent.putExtra("artistoftrack", artistofsongs);
        } else {
            intent = new Intent(mActivity, AlbumListActivity.class);
            intent.putExtra(Globals.KEY_ARTIST_ID, artistid);
            intent.putExtra(Globals.KEY_ARTIST_NAME, artistname);
            intent.putExtra("artistofalbum", artistofalbums);
            intent.putExtra("artistoftrack", artistofsongs);
        }
        return intent;
    }

}
