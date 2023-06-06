package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.IllegalStatementError;
import com.piedpiper.bolt.error.ReferenceError;
import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.error.UnreachableCodeError;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.symboltable.FunctionSymbol;
import com.piedpiper.bolt.symboltable.Symbol;
import com.piedpiper.bolt.symboltable.SymbolTable;

import java.util.ArrayList;
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

    // use analyze() to handle bodies of different structures (program, function, loop, conditional)
    // this should handle nesting and embedded elements
    // will be recursive
    // should return true if a return is found in the specific block (with right context and positioning)

    public void analyze(AbstractSyntaxTree AST) {
        for (AbstractSyntaxTree subTree : AST.getChildren()) {
            if (matchesLabelNode(subTree, "VAR-DECL") || matchesLabelNode(subTree, "ARRAY-DECL")) {
                handleVariableDeclaration(subTree);
            }
            if (isLoopControl(subTree)) { // TODO: handle correct context
                TokenType loopControl = getControlFlowType(subTree.getChildren().get(0));
                throw new IllegalStatementError("Cannot use " + loopControl  + " outside of a loop");
            }

            if (isReturn(subTree)) // TODO: handle correct context
                throw new IllegalStatementError("Cannot return outside of a function");

            // TODO: handle conditionals, loops, and functions then recurse
            // conditional
            if (AST.getLabel().equals("COND")) {
                // check condition is boolean
                // analyze body
            }

            // loop
            if (AST.getName() == TokenType.KW_FOR || AST.getName() == TokenType.KW_WHILE) {
                // check loop signature
                // analyze body
            }

            // function
            if (AST.getName() == TokenType.KW_FN) {
                // check function declaration signature
                // analyze body
            }
        }
    }

    private void validateReturn(AbstractSyntaxTree controlFlow, NodeType returnType) {
        // make sure return is done properly
        if (returnType == NodeType.NONE) {
            if (controlFlow.getChildren().size() == 1)
                return;
            else {
                AbstractSyntaxTree returnValue = controlFlow.getChildren().get(1);
                if (returnValue.getName() == TokenType.KW_NULL)
                    return;
                else {
                    NodeType type = evaluateType(returnValue);
                    throw new TypeError("Cannot return " + type + " from void function", returnValue.getLineNumber());
                }
            }

        }
        else {
            if (controlFlow.getChildren().size() == 1)
                throw new TypeError("Expected return type " + returnType + " but didn't return a value", controlFlow.getChildren().get(0).getLineNumber());
            AbstractSyntaxTree returnValue = controlFlow.getChildren().get(1);
            NodeType actualReturnType = evaluateType(returnValue);
            if (actualReturnType != returnType && returnValue.getName() != TokenType.KW_NULL)
                throw new TypeError("Expected " + returnType + " to be returned but got " + actualReturnType, returnValue.getLineNumber());
        }
    }

    public boolean functionReturns(AbstractSyntaxTree functionBody, NodeType returnType) {
        List<AbstractSyntaxTree> contents = functionBody.getChildren();
        AbstractSyntaxTree node;
        int length = contents.size();
        for (int i = 0; i < length; i++) {
            node = contents.get(i);
            if (node.getLabel().equals("COND")) {
                if (conditionalBlockReturns(node, returnType)) {
                    if (i < length - 1)
                        throw new UnreachableCodeError("Unreachable statement following returning conditional block", contents.get(i+1).getLineNumber());
                    return true;
                }
            }
            if (node.getName() == TokenType.KW_FOR || node.getName() == TokenType.KW_WHILE) {
                AbstractSyntaxTree lastChild = node.getChildren().get(node.getChildren().size() - 1);
                if (lastChild.getLabel().equals("BLOCK-BODY")) {
                    if (functionReturns(lastChild, returnType)) {
                        if (i < length - 1)
                            throw new UnreachableCodeError("Unreachable statement following returning " +  node.getName() + " loop", contents.get(i+1).getLineNumber());
                        return true;
                    }
                }
            }
            if (isReturn(node) && i < length - 1)
                throw new UnreachableCodeError("Unreachable statement following return", contents.get(i+1).getLineNumber());
            else if (isReturn(node)) {
                validateReturn(node, returnType);
                return true;
            }
        }
        return returnType == NodeType.NONE;
    }

    public boolean conditionalBlockReturns(AbstractSyntaxTree conditionalBlock, NodeType returnType) {
        List<AbstractSyntaxTree> conditionals = conditionalBlock.getChildren();
        if (conditionals.size() == 0)
            return false;
        if (conditionals.get(conditionals.size()-1).getName() != TokenType.KW_ELSE)
            return false;
        boolean returns = true;
        AbstractSyntaxTree bodyNode;
        for (AbstractSyntaxTree conditional : conditionals) {
            bodyNode = conditional.getName() == TokenType.KW_ELSE ? conditional.getChildren().get(0) : conditional.getChildren().get(1);
            returns = returns && functionReturns(bodyNode, returnType);
        }
        return returns;
    }

    private TokenType getControlFlowType(AbstractSyntaxTree controlFlow) {
        return controlFlow.getName();
    }
    private boolean isLoopControl(AbstractSyntaxTree node) {
        if (!node.getLabel().equals("CONTROL-FLOW"))
            return false;
        TokenType controlFlow = getControlFlowType(node.getChildren().get(0));
        return controlFlow == TokenType.KW_CNT || controlFlow == TokenType.KW_BRK;
    }

    private boolean isReturn(AbstractSyntaxTree node) {
        if (!node.getLabel().equals("CONTROL-FLOW"))
            return false;
        return node.getChildren().get(0).getName() == TokenType.KW_RET;
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
            analyze(fnDetails.get(index));
            symbolTable.leaveScope();
            symbolTable.leaveScope();
        }
    }

    private void handleFunctionCall() {

    }

    private void handleForLoop(AbstractSyntaxTree node) {

    }

    private void handleWhileLoop(AbstractSyntaxTree node) {
        // TODO: handle loop inside function
        AbstractSyntaxTree conditional = node.getChildren().get(0);
        if (evaluateType(conditional) != NodeType.BOOLEAN)
            throw new TypeError("While loop condition must evaluate to boolean");
        // check for breaks/continues
        List<AbstractSyntaxTree> body = node.getChildren().get(1).getChildren();
        int length = body.size();
        AbstractSyntaxTree current;
        for (int i = 0; i < length; i++) {
            current = body.get(i);
            if (current.getName() == TokenType.KW_WHILE)
                handleWhileLoop(current);
            else if (current.getName() == TokenType.KW_FOR)
                handleForLoop(current);
            else if (current.getLabel().equals("COND"))
                handleConditional(current);
            else if (current.getLabel().equals("CONTROL-FLOW")) {
                if (i != length - 1) { // there are nodes after the current one
                    throw new UnreachableCodeError(current.getChildren().get(0) + " used before end of loop so code after cannot be reached");
                }
                if (current.getChildren().get(0).getName() == TokenType.KW_RET) {
                    throw new IllegalStatementError("Cannot return outside of a function");
                }
            }
        }
    }

    private void handleConditionalBlock(AbstractSyntaxTree node) {
        for (AbstractSyntaxTree child : node.getChildren()) {
            handleConditional(child);
        }
    }

    private void handleConditional(AbstractSyntaxTree node) {
        if (node.getName() == TokenType.KW_IF || node.getLabel().equals("ELSE IF")) {
            if (!node.hasChildren() || evaluateType(node.getChildren().get(0)) != NodeType.BOOLEAN)
                throw new TypeError("Condition in if must evaluate to boolean");
        }
        if (node.getName() == TokenType.KW_ELSE) {
            // TODO: handle any potential errors
        }
        // handle cases where inside loop and/or function
    }

    private NodeType handleTernary(AbstractSyntaxTree node) {
        if (evaluateType(node.getChildren().get(0)) != NodeType.BOOLEAN)
            throw new TypeError("Ternary expression must start with boolean expression");
        // the left and right must be compatible types
        NodeType leftType = evaluateType(node.getChildren().get(1));
        NodeType rightType = evaluateType(node.getChildren().get(2));
        if (leftType == rightType)
            return leftType;
        if (leftType == NodeType.NULL || rightType == NodeType.NULL)
            return leftType == NodeType.NULL ? rightType : leftType;
        throw new TypeError("Ternary expression cannot evaluate to different types");
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
        if (!isArrayLiteral(children.get(0))) {
            NodeType type = evaluateType(children.get(0));
            NodeType currentType;
            for (int i = 1; i < children.size(); i++) {
                if (isArrayLiteral(children.get(i)))
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
        if (node.getLabel().equals("TERNARY"))
            return handleTernary(node);
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
