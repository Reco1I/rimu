package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.reco1l.verktex.Logger;

import androidx.annotation.Nullable;

import java.io.File;

import com.osudroid.audio.SongPlayer;
import com.reco1l.verktex.Platform;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.MainActivity;


public class SongService extends Service {
    public static final int defaultFrequency = 44100;
    private SongPlayer audioFunc;
    private boolean isGaming = false;
    // private boolean isSettingMenu = false;

    /**
     * Starts the audio driver of the platform.
     */
    public static void initAudio() {
        Platform.INSTANCE.getAudioDriver().enumerateDevices();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (audioFunc == null) {
            audioFunc = new SongPlayer();
        }
        return new ReturnBindObject();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("Service unbind");
        exit();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        System.out.println("onReBind");
    }

    public boolean preLoad(String filePath) {
        return preLoad(filePath, 1, false, false);
    }

    public boolean preLoad(String filePath, float speed, boolean adjustPitch) {
        return preLoad(filePath, speed, adjustPitch, false);
    }

    public boolean preLoad(String filePath, float speed, boolean adjustPitch, boolean isLoop) {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) {
                return false;
            }

            return audioFunc.preLoad(filePath, speed, adjustPitch, isLoop);
        }
        return false;
    }

    public void play() {
        if (audioFunc == null) return;
        audioFunc.play();
    }

    public void pause() {
        if (audioFunc == null) return;
        audioFunc.pause();
    }

    public boolean stop() {
        if (audioFunc == null) return false;
        return audioFunc.stop();
    }

    public boolean exit() {
        Logger.w("SongService", "Hei Service is on EXIT()");
        if (audioFunc == null) return false;
        audioFunc.stop();
        Platform.INSTANCE.getAudioDriver().dispose();
        stopSelf();
        return true;
    }

    public void seekTo(int time) {
        if (audioFunc == null) return;
        Logger.i("SongService", "Seeking to " + time + " ms: " + (audioFunc.jump(time) ? "Success" : "Failed"));
    }

    public boolean isGaming() {
        return isGaming;
    }

    public void setGaming(boolean isGaming) {
        audioFunc.setGaming(isGaming);
        Logger.w("Gaming Mode", "In Gamming mode :" + isGaming);
        this.isGaming = isGaming;
    }

    /*
    public boolean isSettingMenu() {
        return isSettingMenu;
    }

    public void setIsSettingMenu(boolean isSettingMenu) {
        this.isSettingMenu = isSettingMenu;
    } */

    public Status getStatus() {
        if (audioFunc != null) {
            return audioFunc.getStatus();
        }
        return Status.STOPPED;
    }

    public int getPosition() {
        return (int) getPositionPrecise();
    }

    public double getPositionPrecise() {
        if (audioFunc != null) {
            return audioFunc.getPosition();
        }
        return 0;
    }

    public int getLength() {
        if (audioFunc != null) {
            return audioFunc.getLength();
        }
        return 0;
    }

    public float[] getSpectrum() {
        if (audioFunc != null) {
            return audioFunc.getSpectrum();
        }
        return new float[0];
    }

    @Nullable
    public float[] getAudioFFT(int resolution) {
        if (audioFunc != null) {
            return audioFunc.getAudioFFT(resolution);
        }
        return null;
    }

    public float getVolume() {
        if (audioFunc != null) {
            return audioFunc.getVolume();
        }
        return 0;
    }

    public void setVolume(float volume) {
        if (audioFunc != null) {
            audioFunc.setVolume(volume);
        }
    }

    public float getSpeed() {
        if (audioFunc != null) {
            return audioFunc.getSpeed();
        }
        return 0;
    }

    public void setSpeed(float speed) {
        if (audioFunc != null) {
            audioFunc.setSpeed(speed);
        }
    }

    public void setAdjustPitch(boolean adjustPitch) {
        if (audioFunc != null) {
            audioFunc.setAdjustPitch(adjustPitch);
        }
    }

    public void setPitchRate(float pitchRate) {
        if (audioFunc != null) {
            audioFunc.setPitchRate(pitchRate);
        }
    }

    public void setFrequencyForcefully(float frequency) {
        if (audioFunc != null) {
            audioFunc.setFrequencyForcefully(frequency);
        }
    }

    public float getFrequency() {
        if (audioFunc != null) {
            return audioFunc.getFrequency();
        }
        return 0f;
    }

    public boolean checkFileExist(String path) {
        if (path == null) return false;
        if (path.trim().isEmpty()) return false;
        else {
            File songFile = new File(path);
            if (!songFile.exists()) return false;
        }
        return true;
    }

    public boolean isRunningForeground() {
        return MainActivity.isActivityVisible();
    }

    public class ReturnBindObject extends Binder {
        public SongService getObject() {
            return SongService.this;
        }
    }

}
