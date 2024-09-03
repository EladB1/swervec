package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.parser.AbstractSyntaxTree;

import java.util.List;

public class IRGenerator {
    private final ProgramIR IR = new ProgramIR();
    private final CFGBlock globalCFGBlock = new CFGBlock();

    public ProgramIR translateAST(AbstractSyntaxTree AST) {
        if (AST.matchesLabel("PROGRAM")) {
            for (AbstractSyntaxTree subTree : AST.getChildren()) {
                if (subTree.matchesLabel("VAR-DECL")) {
                    globalCFGBlock.addInstruction(null); // TODO
                }
                else
                    IR.addFunction(generateFunction(subTree));
            }
        }
        return IR;
    }

    private FunctionIR generateFunction(AbstractSyntaxTree AST) {
        FunctionIR functionRepresentation = new FunctionIR();
        List<AbstractSyntaxTree> fnProperties = AST.getChildren();
        String name = fnProperties.get(0).getValue();
        functionRepresentation.setName(name.equals("main") ? "_entry" : name);
        // we need to keep track of the parameters
        switch(fnProperties.size()) {
            case 1:
                break;
            case 2:
                functionRepresentation.setBlocks(generateFunctionBody(fnProperties.get(1)));
                break;
            case 3:
                /*
                 * TODO: Handle main function with no return type
                 */
                functionRepresentation.setBlocks(generateFunctionBody(fnProperties.get(2)));
                break;
            case 4:
                functionRepresentation.setBlocks(generateFunctionBody(fnProperties.get(3)));
                break;
        }
        return functionRepresentation;
    }

    private List<CFGBlock> generateFunctionBody(AbstractSyntaxTree AST) {
        int loopIndex = 0;
        int conditionalIndex = 0;
        int tempVarIndex = 0;

        return null;
    }
}
