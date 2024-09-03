package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.lexer.StaticToken;
import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestIRGenerator {
    private final IRGenerator generator = new IRGenerator();

    @Test
    void testEmptyFunction() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree("main")
            ))
        ));
        ProgramIR IR = generator.translateAST(AST);
        System.out.println("Name: " + IR.getFunctions().get(0).getName());
    }
}
