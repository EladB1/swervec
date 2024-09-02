package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.parser.AbstractSyntaxTree;

public class IRGenerator {
    ProgramIR IR = new ProgramIR();

    public ProgramIR translate(AbstractSyntaxTree AST) {
        return IR;
    }
}
