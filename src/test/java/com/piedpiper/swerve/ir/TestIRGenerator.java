package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.lexer.StaticToken;
import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.lexer.VariableToken;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestIRGenerator {
    private final IRGenerator subject = new IRGenerator();
    private final List<Instruction> mainCall = List.of(
        new Instruction("temp-0", null, IROpcode.CALL, "main"),
        new Instruction(IROpcode.RETURN, "temp-0")
    );

    @Test
    void generateEmptyMain() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), new VariableToken(TokenType.ID, "main"))
        ));
        List<FunctionBlock> IR = subject.generate(AST);
        assertEquals(IR.size(), 2);
        assertEquals(IR.get(0).getName(), "_entry");
        assertEquals(IR.get(0).getInstructions(), mainCall);
        assertEquals(IR.get(1).getName(), "main");
        assertEquals(IR.get(1).getInstructions().size(), 1);
        assertEquals(IR.get(1).getInstructions().get(0), new Instruction(IROpcode.RETURN, "0"));
    }

    @Test
    void generateGlobalVariableSimple() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL",
                new StaticToken(TokenType.KW_CONST),
                new StaticToken(TokenType.KW_INT),
                new VariableToken(TokenType.ID, "x"),
                new VariableToken(TokenType.NUMBER, "42")
            ),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), new VariableToken(TokenType.ID, "main"))
        ));
        List<FunctionBlock> IR = subject.generate(AST);
        List<Instruction> entryInstructions = List.of(
            new Instruction("x", null, "42").withGlobal(true),
            new Instruction("temp-0", null, IROpcode.CALL, "main"),
            new Instruction(IROpcode.RETURN, "temp-0")
        );
        assertEquals(IR.size(), 2);
        assertEquals(IR.get(0).getName(), "_entry");
        assertNotEquals(IR.get(0).getInstructions(), mainCall);
        assertEquals(IR.get(0).getInstructions(), entryInstructions);
        assertEquals(IR.get(1).getName(), "main");
        assertEquals(IR.get(1).getInstructions().size(), 1);
        assertEquals(IR.get(1).getInstructions().get(0), new Instruction(IROpcode.RETURN, "0"));
    }
}
