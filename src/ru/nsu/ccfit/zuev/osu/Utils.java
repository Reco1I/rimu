package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import com.osudroid.math.Vector2;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


import java.util.Arrays;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;

public class Utils {
    private static final String FSReservedChars = "|\\?*<\":>+[]/";
    private static int accSign = 0;

    public static <T> T[] oneObjectArray(T object, T[] ary) {
        Arrays.fill(ary, object);
        return ary;
    }

    public static void setAccelerometerSign(final int sign) {
        accSign = sign;
    }

    public static int getAccselerometerSign() {
        return accSign;
    }

    public static float sqr(final float x) {
        return (x * x);
    }

    public static Vector2 inter(final Vector2 v1, final Vector2 v2,
                               final float percent) {
        return new Vector2(v1.x * (1 - percent) + v2.x * percent, v1.y
                * (1 - percent) + v2.y * percent);
    }

    public static Vector2 trackToRealCoords(final Vector2 coords) {
        return trackToRealCoords(coords, false);
    }

    public static Vector2 trackToRealCoords(final Vector2 pos, final boolean flipVertically) {
        pos.x *= Constants.MAP_ACTUAL_WIDTH / (float) Constants.MAP_WIDTH;
        pos.y *= Constants.MAP_ACTUAL_HEIGHT / (float) Constants.MAP_HEIGHT;

        pos.y += (Config.getRES_HEIGHT() - toRes(Constants.MAP_ACTUAL_HEIGHT)) / 2f;
        pos.x += (Config.getRES_WIDTH() - toRes(Constants.MAP_ACTUAL_WIDTH)) / 2f;

        if (flipVertically) {
            flipRealCoordsVertically(pos);
        }
        return pos;
    }

    public static void flipRealCoordsVertically(final Vector2 coords) {
        coords.y -= Config.getRES_HEIGHT() / 2f;
        coords.y *= -1;
        coords.y += Config.getRES_HEIGHT() / 2f;
    }

    public static void changeTrackToRealCoords(final Vector2 coords) {
        final Vector2 pos = scaleToRealC(coords);
        pos.y += (Config.getRES_HEIGHT() - toRes(Constants.MAP_ACTUAL_HEIGHT)) / 2f;
        pos.x += (Config.getRES_WIDTH() - toRes(Constants.MAP_ACTUAL_WIDTH)) / 2f;
        if (GameHelper.isHardRock()) {
            pos.y -= (float) Config.getRES_HEIGHT() / 2;
            pos.y *= -1;
            pos.y += (float) Config.getRES_HEIGHT() / 2;
        }
    }

    public static Vector2 realToTrackCoords(final Vector2 coords) {
        return realToTrackCoords(coords, Config.getRES_WIDTH(), Config.getRES_HEIGHT(), false);
    }

    public static Vector2 realToTrackCoords(final Vector2 coords, float width, float height, boolean isOld) {
        final Vector2 pos = new Vector2(coords.x, coords.y);
        if (GameHelper.isHardRock()) {
            pos.y -= height / 2;
            pos.y *= -1;
            pos.y += height / 2;
        }
        pos.y -= (height - toRes((isOld ? Constants.MAP_ACTUAL_HEIGHT_OLD : Constants.MAP_ACTUAL_HEIGHT))) / 2f;
        pos.x -= (width - toRes((isOld ? Constants.MAP_ACTUAL_WIDTH_OLD : Constants.MAP_ACTUAL_WIDTH))) / 2f;
        return scaleToTrack(pos, isOld);
    }

    public static short flipY(final short y) {
        final int height = Config.getRES_HEIGHT() / 2;
        return (short) (((y - height) * -1) + height);
    }

    public static Vector2 scaleToRealC(final Vector2 v) {
        v.x *= toRes(Constants.MAP_ACTUAL_WIDTH) / (float) Constants.MAP_WIDTH;
        v.y *= toRes(Constants.MAP_ACTUAL_HEIGHT)
                / (float) Constants.MAP_HEIGHT;
        return v;
    }

    public static Vector2 scaleToTrack(final Vector2 v, boolean isOld) {
        final Vector2 pos = new Vector2(v.x, v.y);
        pos.x *= Constants.MAP_WIDTH
                / toRes((float) (isOld ? Constants.MAP_ACTUAL_WIDTH_OLD : Constants.MAP_ACTUAL_WIDTH));
        pos.y *= Constants.MAP_HEIGHT
                / toRes((float) (isOld ? Constants.MAP_ACTUAL_HEIGHT_OLD : Constants.MAP_ACTUAL_HEIGHT));
        return pos;
    }

    public static float direction(final float x, final float y) {
        if (x == 0 && y == 0) {
            return 0;
        }

        return (float) Math.atan2(y, x);
    }

    public static float direction(final Vector2 vector) {
        return direction(vector.x, vector.y);
    }

    public static float direction(float x1, float y1, float x2, float y2) {
        return direction(x2 - x1, y2 - y1);
    }

    public static float length(final Vector2 vector) {
        return (float) Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    public static Vector2 normalize(final Vector2 vector) {
        final float len = length(vector);
        if (Math.abs(len) < 0.0001f) {
            return new Vector2(0, 0);
        }
        return new Vector2(vector.x / len, vector.y / len);
    }

    public static float distance(final Vector2 v1, final Vector2 v2) {
        return (float) Math.hypot(v1.x - v2.x, v1.y - v2.y);
    }

    public static float squaredDistance(final Vector2 v1, final Vector2 v2) {
        return ((v2.x - v1.x) * (v2.x - v1.x) + (v2.y - v1.y) * (v2.y - v1.y));
    }

    public static float squaredDistance(final float v1x, final float v1y,
                                        final float v2x, final float v2y) {
        return ((v2x - v1x) * (v2x - v1x) + (v2y - v1y) * (v2y - v1y));
    }

    public static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    public static int toRes(final int i) {
        return i / Config.getTextureQuality();
    }

    public static float toRes(final float i) {
        return i / Config.getTextureQuality();
    }

    public static String toFSValidString(final String s) {
        final StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (FSReservedChars.indexOf(s.charAt(i)) == -1) {
                nameBuilder.append(s.charAt(i));
            }
        }
        return nameBuilder.toString();
    }

    /**
     * 判断当前网络是否是wifi网络
     *
     * @param context
     * @return boolean
     */
    public static boolean isWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    public static float tryParseFloat(String str, float defaultVal) {
        float val;
        try {
            val = Float.parseFloat(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
    }

    public static int tryParseInt(String str, int defaultVal) {
        int val;
        try {
            val = Integer.parseInt(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
    }

    public static double tryParseDouble(String str, double defaultVal) {
        double val;
        try {
            val = Double.parseDouble(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
    }
}
