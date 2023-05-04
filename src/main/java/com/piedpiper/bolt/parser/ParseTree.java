package com.piedpiper.bolt.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.piedpiper.bolt.lexer.VariableToken;
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
        this.children.addAll(children); // setting this.children = children causes the list to become immutable
    }

    public ParseTree(Token token) {
        this.type = "terminal";
        this.token = token;
    }

    public ParseTree(String type, Token token) {
        this.type = type;
        this.token = null;
        this.children.add(new ParseTree(token));
    }

    public static List<ParseTree> tokensToNodes(List<Token> tokens) {
        return tokens.stream().map(ParseTree::new).collect(Collectors.toList());
    }

    // go in sequential order of parents and created nested tree nodes; the last node contains tree
    public static ParseTree createNestedTree(ParseTree tree, String... parents) {
        if (parents.length == 1)
            return new ParseTree(parents[0], List.of(tree));

        ParseTree root = new ParseTree(parents[0]);
        int end = parents.length - 1;
        ParseTree node = root;
        ParseTree subTree;
        for (int i = 1; i < end; i++) {
            subTree = new ParseTree(parents[i]);
            node.appendChildren(subTree);
            node = subTree;
        }
        node.appendChildren(
            new ParseTree(parents[end], List.of(tree))
        );
        return root;
    }

    // go in sequential order of parents and created nested tree nodes; the last node contains token
    public static ParseTree createNestedTree(Token token, String...parents) {
        return createNestedTree(new ParseTree(token), parents);
    }

    // go in sequential order of parents and created nested tree nodes; the last node contains tokens
    public static ParseTree createNestedTree(List<Token> tokens, String... parents) {
        if (parents.length == 1)
            return new ParseTree(parents[0], tokensToNodes(tokens));
        
        ParseTree root = new ParseTree(parents[0]);
        int end = parents.length - 1;
        ParseTree node = root;
        ParseTree subTree;
        for (int i = 1; i < end; i++) {
            subTree = new ParseTree(parents[i]);
            node.appendChildren(subTree);
            node = subTree;
        }
        node.appendChildren(
            new ParseTree(parents[end], tokensToNodes(tokens))
        );
        return root;
    }

    public void appendChildren(ParseTree... children) {
        Collections.addAll(this.children, children);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indentLevel) {
        String indentation = getNestedIndentation(indentLevel);
        StringBuilder output = new StringBuilder(indentation + "ParseTree => ");
        if (type.equals("terminal")) {
            output.append("Token: ").append(token.getName());
            if (token instanceof VariableToken)
                output.append(" ('").append(token.getValue()).append("')");
            if (token.getLineNumber() != 0)
                output.append(", line: ").append(token.getLineNumber());
        }
        else {
            output.append(type);
        }
        if (!children.isEmpty()) {
            output.append(", children: [");
            for (ParseTree child : children) {
                output.append("\n").append(child.toString(indentLevel + 1));
            }
            output.append("\n").append(indentation).append("]");
        }
        return output.toString();
    }

    private String getNestedIndentation(int indentLevel) {
        return "\t".repeat(Math.max(0, indentLevel));
    }
}
