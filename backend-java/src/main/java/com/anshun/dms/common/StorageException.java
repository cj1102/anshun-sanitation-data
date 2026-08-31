package com.anshun.dms.common;

/** A user-safe error raised when the object-storage service is unavailable or rejects a file. */
public class StorageException extends RuntimeException {
    public StorageException(String message) { super(message); }
    public StorageException(String message, Throwable cause) { super(message, cause); }
}
