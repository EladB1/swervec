package com.piedpiper.bolt.parser;

import java.util.List;

import com.piedpiper.bolt.lexer.Token;

public class ParseTree {
    private Token token;
    private List<ParseTree> children;
}
