package com.piedpiper.error;

import lombok.Getter;
import lombok.With;

@Getter
@With
public class SyntaxError extends Exception {
    String message;

    public SyntaxError(String message) {
        this.message = message;
    }
    
    public SyntaxError(String message, int lineNumber, int charNumber) {
        this.message = message + " | line: " + lineNumber + ", index: " + charNumber;
    }
}
