//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package gg.sunken.shop.utils;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class StringUtils {
    private static final DecimalFormat formatter = new DecimalFormat("#,###.#");
    private static final String[] SUFFIXES = new String[]{"k", "M", "B", "T", "Q", "Qt", "Sx", "Sp", "Oct", "Non", "Dec", "UDec", "DDec", "TDec", "QDec", "QtDec"};
    public static final char[] VOWELS = new char[]{'a', 'e', 'i', 'o', 'u'};

    public StringUtils() {
    }

    public static boolean validateEnum(String input, Class<? extends Enum> enumType) {
        if (enumType == null) {
            return false;
        } else {
            try {
                Enum.valueOf(enumType, input);
                return true;
            } catch (IllegalArgumentException var3) {
                return false;
            }
        }
    }

    public static List<String> getEnumNames(Class<? extends Enum<?>> enumType) {
        List<String> list = new ArrayList();

        for(Enum<?> anEnum : (Enum[])enumType.getEnumConstants()) {
            String string = anEnum.toString();
            list.add(string);
        }

        return list;
    }

    public static String capitalizeString(String string) {
        StringBuilder output = new StringBuilder();

        for(String s : string.split(" ")) {
            output.append(" ").append(s.substring(0, 1).toUpperCase(Locale.ROOT)).append(s.substring(1).toLowerCase(Locale.ROOT));
        }

        return output.toString().trim();
    }

    public static String formatEnum(String enumValue) {
        return enumValue == null ? null : capitalizeString(enumValue.replaceAll("_", " "));
    }

    public static String formatEnum(Enum<?> enumValue) {
        return enumValue == null ? null : capitalizeString(enumValue.toString().replaceAll("_", " "));
    }

    public static String getProgressBar(int current, int max, int totalBars, String symbol) {
        if (max == 0) {
            current = 1;
            max = 1;
        }

        if (current > max) {
            current = max;
        }

        float percent = (float)current / (float)max;
        int progressBars = (int)((float)totalBars * percent);
        int leftOver = totalBars - progressBars;
        String var10000 = String.valueOf(symbol).repeat(Math.max(0, progressBars));
        return var10000 + String.valueOf(symbol).repeat(Math.max(0, leftOver));
    }

    public static String findDifferenceDays(long time1, long time2) {
        long differenceInTime = time1 - time2;
        long var10000 = TimeUnit.MILLISECONDS.toDays(differenceInTime);
        return var10000 + "d, " + TimeUnit.MILLISECONDS.toHours(differenceInTime) % 24L + "h, " + TimeUnit.MILLISECONDS.toMinutes(differenceInTime) % 60L + "m, " + TimeUnit.MILLISECONDS.toSeconds(differenceInTime) % 60L + "s";
    }

    public static String findDifferenceHour(long time1, long time2) {
        long differenceInTime = time1 - time2;
        long var10000 = TimeUnit.MILLISECONDS.toHours(differenceInTime) % 24L;
        return var10000 + "h, " + TimeUnit.MILLISECONDS.toMinutes(differenceInTime) % 60L + "m, " + TimeUnit.MILLISECONDS.toSeconds(differenceInTime) % 60L + "s";
    }

    public static String findDifferenceMinutes(long time1, long time2) {
        long differenceInTime = time1 - time2;
        long var10000 = TimeUnit.MILLISECONDS.toMinutes(differenceInTime) % 60L;
        return var10000 + "m, " + TimeUnit.MILLISECONDS.toSeconds(differenceInTime) % 60L + "s";
    }

    public static String timeUntilMinutes(long time1) {
        return findDifferenceMinutes(time1, System.currentTimeMillis());
    }

    public static String timeUntilDays(long time1) {
        return findDifferenceDays(time1, System.currentTimeMillis());
    }

    public static String timeUntilHours(long time1) {
        return findDifferenceHour(time1, System.currentTimeMillis());
    }

    public static String timeUntil(long time1) {
        long differenceInTime = time1 - System.currentTimeMillis();
        long days = TimeUnit.MILLISECONDS.toDays(differenceInTime);
        long hours = TimeUnit.MILLISECONDS.toHours(differenceInTime) % 24L;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(differenceInTime) % 60L;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(differenceInTime) % 60L;
        if (days == 0L && hours == 0L && minutes == 0L && seconds == 0L) {
            return "0s";
        } else if (days == 0L && hours == 0L && minutes == 0L) {
            return seconds + "s";
        } else if (days == 0L && hours == 0L) {
            return minutes + "m, " + seconds + "s";
        } else {
            return days == 0L ? hours + "h, " + minutes + "m, " + seconds + "s" : days + "d, " + hours + "h, " + minutes + "m, " + seconds + "s";
        }
    }

    public static String comma(int number) {
        return formatter.format((long)number);
    }

    public static String comma(double number) {
        return formatter.format(number);
    }

    public static String abbreviate(int number) {
        return abbreviate((double)number, 0);
    }

    public static String abbreviate(double number) {
        return abbreviate(number, 0);
    }

    private static String abbreviate(double number, int iteration) {
        if (number < (double)1000.0F) {
            return String.valueOf(number);
        } else {
            double formattedNumber = number / (double)1000.0F;
            boolean isRounded = formattedNumber * (double)10.0F % (double)10.0F == (double)0.0F;
            if (formattedNumber < (double)1000.0F) {
                if (isRounded) {
                    return (int)formattedNumber + SUFFIXES[iteration];
                } else {
                    String var10000 = String.format("%.2f", formattedNumber);
                    return var10000 + SUFFIXES[iteration];
                }
            } else {
                return abbreviate(formattedNumber, iteration + 1);
            }
        }
    }

    public static long timeFromString(String input, TimeUnit timeUnit) {
        int seconds = 0;
        int minutes = 0;
        int hours = 0;
        int days = 0;
        int weeks = 0;
        String[] split = input.split(" ");

        for(String s : split) {
            if (s.endsWith("s")) {
                seconds = Integer.parseInt(s.substring(0, s.length() - 1));
            } else if (s.endsWith("m")) {
                minutes = Integer.parseInt(s.substring(0, s.length() - 1));
            } else if (s.endsWith("h")) {
                hours = Integer.parseInt(s.substring(0, s.length() - 1));
            } else if (s.endsWith("d")) {
                days = Integer.parseInt(s.substring(0, s.length() - 1));
            } else if (s.endsWith("w")) {
                weeks = Integer.parseInt(s.substring(0, s.length() - 1));
            }
        }

        return timeUnit.convert((long)weeks * 7L * 24L * 60L * 60L + (long)days * 24L * 60L * 60L + (long)hours * 60L * 60L + (long)minutes * 60L + (long)seconds, TimeUnit.SECONDS);
    }

    public static String wrapLine(String line, int lineLength) {
        if (line.length() == 0) {
            return "\n";
        } else if (line.length() <= lineLength) {
            return line + "\n";
        } else {
            String[] words = line.split(" ");
            StringBuilder allLines = new StringBuilder();
            StringBuilder trimmedLine = new StringBuilder();

            for(String word : words) {
                if (trimmedLine.length() + 1 + word.length() <= lineLength) {
                    trimmedLine.append(word).append(" ");
                } else {
                    allLines.append(trimmedLine).append("\n");
                    trimmedLine = new StringBuilder();
                    trimmedLine.append(word).append(" ");
                }
            }

            if (trimmedLine.length() > 0) {
                allLines.append(trimmedLine);
            }

            allLines.append("\n");
            return allLines.toString();
        }
    }

    public static boolean onlyContains(String text, char[] chars) {
        for(char c : text.toCharArray()) {
            boolean found = false;

            for(char c1 : chars) {
                if (c == c1) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    public static boolean isAlphaNumeric(String text) {
        return onlyContains(text, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray());
    }

    public static List<String> wrap(String string, int lineLength, String prefix, String suffix) {
        StringBuilder b = new StringBuilder();

        for(String line : string.split(Pattern.quote("\n"))) {
            b.append(wrapLine(line, lineLength));
        }

        List<String> output = new ArrayList();

        for(String s : Arrays.stream(b.toString().split("\n")).toList()) {
            output.add(prefix + s + suffix);
        }

        return output;
    }

    public static String getProgressBar(int current, int max, int totalBars, String symbol, String completedColor, String notCompletedColor) {
        float percent = (float)current / (float)max;
        int progressBars = (int)((float)totalBars * percent);
        return completedColor + symbol.repeat(Math.max(0, progressBars)) + notCompletedColor + symbol.repeat(Math.max(0, totalBars - progressBars));
    }

    public static List<String> partialCompletions(String input, List<String> options) {
        List<String> matches = new ArrayList();
        if (input != null && options != null) {
            String lowerInput = input.toLowerCase(Locale.ROOT);

            for(String option : options) {
                if (option.toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                    matches.add(option);
                }
            }

            return matches;
        } else {
            return matches;
        }
    }

    public static List<String> partialCompletions(String input, String[] options) {
        List<String> matches = new ArrayList();
        if (input != null && options != null) {
            String lowerInput = input.toLowerCase(Locale.ROOT);

            for(String option : options) {
                if (option.toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                    matches.add(option);
                }
            }

            return matches;
        } else {
            return matches;
        }
    }

    public static List<String> partialCompletion(List<String> completions, String input) {
        return partialCompletions(input, completions);
    }

    public static List<String> partialCompletion(String[] completions, String input) {
        return partialCompletions(input, completions);
    }

    public static String camelCaseToDashCase(String input) {
        StringBuilder output = new StringBuilder();

        for(char c : input.toCharArray()) {
            if (Character.isUpperCase(c)) {
                output.append("-").append(Character.toLowerCase(c));
            } else {
                output.append(c);
            }
        }

        return output.toString();
    }

    public static String dashCaseToCamelCase(String input) {
        StringBuilder output = new StringBuilder();
        boolean capitalize = false;

        for(char c : input.toCharArray()) {
            if (c == '-') {
                capitalize = true;
            } else if (capitalize) {
                output.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                output.append(c);
            }
        }

        return output.toString();
    }

    public static @NotNull String getArticle(@NotNull String string) {
        if (string.isEmpty()) {
            return "";
        } else {
            char firstChar = Character.toLowerCase(string.charAt(0));

            for(char vowel : VOWELS) {
                if (firstChar == vowel) {
                    return "an";
                }
            }

            return "a";
        }
    }
}
