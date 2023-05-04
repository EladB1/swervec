package com.piedpiper.bolt.lexer;

public interface Token {
    // for lombok with methods
    Token withName(TokenType name);
    Token withValue(String value);
    Token withLineNumber(Integer lineNumber);

    // getters
    TokenType getName();
    String getValue();
    Integer getLineNumber();
}
