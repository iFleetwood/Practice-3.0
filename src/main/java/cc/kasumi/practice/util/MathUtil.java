package cc.kasumi.practice.util;

public class MathUtil {

    public static double round(double value, int precision) {
        double scale = Math.pow(10, precision);
        return Math.round(value * scale) / scale;
    }
}
