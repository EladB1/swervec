package com.piedpiper.swerve.error;

public class UnreachableCodeError extends SourceCodeError {
    public UnreachableCodeError(String message) {
        super(message);
    }

    public UnreachableCodeError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
