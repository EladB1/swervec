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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> nonEqualityComparisons = List.of("<", "<=", ">", ">=");
    private final List<String> arithmeticOperators = List.of("-", "/", "%", "**");
    private final List<String> assignmentOperators = List.of("=", "+=", "-=", "*=", "/=");

    private final Map<TokenType, NodeType> typeMappings = Map.ofEntries(
        entry(TokenType.KW_BOOL, NodeType.BOOLEAN),
        entry(TokenType.KW_INT, NodeType.INT),
        entry(TokenType.KW_FLOAT, NodeType.FLOAT),
        entry(TokenType.KW_STR, NodeType.STRING),
        entry(TokenType.KW_ARR, NodeType.ARRAY),
        entry(TokenType.KW_NULL, NodeType.NULL)
    );

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

    private boolean isTypeLabel(AbstractSyntaxTree node) {
        if (node.getName() == null)
            return false;
        return typeMappings.containsKey(node.getName());
    }

    // use analyze() to handle bodies of different structures (program, function, loop, conditional)
    // this should handle nesting and embedded elements
    // will be recursive
    // should return true if a return is found in the specific block (with right context and positioning)

    public void analyze(AbstractSyntaxTree AST) {
        analyze(AST, false, false, NodeType.NONE);
    }

    public void analyze(AbstractSyntaxTree AST, boolean inLoop, boolean inFunc, NodeType returnType) {
        for (AbstractSyntaxTree subTree : AST.getChildren()) {
            if (matchesLabelNode(subTree, "VAR-DECL")) {
                handleVariableDeclaration(subTree);
            }
            else if (matchesLabelNode(subTree, "ARRAY-DECL")) {}
            else if (subTree.getLabel().equals("CONTROL-FLOW")) {
                if (!inFunc && isReturn(subTree))
                    throw new IllegalStatementError("Cannot return outside of a function");
                if (!inLoop && !isReturn(subTree)) {
                    TokenType loopControl = getControlFlowType(subTree.getChildren().get(0));
                    throw new IllegalStatementError("Cannot use " + loopControl  + " outside of a loop");
                }

            }

            // TODO: handle conditionals, loops, and functions then recurse
            // conditional
            else if (subTree.getLabel().equals("COND")) {
                List<AbstractSyntaxTree> conditionals = subTree.getChildren();
                int length = conditionals.size();
                NodeType condition;
                AbstractSyntaxTree body;
                for (int i = 0; i < length - 1; i++) {
                    if (i == 0) {
                        if (conditionals.get(i).getName() != TokenType.KW_IF)
                            throw new IllegalStatementError("Conditional block must begin with if", conditionals.get(i).getLineNumber());
                        condition = evaluateType(conditionals.get(i).getChildren().get(1));
                        if (condition != NodeType.BOOLEAN)
                            throw new TypeError("Conditional statement must evaluate to boolean but instead was " + condition, conditionals.get(i).getLineNumber());
                        body = conditionals.get(i).getChildren().get(1);
                    }

                    else if (i != length - 1) {
                        if (conditionals.get(i).getName() == TokenType.KW_ELSE)
                            throw new IllegalStatementError("else can only be used at the end of conditional block", conditionals.get(i).getLineNumber());
                        condition = evaluateType(conditionals.get(i).getChildren().get(1));
                        if (condition != NodeType.BOOLEAN)
                            throw new TypeError("Conditional statement must evaluate to boolean but instead was " + condition, conditionals.get(i).getLineNumber());
                        body = conditionals.get(i).getChildren().get(1);
                    }

                    else {
                        if (conditionals.get(i).getName() == TokenType.KW_ELSE) {
                            body = conditionals.get(i).getChildren().get(0);
                        }
                        else {
                            condition = evaluateType(conditionals.get(i).getChildren().get(1));
                            if (condition != NodeType.BOOLEAN)
                                throw new TypeError("Conditional statement must evaluate to boolean but instead was " + condition, conditionals.get(i).getLineNumber());
                            body = conditionals.get(i).getChildren().get(1);
                        }
                    }
                    symbolTable.enterScope();
                    analyze(body, inLoop, inFunc, returnType);
                    symbolTable.leaveScope();
                }
            }

            // loop
            else if (subTree.getName() == TokenType.KW_WHILE) {
                // check loop signature
                NodeType condition = evaluateType(subTree.getChildren().get(0));
                if (condition != NodeType.BOOLEAN) {
                    throw new TypeError("While loop condition must evaluate to boolean but instead was " + condition, subTree.getLineNumber());
                }
                // analyze body
                AbstractSyntaxTree body = subTree.getChildren().get(1);
                if (body.getLabel().equals("BLOCK-BODY")) {
                    loopHasControlFlow(body); // this will check for unreachable code
                    symbolTable.enterScope();
                    analyze(body, true, inFunc, returnType);
                    symbolTable.leaveScope();
                }

            }

            else if (subTree.getName() == TokenType.KW_FOR) {
                int scope = symbolTable.enterScope();
                int lineNum = subTree.getLineNumber();
                List<AbstractSyntaxTree> loopDetails = subTree.getChildren();
                int length = loopDetails.size();
                AbstractSyntaxTree body = null;
                switch (length) {
                    case 2: // for (int i = 0) {}
                        if (loopDetails.get(0).getValue().equals("=")) {
                            Symbol symbol = symbolTable.lookup(loopDetails.get(0).getValue());
                            if (symbol == null)
                                throw new ReferenceError("Variable " + loopDetails.get(0).getValue() + " used before being defined in current scope", lineNum);
                            validateAssignment(loopDetails.get(1));
                        }
                        else if (loopDetails.get(0).getLabel().equals("VAR-DECL")) {
                            if (loopDetails.get(0).countChildren() != 3)
                                throw new IllegalStatementError("Loop variable must be non-constant and assigned to a value", lineNum);
                            symbolTable.insert(new Symbol(loopDetails.get(0), scope));
                        }
                        else {
                            throw new IllegalStatementError("For loop must start with variable declaration or assignment", lineNum);
                        }
                        body = loopDetails.get(1);
                        break;
                    case 3: // for (float element : array) {}
                        if (!loopDetails.get(0).getLabel().equals("VAR-DECL"))
                            throw new IllegalStatementError("Variable declaration must be at start of for (each) loop", lineNum);
                        if (loopDetails.get(0).countChildren() != 2)
                            throw new IllegalStatementError("Loop variable must be non-constant and not-initialized", lineNum);
                        Symbol loopVar = new Symbol(loopDetails.get(0), scope);
                        NodeType loopVarType = typeMappings.get(loopVar.getType().getName());

                        Symbol container = symbolTable.lookup(loopDetails.get(1).getValue());
                        if (container == null)
                            throw new ReferenceError("Variable " + loopDetails.get(1).getValue() + " used before being defined in current scope", lineNum);
                        NodeType containerType = typeMappings.get(container.getType().getName());
                        if (containerType != NodeType.ARRAY && containerType != NodeType.STRING)
                            throw new TypeError("Cannot use for (each) loop on non-container type: " + containerType, lineNum);

                        if (loopVarType != NodeType.STRING && containerType == NodeType.STRING) {
                            throw new TypeError("Cannot loop over string with type " + loopVarType, lineNum);
                        }
                        symbolTable.insert(loopVar);
                        body = loopDetails.get(2);
                        break;
                    case 4: // for (int i = 0; i < 10; i++) {}
                        NodeType varType;
                        if (loopDetails.get(0).getLabel().equals("VAR-DECL")) {
                            Symbol symbol = new Symbol(loopDetails.get(0), scope);
                            varType = typeMappings.get(symbol.getType().getName());
                            if (loopDetails.get(0).countChildren() != 3)
                                throw new IllegalStatementError("Loop variable must be non-constant and assigned to a value", lineNum);
                            symbolTable.insert(symbol);
                        }
                        else if (loopDetails.get(0).getValue().equals("=")) {
                            Symbol symbol = symbolTable.lookup(loopDetails.get(0).getValue());
                            if (symbol == null)
                                throw new ReferenceError("Variable " + loopDetails.get(0).getValue() + " used before being defined in current scope", lineNum);
                            varType = typeMappings.get(symbol.getType().getName());
                            validateAssignment(loopDetails.get(1));
                        }
                        else {
                            throw new IllegalStatementError("For loop must start with variable declaration or assignment", lineNum);
                        }
                        if (evaluateType(loopDetails.get(1)) != NodeType.BOOLEAN)
                            throw new IllegalStatementError("Middle for loop expression must be boolean", lineNum);
                        NodeType thirdPartType = evaluateType(loopDetails.get(2));
                        if (varType != thirdPartType)
                            throw new IllegalStatementError("End for loop expression must match type " + varType + " but was " + thirdPartType, lineNum);
                        body = loopDetails.get(3);
                        break;
                }
                analyze(body, true, inFunc, returnType);
                symbolTable.leaveScope();
            }

            // function
            else if (subTree.getName() == TokenType.KW_FN) {
                int scope = symbolTable.enterScope();
                List<AbstractSyntaxTree> fnDetails = subTree.getChildren();
                int length = fnDetails.size();
                String name = fnDetails.get(0).getValue();
                NodeType fnReturnType = NodeType.NONE;
                AbstractSyntaxTree body = null;
                NodeType[] types = {};
                Symbol[] params = {};
                switch (length) {
                    case 1:
                        symbolTable.insert(new FunctionSymbol(name));
                        symbolTable.leaveScope();
                        continue;
                    case 2:
                        if (isTypeLabel(fnDetails.get(1))) // has a returnType but no function body
                            throw new TypeError(
                                "Function " + name + "expected to return " + typeMappings.get(fnDetails.get(1).getName()) + " but returns nothing",
                                subTree.getLineNumber()
                            );
                        else if (fnDetails.get(1).getLabel().equals("FUNC-PARAMS")) {
                            types = getParamTypes(fnDetails.get(1).getChildren());
                            params = paramsToSymbols(fnDetails.get(1).getChildren(), scope);
                        }
                        else {
                            body = fnDetails.get(1);
                        }
                        break;
                    case 3:
                        if (fnDetails.get(1).getLabel().equals("FUNC-PARAMS")) {
                            types = getParamTypes(fnDetails.get(1).getChildren());
                            params = paramsToSymbols(fnDetails.get(1).getChildren(), scope);
                            if (isTypeLabel(fnDetails.get(2)))
                                throw new TypeError(
                                    "Function " + name + "expected to return " + typeMappings.get(fnDetails.get(1).getName()) + " but returns nothing",
                                    subTree.getLineNumber()
                                );
                            else {
                                body = fnDetails.get(2);
                            }
                        }
                        if (isTypeLabel(fnDetails.get(1))) {
                            fnReturnType = typeMappings.get(fnDetails.get(1).getName());
                            body = fnDetails.get(2);
                        }
                        break;
                    case 4:
                        types = getParamTypes(fnDetails.get(1).getChildren());
                        params = paramsToSymbols(fnDetails.get(1).getChildren(), scope);
                        fnReturnType = typeMappings.get(fnDetails.get(2).getName());
                        body = fnDetails.get(3);
                        break;
                }
                for (Symbol param : params) {
                    symbolTable.insert(param);
                }
                //symbolTable.enterScope(); // enter function body scope
                analyze(body, inLoop, true, fnReturnType);
                symbolTable.insert(new FunctionSymbol(name, fnReturnType, types, body));
                if (fnReturnType != NodeType.NONE && !functionReturns(body, fnReturnType))
                    throw new TypeError("Function " + name + " expected to return " + fnReturnType + " but does not return for all branches", subTree.getLineNumber());
                symbolTable.leaveScope();
                //symbolTable.leaveScope();
            }
            else {
                evaluateType(subTree);
            }
        }
    }

    private void validateReturn(AbstractSyntaxTree controlFlow, NodeType returnType) {
        // make sure return is done properly
        if (returnType == NodeType.NONE) {
            if (controlFlow.countChildren() == 1)
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
            if (controlFlow.countChildren() == 1)
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
                AbstractSyntaxTree lastChild = node.getChildren().get(node.countChildren() - 1);
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
        return false;
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

    public boolean loopHasControlFlow(AbstractSyntaxTree loopBody) {
        List<AbstractSyntaxTree> contents = loopBody.getChildren();
        int length = contents.size();
        AbstractSyntaxTree node;
        for (int i = 0; i < length; i++) {
            node = contents.get(i);
            if (node.getLabel().equals("COND")) {
                if (conditionalBlockHasLoopControl(node)) {
                    if (i < length - 1)
                        throw new UnreachableCodeError("Unreachable statement following continuing/breaking conditional block", contents.get(i+1).getLineNumber());
                    return true;
                }
            }
            if (node.getName() == TokenType.KW_WHILE || node.getName() == TokenType.KW_FOR) {
                AbstractSyntaxTree bodyNode = node.getChildren().get(node.countChildren() - 1);
                if (bodyNode.getLabel().equals("BLOCK-BODY"))
                    return loopHasControlFlow(bodyNode);
            }
            if (node.getLabel().equals("CONTROL-FLOW")) {
                node = node.getChildren().get(0);
                if (i < length - 1) {
                    String controlType = node.getName() == TokenType.KW_BRK ? "break" : "continue";
                    throw new UnreachableCodeError("Unreachable statement following " + controlType, contents.get(i+1).getLineNumber());
                }
                if (node.getName() == TokenType.KW_BRK || node.getName() == TokenType.KW_CNT)
                    return true;
            }
        }
        return false;
    }

    public boolean conditionalBlockHasLoopControl(AbstractSyntaxTree conditionalBlock) {
        List<AbstractSyntaxTree> conditionals = conditionalBlock.getChildren();
        if (conditionals.size() == 0)
            return false;
        if (conditionals.get(conditionals.size() - 1).getName() != TokenType.KW_ELSE)
            return false;
        boolean hasLoopControl = true;
        AbstractSyntaxTree bodyNode;
        for (AbstractSyntaxTree conditional : conditionals) {
            bodyNode = conditional.getName() == TokenType.KW_ELSE ? conditional.getChildren().get(0) : conditional.getChildren().get(1);
            hasLoopControl = hasLoopControl && loopHasControlFlow(bodyNode);
        }
        return hasLoopControl;
    }

    private TokenType getControlFlowType(AbstractSyntaxTree controlFlow) {
        return controlFlow.getName();
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

    private Symbol[] paramsToSymbols(List<AbstractSyntaxTree> params, int scope) {
        Symbol[] paramSymbols = new Symbol[params.size()];
        AbstractSyntaxTree param;
        for (int i = 0; i < params.size(); i++) {
            param = params.get(i);
            paramSymbols[i] = new Symbol(param, scope);
        }
        return paramSymbols;
    }

    private NodeType[] getParamTypes(List<AbstractSyntaxTree> params) {
        NodeType[] types = new NodeType[params.size()];
        AbstractSyntaxTree param;
        for (int i = 0; i < params.size(); i++) {
            param = params.get(i);
            types[i] = typeMappings.get(param.getChildren().get(0).getName());
        }
        return types;
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

    public void validateAssignment(AbstractSyntaxTree assignmentNode) {
        String varName = assignmentNode.getChildren().get(0).getValue();
        Symbol symbol = symbolTable.lookup(varName);
        if (symbol == null)
            throw new ReferenceError("Variable " + varName + " is used before being defined in current scope", assignmentNode.getLineNumber());
        NodeType varType = typeMappings.get(symbol.getType().getName());
        AbstractSyntaxTree rightHandSide = assignmentNode.getChildren().get(1);
        NodeType rhsType = evaluateType(rightHandSide);
        switch (assignmentNode.getValue()) {
            case "=":
                if (rhsType != NodeType.NULL && rhsType != varType)
                    throw new TypeError("Cannot assign " + rhsType + " to variable of type " + varType, assignmentNode.getLineNumber());
                break;
            case "+=":
                handleAddition(assignmentNode);
                break;
            case "-=":
            case "/=":
                handleArithmetic(assignmentNode);
                break;
            case "*=":
                handleMultiplication(assignmentNode);
                break;
        }
    }

    public NodeType evaluateType(AbstractSyntaxTree node) {
        if (node.getName() == TokenType.ID) {
            if (!node.hasChildren()) {
                Symbol symbol = symbolTable.lookup(node.getValue());
                if (symbol == null)
                    throw new ReferenceError("Variable '" + node.getValue() + "' used before being defined in current scope", node.getLineNumber());
                return typeMappings.get(symbol.getType().getName());
            }
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
        //if (node.getToken() != null) {
            if (nonEqualityComparisons.contains(node.getValue()))
                return handleComparison(node);
            if (node.getValue().equals("==") || node.getValue().equals("!="))
                return handleEqualityComparison(node);
            if (arithmeticOperators.contains(node.getValue()))
                return handleArithmetic(node);
            if (node.getValue().equals("&") || node.getValue().equals("^"))
                return handleBitwise(node);
            if (node.getValue().equals("*"))
                return handleMultiplication(node);
            if (node.getValue().equals("+"))
                return handleAddition(node);
            if (assignmentOperators.contains(node.getValue()))
                validateAssignment(node);
        //}

        if (node.getLabel().equals("UNARY-OP"))
            return handleUnaryOp(node);
        if (node.getLabel().equals("FUNC-CALL")) {
            List<AbstractSyntaxTree> children = node.getChildren();
            String name = children.get(0).getValue();
            FunctionSymbol matchingDefinition;
            NodeType[] types = {};
            if (children.size() != 1) {
                List<AbstractSyntaxTree> funcParams = children.get(1).getChildren();
                types = new NodeType[funcParams.size()];
                for (int i = 0; i < funcParams.size(); i++) {
                    types[i] = evaluateType(funcParams.get(i));
                }
            }
            matchingDefinition = symbolTable.lookup(name, types);
            if (matchingDefinition == null)
                throw new ReferenceError("Could not find definition for " + name + "(" + Arrays.toString(types) + ")", children.get(0).getLineNumber());
            return matchingDefinition.getReturnType();
        }
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
            throw new TypeError("Cannot compare " + leftType + " with " + rightType + " using " + comparisonOperator, rootNode.getLineNumber());
        return NodeType.BOOLEAN;
    }

    private NodeType handleBitwise(AbstractSyntaxTree rootNode) {
        String comparisonOperator = rootNode.getValue();
        NodeType leftType = evaluateType(rootNode.getChildren().get(0));
        NodeType rightType = evaluateType(rootNode.getChildren().get(1));
        Set<NodeType> acceptedTypes = Set.of(NodeType.INT, NodeType.BOOLEAN);
        if (!(acceptedTypes.contains(leftType) && acceptedTypes.contains(rightType)))
            throw new TypeError("Binary expression (" + comparisonOperator + ") with " + leftType + " and " + rightType + " is not valid", rootNode.getLineNumber());
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
        throw new TypeError("Cannot multiply " + leftType + " with " + rightType, rootNode.getLineNumber());
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
        throw new TypeError("Arithmetic expression (" + operator + ") with " + leftType + " and " + rightType + " is not valid", rootNode.getLineNumber());
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
        throw new TypeError("Cannot add " + leftType + " with " + rightType, rootNode.getLineNumber());
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
        throw new TypeError("Cannot check for equality between " + leftType + " and " + rightType, rootNode.getLineNumber());
    }

    private NodeType handleUnaryOp(AbstractSyntaxTree rootNode) {
        AbstractSyntaxTree left = rootNode.getChildren().get(0);
        AbstractSyntaxTree right = rootNode.getChildren().get(1);
        NodeType leftType = evaluateType(left);
        NodeType rightType = evaluateType(right);
        if (left.getValue().equals("!") && rightType == NodeType.BOOLEAN)
            return NodeType.BOOLEAN;
        if (left.getValue().equals("-") && (rightType == NodeType.INT || rightType == NodeType.FLOAT))
            return rightType;
        if (left.getValue().equals("++") || left.getValue().equals("--")) {
            if (right.getName() != TokenType.ID)
                throw new IllegalStatementError("Can only increment/decrement non-variables using operator " + left.getValue(), left.getLineNumber());
            if (symbolTable.lookup(right.getValue()).getValueNodes() == null)
                throw new IllegalStatementError("Can only increment/decrement initialized variables using operator " + left.getValue(), left.getLineNumber());
            if (rightType == NodeType.INT || rightType == NodeType.FLOAT) {
                // TODO: handle array indexes
                if (right.getName() != TokenType.ID)
                    throw new ReferenceError("Cannot perform prefix expression(" + left.getValue() + ") on " + right.getName(), left.getLineNumber());
                return rightType;
            }
        }
        if (right.getValue().equals("++") || right.getValue().equals("--")) {
            if (left.getName() != TokenType.ID)
                throw new IllegalStatementError("Can only increment/decrement non-variables using operator " + right.getValue(), right.getLineNumber());
            if (symbolTable.lookup(left.getValue()).getValueNodes() == null)
                throw new IllegalStatementError("Can only increment/decrement initialized variables using operator " + right.getValue(), right.getLineNumber());
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
