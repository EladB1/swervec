package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class IRGenerator {
    private final Map<String, IROpcode> operatorMappings = Map.ofEntries(
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
        entry("!", IROpcode.NOT),
        entry("&&", IROpcode.AND),
        entry("||", IROpcode.OR)
    );
    private int loopIndex = 0;
    private final List<FunctionBlock> functions;
    private boolean inFunction = false;
    private int tempVarIndex = 1;
    private final String tempVar = "temp-%d";
    private final String loopStart = ".loop-start-%d";
    private final String loopEnd = ".loop-end-%d";

    public IRGenerator() {
        this.functions = new ArrayList<>(List.of(new FunctionBlock("_entry")));
    }
    // NOTE: Since syntactic and semantic analysis have passed, can assume all inputs are correct programs
    public List<FunctionBlock> generate(AbstractSyntaxTree AST) {
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
                if (node.getName() == TokenType.STRING || node.getName() == TokenType.NUMBER)
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
        List<String> arithmeticOperators = List.of("+", "-", "*", "/", "%", "**", "^", "&", "|");
        for (AbstractSyntaxTree node : body) {
            if (arithmeticOperators.contains(node.getValue())) {
                instructions.addAll(generateArithmetic(node, null, null));
            }
        }
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

    private List<Instruction> generateVarDeclaration() {
        List<Instruction> instructions = new ArrayList<>();
        return instructions;
    }

    private List<Instruction> generateArrayDeclaration() {
        List<Instruction> instructions = new ArrayList<>();
        return instructions;
    }

    private List<Instruction> generateVarAssign() {
        List<Instruction> instructions = new ArrayList<>();
        return instructions;
    }

    public List<Instruction> generateArithmetic(AbstractSyntaxTree AST, String result, List<Integer> indexes) {
        List<Instruction> instructions = new ArrayList<>();
        boolean useTempVar = AST.getHeight() > 2;
        String operand1, operand2;
        AbstractSyntaxTree left = AST.getChildren().get(0);
        AbstractSyntaxTree right = AST.getChildren().get(1);
        String temp;
        if (useTempVar) {
            if (left.getHeight() == 1)
                operand1 = left.getValue();
            else {
                List<Instruction> leftInstructions = generateArithmetic(left, null, null);
                temp = generateTempVar();
                leftInstructions.get(leftInstructions.size() - 1).setResult(temp);
                incrementTempVarIndex();
                operand1 = temp;
                instructions.addAll(leftInstructions);
            }
            if (right.getHeight() == 1)
                operand2 = right.getValue();
            else {
                List<Instruction> rightInstructions = generateArithmetic(right, null, null);
                temp = generateTempVar();
                rightInstructions.get(rightInstructions.size() - 1).setResult(temp);
                incrementTempVarIndex();
                operand2 = temp;
                instructions.addAll(rightInstructions);
            }
        }
        else {
            operand1 = left.getValue();
            operand2 = right.getValue();
        }
        IROpcode operator = operatorToIROpCode(AST);
        instructions.add(new Instruction(result, indexes, operand1, operator, operand2));
        return instructions;
    }

    private IROpcode operatorToIROpCode(AbstractSyntaxTree operator) {
        return operatorMappings.get(operator.getValue());
    }

    private String generateTempVar() {
        return String.format(tempVar, tempVarIndex);
    }

    private void incrementTempVarIndex() {
        this.tempVarIndex++;
    }
}
