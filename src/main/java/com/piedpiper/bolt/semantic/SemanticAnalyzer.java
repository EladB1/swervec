package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.CompilerError;
import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.symboltable.FunctionSymbol;
import com.piedpiper.bolt.symboltable.Symbol;
import com.piedpiper.bolt.symboltable.SymbolTable;

import java.util.List;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();

    /**
     * Try to do as much as possible at compile time but defer some checks to runtime
     * Need to check each expression for type
     * Need to check different parts of expressions
     *  i.e. check operand types of binary expressions
     * Certain parts of the code must resolve to a certain type (array indices must be ints, conditionals must be booleans)
     * function calls must resolve to a signature with matching types
     * array elements must contain n elements of a type matching its declaration
     * Must resolve types of all literals, variables, array indices, and function calls
     * Must check the array bounds
     * Need to check variable assignments/declarations match the type on both sides of the equals operator
    */
    private void emitWarning(String message) {
        System.out.println("Warning: " + message);
    }

    private boolean matchesLabelNode(AbstractSyntaxTree node, String value) {
        return node.getLabel().equals(value);
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
        return node.getName() == type && node.getValue().equals(value);
    }

    private boolean isFloatLiteral(AbstractSyntaxTree node) {
        return node.getName() == TokenType.NUMBER && node.getValue().contains(".");
    }

    private boolean isIntegerLiteral(AbstractSyntaxTree node) {
        return node.getName() == TokenType.NUMBER && !node.getValue().contains(".");
    }

    private boolean isStringLiteral(AbstractSyntaxTree node) {
        return node.getName() == TokenType.STRING;
    }

    private boolean isBooleanLiteral(AbstractSyntaxTree node) {
        return matchesStaticToken(node, TokenType.KW_TRUE) || matchesStaticToken(node, TokenType.KW_FALSE);
    }

    private boolean isArrayLiteral(AbstractSyntaxTree node) {
        return matchesLabelNode(node, "ARRAY-LIT");
    }

    public void analyze(AbstractSyntaxTree AST) { // AST should be the top-level ("PROGRAM") node of abstract syntax tree
        analyze(AST, false);
    }

    public void analyze(AbstractSyntaxTree AST, Boolean isFunctionBody) {
        for (AbstractSyntaxTree subTree : AST.getChildren()) {
            if (matchesLabelNode(subTree, "VAR-DECL") || matchesLabelNode(subTree, "ARRAY-DECL")) {
                handleVariableDeclaration(subTree);
            }
            if (matchesVariableToken(subTree, TokenType.OP))
                handleBinaryExpression(subTree);
            if (matchesStaticToken(subTree, TokenType.KW_FN)) {
                if (isFunctionBody)
                    throw new CompilerError("Cannot define nested functions");
                handleFunctionDefinitionExpression(subTree.getChildren());
            }
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
        if (
            (isIntegerLiteral(operands.get(0)) && isFloatLiteral(operands.get(1)))
            || (isFloatLiteral(operands.get(0)) && isIntegerLiteral(operands.get(1)))
        )
            return TokenType.KW_FLOAT;
        if (isStringLiteral(operands.get(0)) && isStringLiteral(operands.get(1)))
            return TokenType.STRING;
        else
            throw new NameError("Some error");
    }

    private void handleFunctionDefinitionExpression(List<AbstractSyntaxTree> fnDetails) {
        symbolTable.enterScope(); // enter the function parameter scope
        symbolTable.insert(new FunctionSymbol(fnDetails, symbolTable));
        int index = -1;
        for (int i = 0; i < fnDetails.size(); i++) {
            if (fnDetails.get(i).getLabel().equals("BLOCK-BODY")) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            symbolTable.enterScope();
            analyze(fnDetails.get(index), true);
            symbolTable.leaveScope();
            symbolTable.leaveScope();
        }
    }

    private TokenType evaluateType(AbstractSyntaxTree node) {
        if (node.getName() == TokenType.ID) {
            // TODO: handle array indexing
            Symbol symbol = symbolTable.lookup(node.getValue());
        }
        if (isIntegerLiteral(node))
            return TokenType.KW_INT;
        if (isFloatLiteral(node))
            return TokenType.KW_FLOAT;
        if (isStringLiteral(node))
            return TokenType.KW_STR; // STRING represents string literals but KW_STR is the string keyword (for variables, funcs, etc.)
        if (isBooleanLiteral(node))
            return TokenType.KW_BOOL;
        // TODO: handle array literals
        return null;
    }
    private TokenType handleArrayIndexExpression(AbstractSyntaxTree rootNode) {
        return null;
    }
}
