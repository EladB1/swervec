package com.piedpiper.bolt.error;

import lombok.Getter;
import lombok.With;

@Getter
@With
public class CompilerError extends RuntimeException {
    String message;

    public CompilerError(String message) {
        this.message = message;
    }
}
