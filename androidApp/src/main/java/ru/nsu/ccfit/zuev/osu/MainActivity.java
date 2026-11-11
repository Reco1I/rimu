package ru.nsu.ccfit.zuev.osu;

import android.content.Intent;

import com.osudroid.android.AndroidOsuGameHost;

import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * The game's activity, which launches it: the game and its host are {@link AndroidOsuGameHost}'s.
 */
public class MainActivity extends AndroidOsuGameHost {

    @Override
    protected void openPermissionScreen() {
        startActivity(new Intent(this, PermissionActivity.class));
        finish();
    }
}
