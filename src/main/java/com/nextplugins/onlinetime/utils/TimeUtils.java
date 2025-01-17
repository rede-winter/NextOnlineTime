package com.nextplugins.onlinetime.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TimeUtils {
    DAY(86400000, "days", "day", "d", "dia", "dias"),
    HOUR(3600000, "hours", "hour", "h", "hora", "horas"),
    MINUTE(60000, "minutes", "minute", "m", "minuto", "minutos"),
    SECOND(1000, "seconds", "second", "s", "segundo", "segundos");

    private static final Pattern PATTERN = Pattern.compile("(\\d+)(\\s+)?([a-zA-Z]+)");
    private final long millis;
    private final List<String> formats;

    TimeUtils(long millis, String... formats) {
        this.millis = millis;
        this.formats = Arrays.asList(formats);
    }

    public static long unformat(String string) {
        Matcher matcher = PATTERN.matcher(string.replaceAll(",| e", ""));
        long time = 0;

        while (matcher.find()) {
            try {
                int value = Integer.parseInt(matcher.group(1));
                TimeUtils type = fromFormats(matcher.group(3));
                if (type != null) {
                    time += (value * type.getMillis());
                }
            } catch (Exception ignored) {
            }
        }

        return time;
    }

    public static String format(long value) {
        if (value <= 0) return "0 segundos";

        long days = TimeUnit.MILLISECONDS.toDays(value);
        long hours = TimeUnit.MILLISECONDS.toHours(value) - (days * 24);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(value) - (TimeUnit.MILLISECONDS.toHours(value) * 60);
        long second = TimeUnit.MILLISECONDS.toSeconds(value) - (TimeUnit.MILLISECONDS.toMinutes(value) * 60);

        long[] times = {days, hours, minutes, second};
        String[] names = {"dia", "hora", "minuto", "segundo"};

        List<String> values = new ArrayList<>();
        for (int index = 0; index < times.length; index++) {
            long time = times[index];
            if (time > 0) {
                String name = plural(times[index], names[index]);
                values.add(name);
            }
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        return String.join(", ", values.subList(0, values.size() - 1)) + " e " + values.get(values.size() - 1);
    }

    public static String formatOne(long time) {
        if (time == 0) return "0s";

        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) - (days * 24);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - (TimeUnit.MILLISECONDS.toHours(time) * 60);
        long second = TimeUnit.MILLISECONDS.toSeconds(time) - (TimeUnit.MILLISECONDS.toMinutes(time) * 60);

        if (days > 0) return plural(days, "dia");
        if (hours > 0) return plural(hours, "hora");
        if (minutes > 0) return plural(minutes, "minuto");
        if (second > 0) return plural(second, "segundo");

        return "0s";
    }

    public static String formatOneLetter(long time) {
        if (time == 0) return "0s";

        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) - (days * 24);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - (TimeUnit.MILLISECONDS.toHours(time) * 60);
        long second = TimeUnit.MILLISECONDS.toSeconds(time) - (TimeUnit.MILLISECONDS.toMinutes(time) * 60);

        if (days > 0) return days + "d";
        if (hours > 0) return hours + "h";
        if (minutes > 0) return minutes + "m";
        if (second > 0) return second + "s";

        return "0s";
    }

    public static String plural(long quantity, String message) {
        return quantity + " " + message + (quantity == 1 ? "" : "s");
    }

    public static TimeUtils fromFormats(String format) {
        return Arrays.stream(values())
                .filter(type -> type.getFormats().contains(format.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    public long getMillis() {
        return millis;
    }

    public List<String> getFormats() {
        return formats;
    }
}