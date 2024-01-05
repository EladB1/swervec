package com.piedpiper.bolt.lexer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@With
public class StaticToken implements Token {
    @NonNull
    private TokenType name;
    private String value = "";
    private Integer lineNumber = 0;

    public StaticToken(@NonNull TokenType name, Integer lineNumber) {
        this.name = name;
        this.lineNumber = lineNumber;
    }
    
}
