package com.protruly.music.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.protruly.music.R;

/**
 * Created by xiaobin on 17-9-26.
 */

public class HBArtistMusicActivity extends AbstractBaseActivity {

    public static final String ARTIST_NAME = "artist_name";
    public static final String ARTIST_ID = "artist_id";

    private ImageView toolbar_back;
    private TextView toolbar_title;

    private String artistName = "";
    private int artistId;

    private HBMusicTagFragment musicTagFragment;
    private String artistMusicTag = "artistMusicTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_artist_music);

        initData();
        initView();
    }

    private void initData() {
        artistName = getIntent().getStringExtra(ARTIST_NAME);
        artistId = getIntent().getIntExtra(ARTIST_ID, 0);

        musicTagFragment = HBMusicTagFragment.newInstance(artistId);
    }

    private void initView() {
        toolbar_back = (ImageView) findViewById(R.id.toolbar_back);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);

        toolbar_title.setText(artistName);
        toolbar_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.add(R.id.ll_content, musicTagFragment, artistMusicTag);
        transaction.commit();
        fragmentManager.executePendingTransactions();
    }

    @Override
    public void onMediaDbChange(boolean selfChange) {

    }

}
