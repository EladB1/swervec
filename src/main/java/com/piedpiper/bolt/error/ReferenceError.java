package com.piedpiper.bolt.error;

public class ReferenceError extends SourceCodeError {
    public ReferenceError(String message) {
        super(message);
    }

    public ReferenceError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
