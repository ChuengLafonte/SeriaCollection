package id.seria.collection.utils;

import java.util.TreeMap;

public class GuiUtils {

    private static final TreeMap<Integer, String> ROMAN_MAP = new TreeMap<>();

    static {
        ROMAN_MAP.put(1000, "M");
        ROMAN_MAP.put(900, "CM");
        ROMAN_MAP.put(500, "D");
        ROMAN_MAP.put(400, "CD");
        ROMAN_MAP.put(100, "C");
        ROMAN_MAP.put(90, "XC");
        ROMAN_MAP.put(50, "L");
        ROMAN_MAP.put(40, "XL");
        ROMAN_MAP.put(10, "X");
        ROMAN_MAP.put(9, "IX");
        ROMAN_MAP.put(5, "V");
        ROMAN_MAP.put(4, "IV");
        ROMAN_MAP.put(1, "I");
    }

    public static String toRoman(int number) {
        int l = ROMAN_MAP.floorKey(number);
        if (number == l) {
            return ROMAN_MAP.get(number);
        }
        return ROMAN_MAP.get(l) + toRoman(number - l);
    }

    public static String getProgressBar(int current, int max, int totalBars, char symbol, String completedColor, String notCompletedColor) {
        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);
        if (progressBars > totalBars) progressBars = totalBars;

        return completedColor + String.valueOf(symbol).repeat(progressBars)
                + notCompletedColor + String.valueOf(symbol).repeat(totalBars - progressBars);
    }
}
