package com.piedpiper.bolt.parser;

import java.util.ArrayList;
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
        return tokens.stream().map((Token token) -> new ParseTree(token)).collect(Collectors.toList());
    }

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
        for (ParseTree child : children) {
            this.children.add(child);
        }
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indentLevel) {
        String indentation = getNestedIndentation(indentLevel);
        StringBuilder output = new StringBuilder(indentation + "ParseTree => ");
        if (type.equals("terminal")) {
            output.append("Token: " + token.getName());
            if (token instanceof VariableToken)
                output.append(" ('" + token.getValue() + "')");
                if (token.getLineNumber() != 0)
                    output.append(", line: " + token.getLineNumber());
        }
        else {
            output.append(type);
        }
        if (!children.isEmpty()) {
            output.append(", children: [");
            for (int i = 0; i < children.size(); i++) {
                output.append("\n" + children.get(i).toString(indentLevel+1));
            }
            output.append("\n" + indentation + "]");
        }
        return output.toString();
    }

    private String getNestedIndentation(int indentLevel) {
        StringBuilder indentation = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            indentation.append('\t');
        }
        return indentation.toString();
    }
}
