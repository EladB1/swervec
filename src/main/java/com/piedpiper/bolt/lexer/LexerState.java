package com.piedpiper.bolt.lexer;

public enum LexerState {
    DEFAULT,
    IN_NUMBER,
    IN_OPERATOR,
    IN_IDENTIFIER,
    IN_MULTILINE_COMMENT
}
