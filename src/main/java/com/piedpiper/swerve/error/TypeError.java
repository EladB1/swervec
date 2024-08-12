package com.piedpiper.swerve.error;

public class TypeError extends SourceCodeError {
    public TypeError(String message) {
        super(message);
    }

    public TypeError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
