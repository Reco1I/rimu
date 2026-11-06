package ru.nsu.ccfit.zuev.audio.serviceAudio;

import com.osudroid.android.AndroidGame;
import android.app.Application;

import ru.nsu.ccfit.zuev.osu.AppException;
import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class SaveServiceObject extends Application {

    static private SongService songService;

    public static void finishAllActivities() {
        if (AndroidGame.getMainActivity() != null)
            AndroidGame.getMainActivity().finish();
    }

    public SongService getSongService() {
        return songService;
    }

    public void setSongService(SongService object) {
        songService = object;
        if (songService != null) {
            System.out.println("SongService Created!");
        } else {
            System.out.println("SongService is NULL");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler());
    }
}
