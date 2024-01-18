package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.IllegalStatementError;
import com.piedpiper.bolt.error.ReferenceError;
import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.error.UnreachableCodeError;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.symboltable.FunctionSymbol;
import com.piedpiper.bolt.symboltable.PrototypeSymbol;
import com.piedpiper.bolt.symboltable.Symbol;
import com.piedpiper.bolt.symboltable.SymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SemanticAnalyzer {
    private final SymbolTable symbolTable = new SymbolTable();
    private final List<String> nonEqualityComparisons = List.of("<", "<=", ">", ">=");
    private final List<String> arithmeticOperators = List.of("-", "/", "%", "**");
    private final List<String> assignmentOperators = List.of("=", "+=", "-=", "*=", "/=");
    private final Set<String> translatedCalls = new HashSet<>();
    private boolean inFunc = false;
    private boolean inPrototype = false;
    // NOTE: inLoop and translatingPrototype flags cannot be part of class state because loops and translations can be nested

    public SemanticAnalyzer() {
        handleBuiltInPrototype("pop", new EntityType[]{new EntityType(NodeType.ARRAY, NodeType.GENERIC)});
    }

    private void resetState() {
        inFunc = false;
        inPrototype = false;
    }
    private void handleBuiltInPrototype(String name, EntityType[] paramTypes) {
        PrototypeSymbol prototype = symbolTable.lookupPrototype(name, paramTypes);
        int scope = symbolTable.enterScope();
        for (int i = 0; i < paramTypes.length; i++) {
            symbolTable.insert(new Symbol(prototype.getParamNames()[i], paramTypes[i], scope));
        }
        inPrototype = true;
        analyze(prototype.getFnBodyNode(), prototype.getReturnType(), false, false);
        symbolTable.leaveScope();
        resetState();
    }

    public void analyze(AbstractSyntaxTree AST) {
        analyze(AST, new EntityType(NodeType.NONE), false, false);
        FunctionSymbol mainNoParams = symbolTable.lookup("main", new EntityType[] {});
        FunctionSymbol mainWithParams = symbolTable.lookup("main", new EntityType[]{new EntityType(NodeType.INT), new EntityType(NodeType.ARRAY, NodeType.STRING)});
        if (mainNoParams == null && mainWithParams == null)
            throw new ReferenceError("Could not find entry point function 'main()' or 'main(int, Array<string>)'");
        else if (mainNoParams != null && mainWithParams != null)
            throw new ReferenceError("Multiple entry point functions 'main' found. Could not resolve proper entry point.");
        FunctionSymbol main = mainNoParams != null ? mainNoParams : mainWithParams;
        if (!(main.getReturnType() == null || main.getReturnType().isType(NodeType.NONE) || main.getReturnType().isType(NodeType.INT)))
            throw new TypeError("Entry point function 'main' must return INT or not return at all");
    }

    public void analyze(AbstractSyntaxTree AST, EntityType returnType, boolean inLoop, boolean translatingPrototype) {
        for (AbstractSyntaxTree subTree : AST.getChildren()) {
            if (!(inFunc || inPrototype || translatingPrototype)) {
                if (subTree.matchesLabel("CONTROL-FLOW") &&  isReturn(subTree))
                    throw new IllegalStatementError("Cannot return outside of a function", subTree.getLineNumber());
                else if (!(
                    subTree.matchesLabel("VAR-DECL") ||
                    subTree.matchesLabel("ARRAY-DECL") ||
                    subTree.matchesStaticToken(TokenType.KW_FN) ||
                    subTree.matchesStaticToken(TokenType.KW_PROTO)
                ))
                    throw new IllegalStatementError("Can only declare variables outside of a function definition", subTree.getLineNumber());

            }
            if (subTree.matchesLabel("VAR-DECL")) {
                handleVariableDeclaration(subTree, translatingPrototype);
            }
            else if (subTree.matchesLabel("ARRAY-DECL")) {
                handleArrayDeclaration(subTree, translatingPrototype);
            }
            // break / continue / return
            else if (subTree.matchesLabel("CONTROL-FLOW")) {
                if (!inLoop && !isReturn(subTree)) {
                    String controlType = subTree.getChildren().get(0).matchesStaticToken(TokenType.KW_BRK) ? "break" : "continue";
                    throw new IllegalStatementError("Cannot use " + controlType  + " outside of a loop", subTree.getLineNumber());
                }
            }

            // conditional
            else if (subTree.matchesLabel("COND")) {
                handleConditionalBlock(subTree.getChildren(), returnType, inLoop, translatingPrototype);
            }

            // loop
            else if (subTree.matchesStaticToken(TokenType.KW_WHILE)) {
                handleWhileLoop(subTree, returnType, translatingPrototype);
            }

            else if (subTree.matchesStaticToken(TokenType.KW_FOR)) {
                handleForLoop(subTree, returnType, translatingPrototype);
            }

            // function
            else if (subTree.matchesStaticToken(TokenType.KW_FN)) {
                handleFunctionDefinition(subTree.getChildren(), false);
            }
            // prototype
            else if (subTree.matchesStaticToken(TokenType.KW_PROTO)) {
                handleFunctionDefinition(subTree.getChildren(), true);
            }
            else {
                evaluateType(subTree);
            }
        }
    }

    private void handleVariableDeclaration(AbstractSyntaxTree node, boolean translatingPrototype) {
        int scope = symbolTable.getScopeLevel();
        List<AbstractSyntaxTree> details = node.getChildren();
        int offset = 0;
        boolean isConst = details.get(0).matchesStaticToken(TokenType.KW_CONST);
        if (isConst) {
            offset++;
            if (node.countChildren() != 4)
                throw new IllegalStatementError("Constant variable must be initialized to a variable", node.getLineNumber());
        }
        if (details.get(offset).matchesStaticToken(TokenType.KW_GEN) && !(inPrototype || translatingPrototype))
            throw new IllegalStatementError("Cannot use generic variable outside of prototype definition", node.getLineNumber());
        if (node.countChildren() == offset + 3) {
            EntityType rhsType = evaluateType(details.get(offset + 2));
            EntityType lhsType = new EntityType(details.get(offset));
            if (!lhsType.equals(rhsType) && !rhsType.isType(NodeType.NULL)) {
                if (translatingPrototype && lhsType.isType(NodeType.GENERIC)) {
                    Symbol symbol = new Symbol(details.get(offset+1).getValue(), rhsType, isConst, details.get(offset+2), scope);
                    symbolTable.insert(symbol);
                    return;
                }
                throw new TypeError("Right hand side of variable is " + rhsType + " but " + lhsType + " expected", node.getLineNumber());
            }
        }
        symbolTable.insert(new Symbol(node, scope));
    }

    private void handleArrayDeclaration(AbstractSyntaxTree node, boolean translatingPrototype) {
        int scope = symbolTable.getScopeLevel();
        List<AbstractSyntaxTree> details = node.getChildren();
        int valueIndex = 0;
        boolean isConstant = details.get(0).matchesStaticToken(TokenType.KW_CONST);
        int offset = isConstant ? 1 : 0;
        EntityType declaredType = new EntityType(details.get(offset));
        if (declaredType.containsSubType(NodeType.GENERIC) && !(inPrototype || translatingPrototype))
            throw new IllegalStatementError("Cannot declare generic array outside of function definition", node.getLineNumber());
        String name = details.get(offset + 1).getValue();
        List<Integer> sizes = List.of(0);
        switch(details.size()) {
            case 2: // Invalid: Array<type> name;
                throw new IllegalStatementError("Non-constant array '" + name + "' missing size", node.getLineNumber());
            case 3: // Valid: Array<type> name[size]; Invalid: const Array<type> name, Array<type> name = value;
                if (isConstant)
                    throw new IllegalStatementError("Constant array '" + name + "' must be set to a value", node.getLineNumber());
                if (!details.get(2).matchesLabel("ARRAY-INDEX"))
                    throw new IllegalStatementError("Non-constant array '" + name + "' missing size", node.getLineNumber());
                sizes = handleArraySizes(details.get(2));
                break;
            case 4: // Valid: Array<type> name[size] = value, const Array<type> name = value; Invalid: const Array<type> name[size];
                valueIndex = 3;
                if (isConstant) {
                    if (details.get(3).matchesLabel("ARRAY-INDEX"))
                        throw new IllegalStatementError("Constant array '" + name + "' must be set to a value", node.getLineNumber());
                    sizes = ArrayChecks.estimateArraySizes(details.get(valueIndex));
                }
                else
                    sizes = handleArraySizes(details.get(2));
                declaredType = validateArrayType(details.get(valueIndex), declaredType, translatingPrototype);
                break;
            case 5: // Valid const Array<type> name[size] = value;
                valueIndex = 4;
                sizes = handleArraySizes(details.get(valueIndex - 1));
                declaredType = validateArrayType(details.get(valueIndex), declaredType, translatingPrototype);
                break;
        }
        symbolTable.insert(new Symbol(node, sizes, declaredType, valueIndex, scope));
    }

    private EntityType validateArrayType(AbstractSyntaxTree valueNode, EntityType declaredType, boolean translatingPrototype) {
        EntityType literalType = estimateArrayTypes(valueNode);
        if (!declaredType.containsSubType(literalType)) {
            if (translatingPrototype && literalType.startsWith(NodeType.ARRAY) && declaredType.containsSubType(NodeType.GENERIC)) {
                return literalType;
            }
            throw new TypeError("Array literal type " + literalType + " does not match declared array type " + declaredType, valueNode.getLineNumber());
        }
        return declaredType;
    }

    private void handleConditionalBlock(List<AbstractSyntaxTree> conditionals, EntityType returnType, boolean inLoop, boolean translatingPrototype) {
            int length = conditionals.size();
            EntityType condition;
            AbstractSyntaxTree body;
            for (int i = 0; i < length; i++) {
                if (i == 0 && !conditionals.get(i).matchesStaticToken(TokenType.KW_IF))
                    throw new IllegalStatementError("Conditional block must begin with if", conditionals.get(i).getLineNumber());
                if (i != length - 1 && conditionals.get(i).matchesStaticToken(TokenType.KW_ELSE))
                    throw new IllegalStatementError("else can only be used at the end of conditional block", conditionals.get(i).getLineNumber());
                if (conditionals.get(i).matchesStaticToken(TokenType.KW_ELSE))
                    body = conditionals.get(i).getChildren().get(0);
                else {
                    condition = evaluateType(conditionals.get(i).getChildren().get(0));
                    if (!condition.isType(NodeType.BOOLEAN))
                        throw new TypeError("Conditional statement must evaluate to boolean but instead was " + condition, conditionals.get(i).getLineNumber());
                    body = conditionals.get(i).getChildren().get(1);
                }
                symbolTable.enterScope();
                analyze(body, returnType, inLoop, translatingPrototype);
                symbolTable.leaveScope();
            }
    }

    private void handleWhileLoop(AbstractSyntaxTree loopNode, EntityType returnType, boolean translatingPrototype) {
        // check loop signature
        EntityType condition = evaluateType(loopNode.getChildren().get(0));
        if (!condition.isType(NodeType.BOOLEAN)) {
            throw new TypeError("While loop condition must evaluate to boolean but instead was " + condition, loopNode.getLineNumber());
        }
        // analyze body
        AbstractSyntaxTree body = loopNode.getChildren().get(1);
        if (body.matchesLabel("BLOCK-BODY")) {
            loopHasControlFlow(body); // this will check for unreachable code
            symbolTable.enterScope();
            analyze(body, returnType, true, translatingPrototype);
            symbolTable.leaveScope();
        }
    }

    private void handleForLoop(AbstractSyntaxTree loopNode, EntityType returnType, boolean translatingPrototype) {
        int scope = symbolTable.enterScope();
        int lineNum = loopNode.getLineNumber();
        List<AbstractSyntaxTree> loopDetails = loopNode.getChildren();
        int length = loopDetails.size();
        AbstractSyntaxTree body = null;
        switch (length) {
            case 3: // for (float element : array) {}
                if (!loopDetails.get(0).matchesLabel("VAR-DECL"))
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
                    throw new TypeError("Cannot loop over string with type: " + loopVarType, lineNum);
                }
                symbolTable.insert(loopVar);
                body = loopDetails.get(2);
                break;
            case 4: // for (int i = 0; i < 10; i++) {}
                EntityType varType;
                if (loopDetails.get(0).matchesLabel("VAR-DECL")) {
                    Symbol symbol = new Symbol(loopDetails.get(0), scope);
                    varType = symbol.getType();
                    if (loopDetails.get(0).countChildren() != 3)
                        throw new IllegalStatementError("Loop variable must be non-constant and assigned to a value", lineNum);
                    handleVariableDeclaration(loopDetails.get(0), false);
                }
                else if (loopDetails.get(0).matchesValue("=")) {
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
                if (!varType.equals(thirdPartType))
                    throw new IllegalStatementError("End for loop expression must match type " + varType + " but was " + thirdPartType, lineNum);
                body = loopDetails.get(3);
                break;
        }
        if (body != null)
            analyze(body, returnType, true, translatingPrototype);
        symbolTable.leaveScope();
    }

    private void handleFunctionDefinition(List<AbstractSyntaxTree> fnDetails, boolean isPrototype) {
        if (isPrototype)
            inPrototype = true;
        else
            inFunc = true;
        int scope = symbolTable.enterScope();
        int lineNum = fnDetails.get(0).getLineNumber();
        int length = fnDetails.size();
        String name = fnDetails.get(0).getValue();
        EntityType fnReturnType = new EntityType(NodeType.NONE);
        AbstractSyntaxTree body = null;
        EntityType[] types = {};
        String[] paramNames = {};
        Symbol[] params = {};
        String defType = isPrototype ? "Prototype" : "Function";
        switch (length) {
            case 1:
                if (isPrototype)
                    throw new IllegalStatementError("Prototype definition must contain at least one generic parameter", lineNum);
                symbolTable.insert(new FunctionSymbol(name));
                symbolTable.leaveScope();
                return;
            case 2:
                if (fnDetails.get(1).isTypeLabel()) // has a returnType but no function body
                    throw new TypeError(
                        defType + " " + name + "expected to return " + fnDetails.get(1) + " but returns nothing",
                        lineNum
                    );
                else if (fnDetails.get(1).matchesLabel("FUNC-PARAMS")) {
                    types = getParamTypes(fnDetails.get(1).getChildren());
                    params = paramsToSymbols(fnDetails.get(1).getChildren(), scope);
                    if (isPrototype)
                        paramNames = getParamNames(fnDetails.get(1).getChildren());
                }
                else {
                    body = fnDetails.get(1);
                }
                break;
            case 3:
                if (fnDetails.get(1).matchesLabel("FUNC-PARAMS")) {
                    types = getParamTypes(fnDetails.get(1).getChildren());
                    params = paramsToSymbols(fnDetails.get(1).getChildren(), scope);
                    if (isPrototype)
                        paramNames = getParamNames(fnDetails.get(1).getChildren());
                    if (fnDetails.get(2).isTypeLabel())
                        throw new TypeError(
                            defType + " " + name + " expected to return " + fnDetails.get(1) + " but returns nothing",
                            lineNum
                        );
                    else {
                        body = fnDetails.get(2);
                    }
                }
                if (fnDetails.get(1).isTypeLabel()) {
                    fnReturnType = new EntityType(fnDetails.get(1));
                    body = fnDetails.get(2);
                }
                break;
            case 4:
                types = getParamTypes(fnDetails.get(1).getChildren());
                params = paramsToSymbols(fnDetails.get(1).getChildren(), scope);
                if (isPrototype)
                    paramNames = getParamNames(fnDetails.get(1).getChildren());
                fnReturnType = new EntityType(fnDetails.get(2));
                body = fnDetails.get(3);
                break;
        }
        for (Symbol param : params) {
            symbolTable.insert(param);
        }
        if (isPrototype) {
            PrototypeSymbol function = new PrototypeSymbol(name, fnReturnType, types, paramNames, body);
            if (!function.hasGenericParam())
                throw new IllegalStatementError("Prototype definition must contain at least one generic parameter", lineNum);
            symbolTable.insert(function);

        }
        else {
            FunctionSymbol function = new FunctionSymbol(name, fnReturnType, types, body);
            symbolTable.insert(function);
        }
        if (body != null) {
            // analyze needs to come first to get variables in scope
            // but this will mean unreachable code errors come after other errors (even if they don't in the code)
            analyze(body, fnReturnType, false, false);
            if (!fnReturnType.isType(NodeType.NONE) && !functionReturns(body, fnReturnType))
                throw new TypeError(defType + " " + name + " expected to return " + fnReturnType + " but does not return for all branches", lineNum);
        }

        symbolTable.leaveScope();
        resetState();
    }

    private List<Integer> handleArraySizes(AbstractSyntaxTree sizeNode) {
        List<Integer> sizes = new ArrayList<>();
        AbstractSyntaxTree current = sizeNode;
        while (current.hasChildren()) {
            if (!current.getChildren().get(0).isIntegerLiteral() || current.matchesValue("0"))
                throw new IllegalStatementError("Array index must be a non-zero integer literal", current.getLineNumber());
            sizes.add(Integer.valueOf(current.getChildren().get(0).getValue()));
            if (current.getChildren().size() == 2) // first node will be index value, second will be an ARRAY-INDEX node with children
                current = current.getChildren().get(1);
            else
                break;
        }
        if (sizes.isEmpty())
            throw new IllegalStatementError("Array sizes missing valid values", sizeNode.getLineNumber());
        return sizes;
    }



    private void validateReturn(AbstractSyntaxTree controlFlow, EntityType returnType) {
        // make sure return is done properly
        if (returnType.isType(NodeType.NONE)) {
            if (controlFlow.countChildren() != 1) {
                AbstractSyntaxTree returnValue = controlFlow.getChildren().get(1);
                throw new TypeError("Cannot return value from void function", returnValue.getLineNumber());
            }

        }
        else {
            if (controlFlow.countChildren() == 1)
                throw new TypeError("Expected return type " + returnType + " but didn't return a value", controlFlow.getChildren().get(0).getLineNumber());
            AbstractSyntaxTree returnValue = controlFlow.getChildren().get(1);
            EntityType actualReturnType = evaluateType(returnValue);
            if (!actualReturnType.equals(returnType) && !returnValue.matchesStaticToken(TokenType.KW_NULL))
                throw new TypeError("Expected " + returnType + " to be returned but got " + actualReturnType, returnValue.getLineNumber());
        }
    }

    public boolean functionReturns(AbstractSyntaxTree functionBody, EntityType returnType) {
        List<AbstractSyntaxTree> contents = functionBody.getChildren();
        AbstractSyntaxTree node;
        int length = contents.size();
        for (int i = 0; i < length; i++) {
            node = contents.get(i);
            if (node.matchesLabel("COND")) {
                if (conditionalBlockReturns(node, returnType)) {
                    if (i < length - 1)
                        throw new UnreachableCodeError("Unreachable statement following returning conditional block", contents.get(i+1).getLineNumber());
                    return true;
                }
            }
            if (node.matchesStaticToken(TokenType.KW_FOR) || node.matchesStaticToken(TokenType.KW_WHILE)) {
                AbstractSyntaxTree lastChild = node.getChildren().get(node.countChildren() - 1);
                if (lastChild.matchesLabel("BLOCK-BODY")) {
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
        if (conditionals.isEmpty())
            return false;
        if (!conditionals.get(conditionals.size()-1).matchesStaticToken(TokenType.KW_ELSE))
            return false;
        boolean returns = true;
        AbstractSyntaxTree bodyNode;
        for (AbstractSyntaxTree conditional : conditionals) {
            bodyNode = conditional.matchesStaticToken(TokenType.KW_ELSE) ? conditional.getChildren().get(0) : conditional.getChildren().get(1);
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
            if (node.matchesLabel("COND")) {
                if (conditionalBlockHasLoopControl(node)) {
                    if (i < length - 1)
                        throw new UnreachableCodeError("Unreachable statement following continuing/breaking conditional block", contents.get(i+1).getLineNumber());
                    return true;
                }
            }
            if (node.matchesStaticToken(TokenType.KW_WHILE) || node.matchesStaticToken(TokenType.KW_FOR)) {
                AbstractSyntaxTree bodyNode = node.getChildren().get(node.countChildren() - 1);
                if (bodyNode.matchesLabel("BLOCK-BODY"))
                    return loopHasControlFlow(bodyNode);
            }
            if (node.matchesLabel("CONTROL-FLOW")) {
                node = node.getChildren().get(0);
                if (i < length - 1) {
                    String controlType = node.matchesStaticToken(TokenType.KW_BRK) ? "break" : "continue";
                    throw new UnreachableCodeError("Unreachable statement following " + controlType, contents.get(i+1).getLineNumber());
                }
                if (node.matchesStaticToken(TokenType.KW_BRK) || node.matchesStaticToken(TokenType.KW_CNT))
                    return true;
            }
        }
        return false;
    }

    public boolean conditionalBlockHasLoopControl(AbstractSyntaxTree conditionalBlock) {
        List<AbstractSyntaxTree> conditionals = conditionalBlock.getChildren();
        if (conditionals.isEmpty())
            return false;
        if (!conditionals.get(conditionals.size() - 1).matchesStaticToken(TokenType.KW_ELSE))
            return false;
        boolean hasLoopControl = true;
        AbstractSyntaxTree bodyNode;
        for (AbstractSyntaxTree conditional : conditionals) {
            bodyNode = conditional.matchesStaticToken(TokenType.KW_ELSE) ? conditional.getChildren().get(0) : conditional.getChildren().get(1);
            hasLoopControl = hasLoopControl && loopHasControlFlow(bodyNode);
        }
        return hasLoopControl;
    }

    private boolean isReturn(AbstractSyntaxTree node) {
        if (!node.matchesLabel("CONTROL-FLOW"))
            return false;
        return node.getChildren().get(0).matchesStaticToken(TokenType.KW_RET);
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

    private String[] getParamNames(List<AbstractSyntaxTree> params) {
        String[] names = new String[params.size()];
        AbstractSyntaxTree param;
        for (int i = 0; i < params.size(); i++) {
            param = params.get(i);
            names[i] = param.getChildren().get(1).getValue();
        }
        return names;
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
        if (!node.matchesLabel("ARRAY-LIT") && !node.matchesStaticToken(TokenType.KW_NULL))
            return evaluateType(node);
        // empty array literal
        if (!node.hasChildren())
            return new EntityType(NodeType.ARRAY);

        List<AbstractSyntaxTree> children = node.getChildren();
        // non-nested array literal
        if (!children.get(0).isArrayLiteral()) {
            EntityType type = evaluateType(children.get(0));
            EntityType currentType = new EntityType(NodeType.NONE);
            for (int i = 1; i < children.size(); i++) {
                if (children.get(i).isArrayLiteral())
                    throw new TypeError("Cannot mix non-array elements with nested array elements in array literal", node.getLineNumber());
                currentType = evaluateType(children.get(i));
                if (type.isType(NodeType.NULL) && !currentType.isType(NodeType.NULL))
                    type = currentType;
                else if (currentType.isType(NodeType.FLOAT) && type.isType(NodeType.INT))
                    type = currentType;
                else if (!((currentType.isType(NodeType.INT) && type.isType(NodeType.FLOAT))) && !currentType.isType(NodeType.NULL) && !type.equals(currentType))
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
        String varName = assignmentNode.getChildren().get(0).getValue();
        if (assignmentNode.getChildren().get(0).hasChildren()) {
            varType = handleArrayIndex(assignmentNode.getChildren().get(0));
        }
        else {
            Symbol symbol = symbolTable.lookup(varName);
            if (symbol == null)
                throw new ReferenceError("Variable " + varName + " is used before being defined in current scope", assignmentNode.getLineNumber());
            varType = symbol.getType();
            if (varType.startsWith(NodeType.ARRAY)) {
                if (symbol.isConstant())
                    throw new IllegalStatementError("Cannot reassign immutable array", assignmentNode.getLineNumber());
            }
            else {
                if (symbol.isConstant())
                    throw new IllegalStatementError("Cannot reassign value of constant variable " + symbol.getName(), assignmentNode.getLineNumber());
            }
        }
        AbstractSyntaxTree rightHandSide = assignmentNode.getChildren().get(1);
        EntityType rhsType = evaluateType(rightHandSide);
        switch (assignmentNode.getValue()) {
            case "=":
                if (!rhsType.isType(NodeType.NULL) && !rhsType.equals(varType)) {
                    if (varType.isType(NodeType.GENERIC) || varType.containsSubType(NodeType.GENERIC)) {
                        Symbol concreteSymbol = new Symbol(varName, rhsType, false, rightHandSide, symbolTable.getScopeLevel());
                        symbolTable.replace(varName, concreteSymbol);
                        return;
                    }
                    throw new TypeError("Cannot assign " + rhsType + " to variable of type " + varType, assignmentNode.getLineNumber());
                }
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
        if (node.isIntegerLiteral())
            return new EntityType(NodeType.INT);
        if (node.isFloatLiteral())
            return new EntityType(NodeType.FLOAT);
        if (node.isStringLiteral())
            return new EntityType(NodeType.STRING);
        if (node.isBooleanLiteral())
            return new EntityType(NodeType.BOOLEAN);
        if (node.matchesStaticToken(TokenType.KW_NULL))
            return new EntityType(NodeType.NULL);
        if (nonEqualityComparisons.contains(node.getValue()))
            return handleComparison(node);
        if (node.matchesValue("==") || node.matchesValue("!="))
            return handleEqualityComparison(node);
        if (node.matchesValue("||") || node.matchesValue("&&")) {
            EntityType leftType = evaluateType(node.getChildren().get(0));
            EntityType rightType = evaluateType(node.getChildren().get(1));
            if (!(leftType.isType(NodeType.BOOLEAN) && rightType.isType(NodeType.BOOLEAN)))
                throw new TypeError("Both sides of logical statement must be boolean but instead got " + leftType + " " + node.getValue() + " " + rightType, node.getLineNumber());
            return new EntityType(NodeType.BOOLEAN);
        }
        if (arithmeticOperators.contains(node.getValue()))
            return handleArithmetic(node);
        if (node.matchesValue("&") || node.matchesValue("^"))
            return handleBitwise(node);
        if (node.matchesValue("*"))
            return handleMultiplication(node);
        if (node.matchesValue("+"))
            return handleAddition(node);
        if (assignmentOperators.contains(node.getValue()))
            validateAssignment(node);
        if (node.matchesLabel("UNARY-OP"))
            return handleUnaryOp(node);
        if (node.matchesLabel("FUNC-CALL")) {
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
            if (matchingDefinition == null) {
                // check if there is a matching prototype
                PrototypeSymbol prototype = symbolTable.lookupPrototype(name, types);
                if (prototype == null) {
                    System.out.println(Arrays.toString(types));
                    throw new ReferenceError("Could not find function definition for " + name + "(" + Arrays.toString(types) + ")", children.get(0).getLineNumber());
                }
                if (translatedCalls.contains(prototype.formSignature()) && prototype.returnsGeneric())
                    return new EntityType(NodeType.NULL);
                matchingDefinition = prototypeToFunction(prototype, types);
            }
            return matchingDefinition.getReturnType();
        }
        if (node.isArrayLiteral())
            return estimateArrayTypes(node);
        if (node.matchesLabel("TERNARY"))
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
        if (!rootNode.hasChildren()) // handles the minus unary operator case
            return new EntityType(NodeType.NONE);
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
        if (left.matchesValue("!") && rightType.isType(NodeType.BOOLEAN))
            return new EntityType(NodeType.BOOLEAN);
        if (left.matchesValue("-") && (rightType.isType(NodeType.INT) || rightType.isType(NodeType.FLOAT)))
            return rightType;
        if (left.matchesValue("++") || left.matchesValue("--")) {
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
        if (right.matchesValue("++") || right.matchesValue("--")) {
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
        while (current.countChildren() == 2) { // While the current node has children (for nested array indexing)
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

    private FunctionSymbol prototypeToFunction(PrototypeSymbol prototypeSymbol, EntityType[] calledParams) {
        FunctionSymbol fnDefinition = new FunctionSymbol(prototypeSymbol.getName(), calledParams, prototypeSymbol.getBuiltIn());
        // transform body
        int scope = symbolTable.enterScope();
        for (int i = 0; i < calledParams.length; i++) {
            symbolTable.insert(new Symbol(prototypeSymbol.getParamNames()[i], calledParams[i], scope));
        }
        if (!prototypeSymbol.isBuiltIn() || (prototypeSymbol.isBuiltIn() && prototypeSymbol.getFnBodyNode() != null))
            analyze(prototypeSymbol.getFnBodyNode(), prototypeSymbol.getReturnType(), false, true);
        fnDefinition.setFnBodyNode(prototypeSymbol.getFnBodyNode());
        if (prototypeSymbol.getReturnType() != null && prototypeSymbol.returnsGeneric()) {
            fnDefinition.setReturnType(estimateReturnType(prototypeSymbol, calledParams));
        }
        else {
            fnDefinition.setReturnType(prototypeSymbol.getReturnType());
        }
        symbolTable.leaveScope();
        symbolTable.insert(fnDefinition, true);
        return fnDefinition;
    }

    private EntityType estimateReturnType(PrototypeSymbol prototype, EntityType[] calledParams) {
        String signature = prototype.formSignature();
        translatedCalls.add(signature);
        Set<EntityType> returnTypes = new HashSet<>();
        EntityType nullType = new EntityType(NodeType.NULL);
        for (AbstractSyntaxTree child : prototype.getFnBodyNode().getChildren()) {
            if (child.matchesLabel("COND") || (child.getName() != null && (child.matchesStaticToken(TokenType.KW_FOR) || child.matchesStaticToken(TokenType.KW_WHILE))))
                returnTypes.addAll(getReturnTypesFromBlocks(child));
            if (child.matchesLabel("CONTROL-FLOW") && child.getChildren().size() == 2 && child.getChildren().get(0).matchesStaticToken(TokenType.KW_RET))
                returnTypes.add(evaluateType(child.getChildren().get(1)));
        }
        if (returnTypes.size() > 1) {
            if (returnTypes.contains(nullType)) {
                returnTypes.remove(nullType);
                if (returnTypes.size() == 1) {
                    translatedCalls.remove(signature);
                    return returnTypes.iterator().next();
                }
            }
            throw new TypeError("Prototype " + prototype.getName() + " returns more than one type with parameter types " + Arrays.toString(calledParams));
        }
        translatedCalls.remove(signature);
        return returnTypes.iterator().hasNext() ? returnTypes.iterator().next() : new EntityType(NodeType.NONE);
    }

    private Set<EntityType> getReturnTypesFromBlocks(AbstractSyntaxTree block) {
        Set<EntityType> returnTypes = new HashSet<>();
        if (block.getName() != null && (block.matchesStaticToken(TokenType.KW_FOR) || block.matchesStaticToken(TokenType.KW_WHILE))) {
            AbstractSyntaxTree lastChild = block.getChildren().get(block.countChildren() - 1);
            if (lastChild.matchesLabel("BLOCK-BODY")) {
                for (AbstractSyntaxTree child : lastChild.getChildren()) {
                    if (isReturn(child) && child.getChildren().size() == 2)
                        returnTypes.add(evaluateType(child.getChildren().get(1)));
                    if (child.matchesLabel("COND") || (child.getName() != null && (child.matchesStaticToken(TokenType.KW_FOR) || child.matchesStaticToken(TokenType.KW_WHILE))))
                        returnTypes.addAll(getReturnTypesFromBlocks(child));
                }
            }
        }
        if (block.matchesLabel("COND")) {
            for (AbstractSyntaxTree child : block.getChildren()) {
                AbstractSyntaxTree lastChild = child.getChildren().get(child.countChildren() - 1);
                if (lastChild.matchesLabel("BLOCK-BODY")) {
                    for (AbstractSyntaxTree body : lastChild.getChildren()) {
                        if (body.matchesLabel("CONTROL-FLOW") && body.getChildren().size() == 2 && body.getChildren().get(0).matchesStaticToken(TokenType.KW_RET))
                            returnTypes.add(evaluateType(body.getChildren().get(1)));
                        if (child.matchesLabel("COND") || (child.getName() != null && (child.matchesStaticToken(TokenType.KW_FOR) || child.matchesStaticToken(TokenType.KW_WHILE))))
                            returnTypes.addAll(getReturnTypesFromBlocks(body));
                    }
                }
            }
        }
        return returnTypes;
    }
}
