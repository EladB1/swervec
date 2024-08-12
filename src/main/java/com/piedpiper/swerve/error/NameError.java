package com.piedpiper.swerve.error;

public class NameError extends SourceCodeError {
    public NameError(String message) {
        super(message);
    }

    public NameError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
