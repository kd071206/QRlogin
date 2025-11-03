package com.university.attendance.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeUtil {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DISPLAY_FORMAT);
    }

    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_FORMAT);
    }

    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(TIME_FORMAT);
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DISPLAY_FORMAT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid date format: " + dateTimeStr, ex);
            }
        }
    }

    public static int calculateDurationMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }

        Duration duration = Duration.between(start, end);
        return (int) Math.ceil(duration.toMinutes());
    }

    public static String formatDuration(int minutes) {
        if (minutes <= 0) {
            return "0 minutes";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours == 0) {
            return String.format("%d minute%s", remainingMinutes, remainingMinutes == 1 ? "" : "s");
        } else if (remainingMinutes == 0) {
            return String.format("%d hour%s", hours, hours == 1 ? "" : "s");
        } else {
            return String.format("%d hour%s %d minute%s",
                               hours, hours == 1 ? "" : "s",
                               remainingMinutes, remainingMinutes == 1 ? "" : "s");
        }
    }

    public static String formatDurationFromStart(LocalDateTime startTime) {
        if (startTime == null) {
            return "N/A";
        }

        int minutes = calculateDurationMinutes(startTime, LocalDateTime.now());
        return formatDuration(minutes);
    }

    public static boolean isWithinTimeWindow(LocalDateTime timestamp, LocalDateTime reference, int windowMinutes) {
        if (timestamp == null || reference == null) {
            return false;
        }

        Duration duration = Duration.between(reference, timestamp);
        return Math.abs(duration.toMinutes()) <= windowMinutes;
    }

    public static LocalDateTime getStartOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atStartOfDay();
    }

    public static LocalDateTime getEndOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atTime(LocalTime.MAX);
    }

    public static boolean isSameDay(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.toLocalDate().equals(dt2.toLocalDate());
    }

    public static long getHoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start, end).toHours();
    }

    public static boolean isBusinessHours(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }

        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(LocalTime.of(8, 0)) && !time.isAfter(LocalTime.of(18, 0));
    }
}