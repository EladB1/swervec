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
import java.util.Set;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> nonEqualityComparisons = List.of("<", "<=", ">", ">=");
    private final List<String> arithmeticOperators = List.of("-", "/", "%", "**");
    private final List<String> assignmentOperators = List.of("=", "+=", "-=", "*=", "/=");

    private final List<TokenType> typeTokens = List.of(
        TokenType.KW_BOOL,
        TokenType.KW_INT,
        TokenType.KW_FLOAT,
        TokenType.KW_STR,
        TokenType.KW_ARR
    );

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
        return node.getName() == TokenType.NUMBER && (node.getValue().contains(".") || node.getValue().equals("0"));
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
        return typeTokens.contains(node.getName());
    }

    public void analyze(AbstractSyntaxTree AST) {
        analyze(AST, false, false, new EntityType(NodeType.NONE));
    }

    public void analyze(AbstractSyntaxTree AST, boolean inLoop, boolean inFunc, EntityType returnType) {
        for (AbstractSyntaxTree subTree : AST.getChildren()) {
            if (matchesLabelNode(subTree, "VAR-DECL")) {
                handleVariableDeclaration(subTree);
            }
            else if (matchesLabelNode(subTree, "ARRAY-DECL")) {
                handleArrayDeclaration(subTree);
            }
            // break / continue / return
            else if (subTree.getLabel().equals("CONTROL-FLOW")) {
                if (!inFunc && isReturn(subTree))
                    throw new IllegalStatementError("Cannot return outside of a function");
                if (!inLoop && !isReturn(subTree)) {
                    TokenType loopControl = getControlFlowType(subTree.getChildren().get(0));
                    throw new IllegalStatementError("Cannot use " + loopControl  + " outside of a loop");
                }
            }

            // conditional
            else if (subTree.getLabel().equals("COND")) {
                handleConditionalBlock(subTree.getChildren(), inLoop, inFunc, returnType);
            }

            // loop
            else if (subTree.getName() == TokenType.KW_WHILE) {
                handleWhileLoop(subTree, inFunc, returnType);
            }

            else if (subTree.getName() == TokenType.KW_FOR) {
                handleForLoop(subTree, inFunc, returnType);
            }

            // function
            else if (subTree.getName() == TokenType.KW_FN) {
                handleFunctionDefintion(subTree.getChildren());
            }
            else {
                evaluateType(subTree);
            }
        }
    }

    private void handleVariableDeclaration(AbstractSyntaxTree node) {
        int scope = symbolTable.getScopeLevel();
        List<AbstractSyntaxTree> details = node.getChildren();
        int offset = 0;
        if (details.get(0).getName() == TokenType.KW_CONST) {
            offset++;
            if (node.countChildren() != 4)
                throw new IllegalStatementError("Constant variable must be initialized to a variable", node.getLineNumber());
        }
        if (node.countChildren() == offset + 3) {
            EntityType rhsType = evaluateType(details.get(offset + 2));
            EntityType lhsType = new EntityType(details.get(offset));
            if (!lhsType.equals(rhsType) && !rhsType.isType(NodeType.NULL))
                throw new TypeError("Right hand side of variable is " + rhsType + " but " + lhsType + " expected", node.getLineNumber());
        }
        symbolTable.insert(new Symbol(node, scope));
    }

    private void handleArrayDeclaration(AbstractSyntaxTree node) {
        int scope = symbolTable.getScopeLevel();
        List<AbstractSyntaxTree> details = node.getChildren();
        int offset = 0;
        int valueIndex = 0;
        boolean isConstantImmutable = false;
        EntityType literalType;
        if (details.get(0).getName() == TokenType.KW_CONST) {
            if (details.get(1).getName() == TokenType.KW_MUT)
                offset = 2;
            else {
                offset = 1;
                isConstantImmutable = true;
            }
        }
        else if (details.get(0).getName() == TokenType.KW_MUT)
            offset = 1;
        EntityType declaredType = new EntityType(details.get(offset));
        String name = details.get(offset + 1).getValue();
        List<Integer> sizes = List.of(0);
        if (isConstantImmutable) {
            switch (details.size()) {
                case 3:
                    throw new IllegalStatementError("Constant immutable array " + name + " must be set to a value", node.getLineNumber());
                case 4:
                    if (details.get(3).getLabel().equals("ARRAY-INDEX"))
                        throw new IllegalStatementError("Constant immutable array " + name + " must be set to a value", node.getLineNumber());
                    valueIndex = 3;
                    sizes = ArrayChecks.estimateArraySizes(details.get(3));
                    literalType = estimateArrayTypes(details.get(3));
                    if (!declaredType.containsSubType(literalType))
                        throw new TypeError("Array literal type " + literalType + " does not match declared array type " + declaredType, node.getLineNumber());
                    break;
                case 5:
                    valueIndex = 4;
                    sizes = handleArraySizes(details.get(3));
                    literalType = estimateArrayTypes(details.get(4));
                    if (!declaredType.containsSubType(literalType))
                        throw new TypeError("Array literal type " + literalType + " does not match declared array type " + declaredType, node.getLineNumber());
                    break;
            }
        } else {
            if (details.size() < offset + 3)
                throw new IllegalStatementError("Array " + name + " missing size", node.getLineNumber());
            sizes = handleArraySizes(details.get(offset + 2));
            if (offset == 2 && details.size() < offset + 4)
                throw new IllegalStatementError("Constant mutable array " + name + " missing value", node.getLineNumber());
            if (details.size() == offset + 4) {
                valueIndex = offset + 3;
                literalType = estimateArrayTypes(details.get(offset + 3));
                if (!declaredType.containsSubType(literalType))
                    throw new TypeError("Array literal type " + literalType + " does not match declared array type " + declaredType, node.getLineNumber());
            }
        }
        symbolTable.insert(new Symbol(node, sizes, declaredType, valueIndex, scope));
    }

    private void handleConditionalBlock(List<AbstractSyntaxTree> conditionals, boolean inLoop, boolean inFunc, EntityType returnType) {
            int length = conditionals.size();
            EntityType condition;
            AbstractSyntaxTree body;
            for (int i = 0; i < length - 1; i++) {
                if (i == 0) {
                    if (conditionals.get(i).getName() != TokenType.KW_IF)
                        throw new IllegalStatementError("Conditional block must begin with if", conditionals.get(i).getLineNumber());
                    condition = evaluateType(conditionals.get(i).getChildren().get(1));
                    if (!condition.isType(NodeType.BOOLEAN))
                        throw new TypeError("Conditional statement must evaluate to boolean but instead was " + condition, conditionals.get(i).getLineNumber());
                    body = conditionals.get(i).getChildren().get(1);
                }

                else if (i != length - 1) {
                    if (conditionals.get(i).getName() == TokenType.KW_ELSE)
                        throw new IllegalStatementError("else can only be used at the end of conditional block", conditionals.get(i).getLineNumber());
                    condition = evaluateType(conditionals.get(i).getChildren().get(1));
                    if (!condition.isType(NodeType.BOOLEAN))
                        throw new TypeError("Conditional statement must evaluate to boolean but instead was " + condition, conditionals.get(i).getLineNumber());
                    body = conditionals.get(i).getChildren().get(1);
                }

                else {
                    if (conditionals.get(i).getName() == TokenType.KW_ELSE) {
                        body = conditionals.get(i).getChildren().get(0);
                    }
                    else {
                        condition = evaluateType(conditionals.get(i).getChildren().get(1));
                        if (!condition.isType(NodeType.BOOLEAN))
                            throw new TypeError("Conditional statement must evaluate to boolean but instead was " + condition, conditionals.get(i).getLineNumber());
                        body = conditionals.get(i).getChildren().get(1);
                    }
                }
                symbolTable.enterScope();
                analyze(body, inLoop, inFunc, returnType);
                symbolTable.leaveScope();
            }
    }

    private void handleFunctionDefintion(List<AbstractSyntaxTree> fnDetails) {
        int scope = symbolTable.enterScope();
        int lineNum = fnDetails.get(0).getLineNumber();
        int length = fnDetails.size();
        String name = fnDetails.get(0).getValue();
        EntityType fnReturnType = new EntityType(NodeType.NONE);
        AbstractSyntaxTree body = null;
        EntityType[] types = {};
        Symbol[] params = {};
        switch (length) {
            case 1:
                symbolTable.insert(new FunctionSymbol(name));
                symbolTable.leaveScope();
                return;
            case 2:
                if (isTypeLabel(fnDetails.get(1))) // has a returnType but no function body
                    throw new TypeError(
                        "Function " + name + "expected to return " + fnDetails.get(1) + " but returns nothing",
                        lineNum
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
                            "Function " + name + "expected to return " + fnDetails.get(1) + " but returns nothing",
                            lineNum
                        );
                    else {
                        body = fnDetails.get(2);
                    }
                }
                if (isTypeLabel(fnDetails.get(1))) {
                    fnReturnType = new EntityType(fnDetails.get(1));
                    body = fnDetails.get(2);
                }
                break;
            case 4:
                types = getParamTypes(fnDetails.get(1).getChildren());
                params = paramsToSymbols(fnDetails.get(1).getChildren(), scope);
                fnReturnType = new EntityType(fnDetails.get(2));
                body = fnDetails.get(3);
                break;
        }
        for (Symbol param : params) {
            symbolTable.insert(param);
        }
        analyze(body, false, true, fnReturnType);
        symbolTable.insert(new FunctionSymbol(name, fnReturnType, types, body));
        if (!fnReturnType.isType(NodeType.NONE) && !functionReturns(body, fnReturnType))
            throw new TypeError("Function " + name + " expected to return " + fnReturnType + " but does not return for all branches", lineNum);
        symbolTable.leaveScope();
    }

    private List<Integer> handleArraySizes(AbstractSyntaxTree sizeNode) {
        List<Integer> sizes = new ArrayList<>();
        AbstractSyntaxTree current = sizeNode;
        while (current.hasChildren()) {
            if (!isIntegerLiteral(current.getChildren().get(0)) || current.getValue().equals("0"))
                throw new IllegalStatementError("Array index must be a non-zero integer literal", current.getLineNumber());
            sizes.add(Integer.valueOf(current.getChildren().get(0).getValue()));
            if (current.getChildren().size() == 2)
                current = current.getChildren().get(1);
            else
                break;
        }
        if (sizes.size() == 0)
            throw new IllegalStatementError("Array sizes missing valid values", sizeNode.getLineNumber());
        return sizes;
    }

    private void handleWhileLoop(AbstractSyntaxTree loopNode, boolean inFunc, EntityType returnType) {
        // check loop signature
        EntityType condition = evaluateType(loopNode.getChildren().get(0));
        if (!condition.isType(NodeType.BOOLEAN)) {
            throw new TypeError("While loop condition must evaluate to boolean but instead was " + condition, loopNode.getLineNumber());
        }
        // analyze body
        AbstractSyntaxTree body = loopNode.getChildren().get(1);
        if (body.getLabel().equals("BLOCK-BODY")) {
            loopHasControlFlow(body); // this will check for unreachable code
            symbolTable.enterScope();
            analyze(body, true, inFunc, returnType);
            symbolTable.leaveScope();
        }
    }

    private void handleForLoop(AbstractSyntaxTree loopNode, boolean inFunc, EntityType returnType) {
        int scope = symbolTable.enterScope();
            int lineNum = loopNode.getLineNumber();
            List<AbstractSyntaxTree> loopDetails = loopNode.getChildren();
            int length = loopDetails.size();
            AbstractSyntaxTree body = null;
            switch (length) {
                case 3: // for (float element : array) {}
                    if (!loopDetails.get(0).getLabel().equals("VAR-DECL"))
                        throw new IllegalStatementError("Variable declaration must be at start of for (each) loop", lineNum);
                    if (loopDetails.get(0).countChildren() != 2)
                        throw new IllegalStatementError("Loop variable must be non-constant and not-initialized", lineNum);
                    Symbol loopVar = new Symbol(loopDetails.get(0), scope);
                    EntityType loopVarType = loopVar.getType();

                    Symbol container = symbolTable.lookup(loopDetails.get(1).getValue());
                    if (container == null)
                        throw new ReferenceError("Variable " + loopDetails.get(1).getValue() + " used before being defined in current scope", lineNum);
                    EntityType containerType = container.getType();
                    if (!containerType.startsWith(NodeType.ARRAY) && !containerType.isType(NodeType.STRING))
                        throw new TypeError("Cannot use for (each) loop on non-container type: " + containerType, lineNum);

                    if (!loopVarType.isType(NodeType.STRING) && containerType.isType(NodeType.STRING)) {
                        throw new TypeError("Cannot loop over string with type " + loopVarType, lineNum);
                    }
                    symbolTable.insert(loopVar);
                    body = loopDetails.get(2);
                    break;
                case 4: // for (int i = 0; i < 10; i++) {}
                    EntityType varType;
                    if (loopDetails.get(0).getLabel().equals("VAR-DECL")) {
                        Symbol symbol = new Symbol(loopDetails.get(0), scope);
                        varType = symbol.getType();
                        if (loopDetails.get(0).countChildren() != 3)
                            throw new IllegalStatementError("Loop variable must be non-constant and assigned to a value", lineNum);
                        symbolTable.insert(symbol);
                    }
                    else if (loopDetails.get(0).getValue().equals("=")) {
                        Symbol symbol = symbolTable.lookup(loopDetails.get(0).getValue());
                        if (symbol == null)
                            throw new ReferenceError("Variable " + loopDetails.get(0).getValue() + " used before being defined in current scope", lineNum);
                        varType = symbol.getType();
                        validateAssignment(loopDetails.get(1));
                    }
                    else {
                        throw new IllegalStatementError("For loop must start with variable declaration or assignment", lineNum);
                    }
                    if (!evaluateType(loopDetails.get(1)).isType(NodeType.BOOLEAN))
                        throw new IllegalStatementError("Middle for loop expression must be boolean", lineNum);
                    EntityType thirdPartType = evaluateType(loopDetails.get(2));
                    if (varType != thirdPartType)
                        throw new IllegalStatementError("End for loop expression must match type " + varType + " but was " + thirdPartType, lineNum);
                    body = loopDetails.get(3);
                    break;
            }
            analyze(body, true, inFunc, returnType);
            symbolTable.leaveScope();
    }

    private void validateReturn(AbstractSyntaxTree controlFlow, EntityType returnType) {
        // make sure return is done properly
        if (returnType.isType(NodeType.NONE)) {
            if (controlFlow.countChildren() == 1)
                return;
            else {
                AbstractSyntaxTree returnValue = controlFlow.getChildren().get(1);
                throw new TypeError("Cannot return value from void function", returnValue.getLineNumber());
            }

        }
        else {
            if (controlFlow.countChildren() == 1)
                throw new TypeError("Expected return type " + returnType + " but didn't return a value", controlFlow.getChildren().get(0).getLineNumber());
            AbstractSyntaxTree returnValue = controlFlow.getChildren().get(1);
            EntityType actualReturnType = evaluateType(returnValue);
            if (!actualReturnType.equals(returnType) && returnValue.getName() != TokenType.KW_NULL)
                throw new TypeError("Expected " + returnType + " to be returned but got " + actualReturnType, returnValue.getLineNumber());
        }
    }

    public boolean functionReturns(AbstractSyntaxTree functionBody, EntityType returnType) {
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

    public boolean conditionalBlockReturns(AbstractSyntaxTree conditionalBlock, EntityType returnType) {
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

    private Symbol[] paramsToSymbols(List<AbstractSyntaxTree> params, int scope) {
        Symbol[] paramSymbols = new Symbol[params.size()];
        AbstractSyntaxTree param;
        for (int i = 0; i < params.size(); i++) {
            param = params.get(i);
            paramSymbols[i] = new Symbol(param, scope);
        }
        return paramSymbols;
    }

    private EntityType[] getParamTypes(List<AbstractSyntaxTree> params) {
        EntityType[] types = new EntityType[params.size()];
        AbstractSyntaxTree param;
        for (int i = 0; i < params.size(); i++) {
            param = params.get(i);
            types[i] = new EntityType(param.getChildren().get(0));
        }
        return types;
    }

    private EntityType handleTernary(AbstractSyntaxTree node) {
        if (!evaluateType(node.getChildren().get(0)).isType(NodeType.BOOLEAN))
            throw new TypeError("Ternary expression must start with boolean expression");
        // the left and right must be compatible types
        EntityType leftType = evaluateType(node.getChildren().get(1));
        EntityType rightType = evaluateType(node.getChildren().get(2));
        if (leftType.equals(rightType))
            return leftType;
        if (leftType.isType(NodeType.NULL) || rightType.isType(NodeType.NULL))
            return leftType.isType(NodeType.NULL) ? rightType : leftType;
        throw new TypeError("Ternary expression cannot evaluate to different types");
    }

    public EntityType estimateArrayTypes(AbstractSyntaxTree node) {
        // empty array literal
        if (!node.hasChildren())
            return new EntityType(NodeType.ARRAY);

        List<AbstractSyntaxTree> children = node.getChildren();
        // non-nested array literal
        if (!isArrayLiteral(children.get(0))) {
            EntityType type = evaluateType(children.get(0));
            EntityType currentType = new EntityType(NodeType.NONE);
            for (int i = 1; i < children.size(); i++) {
                if (isArrayLiteral(children.get(i)))
                    throw new TypeError("Cannot mix non-array elements with nested array elements in array literal", node.getLineNumber());
                currentType = evaluateType(children.get(i));
                if (type.isType(NodeType.NULL) && !currentType.isType(NodeType.NULL))
                    type = currentType;
                else if (!currentType.isType(NodeType.NULL) && !type.equals(currentType))
                    throw new TypeError("Cannot mix " + type + " elements with " + currentType + " elements in array literal", node.getLineNumber());
            }
            return currentType.isType(NodeType.NULL) ? new EntityType(NodeType.ARRAY) : new EntityType(NodeType.ARRAY, type);
        }
        // nested array literal
        EntityType types;
        if (children.get(0).hasChildren())
            types = estimateArrayTypes(children.get(0));
        else {
            // find first element with children
            int i = 1;
            while (i < children.size() && !children.get(i).hasChildren()) {
                i++;
            }
            types = i < children.size() ? estimateArrayTypes(children.get(i)) : new EntityType(NodeType.ARRAY);
        }
        EntityType currentTypes;
        for (AbstractSyntaxTree child : children) {
            currentTypes = estimateArrayTypes(child);
            if (!types.containsSubType(currentTypes)) {
                throw new TypeError("Cannot mix types of arrays", node.getLineNumber());
            }
        }
        return new EntityType(NodeType.ARRAY, types);
    }

    public void validateAssignment(AbstractSyntaxTree assignmentNode) {
        EntityType varType;
        if (assignmentNode.getChildren().get(0).hasChildren()) {
            varType = handleArrayIndex(assignmentNode.getChildren().get(0));
        }
        else {
            String varName = assignmentNode.getChildren().get(0).getValue();
            Symbol symbol = symbolTable.lookup(varName);
            if (symbol == null)
                throw new ReferenceError("Variable " + varName + " is used before being defined in current scope", assignmentNode.getLineNumber());
            varType = symbol.getType();
        }
        AbstractSyntaxTree rightHandSide = assignmentNode.getChildren().get(1);
        EntityType rhsType = evaluateType(rightHandSide);
        switch (assignmentNode.getValue()) {
            case "=":
                if (!rhsType.isType(NodeType.NULL) && !rhsType.equals(varType))
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

    public EntityType evaluateType(AbstractSyntaxTree node) {
        if (node.getName() == TokenType.ID) {
            if (!node.hasChildren()) {
                Symbol symbol = symbolTable.lookup(node.getValue());
                if (symbol == null)
                    throw new ReferenceError("Variable '" + node.getValue() + "' used before being defined in current scope", node.getLineNumber());
                return symbol.getType();
            }
            else
                return handleArrayIndex(node);
        }
        if (isIntegerLiteral(node))
            return new EntityType(NodeType.INT);
        if (isFloatLiteral(node))
            return new EntityType(NodeType.FLOAT);
        if (isStringLiteral(node))
            return new EntityType(NodeType.STRING);
        if (isBooleanLiteral(node))
            return new EntityType(NodeType.BOOLEAN);
        if (node.getName() == TokenType.KW_NULL)
            return new EntityType(NodeType.NULL);
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
        if (node.getLabel().equals("UNARY-OP"))
            return handleUnaryOp(node);
        if (node.getLabel().equals("FUNC-CALL")) {
            List<AbstractSyntaxTree> children = node.getChildren();
            String name = children.get(0).getValue();
            FunctionSymbol matchingDefinition;
            EntityType[] types = {};
            if (children.size() != 1) {
                List<AbstractSyntaxTree> funcParams = children.get(1).getChildren();
                types = new EntityType[funcParams.size()];
                for (int i = 0; i < funcParams.size(); i++) {
                    types[i] = evaluateType(funcParams.get(i));
                }
            }
            matchingDefinition = symbolTable.lookup(name, types);
            if (matchingDefinition == null)
                throw new ReferenceError("Could not find function definition for " + name + "(" + Arrays.toString(types) + ")", children.get(0).getLineNumber());
            return matchingDefinition.getReturnType();
        }
        if (node.getLabel().equals("ARRAY-LIT"))
            return estimateArrayTypes(node);
        if (node.getLabel().equals("TERNARY"))
            return handleTernary(node);
        return new EntityType(NodeType.NONE);
    }

    private EntityType handleComparison(AbstractSyntaxTree rootNode) {
        String comparisonOperator = rootNode.getValue();
        EntityType leftType = evaluateType(rootNode.getChildren().get(0));
        EntityType rightType = evaluateType(rootNode.getChildren().get(1));
        Set<EntityType> acceptedTypes = Set.of(new EntityType(NodeType.INT), new EntityType(NodeType.FLOAT));
        if (!(acceptedTypes.contains(leftType) && acceptedTypes.contains(rightType)))
            throw new TypeError("Cannot compare " + leftType + " with " + rightType + " using " + comparisonOperator, rootNode.getLineNumber());
        return new EntityType(NodeType.BOOLEAN);
    }

    private EntityType handleBitwise(AbstractSyntaxTree rootNode) {
        String comparisonOperator = rootNode.getValue();
        EntityType leftType = evaluateType(rootNode.getChildren().get(0));
        EntityType rightType = evaluateType(rootNode.getChildren().get(1));
        Set<EntityType> acceptedTypes = Set.of(new EntityType(NodeType.INT), new EntityType(NodeType.BOOLEAN));
        if (!(acceptedTypes.contains(leftType) && acceptedTypes.contains(rightType)))
            throw new TypeError("Binary expression (" + comparisonOperator + ") with " + leftType + " and " + rightType + " is not valid", rootNode.getLineNumber());
        return new EntityType(NodeType.INT);
    }

    private EntityType handleMultiplication(AbstractSyntaxTree rootNode) {
        EntityType leftType = evaluateType(rootNode.getChildren().get(0));
        EntityType rightType = evaluateType(rootNode.getChildren().get(1));
        if (leftType.isType(NodeType.INT)) {
            if (rightType.isType(NodeType.INT))
                return new EntityType(NodeType.INT);
            if (rightType.isType(NodeType.FLOAT))
                return new EntityType(NodeType.FLOAT);
            if (rightType.isType(NodeType.STRING))
                return new EntityType(NodeType.STRING);
        }
        else if (leftType.isType(NodeType.FLOAT)) {
            if (rightType.isType(NodeType.FLOAT) || rightType.isType(NodeType.INT))
                return new EntityType(NodeType.FLOAT);
        }
        else if (leftType.isType(NodeType.STRING)) {
            if (rightType.isType(NodeType.INT))
                return new EntityType(NodeType.STRING);
        }
        throw new TypeError("Cannot multiply " + leftType + " with " + rightType, rootNode.getLineNumber());
    }

    private EntityType handleArithmetic(AbstractSyntaxTree rootNode) {
        String operator = rootNode.getValue();
        EntityType leftType = evaluateType(rootNode.getChildren().get(0));
        EntityType rightType = evaluateType(rootNode.getChildren().get(1));
        if (leftType.isType(NodeType.INT)) {
            if (rightType.isType(NodeType.INT))
                return new EntityType(NodeType.INT);
            if (rightType.isType(NodeType.FLOAT))
                return new EntityType(NodeType.FLOAT);
        }
        else if (leftType.isType(NodeType.FLOAT)) {
            if (rightType.isType(NodeType.FLOAT) || rightType.isType(NodeType.INT))
                return new EntityType(NodeType.FLOAT);
        }
        throw new TypeError("Arithmetic expression (" + operator + ") with " + leftType + " and " + rightType + " is not valid", rootNode.getLineNumber());
    }

    private EntityType handleAddition(AbstractSyntaxTree rootNode) {
        EntityType leftType = evaluateType(rootNode.getChildren().get(0));
        EntityType rightType = evaluateType(rootNode.getChildren().get(1));
        if (leftType.isType(NodeType.INT)) {
            if (rightType.isType(NodeType.INT))
                return new EntityType(NodeType.INT);
            if (rightType.isType(NodeType.FLOAT))
                return new EntityType(NodeType.FLOAT);
        }
        else if (leftType.isType(NodeType.FLOAT)) {
            if (rightType.isType(NodeType.FLOAT) || rightType.isType(NodeType.INT))
                return new EntityType(NodeType.FLOAT);
        }
        else if (leftType.isType(NodeType.STRING) && rightType.isType(NodeType.STRING))
            return new EntityType(NodeType.STRING);
        else if (leftType.startsWith(NodeType.ARRAY) && rightType.startsWith(NodeType.ARRAY)) {
            if (leftType.equals(rightType))
                return leftType;
            if (leftType.containsSubType(rightType))
                return leftType;
            if (rightType.containsSubType(leftType))
                return rightType;
        }
        throw new TypeError("Cannot add " + leftType + " with " + rightType, rootNode.getLineNumber());
    }

    private EntityType handleEqualityComparison(AbstractSyntaxTree rootNode) {
        EntityType leftType = evaluateType(rootNode.getChildren().get(0));
        EntityType rightType = evaluateType(rootNode.getChildren().get(1));
        if (
            leftType.equals(rightType)
            || (leftType.isType(NodeType.NULL) || rightType.isType(NodeType.NULL))
            || (
                (leftType.isType(NodeType.INT) && rightType.isType(NodeType.FLOAT))
                || (leftType.isType(NodeType.FLOAT) && rightType.isType(NodeType.INT))
            )
            || (leftType.startsWith(NodeType.ARRAY) && rightType.startsWith(NodeType.ARRAY))
        )
            return new EntityType(NodeType.BOOLEAN);
        throw new TypeError("Cannot check for equality between " + leftType + " and " + rightType, rootNode.getLineNumber());
    }

    private EntityType handleUnaryOp(AbstractSyntaxTree rootNode) {
        AbstractSyntaxTree left = rootNode.getChildren().get(0);
        AbstractSyntaxTree right = rootNode.getChildren().get(1);
        EntityType leftType = evaluateType(left);
        EntityType rightType = evaluateType(right);
        if (left.getValue().equals("!") && rightType.isType(NodeType.BOOLEAN))
            return new EntityType(NodeType.BOOLEAN);
        if (left.getValue().equals("-") && (rightType.isType(NodeType.INT) || rightType.isType(NodeType.FLOAT)))
            return rightType;
        if (left.getValue().equals("++") || left.getValue().equals("--")) {
            if (right.getName() != TokenType.ID)
                throw new IllegalStatementError("Can only increment/decrement non-variables using operator " + left.getValue(), left.getLineNumber());
            if (symbolTable.lookup(right.getValue()).getValueNodes() == null)
                throw new IllegalStatementError("Can only increment/decrement initialized variables using operator " + left.getValue(), left.getLineNumber());
            if (rightType.isType(NodeType.INT) || rightType.isType(NodeType.FLOAT)) {
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
            if (leftType.isType(NodeType.INT) || leftType.isType(NodeType.FLOAT)) {
                if (left.getName() != TokenType.ID)
                    throw new ReferenceError("Cannot perform postfix expression(" + right.getValue() + ") on " + left.getName());
                return leftType;
            }
        }
        String unaryOp = leftType.isType(NodeType.NONE) ? left.getValue() : right.getValue();
        EntityType type = leftType.isType(NodeType.NONE) ? rightType : leftType;
        throw new TypeError("Cannot perform unary operator (" + unaryOp + ") on " + type);
    }

    private EntityType handleArrayIndex(AbstractSyntaxTree node) {
        Symbol symbol = symbolTable.lookup(node.getValue());
        if (symbol == null)
            throw new ReferenceError("Variable '" + node.getValue() + "' used before being defined in current scope", node.getLineNumber());
        if (!symbol.getType().startsWith(NodeType.ARRAY))
            throw new TypeError("Cannot index variable of type " + symbol.getType(), node.getLineNumber());
        int depth = 1;
        AbstractSyntaxTree current = node.getChildren().get(0);
        EntityType indexType;
        while (current.countChildren() == 2) {
            indexType = evaluateType(current.getChildren().get(0));
            if (!indexType.isType(NodeType.INT))
                throw new TypeError("Array can only be indexed using int values, not " + indexType + " values", node.getLineNumber());
            current = current.getChildren().get(1);
            depth++;
        }
        indexType = evaluateType(current.getChildren().get(0));
        if (!indexType.isType(NodeType.INT))
            throw new TypeError("Array can only be indexed using int values, not " + indexType + " values", node.getLineNumber());
        return symbol.getType().index(depth, node.getLineNumber());
    }
}
