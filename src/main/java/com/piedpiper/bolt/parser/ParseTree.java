package com.piedpiper.bolt.parser;

import java.util.ArrayList;
import java.util.List;

import com.piedpiper.bolt.lexer.Token;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class ParseTree {
    @NonNull
    private Token token;
    private List<ParseTree> children = new ArrayList<>();
}
