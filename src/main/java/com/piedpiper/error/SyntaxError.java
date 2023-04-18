package com.piedpiper.error;

import lombok.Getter;

@Getter
public class SyntaxError extends Exception {
    String message;
    
    public SyntaxError(String message, int lineNumber, int charNumber) {
        this.message = message + " | line: " + lineNumber + ", index: " + charNumber;
    }
}
