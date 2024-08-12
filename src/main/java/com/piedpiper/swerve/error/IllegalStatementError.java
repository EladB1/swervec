package com.piedpiper.swerve.error;

public class IllegalStatementError extends SourceCodeError {
    public IllegalStatementError(String message) {
        super(message);
    }

    public IllegalStatementError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
