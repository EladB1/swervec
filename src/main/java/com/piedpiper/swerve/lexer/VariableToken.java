package com.piedpiper.swerve.lexer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@With
public class VariableToken implements Token {
    @NonNull
    private TokenType name;
    @NonNull
    private String value;
    private Integer lineNumber = 0;
}
