package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.CompilerError;
import com.piedpiper.bolt.error.ReferenceError;
import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.symboltable.FunctionSymbol;
import com.piedpiper.bolt.symboltable.Symbol;
import com.piedpiper.bolt.symboltable.SymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> nonEqualityComparisons = List.of("<", "<=", ">", ">=");
    private final List<String> arithmeticOperators = List.of("-", "-=", "/", "/=", "%", "**");

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

    private void handleFunctionCall() {

    }

    private void handleForLoop() {

    }

    private void handleWhileLoop() {

    }

    private void handleConditionalBlock() {

    }

    private boolean isSubList(List<?> list, List<?> subList) {
        if (list.size() < subList.size())
            return false;
        for (int i = 0; i < subList.size(); i++) {
            if (!subList.get(i).equals(list.get(i)))
                return false;
        }
        return true;
    }

    public List<NodeType> estimateArrayTypes(AbstractSyntaxTree node) {
        // empty array literal
        if (!node.hasChildren())
            return List.of(NodeType.ARRAY);

        List<AbstractSyntaxTree> children = node.getChildren();
        // non-nested array literal
        if (!children.get(0).getLabel().equals("ARRAY-LIT")) {
            NodeType type = evaluateType(children.get(0));
            NodeType currentType;
            for (int i = 1; i < children.size(); i++) {
                if (children.get(i).getLabel().equals("ARRAY-LIT"))
                    throw new TypeError("Cannot mix non-array elements with nested array elements in array literal");
                currentType = evaluateType(children.get(i));
                if (type == NodeType.NULL && currentType != NodeType.NULL)
                    type = currentType;
                else if (currentType != NodeType.NULL && type != currentType)
                    throw new TypeError("Cannot mix " + type + " elements with " + currentType + " elements in array literal");
            }
            return type == NodeType.NULL ? List.of(NodeType.ARRAY) : List.of(NodeType.ARRAY, type);
        }
        // nested array literal
        List<NodeType> types;
        if (children.get(0).hasChildren())
            types = estimateArrayTypes(children.get(0));
        else {
            int i = 1;
            while (i < children.size() && !children.get(i).hasChildren()) {
                i++;
            }
            types = i < children.size() ? estimateArrayTypes(children.get(i)) : List.of(NodeType.ARRAY);
        }
        List<NodeType> currentTypes;
        for (AbstractSyntaxTree child : children) {
            currentTypes = estimateArrayTypes(child);
            if (!isSubList(types, currentTypes))
                throw new TypeError("Cannot mix types of arrays");
        }
        List<NodeType> arrayTypes = new ArrayList<>();
        arrayTypes.add(NodeType.ARRAY);
        arrayTypes.addAll(types);
        return arrayTypes;
    }

    public NodeType evaluateType(AbstractSyntaxTree node) {
        if (node.getName() == TokenType.ID) {
            // TODO: handle array indexing
            Symbol symbol = symbolTable.lookup(node.getValue());
        }
        if (isIntegerLiteral(node))
            return NodeType.INT;
        if (isFloatLiteral(node))
            return NodeType.FLOAT;
        if (isStringLiteral(node))
            return NodeType.STRING;
        if (isBooleanLiteral(node))
            return NodeType.BOOLEAN;
        if (node.getName() == TokenType.KW_NULL)
            return NodeType.NULL;
        if (nonEqualityComparisons.contains(node.getValue()))
            return handleComparison(node);
        if (node.getValue().equals("==") || node.getValue().equals("!="))
            return handleEqualityComparison(node);
        if (arithmeticOperators.contains(node.getValue()))
            return handleArithmetic(node);
        if (node.getValue().equals("&") || node.getValue().equals("^"))
            return handleBitwise(node);
        if (node.getValue().equals("*") || node.getValue().equals("*="))
            return handleMultiplication(node);
        if (node.getValue().equals("+") || node.getValue().equals("+="))
            return handleAddition(node);
        if (node.getLabel().equals("UNARY-OP"))
            return handleUnaryOp(node);
        //if (node.getLabel().equals("FUNC-CALL"))
        // TODO: handle array literals
        return NodeType.NONE;
    }

    private NodeType handleComparison(AbstractSyntaxTree rootNode) {
        String comparisonOperator = rootNode.getValue();
        NodeType leftType = evaluateType(rootNode.getChildren().get(0));
        NodeType rightType = evaluateType(rootNode.getChildren().get(1));
        Set<NodeType> acceptedTypes = Set.of(NodeType.INT, NodeType.FLOAT);
        if (!(acceptedTypes.contains(leftType) && acceptedTypes.contains(rightType)))
            throw new TypeError("Cannot compare " + leftType + " with " + rightType + " using " + comparisonOperator);
        return NodeType.BOOLEAN;
    }

    private NodeType handleBitwise(AbstractSyntaxTree rootNode) {
        String comparisonOperator = rootNode.getValue();
        NodeType leftType = evaluateType(rootNode.getChildren().get(0));
        NodeType rightType = evaluateType(rootNode.getChildren().get(1));
        Set<NodeType> acceptedTypes = Set.of(NodeType.INT, NodeType.BOOLEAN);
        if (!(acceptedTypes.contains(leftType) && acceptedTypes.contains(rightType)))
            throw new TypeError("Binary expression (" + comparisonOperator + ") with " + leftType + " and " + rightType + " is not valid");
        return NodeType.INT;
    }

    private NodeType handleMultiplication(AbstractSyntaxTree rootNode) {
        NodeType leftType = evaluateType(rootNode.getChildren().get(0));
        NodeType rightType = evaluateType(rootNode.getChildren().get(1));
        if (leftType == NodeType.INT) {
            if (rightType == NodeType.INT)
                return NodeType.INT;
            if (rightType == NodeType.FLOAT)
                return NodeType.FLOAT;
            if (rightType == NodeType.STRING)
                return NodeType.STRING;
        }
        else if (leftType == NodeType.FLOAT) {
            if (rightType == NodeType.FLOAT || rightType == NodeType.INT)
                return NodeType.FLOAT;
        }
        else if (leftType == NodeType.STRING) {
            if (rightType == NodeType.INT)
                return NodeType.STRING;
        }
        throw new TypeError("Cannot multiply " + leftType + " with " + rightType);
    }

    private NodeType handleArithmetic(AbstractSyntaxTree rootNode) {
        String operator = rootNode.getValue();
        NodeType leftType = evaluateType(rootNode.getChildren().get(0));
        NodeType rightType = evaluateType(rootNode.getChildren().get(1));
        if (leftType == NodeType.INT) {
            if (rightType == NodeType.INT)
                return NodeType.INT;
            if (rightType == NodeType.FLOAT)
                return NodeType.FLOAT;
        }
        else if (leftType == NodeType.FLOAT) {
            if (rightType == NodeType.FLOAT || rightType == NodeType.INT)
                return NodeType.FLOAT;
        }
        throw new TypeError("Arithmetic expression (" + operator + ") with " + leftType + " and " + rightType + " is not valid");
    }

    private NodeType handleAddition(AbstractSyntaxTree rootNode) {
        NodeType leftType = evaluateType(rootNode.getChildren().get(0));
        NodeType rightType = evaluateType(rootNode.getChildren().get(1));
        if (leftType == NodeType.INT) {
            if (rightType == NodeType.INT)
                return NodeType.INT;
            if (rightType == NodeType.FLOAT)
                return NodeType.FLOAT;
        }
        else if (leftType == NodeType.FLOAT) {
            if (rightType == NodeType.FLOAT || rightType == NodeType.INT)
                return NodeType.FLOAT;
        }
        else if (leftType == NodeType.STRING && rightType == NodeType.STRING)
            return NodeType.STRING;
        // TODO: handle arrays
        throw new TypeError("Cannot add " + leftType + " with " + rightType);
    }

    private NodeType handleEqualityComparison(AbstractSyntaxTree rootNode) {
        // TODO: handle arrays
        NodeType leftType = evaluateType(rootNode.getChildren().get(0));
        NodeType rightType = evaluateType(rootNode.getChildren().get(1));
        if (
            leftType == rightType
            || (leftType == NodeType.NULL || rightType == NodeType.NULL)
            || (
                (leftType == NodeType.INT && rightType == NodeType.FLOAT)
                || (leftType == NodeType.FLOAT && rightType == NodeType.INT)
            )
        )
            return NodeType.BOOLEAN;
        throw new TypeError("Cannot check for equality between " + leftType + " and " + rightType);
    }

    private NodeType handleUnaryOp(AbstractSyntaxTree rootNode) {
        // TODO: handle variable or not for ++ and --
        AbstractSyntaxTree left = rootNode.getChildren().get(0);
        AbstractSyntaxTree right = rootNode.getChildren().get(1);
        NodeType leftType = evaluateType(left);
        NodeType rightType = evaluateType(right);
        if (left.getValue().equals("!") && rightType == NodeType.BOOLEAN)
            return NodeType.BOOLEAN;
        if (left.getValue().equals("-") && (rightType == NodeType.INT || rightType == NodeType.FLOAT))
            return rightType;
        if (left.getValue().equals("++") || left.getValue().equals("--")) {
            if (rightType == NodeType.INT || rightType == NodeType.FLOAT) {
                // TODO: handle array indexes
                if (right.getName() != TokenType.ID)
                    throw new ReferenceError("Cannot perform prefix expression(" + left.getValue() + ") on " + right.getName());
                return rightType;
            }
        }
        if (right.getValue().equals("++") || right.getValue().equals("--")) {
            if (leftType == NodeType.INT || leftType == NodeType.FLOAT) {
                if (left.getName() != TokenType.ID)
                    throw new ReferenceError("Cannot perform postfix expression(" + right.getValue() + ") on " + left.getName());
                return leftType;
            }
        }
        String unaryOp = leftType == NodeType.NONE ? left.getValue() : right.getValue();
        NodeType type = leftType == NodeType.NONE ? rightType: leftType;
        throw new TypeError("Cannot perform unary operator (" + unaryOp + ") on " + type);
    }
}
