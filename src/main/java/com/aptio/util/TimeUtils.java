package com.aptio.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeUtils {

    public static boolean doTimeRangesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return (start1.isBefore(end2) && end1.isAfter(start2)) ||
                start1.equals(start2) || end1.equals(end2);
    }

    public static String formatTime(LocalTime time) {
        if (time == null) return "";
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return null;

        String[] parts = timeStr.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Time string must be in HH:MM format");
        }

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        return LocalTime.of(hour, minute);
    }

    public static LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
}