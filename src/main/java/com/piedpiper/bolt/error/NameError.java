package com.piedpiper.bolt.error;

public class NameError extends SourceCodeError {
    String message;

    public NameError(String message) {
        super(message);
    }

    public NameError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
