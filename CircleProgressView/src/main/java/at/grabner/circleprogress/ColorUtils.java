package at.grabner.circleprogress;

import android.graphics.Color;
import android.support.annotation.ColorInt;

public class ColorUtils {

    public static int getRGBGradient(@ColorInt int startColor, @ColorInt int endColor, float proportion) {

        int[] rgb = new int[3];
        rgb[0] = interpolate(Color.red(startColor), Color.red(endColor), proportion);
        rgb[1] = interpolate(Color.green(startColor), Color.green(endColor), proportion);
        rgb[2] = interpolate(Color.blue(startColor), Color.blue(endColor), proportion);
        return Color.argb(255, rgb[0], rgb[1], rgb[2]);
    }

    private static int interpolate(float a, float b, float proportion) {
        return Math.round((a * ( proportion)) + (b * (1-proportion)));
    }
}
