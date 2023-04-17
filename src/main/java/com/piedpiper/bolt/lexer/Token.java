package com.piedpiper.bolt.lexer;

import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@RequiredArgsConstructor
@Data
public class Token {
    @NonNull
    String name;
    @NonNull
    String value;
    Integer lineNumber;
}
