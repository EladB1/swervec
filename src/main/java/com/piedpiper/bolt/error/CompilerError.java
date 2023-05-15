package com.piedpiper.bolt.error;

import lombok.Getter;
import lombok.With;

/**
 * Handle errors that aren't directly issues with source code (like no file being defined) 
*/
@Getter
@With
public class CompilerError extends RuntimeException {
    String message;

    public CompilerError(String message) {
        this.message = message;
    }
}
