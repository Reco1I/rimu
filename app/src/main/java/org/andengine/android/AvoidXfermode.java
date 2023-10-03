package org.andengine.android;

import android.graphics.Xfermode;

public class AvoidXfermode extends Xfermode {
    // these need to match the enum in AvoidXfermode.h on the native side
    public enum Mode {
        AVOID   (0),    //!< draw everywhere except on the opColor
        TARGET  (1);    //!< draw only on top of the opColor

        Mode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public AvoidXfermode(int opColor, int tolerance, Mode mode) {
        if (tolerance < 0 || tolerance > 255) {
            throw new IllegalArgumentException("tolerance must be 0..255");
        }
    }
}
