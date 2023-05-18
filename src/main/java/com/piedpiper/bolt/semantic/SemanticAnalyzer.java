package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.symboltable.Symbol;
import com.piedpiper.bolt.symboltable.SymbolTable;

import java.util.List;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();

    /*
        * Principles:
        * Every expression needs a type
        * Every reference must be in scope
        * comparison, logical-and, logical-or -> boolean
        * int ( addition |  multiplication ) float -> float
        * string + string -> string (concat)
        * array + array -> array (concat)
        * string * int -> string (repeat)
        * boolean (^ / &) boolean -> boolean
        * (- | ++ | --)(int/float)(++ | --) -> int/float
        * !boolean -> boolean
        * while (boolean)
        * for (declaration; boolean; expression)
        * for (type id : id)
        * if (boolean) {} else if (boolean) {} else {}
        * null valid for any variable types but cannot do any addition/multiplication with it
        * can check for equality with null but not any other comparisons
        * array[int] -> element value
        * all arrays elements must be of the same type (and can be null)
        * variable/function call/array index by itself in conditional/logical-and/or statements will be checked for null equality
    */
    private void emitWarning(String message) {
        System.out.println("Warning: " + message);
    }

    private boolean matchesStringNode(AbstractSyntaxTree node, String value) {
        return node.getType().equals(value);
    }

    private boolean matchesStaticToken(AbstractSyntaxTree node, TokenType value) {
        return node.getName() == value;
    }

    private boolean matchesVariableToken(AbstractSyntaxTree node, String value) {
        return node.getValue().equals(value);
    }

    private boolean matchesVariableToken(AbstractSyntaxTree node, TokenType type) {
        return node.getName() == type;
    }

    private boolean matchesVariableToken(AbstractSyntaxTree node, TokenType type, String value) {
        Token token = node.getToken();
        return token.getName() == type && token.getValue().equals(value);
    }

    private boolean isFloatLiteral(AbstractSyntaxTree node) {
        Token token = node.getToken();
        return token.getName() == TokenType.NUMBER && token.getValue().contains(".");
    }

    private boolean isIntegerLiteral(AbstractSyntaxTree node) {
        Token token = node.getToken();
        return token.getName() == TokenType.NUMBER && !token.getValue().contains(".");
    }

    private boolean isStringLteral(AbstractSyntaxTree node) {
        return node.getToken().getName() == TokenType.STRING;
    }

    private boolean isBooleanLiteral(AbstractSyntaxTree node) {
        return matchesStaticToken(node, TokenType.KW_TRUE) || matchesStaticToken(node, TokenType.KW_FALSE);
    }

    public void analyze(AbstractSyntaxTree AST) { // AST should be the top-level ("PROGRAM") node of abstract syntax tree
        for (AbstractSyntaxTree subTree : AST.getChildren()) {
            if (matchesStringNode(subTree, "VAR-DECL") || matchesStringNode(subTree, "ARRAY-DECL")) {
                handleVariableDeclaration(subTree);
            }
            if (matchesVariableToken(subTree, TokenType.OP))
                handleBinaryExpression(subTree);
        }
    }

    private void handleVariableDeclaration(AbstractSyntaxTree node) {
        int scope = symbolTable.getScopeLevel();
        symbolTable.insert(new Symbol(node, scope));
    }

    private TokenType handleBinaryExpression(AbstractSyntaxTree rootNode) {
        // return the type of the result of the expression
        if (matchesVariableToken(rootNode, "+")) {
            return handlePlusExpression(rootNode.getChildren());
        }
        return null;
    }

    private TokenType handlePlusExpression(List<AbstractSyntaxTree> operands) {
        if (isIntegerLiteral(operands.get(0)) && isIntegerLiteral(operands.get(1)))
            return TokenType.KW_INT;
        else
            throw new NameError("Some error");
    }

    private TokenType handleArrayIndexExpression(AbstractSyntaxTree rootNode) {
        return null;
    }
}
