package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.piedpiper.swerve.Compiler.assignmentOperators;
import static java.util.Map.entry;

public class IRGenerator {
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
    private int loopIndex = 0;
    private final List<FunctionBlock> functions;
    private boolean inFunction = false;
    private int tempVarIndex = 1;
    private final String loopStart = ".loop-start-%d";
    private final String loopEnd = ".loop-end-%d";

    public IRGenerator() {
        this.functions = new ArrayList<>(List.of(new FunctionBlock("_entry")));
    }
    // NOTE: Since syntactic and semantic analysis have passed, can assume all inputs are correct programs
    public List<FunctionBlock> generateIR(AbstractSyntaxTree AST) {
        List<Instruction> mainCall = List.of(
            new Instruction("temp-0", null, IROpcode.CALL, "main"),
            new Instruction(IROpcode.RETURN, "temp-0")
        );
        final FunctionBlock entryFunction = functions.get(0);
        FunctionBlock functionBlock;
        List<AbstractSyntaxTree> children;
        for (AbstractSyntaxTree node : AST.getChildren()) {
            functionBlock = functions.get(functions.size() - 1);
            children = node.getChildren();
            if (node.matchesStaticToken(TokenType.KW_FN)) {
                functions.add(generateFunction(node));
            }
            if (node.matchesLabel("VAR-DECL")) {
                int offset = children.get(0).matchesStaticToken(TokenType.KW_CONST) ? 1 : 0;
                String name =  children.get(offset + 1).getValue();
                node = children.get(offset + 2);
                String value = null;
                if (node.getName() == TokenType.STRING || node.getName() == TokenType.NUMBER || node.getName() == TokenType.ID)
                    value = node.getValue();
                if (node.getName() == TokenType.KW_TRUE)
                    value = "true";
                if (node.getName() == TokenType.KW_FALSE)
                    value = "false";
                Instruction instruction = new Instruction(name, null, null, value);
                if (!inFunction) {
                    instruction.setGlobal(true);
                    entryFunction.addInstruction(instruction);
                }
                else {
                    functionBlock.addInstruction(instruction);
                }
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
            if (children.get(i).matchesLabel("PARAMS")) {
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
            functionBlock.addInstruction(new Instruction(IROpcode.RETURN, "0"));
        }
        inFunction = false;
        return functionBlock;
    }

    private List<Instruction> generateBlockBody(List<AbstractSyntaxTree> body) {
        List<Instruction> instructions = new ArrayList<>();
        for (AbstractSyntaxTree node : body) {
            instructions.addAll(generate(node));
        }
        return instructions;
    }

    // use this to decide where what subexpressions call
    private List<Instruction> generate(AbstractSyntaxTree AST) {
        if (AST.getHeight() == 1) {
            if (AST.getName() == TokenType.STRING || AST.getName() == TokenType.NUMBER || AST.getName() == TokenType.ID)
                return List.of(new Instruction(null, (List<Integer>) null, AST.getValue()));
            if (AST.getName() == TokenType.KW_TRUE)
                return List.of(new Instruction(null, (List<Integer>) null, "true"));
            if (AST.getName() == TokenType.KW_FALSE)
                return List.of(new Instruction(null, (List<Integer>) null, "false"));
        }
        List<Instruction> instructions = new ArrayList<>();
        if (binaryOperators.containsKey(AST.getValue())) {
            instructions.addAll(generateBinaryExpression(AST, null, null));
        }
        if (AST.matchesLabel("UNARY-OP")) {
            if (AST.getChildren().get(0).matchesValue("++") || AST.getChildren().get(0).matchesValue("--")) {
                // increment the operand value
                // then use it
                instructions.add(generateIncrementOrDecrement(AST.getChildren(), 1, 0));
            }
            else if (AST.getChildren().get(1).matchesValue("++") || AST.getChildren().get(1).matchesValue("--")) {
                // First use the operand value
                // then increment
                instructions.add(generateIncrementOrDecrement(AST.getChildren(), 0, 1));
            }
            else
                instructions.addAll(generateUnaryExpression(AST));
        }
        if (assignmentOperators.contains(AST.getValue()))
            instructions.addAll(generateVarAssign(AST));
        if (AST.matchesLabel("VAR-DECL"))
            instructions.addAll(generateVarDeclaration(AST.getChildren()));
        return instructions;
    }

    private List<Instruction> generateWhileLoop() {
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new Instruction(IROpcode.JMP, String.format(loopStart, loopIndex)));

        // TODO: Find where to put the end label
        loopIndex++;
        return instructions;
    }

    private List<Instruction> generateFunctionCall(List<AbstractSyntaxTree> callDetails) {
        List<Instruction> instructions = new ArrayList<>();
        if (callDetails.size() == 1) {
            return List.of(new Instruction(callDetails.get(0).getValue(), IROpcode.CALL, "0"));
        }
        for (AbstractSyntaxTree param : callDetails.get(1).getChildren()) {
            if (param.hasChildren()) {
                // TODO
                return null;
            }
            else
                instructions.add(new Instruction(IROpcode.PARAM, param.getValue()));
        }
        return instructions;
    }

    private List<Instruction> generateForLoop() {
        List<Instruction> instructions = new ArrayList<>();
        return instructions;
    }

    private List<Instruction> generateConditionals() {
        List<Instruction> instructions = new ArrayList<>();
        return instructions;
    }

    public List<Instruction> generateVarDeclaration(List<AbstractSyntaxTree> nodes) {
        List<Instruction> instructions = new ArrayList<>();
        int length = nodes.size();
        String value = "";
        switch (length) {
            case 2:
                if (nodes.get(0).getName() == TokenType.KW_INT || nodes.get(0).getName() == TokenType.KW_DOUBLE)
                    value = "0";
                else if (nodes.get(0).getName() == TokenType.KW_BOOL)
                    value = "false";
                else if (nodes.get(0).getName() == TokenType.KW_STR)
                    value = "\"\"";
                instructions.add(new Instruction(nodes.get(1).getValue(), (List<Integer>) null, value));
                break;
            case 3:
                instructions.addAll(generate(nodes.get(length - 1)));
                instructions.get(instructions.size() - 1).setResult(nodes.get(1).getValue());
                break;
            case 4:
                instructions.addAll(generate(nodes.get(length - 1)));
                instructions.get(instructions.size() - 1).setResult(nodes.get(2).getValue());
                break;
        }
        return instructions;
    }

    private List<Instruction> generateArrayDeclaration() {
        List<Instruction> instructions = new ArrayList<>();
        return instructions;
    }

    private List<Instruction> generateVarAssign(AbstractSyntaxTree AST) {
        String name = AST.getChildren().get(0).getValue();
        List<Instruction> instructions = new ArrayList<>(generate(AST.getChildren().get(1)));
        if (AST.matchesValue("=")) {
            instructions.get(instructions.size() - 1).setResult(name);
            return instructions;
        }
        IROpcode operand;
        String temp = generateTempVar();
        incrementTempVarIndex();
        instructions.get(instructions.size() - 1).setResult(temp);
        if (AST.matchesValue("+="))
            operand = IROpcode.ADD;
        else if (AST.matchesValue("-="))
            operand = IROpcode.SUB;
        else if (AST.matchesValue("*="))
            operand = IROpcode.MULTIPLY;
        else
            operand = IROpcode.DIVIDE;
        instructions.add(new Instruction(name, null, name, operand, temp));

        return instructions;
    }

    /**
     * Use for arithmetic, comparisons, and logical and/or expressions
     *
     * @param AST The Abstract syntax tree node
     * @param result The variable name of the result
     * @param indexes Any array indexes if the result is part of an array
     * @return list of instructions
     */
    public List<Instruction> generateBinaryExpression(AbstractSyntaxTree AST, String result, List<Integer> indexes) {
        List<Instruction> instructions = new ArrayList<>();
        boolean useTempVar = AST.getHeight() > 2;
        String operand1, operand2;
        AbstractSyntaxTree left = AST.getChildren().get(0);
        AbstractSyntaxTree right = AST.getChildren().get(1);
        String temp;
        if (useTempVar) {
            if (left.getHeight() == 1)
                operand1 = generate(left).get(0).getOperand2();
            else {
                List<Instruction> leftInstructions = generateBinaryExpression(left, null, null);
                temp = generateTempVar();
                leftInstructions.get(leftInstructions.size() - 1).setResult(temp);
                incrementTempVarIndex();
                operand1 = temp;
                instructions.addAll(leftInstructions);
            }
            if (right.getHeight() == 1)
                operand2 = generate(right).get(0).getOperand2();
            else {
                List<Instruction> rightInstructions = generate(right);
                temp = generateTempVar();
                rightInstructions.get(rightInstructions.size() - 1).setResult(temp);
                incrementTempVarIndex();
                operand2 = temp;
                instructions.addAll(rightInstructions);
            }
        }
        else {
            operand1 = generate(left).get(0).getOperand2();
            operand2 = generate(right).get(0).getOperand2();
        }
        IROpcode operator = operatorToIROpCode(AST);
        instructions.add(new Instruction(result, indexes, operand1, operator, operand2));
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
            instructions.get(instructions.size() - 1).setResult(operand);
        }
        if (children.get(0).matchesValue("!"))
            instructions.add(new Instruction(null, null, IROpcode.NOT, operand));
        else
            instructions.add(new Instruction(null, null, "0", IROpcode.SUB, operand));
        return instructions;
    }

    private Instruction generateIncrementOrDecrement(List<AbstractSyntaxTree> nodes, int operand, int operator) {
        IROpcode action = nodes.get(operator).matchesValue("--") ? IROpcode.SUB : IROpcode.ADD;
        String value = nodes.get(operand).getValue();
        return new Instruction(value, null, value, action, "1");

    }

    private IROpcode operatorToIROpCode(AbstractSyntaxTree operator) {
        return binaryOperators.get(operator.getValue());
    }

    private String generateTempVar() {
        String tempVar = "temp-%d";
        return String.format(tempVar, tempVarIndex);
    }

    private void incrementTempVarIndex() {
        this.tempVarIndex++;
    }
}
