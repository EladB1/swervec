package com.piedpiper.bolt.error;

import lombok.Getter;
import lombok.With;

@Getter
@With
public class SyntaxError extends RuntimeException {
    String message;

    public SyntaxError(String message) {
        this.message = message;
    }

    public SyntaxError(String message, int lineNumber) {
        this.message = lineNumber != 0 ? "Line " + lineNumber + "\n\t" + message : message;
    }
}
