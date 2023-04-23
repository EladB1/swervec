package com.piedpiper.bolt.lexer;

import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@With
public class Token {
    @NonNull
    TokenType name;
    @NonNull
    String value;
    Integer lineNumber = 0;
}
