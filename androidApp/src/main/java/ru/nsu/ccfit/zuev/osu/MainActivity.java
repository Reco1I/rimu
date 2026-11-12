package ru.nsu.ccfit.zuev.osu;

import android.content.Intent;

import com.osudroid.android.AndroidOsuGameHost;

import ru.nsu.ccfit.zuev.osuplus.app.BuildConfig;

/**
 * The game's activity, which launches it: the game and its host are {@link AndroidOsuGameHost}'s.
 * Without access to the storage it opens {@link PermissionActivity} instead, which starts it again
 * once access is granted.
 */
public class MainActivity extends AndroidOsuGameHost {

    @Override
    protected String getBuildType() {
        return BuildConfig.BUILD_TYPE;
    }

    @Override
    public void onEngineInitialized() {
        if (!PermissionActivity.hasStorageAccess(this)) {
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
            return;
        }
        super.onEngineInitialized();
    }
}
