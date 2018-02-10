package com.protruly.music.ui.album;

import android.app.Activity;
import android.app.ListFragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.util.DataConvertUtil;
import com.protruly.music.util.DialogUtil;
import com.protruly.music.util.Globals;
import com.protruly.music.util.DialogUtil.OnAddPlaylistSuccessListener;
import com.protruly.music.util.HBAlbum;
import com.protruly.music.adapter.HBAlbumListAdapter;
import com.protruly.music.util.HBMusicUtil;

import java.util.ArrayList;

import hb.widget.HbListView;
import hb.app.dialog.AlertDialog;
/**
 * Created by hujianwei on 17-9-1.
 */

public class AlbumListFragment extends ListFragment {
    private AlbumListActivity mActivity;
    private AlbumQueryHandler mAlbumQueryHandler;
    private int selectedNumAlbum = 0;

    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id) {
        }
    };

    private DialogUtil.OnAddPlaylistSuccessListener mAddPlaylistSuccessListener = new OnAddPlaylistSuccessListener() {

        @Override
        public void OnAddPlaylistSuccess() {
            quitEditMode();
        }
    };

    // Data
    ArrayList<HBAlbum> albums = new ArrayList<HBAlbum>();
    HbListView mListView;
    HBAlbumListAdapter mAdapter;
    HBAlbum selectedAlbum;

    class AlbumQueryHandler extends AsyncQueryHandler {

        AlbumQueryHandler(ContentResolver res) {
            super(res);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // Log.i("@@@", "query complete");
            if (cursor != null)
                init(cursor);
        }
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AlbumListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlbumQueryHandler = new AlbumQueryHandler(getActivity()
                .getContentResolver());
        getQueryCursor(mAlbumQueryHandler, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.album_list_content,
                container, false);
        mListView = (HbListView) rootView.findViewById(android.R.id.list);
        mListView.setSelector(R.drawable.hb_playlist_item_clicked);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
//
//        super.onViewCreated(view, savedInstanceState);
//
//        if (null != mListView) {
//
//            mListView.setVisibility(View.GONE);
//            mListView.hbSetNeedSlideDelete(true);
//            mListView.hbSetSelectorToContentBg(false);
//            mListView.hbSetHbackOnClickListener(new HbBackOnClickListener() {
//
//                @Override
//                public void hbPrepareDraged(int arg0) {
//                }
//
//                @Override
//                public void hbOnClick(int position) {
//                    HBAlbum mAlbum = albums.get(position);
//                    int title = R.string.delete;
//                    String message = getResources().getString(
//                            R.string.delete_album_desc, mAlbum.getAlbumName());
//                    AlertDialog mDeleteConDialog = new AlertDialog.Builder(
//                            mActivity,
//                            AlertDialog.THEME_AMIGO_FULLSCREEN)
//                            .setTitle(title)
//                            .setMessage(message)
//                            .setNegativeButton(android.R.string.cancel,
//                                    null)
//                            .setPositiveButton(android.R.string.ok,
//                                    new DialogInterface.OnClickListener() {
//
//                                        @Override
//                                        public void onClick(
//                                                DialogInterface dialog,
//                                                int which) {
//                                            mAdapter.setDeleteAction(true);
//                                            mListView.hbDeleteSelectedItemAnim();
//                                            mListView.hbSetRubbishBack();
//                                        }
//
//                                    }).create();
//                    mDeleteConDialog.show();
//
//                }
//
//                @Override
//                public void hbDragedUnSuccess(int arg0) {
//                }
//
//                @Override
//                public void hbDragedSuccess(int arg0) {
//                }
//            });
//
//            mListView.hbSetDeleteItemListener(new HbDeleteItemListener() {
//
//                @Override
//                public void hbDeleteItem(View v, int position) {
//                    HBAlbum mAlbum = albums.get(position);
//                    long aid = mAlbum.getAlbumId();
//                    long singerid = Long.parseLong(mActivity.getArtistId());
//                    long[] list = MusicUtils.getSongListForHBAlbum(mActivity, aid, singerid);
//                    MusicUtils.deleteTracks(mActivity, list);
//                    albums.remove(position);
//                    mAdapter.notifyDataSetChanged();
//                    mActivity.updateHeader(mActivity.getNumAlbums() - 1, mActivity.getNumTracks() - mAlbum.getTrackNumber());
//                    isNoAlbums();
//                }
//            });
//
//            //长按事件
//            mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//
//                @Override
//                public boolean onItemLongClick(AdapterView<?> parent, View view,
//                                               int position, long id) {
//                    if (!mAdapter.isEditMode()) {
//
//                        enterEditMode();
//                        mAdapter.setNeedin(position);
//                        return true;
//                    }
//                    return false;
//                }
//
//            });
//            //以下设置list各种属性
//        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;

        if (activity instanceof AlbumListActivity) {
            mActivity = (AlbumListActivity) activity;
        } else {
            throw new IllegalStateException(
                    "Activity must extend AlbumListActivity.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position,
                                long id) {
//		super.onListItemClick(listView, view, position, id);

        Object ob = mListView.getAdapter().getItem(position);
        if (ob instanceof HBAlbum) {

            HBAlbum album = (HBAlbum) ob;
            if (!mAdapter.isEditMode()) {
                Intent detailIntent = new Intent(mActivity, AlbumDetailActivity.class);
                Bundle bl = new Bundle();
                bl.putParcelable(Globals.KEY_ALBUM_ITEM, album);
                detailIntent.putExtra(Globals.KEY_ARTIST_ID, mActivity.getArtistId());
                detailIntent.putExtra(Globals.KEY_ARTIST_NAME, mActivity.getArtistName());
                detailIntent.putExtras(bl);
                startActivityForResult(detailIntent, Globals.REQUEST_CODE_BROWSER);

            } else {
//                CheckBox checkBox = (CheckBox) view.findViewById(com.hb.R.id.hb_list_left_checkbox);
//                if (null != checkBox) {
//                    if (checkBox.isChecked()) {
//                        checkBox.hbSetChecked(false, true);
//                        mAdapter.setCheckedArrayValue(position, false);
//                        mActivity.changeMenuState();
//                    } else {
//                        checkBox.hbSetChecked(true, true);
//                        mAdapter.setCheckedArrayValue(position, true);
//                        mActivity.changeMenuState();
//                    }
//                }
            }

        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public void init(Cursor cursor) {
        if (cursor == null) {
            return;
        }

        albums = DataConvertUtil.ConvertToAlbum(cursor);
        int numOfAlbum = albums == null? 0 : albums.size();
        if (numOfAlbum == 0) {
            mActivity.updateHeader(0, 0);
            mActivity.finish();
        } else {
            Log.e("liumx", "albums.size():" + albums.size());
            mActivity.updateHeader(numOfAlbum, getTotalTracks());
            mAdapter = new HBAlbumListAdapter(getActivity(), albums);
            setListAdapter(mAdapter);
            mListView.setVisibility(View.VISIBLE);

        }
    }

    public int getTotalTracks() {
        int count = 0;
        for (int i = 0; i < albums.size(); i++) {
            count += albums.get(i).getTrackNumber();
        }
        return count;
    }

    private Cursor getQueryCursor(AsyncQueryHandler async, String filter) {

        String id_row = DataConvertUtil.TRACK_ALBUM_ID + " AS "
                + DataConvertUtil.ALBUM_ID;
        String albumdate_row = "MAX(" + DataConvertUtil.TRACK_YEAR + ") AS "
                + DataConvertUtil.ALBUM_RELEASE_DATE;
        String numsongs_row = "count(*) AS "
                + DataConvertUtil.ALBUM_TRACK_NUMBER;
        String[] cols = new String[] { id_row,
                DataConvertUtil.TRACK_ALBUM_NAME,
                DataConvertUtil.TRACK_ARTIST_NAME, numsongs_row, albumdate_row ,DataConvertUtil.TRACK_DATA};//fix bug 17671
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        StringBuilder where = new StringBuilder();

        Cursor ret = null;
        if (mActivity.getArtistId() != null) {

            where.append(Globals.QUERY_SONG_FILTER+ HBMusicUtil.getFileString(mActivity) + " AND "
                    + MediaStore.Audio.Media.ARTIST_ID + "="
                    + mActivity.getArtistId() + ") GROUP BY ("
                    + DataConvertUtil.TRACK_ALBUM_ID);

            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon()
                        .appendQueryParameter("filter", Uri.encode(filter))
                        .build();
            }
            if (async != null) {
                async.startQuery(0, null, uri, cols, where.toString(), null,
                        DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            } else {
                ret = MusicUtils.query(mActivity, uri, cols, where.toString(),
                        null, DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            }
        } else {
            where.append(Globals.QUERY_SONG_FILTER+HBMusicUtil.getFileString(mActivity) + ") GROUP BY ("
                    + DataConvertUtil.TRACK_ALBUM_ID);

            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon()
                        .appendQueryParameter("filter", Uri.encode(filter))
                        .build();
            }
            if (async != null) {
                async.startQuery(0, null, uri, cols, where.toString(), null,
                        DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            } else {
                ret = MusicUtils.query(mActivity, uri, cols, where.toString(),
                        null, DataConvertUtil.ALBUM_RELEASE_DATE + " desc, "+ MediaStore.Audio.Media.ALBUM_KEY);
            }
        }
        return ret;
    }

    @Override
    public void onPause() {
//        mListView.hbOnPause();
//		if (mAdapter != null) {
//			mAdapter.imageCachePause();
//		}
        super.onPause();
    }

    @Override
    public void onResume() {
//        mListView.hbOnResume();
        if (mAdapter != null) {
//			mAdapter.imageCacheResume();
            mAdapter.notifyDataSetChanged();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
//		if (mAdapter != null) {
//			mAdapter.imageCacheDestroy();
//		}
        super.onDestroy();
    }

    public void enterEditMode() {
//        if (!mActivity.getHbActionBar().hbIsEntryEditModeAnimRunning()) {
//            mActivity.getHbActionBar().setShowBottomBarMenu(true);
//            mActivity.getHbActionBar().showActionBarDashBoard();
//        }
//        mActivity.getIvPlayAll().setEnabled(false);
//        mListView.hbSetNeedSlideDelete(false);
//        mListView.hbEnableSelector(false);
//        mAdapter.setEditMode(true);
//        mListView.setSelector(android.R.color.transparent);
    }

    public void  quitEditMode() {
        mActivity.setPlayAnimation();
//        if (!mActivity.getHbActionBar().hbIsExitEditModeAnimRunning()) {
//            mActivity.getHbActionBar().setShowBottomBarMenu(false);
//            mActivity.getHbActionBar().showActionBarDashBoard();
//        }
        ((TextView) mActivity.btn_selectAll).setText(mActivity.selectAll);
        mActivity.getIvPlayAll().setEnabled(true);
        getAdapter().setEditMode(false);
        getAdapter().notifyDataSetChanged();
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                mActivity.setPlayAnimation();
            }
        }, 500);
//        mListView.hbSetNeedSlideDelete(true);
//        mListView.hbEnableSelector(true);
        mListView.setSelector(R.drawable.hb_playlist_item_clicked);
    }

    public HBAlbumListAdapter getAdapter() {
        return mAdapter;
    }

    public HbListView getHbListView() {
        return mListView;
    }

    public ArrayList<Long> getAlbumsCheckedId() {
        selectedNumAlbum = 0;

        ArrayList<Long> checkedIds = new ArrayList<Long>();
        int numAlbum = mAdapter.getCount();
        for (int index = 0 ; index < numAlbum; index ++) {
            Log.e("liumx","index:"+index+"---"+mAdapter.getCheckedArrayValue(index));
            if (mAdapter.getCheckedArrayValue(index)) {
                HBAlbum album = (HBAlbum) mAdapter.getItem(index);
                selectedNumAlbum ++;
                long mCurrentAlbumId = album.getAlbumId();
                Log.e("liumx","position:"+index+" mselectedeAlbumId :"+"-----"+mCurrentAlbumId);
                checkedIds.add(mCurrentAlbumId);
            }
        }
        return checkedIds;
    }

    public boolean deleteAlbums() {
         AlertDialog mDeleteDialog = new  AlertDialog.Builder(mActivity,
                 AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(R.string.delete)
                .setMessage(R.string.dialog_delete_albums_con_message)
                .setNegativeButton(android.R.string.cancel,
                        null)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                realRemoveAlbums();
                                quitEditMode();
                                mAdapter.notifyDataSetChanged();
                                isNoAlbums();

                            }
                        }).create();//modify by tangjie 2014/08/25
        mDeleteDialog.show();
        return true;
    }

    private void realRemoveAlbums() {
        long aid = Long.parseLong(mActivity.getArtistId());
        long[] list = MusicUtils.getSongListForHBAlbums(
                mActivity, getAlbumsCheckedId(), aid);
        MusicUtils.deleteTracks(mActivity, list);
        mActivity.updateHeader(mActivity.getNumAlbums() - selectedNumAlbum, mActivity.getNumTracks() - list.length);
        getQueryCursor(mAlbumQueryHandler, null);
    }

    public boolean addAlbumsToPlaylist() {
        long aid = Long.parseLong(mActivity.getArtistId());
        long[] idList = MusicUtils.getSongListForHBAlbums(
                mActivity, getAlbumsCheckedId(), aid);
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < idList.length; i++) {
            list.add(String.valueOf(idList[i]));
        }
        DialogUtil.showAddDialog(mActivity,
                list, mAddPlaylistSuccessListener);
        return true;
    }

    public void isNoAlbums() {
        mActivity.setResult(Globals.RESULT_CODE_MODIFY);
        if (albums.isEmpty()) {
            mActivity.setResult(Globals.RESULT_CODE_MODIFY);
            mActivity.finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Globals.RESULT_CODE_MODIFY) {
            getQueryCursor(mAlbumQueryHandler, null);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void reloadData() {
        getQueryCursor(mAlbumQueryHandler, null);
    }
}
