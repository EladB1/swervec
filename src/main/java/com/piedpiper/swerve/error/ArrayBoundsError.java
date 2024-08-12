package com.piedpiper.swerve.error;

public class ArrayBoundsError extends SourceCodeError {
    public ArrayBoundsError(String message) {
        super(message);
    }

    public ArrayBoundsError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
