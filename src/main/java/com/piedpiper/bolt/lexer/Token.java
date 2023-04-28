package com.piedpiper.bolt.lexer;

public interface Token {
    // for lombok with methods
    public Token withName(TokenType name);
    public Token withValue(String value);
    public Token withLineNumber(Integer lineNumber);

    // getters
    public TokenType getName();
    public String getValue();
    public Integer getLineNumber();
}
