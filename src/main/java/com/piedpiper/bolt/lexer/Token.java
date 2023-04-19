package com.piedpiper.bolt.lexer;

import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class Token {
    @NonNull
    String name;
    @NonNull
    String value;
    Integer lineNumber = 0;
}
