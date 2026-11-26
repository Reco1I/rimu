package ru.nsu.ccfit.zuev.osu;

import static android.widget.LinearLayout.LayoutParams.*;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import java.util.Collections;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import ru.nsu.ccfit.zuev.osuplus.R;

public class PermissionActivity extends AppCompatActivity {

    // Android 10 and older: the storage permission. Once it's denied for good, the app's settings are
    // offered instead.
    private final ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    checkPermissions();
                } else if (EasyPermissions.somePermissionPermanentlyDenied(this,
                        Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                    new AppSettingsDialog.Builder(this).build().show();
                }
            });

    /**
     * Whether the game can access the storage, which this screen asks for when it can't.
     */
    public static boolean hasStorageAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return PermissionChecker.checkCallingOrSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PermissionChecker.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout view = new LinearLayout(this);
        view.setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));
        view.setBackgroundColor(0x00000000);

        setContentView(view);
    }

    private void startGameActivity() {
        Intent game = new Intent(this, MainActivity.class);
        startActivity(game);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        if (hasStorageAccess(this)) {
            startGameActivity();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Access is checked again in onResume, once the player is back from the settings.
            Uri uri = Uri.parse("package:" + getPackageName());
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
        } else {
            requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }
}
