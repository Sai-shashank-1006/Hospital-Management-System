package com.hms.dao;

/** Unchecked wrapper so servlets are not forced to catch {@link java.sql.SQLException}. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
