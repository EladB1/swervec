package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.lexer.TokenType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Data
public class Symbol {
    private String name;
    private TokenType type;
    // functions only
    private TokenType returnType = null;
    // functions returning arrays only
    private List<TokenType> subReturnTypes = new ArrayList<>();
    // arrays only
    private List<TokenType> subTypes = new ArrayList<>();
    private Integer scope;
    // TODO: store variable value (or function body?)

    public Symbol(Integer scope, String name, TokenType type) {
        this.scope = scope;
        this.name = name;
        this.type = type;
    }

    public Symbol(Integer scope, String name, TokenType type, TokenType... subTypes) {
        this.scope = scope;
        this.name = name;
        this.type = type;
        Collections.addAll(this.subTypes, subTypes);
    }

    public Symbol(Integer scope, String name, TokenType type, TokenType returnType) {
        this.scope = scope;
        this.name = name;
        this.type = type;
        this.returnType = returnType;
    }

    public Symbol(Integer scope, String name, TokenType type, TokenType returnType, TokenType... subReturnTypes) {
        this.scope = scope;
        this.name = name;
        this.type = type;
        this.returnType = returnType;
        Collections.addAll(this.subReturnTypes, subReturnTypes);
    }
}
