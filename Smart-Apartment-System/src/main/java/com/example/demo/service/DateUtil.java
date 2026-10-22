package com.example.demo.service;

import com.example.demo.exception.ApiException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class DateUtil {
    private DateUtil() {}

    public static LocalDate parse(String field, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + ": use the format yyyy-MM-dd");
        }
    }

    public static void order(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to))
            throw new ApiException(HttpStatus.BAD_REQUEST, "from: cannot be after the 'to' date");
    }

    public static boolean within(LocalDate d, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (d == null) return false;
        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
    }
}