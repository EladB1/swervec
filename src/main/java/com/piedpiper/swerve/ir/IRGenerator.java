package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;

import java.util.ArrayList;
import java.util.List;

public class IRGenerator {
    private int loopIndex = 0;
    private final List<FunctionBlock> functions;

    public IRGenerator() {
        this.functions = new ArrayList<>(List.of(new FunctionBlock("_entry")));
    }

    public List<FunctionBlock> generate(AbstractSyntaxTree AST) {
        List<Instruction> mainCall = List.of(
            new Instruction("temp-0", null, IROpcode.CALL, "main"),
            new Instruction(IROpcode.RETURN, "temp-0")
        );
        boolean inFunction = false;
        final String loopStart = ".loop-start-%d";
        final String loopEnd = ".loop-end-%d";
        final FunctionBlock entryFunction = functions.get(0);
        FunctionBlock functionBlock;
        boolean hasReturnType = false;
        List<AbstractSyntaxTree> children;
        for (AbstractSyntaxTree node : AST.getChildren()) {
            functionBlock = functions.get(functions.size() - 1);
            children = node.getChildren();
            if (node.matchesStaticToken(TokenType.KW_FN)) {
                functions.add(new FunctionBlock(children.get(0).getValue()));
                functionBlock = functions.get(functions.size() - 1);
                AbstractSyntaxTree functionBody = null;
                for (int i = 1; i < children.size(); i++) {
                    if (children.get(i).matchesLabel("PARAMS")) {
                        // TODO: handle parameters
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
                    generate(functionBody);
                }
                if (functionBlock.getName().equals("main") && !hasReturnType) {
                    // TODO: add return 0 to the end of the function body
                    functionBlock.addInstruction(new Instruction(IROpcode.RETURN, "0"));
                }
                inFunction = false;
                hasReturnType = false;
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

            if (node.matchesStaticToken(TokenType.KW_WHILE)) {
                functionBlock.addInstruction(new Instruction(IROpcode.JMP, String.format(loopStart, loopIndex)));

                // TODO: Find where to put the end label
                loopIndex++;
            }

        }
        // have the _entry function call main and return main's return code as the program result
        entryFunction.addMultipleInstructions(mainCall);
        return functions;
    }
}
