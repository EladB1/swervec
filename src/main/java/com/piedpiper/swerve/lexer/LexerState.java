package com.piedpiper.swerve.lexer;

public enum LexerState {
    DEFAULT,
    IN_NUMBER,
    IN_OPERATOR,
    IN_IDENTIFIER,
    IN_MULTILINE_COMMENT,
    IN_MULTILINE_STRING
}
