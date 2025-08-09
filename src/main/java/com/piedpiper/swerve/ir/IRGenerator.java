package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;
import com.piedpiper.swerve.semantic.EntityType;
import com.piedpiper.swerve.semantic.NodeType;
import com.piedpiper.swerve.symboltable.Symbol;
import com.piedpiper.swerve.symboltable.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.piedpiper.swerve.Compiler.assignmentOperators;
import static java.util.Map.entry;

public class IRGenerator {
    private final SymbolTable symbolTable;
    private final Map<String, IROpcode> binaryOperators = Map.ofEntries(
        entry("+", IROpcode.ADD),
        entry("-", IROpcode.SUB),
        entry("*", IROpcode.MULTIPLY),
        entry("/", IROpcode.DIVIDE),
        entry("%", IROpcode.REM),
        entry("**", IROpcode.POW),
        entry("^", IROpcode.XOR),
        entry("&", IROpcode.BINARY_AND),
        entry("|", IROpcode.BINARY_OR),
        entry("<", IROpcode.LESS_THAN),
        entry("<=", IROpcode.LESS_EQUAL),
        entry(">", IROpcode.GREATER_THAN),
        entry(">=", IROpcode.GREATER_EQUAL),
        entry("==", IROpcode.EQUAL),
        entry("!=", IROpcode.NOT_EQUAL),
        entry("&&", IROpcode.AND),
        entry("||", IROpcode.OR)
    );
    private final List<FunctionBlock> functions;
    private boolean inFunction = false;
    private int tempVarIndex = 1;
    private int ifIndex = 0;
    private int elseIfIndex = 0;
    private int loopIndex = 0;
    private int arrayFillIndex = 0;
    private final String elseLabel = ".else-start-%d";
    private final String loopStart = ".loop-start-%d";
    private final String loopEnd = ".loop-end-%d";

    public IRGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.functions = new ArrayList<>(List.of(new FunctionBlock("_entry")));
    }

    // NOTE: Since syntactic and semantic analysis have passed, can assume all inputs are correct programs
    public List<FunctionBlock> generateIR(AbstractSyntaxTree AST) {
        List<Instruction> mainCall = List.of(
            Instruction.builder()
                .result("temp-0")
                .operand1("main")
                .operator(IROpcode.CALL)
                .operand2("0")
                .build(),
            Instruction.builder()
                .operand1("temp-0")
                .operator(IROpcode.RETURN)
                .build()
        );
        final FunctionBlock entryFunction = functions.get(0);
        List<AbstractSyntaxTree> children;
        for (AbstractSyntaxTree node : AST.getChildren()) {
            children = node.getChildren();
            if (node.matchesStaticToken(TokenType.KW_FN)) {
                functions.add(generateFunction(node));
            }
            if (node.matchesLabel("VAR-DECL")) {
                entryFunction.addMultipleInstructions(generateVarDeclaration(children));
                int index = entryFunction.getInstructions().size() - 1;
                entryFunction.getInstructions().get(index).setGlobal(true);
            }
        }
        // have the _entry function call main and return main's return code as the program result
        entryFunction.addMultipleInstructions(mainCall);
        return functions;
    }

    private FunctionBlock generateFunction(AbstractSyntaxTree AST) {
        List<AbstractSyntaxTree> children = AST.getChildren();
        FunctionBlock functionBlock = new FunctionBlock(children.get(0).getValue());
        boolean hasReturnType = false;
        AbstractSyntaxTree functionBody = null;
        List<AbstractSyntaxTree> params;
        for (int i = 1; i < children.size(); i++) {
            if (children.get(i).matchesLabel("FUNC-PARAMS")) {
                params = children.get(i).getChildren();
                for (AbstractSyntaxTree param : params) {
                    functionBlock.addParam(param.getChildren().get(1).getValue());
                }
            }
            else if (children.get(i).matchesLabel("BLOCK-BODY")) {
                functionBody = children.get(i);
                inFunction = true;
            }
            else {
                hasReturnType = true;
            }
        }
        if (functionBody != null) {
            functionBlock.addMultipleInstructions(generateBlockBody(functionBody.getChildren()));
        }
        if (functionBlock.getName().equals("main") && !hasReturnType) {
            // add return 0 to the end of the main function body if it doesn't have return
            functionBlock.addInstruction(
                Instruction.builder()
                    .operand1("0")
                    .operator(IROpcode.RETURN)
                    .build()
            );
        }
        inFunction = false;
        return functionBlock;
    }

    private List<Instruction> generateBlockBody(List<AbstractSyntaxTree> body) {
        return generateBlockBody(body, null);
    }

    private List<Instruction> generateBlockBody(List<AbstractSyntaxTree> body, String label) {
        List<Instruction> instructions = new ArrayList<>();
        for (AbstractSyntaxTree node : body) {
            instructions.addAll(generate(node));
        }
        if (label != null)
            instructions.get(0).setLabel(label);
        return instructions;
    }

    // use this to decide where what subexpressions call
    private List<Instruction> generate(AbstractSyntaxTree AST) {
        if (AST.getHeight() == 1) {
            if (AST.getName() == TokenType.STRING || AST.getName() == TokenType.NUMBER || AST.getName() == TokenType.ID)
                return List.of(
                    Instruction.builder()
                        .operand1(AST.getValue())
                        .build()
                );
            if (AST.getName() == TokenType.KW_TRUE)
                return List.of(
                    Instruction.builder()
                        .operand1("true")
                        .build()
                );
            if (AST.getName() == TokenType.KW_FALSE)
                return List.of(
                    Instruction.builder()
                        .operand1("false")
                        .build()
                );
        }
        List<Instruction> instructions = new ArrayList<>();
        if (binaryOperators.containsKey(AST.getValue())) {
            instructions.addAll(generateBinaryExpression(AST, null));
        }
        if (AST.matchesLabel("UNARY-OP")) {
            if (AST.getChildren().get(0).matchesValue("++") || AST.getChildren().get(0).matchesValue("--")) {
                instructions.add(generatePreIncrementOrDecrement(AST.getChildren()));
            }
            else if (isPostOrderUnaryExpression(AST)) {
                instructions.addAll(generatePostIncrementOrDecrement(AST.getChildren()));
            }
            else
                instructions.addAll(generateUnaryExpression(AST));
        }
        if (assignmentOperators.contains(AST.getValue()))
            instructions.addAll(generateVarAssign(AST));
        if (AST.matchesLabel("VAR-DECL"))
            instructions.addAll(generateVarDeclaration(AST.getChildren()));
        if (AST.matchesLabel("ARRAY-DECL"))
            instructions.addAll(generateArrayDeclaration(AST.getChildren()));
        if (AST.matchesLabel("FUNC-CALL"))
            instructions.addAll(generateFunctionCall(AST.getChildren()));
        if (AST.matchesLabel("COND"))
            instructions.addAll(generateConditionals(AST.getChildren()));
        if (AST.matchesStaticToken(TokenType.KW_WHILE))
            instructions.addAll(generateWhileLoop(AST.getChildren()));
        if (AST.matchesStaticToken(TokenType.KW_FOR))
            instructions.addAll(generateForLoop(AST.getChildren()));
        if (AST.getName() == TokenType.ID && AST.getChildren().get(0).matchesLabel("ARRAY-INDEX"))
            instructions.addAll(generateArrayIndex(AST.getValue(), AST.getChildren().get(0).getChildren()));
        if (AST.matchesLabel("CONTROL-FLOW")) {
            if (AST.getChildren().get(0).matchesStaticToken(TokenType.KW_RET)) {
                instructions.addAll(generateReturn(AST.getChildren()));
            }
        }
        return instructions;
    }

    private List<Instruction> generateReturn(List<AbstractSyntaxTree> returnStatement) {
        List<Instruction> instructions = new ArrayList<>();
        String var_name = "null";
        if (returnStatement.size() > 1) {
            if (returnStatement.get(1).getHeight() > 1) {
                var_name = generateTempVar();
                
                instructions.addAll(generate(returnStatement.get(1)));
                getLastInstruction(instructions).setResult(var_name);
            }
            else
                var_name = returnStatement.get(1).getValue();
        }
        instructions.add(
            Instruction.builder()
                .operand1(var_name)
                .operator(IROpcode.RETURN)
                .build()
        );
        return instructions;
    }

    private List<Instruction> generateWhileLoop(List<AbstractSyntaxTree> loopDetails) {
        AbstractSyntaxTree condition = loopDetails.get(0);
        List<AbstractSyntaxTree> body = loopDetails.get(1).getChildren();
        List<Instruction> instructions = new ArrayList<>();
        String start = String.format(loopStart, loopIndex);
        String end = String.format(loopEnd, loopIndex);
        String temp = condition.getValue();
        instructions.add(
            Instruction.builder()
                .operand1(start)
                .operator(IROpcode.JMP)
                .build()
        );
        instructions.addAll(generate(condition));
        if (condition.getHeight() > 1) {
            temp = generateTempVar();
            getLastInstruction(instructions).setResult(temp);
        }
        instructions.get(1).setLabel(start);
        instructions.add(
            Instruction.builder()
                .operand1(temp)
                .operator(IROpcode.JMPF)
                .operand2(end)
                .build()
        );
        instructions.addAll(generateBlockBody(body));
        instructions.add(
            Instruction.builder()
                .label(end)
                .operator(IROpcode.NO_OP)
                .build()
        );
        loopIndex++;
        return instructions;
    }

    private List<Instruction> generateForEachLoop(List<AbstractSyntaxTree> loopDetails, String start, String end) {
        String temp;
        List<AbstractSyntaxTree> body;
        String container = loopDetails.get(1).getValue();
        Symbol symbol = symbolTable.lookup(container);
        String iterator = generateTempVar();
        String loopVar = loopDetails.get(0).getChildren().get(loopDetails.get(0).matchesLabel("VAR-ASSIGN") ? 0 : 1).getValue();
        String length = container + "-length";
        temp = generateTempVar();
        List<Instruction> instructions = new ArrayList<>(List.of(
            Instruction.builder()
                .operator(IROpcode.PARAM)
                .operand1(container)
                .build(),
            Instruction.builder()
                .result(length)
                .operand1("length")
                .operator(IROpcode.CALL)
                .operand2("1")
                .build(),
            Instruction.builder()
                .result(iterator)
                .operand1("0")
                .build(),
            Instruction.builder()
                .result(temp)
                .operand1(iterator)
                .operator(IROpcode.LESS_THAN)
                .operand2(length)
                .label(start)
                .build(),
            Instruction.builder()
                .operand1(temp)
                .operator(IROpcode.JMPF)
                .operand2(end)
                .build()
        ));
        if (symbol.getType().isType(NodeType.STRING)) {
            instructions.addAll(List.of(
                Instruction.builder()
                    .operator(IROpcode.PARAM)
                    .operand1(container)
                    .build(),
                Instruction.builder()
                    .operator(IROpcode.PARAM)
                    .operand1(iterator)
                    .build(),
                Instruction.builder()
                    .result(loopVar)
                    .operand1("at")
                    .operator(IROpcode.CALL)
                    .operand2("2")
                    .build()
            ));
        }
        else {
            String addr = generateTempVar();

            instructions.addAll(List.of(
                Instruction.builder()
                    .result(addr)
                    .operand1(container)
                    .operator(IROpcode.OFFSET)
                    .operand2(iterator)
                    .build(),
                Instruction.builder()
                    .result(loopVar)
                    .operand1(dereferenceArrayPointer(addr))
                    .build()
            ));
        }
        body = loopDetails.get(2).getChildren();
        instructions.addAll(generateBlockBody(body));
        instructions.addAll(List.of(
            Instruction.builder()
                .result(iterator)
                .operand1(iterator)
                .operator(IROpcode.ADD)
                .operand2("1")
                .build(),
            Instruction.builder()
                .operand1(start)
                .operator(IROpcode.JMP)
                .build()
        ));
        return instructions;
    }

    private List<Instruction> generateForLoop(List<AbstractSyntaxTree> loopDetails) {
        String start = String.format(loopStart, loopIndex);
        String end = String.format(loopEnd, loopIndex);
        List<Instruction> instructions = new ArrayList<>(List.of(
            Instruction.builder()
                .operand1(start)
                .operator(IROpcode.JMP)
                .build()
        ));
        if (loopDetails.size() == 3)
            instructions.addAll(generateForEachLoop(loopDetails, start, end));
        else {
            AbstractSyntaxTree condition;
            List<AbstractSyntaxTree> body;
            String temp = null;
            // two types of loops: regular (4) and foreach (3)
            instructions.addAll(generate(loopDetails.get(0)));
            condition = loopDetails.get(1);
            body = loopDetails.get(3).getChildren();
            instructions.addAll(generate(condition));
            if (condition.getHeight() > 1) {
                temp = generateTempVar();
                getLastInstruction(instructions).setResult(temp);
            }
            instructions.get(1).setLabel(start);
            instructions.add(
                Instruction.builder()
                    .operand1(temp)
                    .operator(IROpcode.JMPF)
                    .operand2(end)
                    .build()
            );
            instructions.addAll(generateBlockBody(body));
            instructions.addAll(generate(loopDetails.get(2)));
            instructions.add(
                Instruction.builder()
                    .operand1(loopStart)
                    .operator(IROpcode.JMP)
                    .build()
            );
        }
        instructions.add(
            Instruction.builder()
                .label(end)
                .operator(IROpcode.NO_OP)
                .build()
        );
        loopIndex++;
        return instructions;
    }

    private List<Instruction> generateFunctionCall(List<AbstractSyntaxTree> callDetails) {
        List<Instruction> instructions = new ArrayList<>();
        String operand;
        // TODO: handle built-ins and overriding functions
        if (callDetails.size() == 1) {
            return List.of(
                Instruction.builder()
                    .operand1(callDetails.get(0).getValue())
                    .operator(IROpcode.CALL)
                    .operand2("0")
                    .build()
            );
        }
        List<AbstractSyntaxTree> params = callDetails.get(1).getChildren();
        for (AbstractSyntaxTree param : params) {
            operand = param.getValue();
            if (param.getHeight() > 1) {
                operand = generateTempVar();
                instructions.addAll(generate(param));
                getLastInstruction(instructions).setResult(operand);
            }
            instructions.add(
                Instruction.builder()
                    .operand1(operand)
                    .operator(IROpcode.PARAM)
                    .build()
            );
        }
        instructions.add(
            Instruction.builder()
                .operand1(callDetails.get(0).getValue())
                .operator(IROpcode.CALL)
                .operand2(String.valueOf(params.size()))
                .build()
        );
        return instructions;
    }

    private List<Instruction> generateConditionals(List<AbstractSyntaxTree> conditionals) {
        List<Instruction> instructions = new ArrayList<>();
        List<List<Instruction>> blockInstructions = new ArrayList<>();
        String temp, label;
        String endLabel = String.format(elseLabel, ifIndex);
        AbstractSyntaxTree boolExpr;
        boolean hasElse = false;
        for (AbstractSyntaxTree condition : conditionals) {
            label = generateConditionalLabel(condition);
            if (!condition.matchesStaticToken(TokenType.KW_ELSE)) {
                boolExpr = condition.getChildren().get(0);
                instructions.addAll(generate(boolExpr));
                if (boolExpr.getHeight() > 1) {
                    temp = generateTempVar();
                    getLastInstruction(instructions).setResult(temp);
                    instructions.add(
                        Instruction.builder()
                            .operand1(temp)
                            .operator(IROpcode.JMPT)
                            .operand2(label)
                            .build()
                    );
                }
                else {
                    instructions.add(
                        Instruction.builder()
                            .operand1(boolExpr.getValue())
                            .operator(IROpcode.JMPT)
                            .operand2(label)
                            .build()
                    );
                }
            }
            else {
                hasElse = true;
                instructions.add(
                    Instruction.builder()
                        .operand1(endLabel)
                        .operator(IROpcode.JMP)
                        .build()
                );
            }
            blockInstructions.add(generateBlockBody(condition.getChildren().get(1).getChildren(), generateConditionalLabel(condition)));
            if (!hasElse) {
                instructions.add(
                    Instruction.builder()
                        .operand1(endLabel)
                        .operator(IROpcode.JMP)
                        .build()
                );
                blockInstructions.add(List.of(
                    Instruction.builder()
                        .label(endLabel)
                        .operator(IROpcode.NO_OP)
                        .build()
                ));
            }
            for (List<Instruction> block : blockInstructions) {
                instructions.addAll(block);
            }
        }
        ifIndex++;
        return instructions;
    }

    public List<Instruction> generateVarDeclaration(List<AbstractSyntaxTree> nodes) {
        List<Instruction> instructions = new ArrayList<>();
        int length = nodes.size();
        String value;
        switch (length) {
            case 2:
                value = getDefaultValue(nodes.get(0));
                instructions.add(
                    Instruction.builder()
                        .result(nodes.get(1).getValue())
                        .operand1(value)
                        .build()
                );
                break;
            case 3:
                instructions.addAll(generate(nodes.get(length - 1)));
                getLastInstruction(instructions).setResult(nodes.get(1).getValue());
                break;
            case 4:
                instructions.addAll(generate(nodes.get(length - 1)));
                getLastInstruction(instructions).setResult(nodes.get(2).getValue());
                break;
        }
        return instructions;
    }

    private List<Instruction> generateVarAssign(AbstractSyntaxTree AST) {
        List<Instruction> instructions = new ArrayList<>();
        AbstractSyntaxTree idNode = AST.getChildren().get(0);
        String name = idNode.getValue();
        if (idNode.hasChildren() && idNode.getChildren().get(0).matchesLabel("ARRAY-INDEX")) {
            List<Instruction> arrayIndex = generateArrayIndex(name, idNode.getChildren().get(0).getChildren());
            name = arrayIndex.get(arrayIndex.size() - 1).getResult();
            instructions.addAll(arrayIndex);
        }
        instructions.addAll(generate(AST.getChildren().get(1)));
        if (AST.matchesValue("=")) {
            getLastInstruction(instructions).setResult(name);
            return instructions;
        }
        IROpcode operator;
        String temp = generateTempVar();
        getLastInstruction(instructions).setResult(temp);
        if (AST.matchesValue("+="))
            operator = IROpcode.ADD;
        else if (AST.matchesValue("-="))
            operator = IROpcode.SUB;
        else if (AST.matchesValue("*="))
            operator = IROpcode.MULTIPLY;
        else
            operator = IROpcode.DIVIDE;
        instructions.add(
            Instruction.builder()
                .result(name)
                .operand1(name)
                .operator(operator)
                .operand2(temp)
                .build()
        );

        return instructions;
    }

    /**
     * Use for arithmetic, comparisons, and logical and/or expressions
     *
     * @param AST The Abstract syntax tree node
     * @param result The variable name of the result
     * @return list of instructions
     */
    public List<Instruction> generateBinaryExpression(AbstractSyntaxTree AST, String result) {
        List<Instruction> instructions = new ArrayList<>();
        boolean useTempVar = AST.getHeight() > 2;
        String operand1, operand2;
        AbstractSyntaxTree left = AST.getChildren().get(0);
        AbstractSyntaxTree right = AST.getChildren().get(1);
        String temp;
        if (useTempVar) {
            if (left.getHeight() == 1)
                operand1 = generate(left).get(0).getOperand1();
            else {
                List<Instruction> leftInstructions = generate(left);
                if (isPostOrderUnaryExpression(left)) {
                    temp = leftInstructions.get(0).getResult();
                }
                else {
                    temp = generateTempVar();
                    leftInstructions.get(leftInstructions.size() - 1).setResult(temp);
                }
                operand1 = temp;
                instructions.addAll(leftInstructions);
            }
            if (right.getHeight() == 1)
                operand2 = generate(right).get(0).getOperand1();
            else {
                List<Instruction> rightInstructions = generate(right);
                if (isPostOrderUnaryExpression(right)) {
                    temp = rightInstructions.get(0).getResult();
                }
                else {
                    temp = generateTempVar();
                    rightInstructions.get(rightInstructions.size() - 1).setResult(temp);
                }
                operand2 = temp;
                instructions.addAll(rightInstructions);
            }
        }
        else {
            operand1 = generate(left).get(0).getOperand1();
            operand2 = generate(right).get(0).getOperand1();
        }
        IROpcode operator = operatorToIROpCode(AST);
        instructions.add(
            Instruction.builder()
                .result(result)
                .operand1(operand1)
                .operator(operator)
                .operand2(operand2)
                .build()
        );
        return instructions;
    }

    public List<Instruction> generateUnaryExpression(AbstractSyntaxTree AST) {
        List<AbstractSyntaxTree> children = AST.getChildren();
        List<Instruction> instructions = new ArrayList<>();
        AbstractSyntaxTree right = children.get(1);
        String operand = right.getValue();
        boolean useTempVar = right.getHeight() > 1;
        if (useTempVar) {
            instructions.addAll(generate(right));
            operand = generateTempVar();
            getLastInstruction(instructions).setResult(operand);
        }
        if (children.get(0).matchesValue("!"))
            instructions.add(
                Instruction.builder()
                    .operand1(operand)
                    .operator(IROpcode.NOT)
                    .build()
            );
        else
            instructions.add(
                Instruction.builder()
                    .operand1("0")
                    .operator(IROpcode.SUB)
                    .operand2(operand)
                    .build()
            );
        return instructions;
    }

    private Instruction generatePreIncrementOrDecrement(List<AbstractSyntaxTree> nodes) {
        IROpcode action = nodes.get(0).matchesValue("--") ? IROpcode.SUB : IROpcode.ADD;
        String value = nodes.get(1).getValue();
        return Instruction.builder()
            .result(value)
            .operand1(value)
            .operator(action)
            .operand2("1")
            .build();
    }

    private List<Instruction> generatePostIncrementOrDecrement(List<AbstractSyntaxTree> nodes) {
        IROpcode action = nodes.get(1).matchesValue("--") ? IROpcode.SUB : IROpcode.ADD;
        String value = nodes.get(0).getValue();
        String temp = generateTempVar();
        return List.of(
            Instruction.builder()
                .result(temp)
                .operand1(value)
                .build(),
            Instruction.builder()
                .result(value)
                .operand1(value)
                .operator(action)
                .operand2("1")
                .build()
        );
    }

    private IROpcode operatorToIROpCode(AbstractSyntaxTree operator) {
        return binaryOperators.get(operator.getValue());
    }

    private String generateConditionalLabel(AbstractSyntaxTree conditional) {
        String label;
        int index;
        if (conditional.matchesStaticToken(TokenType.KW_IF)) {
            label = ".if-start-%d";
            index = ifIndex; // increment this at a different point
        }
        else if (conditional.matchesStaticToken(TokenType.KW_ELSE)) {
            label = elseLabel;
            index = ifIndex;
        }
        else {
            label = ".else-if-start-%d";
            index = elseIfIndex++;
        }
        return String.format(label, index);
    }

    private String generateTempVar() {
        String tempVar = "temp-%d";
        return String.format(tempVar, tempVarIndex++);
    }

    private boolean isPostOrderUnaryExpression(AbstractSyntaxTree AST) {
        if (!AST.matchesLabel("UNARY-OP"))
            return false;
        AbstractSyntaxTree left = AST.getChildren().get(0);
        return left.matchesValue("++") || left.matchesValue("--");
    }

    public List<Instruction> generateArrayDeclaration(List<AbstractSyntaxTree> declaration) {
        String name;
        if (declaration.get(0).matchesStaticToken(TokenType.KW_CONST)) {
            name = declaration.get(2).getValue();
        } else {
            name = declaration.get(1).getValue();
        }
        Symbol symbol = symbolTable.lookup(name, false);
        List<Instruction> instructions = new ArrayList<>(generateArrayAllocation(symbol));
        String capacity = getLastInstruction(instructions).getOperand1();
        if (symbol.getValueNodes() != null) {
            instructions.addAll(generateArrayLiteral(name, capacity, symbol.getType(), symbol.getValueNodes()));
        }
        return instructions;
    }

    public List<Instruction> generateArrayAllocation(Symbol symbol) {
        // TODO: handle multidimensional arrays
        List<Instruction> instructions = new ArrayList<>();
        String name = symbol.getName();
        List<AbstractSyntaxTree> arraySizes = symbol.getArraySizes();
        String size = "1";
        for (AbstractSyntaxTree arraySize : arraySizes) {
            if (arraySize.getHeight() == 1) {
                size = arraySize.getValue();
            }
            else {
                instructions.addAll(generate(arraySize));
                if (getLastInstruction(instructions).getResult() == null) {
                    size = generateTempVar();
                    getLastInstruction(instructions).setResult(size);
                }
            }
        }
        instructions.add(Instruction.builder()
            .result(name)
            .operand1(size)
            .operator(IROpcode.MALLOC)
            .build()
        );
        return instructions;
    }

    public List<Instruction> generateArrayIndex(String name, List<AbstractSyntaxTree> nodes) {
        Symbol symbol = symbolTable.lookup(name, false);
        return generateArrayIndex(name, nodes, symbol.getType());
    }

    // TODO: Fix/Investigate this
    public List<Instruction> generateArrayIndex(String name, List<AbstractSyntaxTree> nodes, EntityType type) {
        List<Instruction> instructions = new ArrayList<>();
        List<Instruction> indexInstructions;
        String temp, value;
        // name = array nodes = (0, ARRAY-INDEX->(i, ARRAY-INDEX->(j))
        boolean addInstructions = false;
        indexInstructions = generate(nodes.get(0));
        if (nodes.get(0).getHeight() > 1) {
            if (isPostOrderUnaryExpression(nodes.get(0)))
                temp = indexInstructions.get(0).getResult();
            else {
                temp = generateTempVar();
                getLastInstruction(indexInstructions).setResult(temp);
            }
            addInstructions = true;
        }
        else
            temp = nodes.get(0).getValue();
        if (addInstructions)
            instructions.addAll(indexInstructions);
        String offset = generateTempVar();
        instructions.add(
            Instruction.builder()
                .result(offset)
                .operand1(name)
                .operator(IROpcode.OFFSET)
                .operand2(temp)
                .build()
        );
        value = generateTempVar();
        instructions.add(
            Instruction.builder()
                .result(value)
                .operand1(dereferenceArrayPointer(temp))
                .build()
        );
        if (nodes.size() == 2)
            instructions.addAll(generateArrayIndex(value, nodes.get(1).getChildren(), type.index(1)));
        return instructions;
    }

    public List<Instruction> generateArrayLiteral(String name, String size, EntityType type, AbstractSyntaxTree literal) {
        List<Instruction> instructions = new ArrayList<>();
        // ignore nested arrays for now
        String pointer, temp;
        List<Instruction> elemInstructions;
        AbstractSyntaxTree element;
        int length = literal.getChildren().size();
        for (int i = 0; i < length; i++) {
            element = literal.getChildren().get(i);
            if (element.getHeight() > 1) {
                if (isPostOrderUnaryExpression(literal)) {
                    elemInstructions = generatePostIncrementOrDecrement(element.getChildren());
                    temp = elemInstructions.get(0).getResult();
                }
                else {
                    elemInstructions = generate(element);
                    if (elemInstructions.get(elemInstructions.size() - 1).getResult() == null) {
                        temp = generateTempVar();
                        elemInstructions.get(elemInstructions.size() - 1).setResult(temp);
                    } else
                        temp = elemInstructions.get(elemInstructions.size() - 1).getResult();
                }
                instructions.addAll(elemInstructions);
            }
            else
                temp = generate(element).get(0).getOperand1();
            pointer = generateTempVar();
            instructions.add(
                Instruction.builder()
                    .result(pointer)
                    .operand1(name)
                    .operator(IROpcode.OFFSET)
                    .operand2(String.valueOf(i))
                    .build()
            );
            instructions.add(
                Instruction.builder()
                    .result(dereferenceArrayPointer(pointer))
                    .operand1(temp)
                    .build()
            );
        }
        // NOTE: Can fill unused array capacity with default values but may prefer to do that at runtime rather than in IR
        //instructions.addAll(fillArrayWithDefaultValues(name, size, type, String.valueOf(length)));
        return instructions;
    }

    /**
     * Generate instructions for automatically filling unused array elements with default values
     * @param name Name of the variable
     * @param size Capacity of the array
     * @param type Array subtype
     * @param startIndex Where is the first unused element
     * @return List<Instruction>
     */
    private List<Instruction> fillArrayWithDefaultValues(String name, String size, EntityType type, String startIndex) {
        String loopVar = generateTempVar();
        String condition = generateTempVar();
        String arrayOffset = generateTempVar();

        String start = String.format(".fill-array-start-%d", arrayFillIndex);
        String end = String.format(".fill-array-end-%d", arrayFillIndex++);

        Instruction startLoop = Instruction.builder()
            .operand1(start)
            .operator(IROpcode.JMP)
            .build();

        return new ArrayList<>(List.of(
            Instruction.builder()
                .result(loopVar)
                .operand1(startIndex)
                .build(),
            startLoop,
            Instruction.builder()
                .label(start)
                .result(condition)
                .operand1(loopVar)
                .operator(IROpcode.LESS_THAN)
                .operand2(size)
                .build(),
            Instruction.builder()
                .operand1(condition)
                .operator(IROpcode.JMPF)
                .operand2(end)
                .build(),
            Instruction.builder()
                .result(arrayOffset)
                .operand1(name)
                .operator(IROpcode.OFFSET)
                .operand2(loopVar)
                .build(),
            Instruction.builder()
                .result(dereferenceArrayPointer(arrayOffset))
                .operand1(getDefaultValue(type))
                .build(),
            Instruction.builder()
                .result(loopVar)
                .operand1(loopVar)
                .operator(IROpcode.ADD)
                .operand2("1")
                .build(),
            startLoop,
            Instruction.builder()
                .label(end)
                .operator(IROpcode.NO_OP)
                .build()
        ));
    }

    private String dereferenceArrayPointer(String arrayPointer) {
        return "*" + arrayPointer;
    }

    private String getDefaultValue(AbstractSyntaxTree node) {
        if (node.matchesStaticToken(TokenType.KW_INT) || node.matchesStaticToken(TokenType.KW_DOUBLE))
            return "0";
        else if (node.matchesStaticToken(TokenType.KW_BOOL))
            return "false";
        else if (node.matchesStaticToken(TokenType.KW_STR))
            return "\"\"";
        return "null"; // TODO: handle array literal
    }

    private String getDefaultValue(EntityType type) {
        EntityType subType = type.index(1);
        if (subType.isType(NodeType.INT) || subType.isType(NodeType.DOUBLE))
            return "0";
        else if (subType.isType(NodeType.BOOLEAN))
            return "false";
        else if (subType.isType(NodeType.STRING))
            return "\"\"";
        return "null"; // TODO: handle nested array
    }

    private Instruction getFirstInstruction(List<Instruction> instructions) {
        return instructions.get(0);
    }

    private Instruction getLastInstruction(List<Instruction> instructions) {
        return instructions.get(instructions.size() - 1);
    }
}
