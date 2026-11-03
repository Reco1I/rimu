package ru.nsu.ccfit.zuev.audio.effect;

import com.osudroid.beatmaps.timings.TimingControlPoint;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import com.osudroid.ui.v2.SkinSounds;

public class Metronome {


    private final BassSoundProvider kickSound = SkinSounds.get("nightcore-kick");
    private final BassSoundProvider finishSound = SkinSounds.get("nightcore-finish");
    private final BassSoundProvider clapSound = SkinSounds.get("nightcore-clap");
    private final BassSoundProvider hatSound = SkinSounds.get("nightcore-hat");

    private int lastBeatIndex = -1;

    public void update(float elapsedTime, TimingControlPoint activeTimingPoint) {
        float timingPointTime = (float) activeTimingPoint.time / 1000;

        if (elapsedTime - timingPointTime <= 0) {
            return;
        }

        float playSeconds = elapsedTime - timingPointTime;
        int beatIndex = (int) (playSeconds * 2 / (activeTimingPoint.msPerBeat / 1000));

        if (beatIndex < 0) {
            return;
        }
        if (beatIndex == lastBeatIndex) {
            return;
        }
        lastBeatIndex = beatIndex;

        int beatInBar = beatIndex % activeTimingPoint.timeSignature;

        // 每隔8小节在第4拍kick+finish
        if (beatIndex % (8 * activeTimingPoint.timeSignature) == 0) {
            kickSound.play();
            if (beatIndex > 0) {
                finishSound.play();
            }
            return;
        }
        // 每小节第4拍kick
        if (beatInBar % 4 == 0) {
            kickSound.play();
            return;
        }
        // 每小节第2拍clap
        if (beatInBar % 4 == 2) {
            clapSound.play();
            return;
        }
        // 每小节奇数拍hat
        hatSound.play();
    }

    public void setVolume(float volume) {
        kickSound.setVolume(volume);
        finishSound.setVolume(volume);
        clapSound.setVolume(volume);
        hatSound.setVolume(volume);
    }
}
