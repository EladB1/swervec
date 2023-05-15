package com.piedpiper.bolt.error;


/*
  Base class for any source code related errors (syntax errors, name errors, etc.)
*/

import lombok.Getter;

@Getter
public abstract class SourceCodeError extends RuntimeException {
    String message;
    Integer lineNumber = 0;

    public SourceCodeError(String message) {
        this.message = message;
    }

    public SourceCodeError(String message, Integer lineNumber) {
        this.lineNumber = lineNumber;
        this.message = lineNumber != 0 ? "Line " + lineNumber + "\n\t" + message : message;
    }
}
