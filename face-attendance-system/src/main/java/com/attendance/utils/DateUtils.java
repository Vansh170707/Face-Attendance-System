package com.attendance.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date and time utility functions
 */
public class DateUtils {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a");

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "-";
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMAT) : "-";
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMAT) : "-";
    }

    public static LocalDate getStartOfWeek() {
        return LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
    }

    public static LocalDate getStartOfMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }
}
