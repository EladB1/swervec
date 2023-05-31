package com.piedpiper.bolt.error;

public class DeclarationError extends SourceCodeError {
    public DeclarationError(String message) {
        super(message);
    }

    public DeclarationError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
