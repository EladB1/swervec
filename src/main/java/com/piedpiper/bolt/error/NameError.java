package com.piedpiper.bolt.error;

import lombok.Getter;
import lombok.With;

@Getter
@With
public class NameError extends RuntimeException {
    String message;

    public NameError(String message) {
        this.message = message;
    }

    public NameError(String message, int lineNumber) {
        this.message = lineNumber != 0 ? "Line " + lineNumber + "\n\t" + message : message;
    }
}
