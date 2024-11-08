package com.piedpiper.swerve.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.piedpiper.swerve.lexer.Token;
import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.lexer.VariableToken;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class AbstractSyntaxTree {
    private String label;
    @Getter
    private TokenType name = null;
    @Getter
    private String value = "";
    private Integer lineNumber = 0;
    private List<AbstractSyntaxTree> children = new ArrayList<>();

    public AbstractSyntaxTree(String label) {
        this.label = label;
    }

    public AbstractSyntaxTree(String label, int lineNumber) {
        this.label = label;
        this.lineNumber = lineNumber;
    }

    public AbstractSyntaxTree(String label, List<AbstractSyntaxTree> children) {
        this.label = label;
        this.children.addAll(children); // setting this.children = children causes the list to become immutable
    }

    public AbstractSyntaxTree(String label, int lineNumber, List<AbstractSyntaxTree> children) {
        this.label = label;
        this.lineNumber = lineNumber;
        this.children.addAll(children); // setting this.children = children causes the list to become immutable
    }

    public AbstractSyntaxTree(String label, Token... children) {
        this.label = label;
        this.children.addAll(tokensToNodes(children));
    }

    public AbstractSyntaxTree(String label, int lineNumber, Token... children) {
        this.label = label;
        this.lineNumber = lineNumber;
        this.children.addAll(tokensToNodes(children));
    }

    public AbstractSyntaxTree(Token token) {
        this.label = "terminal";
        this.name = token.getName();
        if (token instanceof VariableToken)
            this.value = token.getValue();
        this.lineNumber = token.getLineNumber();
    }

    public AbstractSyntaxTree(String label, Token token) {
        this.label = label;
        this.children.add(new AbstractSyntaxTree(token));
    }

    public AbstractSyntaxTree(Token token, List<AbstractSyntaxTree> children) {
        this.label = "terminal";
        this.name = token.getName();
        if (token instanceof VariableToken)
            this.value = token.getValue();
        this.children.addAll(children);
    }

    public AbstractSyntaxTree(Token... tokens) { // the first element becomes the parent of the rest of the tokens
        this(tokens[0], tokensToNodes(Arrays.copyOfRange(tokens, 1, tokens.length)));
    }

    public static List<AbstractSyntaxTree> tokensToNodes(Token[] tokens) {
        return Arrays.stream(tokens).map(AbstractSyntaxTree::new).collect(Collectors.toList());
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void appendChildren(AbstractSyntaxTree... children) {
        Collections.addAll(this.children, children);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public int countChildren() {
        if (!hasChildren())
            return 0;
        return children.size();
    }

    public boolean matchesLabel(String value) {
        return label.equals(value);
    }

    public boolean matchesStaticToken(TokenType value) {
        return name == value;
    }

    public boolean matchesValue(String value) {
        return this.value.equals(value);
    }

    public boolean isDoubleLiteral() {
        return name == TokenType.NUMBER;
    }

    public boolean isIntegerLiteral() {
        return name == TokenType.NUMBER && !value.contains(".");
    }

    public boolean isStringLiteral() {
        return name == TokenType.STRING;
    }

    public boolean isBooleanLiteral() {
        return matchesStaticToken(TokenType.KW_TRUE) || matchesStaticToken(TokenType.KW_FALSE);
    }

    public boolean isArrayLiteral() {
        return matchesLabel("ARRAY-LIT");
    }

    public boolean isTypeLabel() {
        final List<TokenType> typeTokens = List.of(
            TokenType.KW_BOOL,
            TokenType.KW_INT,
            TokenType.KW_DOUBLE,
            TokenType.KW_STR,
            TokenType.KW_ARR,
            TokenType.KW_GEN
        );
        if (name == null)
            return false;
        return typeTokens.contains(name);
    }

    /**
     * Calculate the height of each child node of the current tree and return the max value.
     * Leaf nodes have a height of 1.
     * A subtree has a height of 1 + child.getHeight().
     */
    public int getHeight() {
        int max = 0;
        if (!this.hasChildren())
            return 1;
        int depth;
        for (AbstractSyntaxTree child : this.getChildren()) {
            depth = child.getHeight() + 1;
            max = Math.max(max, depth);
        }
        return max;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indentLevel) {
        String indentation = getNestedIndentation(indentLevel);
        StringBuilder output = new StringBuilder(indentation + "AST => ");
        if (label.equals("terminal")) {
            output.append("Token: ").append(name);
            if (!value.isEmpty())
                output.append(" ('").append(value).append("')");
        }
        else {
            output.append(label);
        }
        if (lineNumber != 0)
                output.append(", line: ").append(lineNumber);
        if (!children.isEmpty()) {
            output.append(", children: [");
            for (AbstractSyntaxTree child : children) {
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
