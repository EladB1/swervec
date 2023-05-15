package com.piedpiper.bolt.error;

import lombok.With;

@With
public class SyntaxError extends SourceCodeError {
    String message;

    public SyntaxError(String message) {
        super(message);
    }

    public SyntaxError(String message, int lineNumber) {
        super(message, lineNumber);
    }
}
