package com.piedpiper.bolt.parser;

import java.util.ArrayList;
import java.util.List;

import com.piedpiper.bolt.lexer.Token;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class ParseTree {
    private String type;
    private Token token;
    private List<ParseTree> children = new ArrayList<>();

    public ParseTree(String type) {
        this.type = type;
        this.token = null;
    }

    public ParseTree(String type, List<ParseTree> children) {
        this.type = type;
        this.token = null;
        this.children = children;
    }

    public ParseTree(Token token) {
        this.type = "non-terminal";
        this.token = token;
    }

    public void appendChildren(ParseTree... children) {
        for (ParseTree child : children) {
            this.children.add(child);
        }
    }
}
