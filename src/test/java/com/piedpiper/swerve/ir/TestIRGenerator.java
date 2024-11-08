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
        assertEquals(2, IR.size());

        assertEquals("_entry", IR.get(0).getName());
        assertEquals(mainCall, IR.get(0).getInstructions());

        assertEquals("main", IR.get(1).getName());
        assertEquals(1, IR.get(1).getInstructions().size());
        assertEquals(new Instruction(IROpcode.RETURN, "0"), IR.get(1).getInstructions().get(0));
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
            new Instruction("x", (List<Integer>) null, "42").withGlobal(true),
            new Instruction("temp-0", null, IROpcode.CALL, "main"),
            new Instruction(IROpcode.RETURN, "temp-0")
        );
        assertEquals(2, IR.size());

        assertEquals("_entry", IR.get(0).getName());
        assertNotEquals(mainCall, IR.get(0).getInstructions());
        assertEquals(entryInstructions, IR.get(0).getInstructions());

        assertEquals("main", IR.get(1).getName());
        assertEquals(1, IR.get(1).getInstructions().size(), 1);
        assertEquals(new Instruction(IROpcode.RETURN, "0"), IR.get(1).getInstructions().get(0));
    }

    @Test
    void generateEmptyFunctionWithParams() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "test")),
                new AbstractSyntaxTree("PARAMS", List.of(
                    new AbstractSyntaxTree("PARAM", new StaticToken(TokenType.KW_INT), new VariableToken(TokenType.ID, "i"))
                ))
            )),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), new VariableToken(TokenType.ID, "main"))

        ));

        List<FunctionBlock> IR = subject.generate(AST);
        assertEquals(3, IR.size());

        assertEquals("_entry", IR.get(0).getName());
        assertEquals(mainCall, IR.get(0).getInstructions());

        assertEquals("test", IR.get(1).getName());
        assertEquals(1, IR.get(1).getParameters().size());
        assertEquals("i", IR.get(1).getParameters().get(0));

        assertEquals("main", IR.get(2).getName());
        assertEquals(1, IR.get(2).getInstructions().size(), 1);
        assertEquals(new Instruction(IROpcode.RETURN, "0"), IR.get(2).getInstructions().get(0));
    }

    @Test
    void testSimpleArithmetic() {
        // result = y + 1;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), new VariableToken(TokenType.ID, "y"), new VariableToken(TokenType.NUMBER, "1"));
        List<Instruction> IR = subject.generateArithmetic(AST, "result", null);

        assertEquals(1, IR.size());
        assertEquals(new Instruction("result", null, "y", IROpcode.ADD, "1"), IR.get(0));
    }

    @Test
    void testLeftSubExpressionArithmetic() {
        // result = y / 2 + 1;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "/"), new VariableToken(TokenType.ID, "y"), new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        List<Instruction> IR = subject.generateArithmetic(AST, "result", null);

        assertEquals(2, IR.size());
        assertEquals(new Instruction("temp-1", null, "y", IROpcode.DIVIDE, "2"), IR.get(0));
        assertEquals(new Instruction("result", null, "temp-1", IROpcode.ADD, "1"), IR.get(1));
    }

    @Test
    void testRightSubExpressionArithmetic() {
        // result = y + 2 * x;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "y")),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.ID, "x"))
        ));
        List<Instruction> IR = subject.generateArithmetic(AST, "result", null);

        assertEquals(2, IR.size());
        assertEquals(new Instruction("temp-1", null, "2", IROpcode.MULTIPLY, "x"), IR.get(0));
        assertEquals(new Instruction("result", null, "y", IROpcode.ADD, "temp-1"), IR.get(1));
    }

    @Test
    void testMultipleSubExpressionsArithmetic() {
        // result = y / 2 + 2 * x;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "/"), new VariableToken(TokenType.ID, "y"), new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.ID, "x"))
        ));
        List<Instruction> IR = subject.generateArithmetic(AST, "result", null);

        assertEquals(3, IR.size());
        assertEquals(new Instruction("temp-1", null, "y", IROpcode.DIVIDE, "2"), IR.get(0));
        assertEquals(new Instruction("temp-2", null, "2", IROpcode.MULTIPLY, "x"), IR.get(1));
        assertEquals(new Instruction("result", null, "temp-1", IROpcode.ADD, "temp-2"), IR.get(2));
    }

    @Test
    void testComplexArithmetic() {
        // (i + 1) * (j - 2 * (k + 1)) / (2 * x + 2 * y)
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "/"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), new VariableToken(TokenType.ID, "i"), new VariableToken(TokenType.NUMBER, "1")),
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "-"), List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.ID, "j")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(
                        new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                        new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), new VariableToken(TokenType.ID, "k"), new VariableToken(TokenType.NUMBER, "1"))
                    ))
                ))
            )),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.ID, "x")),
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.ID, "y"))
            ))
        ));
        List<Instruction> IR = subject.generateArithmetic(AST, "result", null);
        System.out.println(IR);
        assertEquals(9, IR.size());
        assertEquals(new Instruction("temp-1", null, "i", IROpcode.ADD, "1"), IR.get(0));
        assertEquals(new Instruction("temp-2", null, "k", IROpcode.ADD, "1"), IR.get(1));
        assertEquals(new Instruction("temp-3", null, "2", IROpcode.MULTIPLY, "temp-2"), IR.get(2));
        assertEquals(new Instruction("temp-4", null, "j", IROpcode.SUB, "temp-3"), IR.get(3));
        assertEquals(new Instruction("temp-5", null, "temp-1", IROpcode.MULTIPLY, "temp-4"), IR.get(4));
        assertEquals(new Instruction("temp-6", null, "2", IROpcode.MULTIPLY, "x"), IR.get(5));
        assertEquals(new Instruction("temp-7", null, "2", IROpcode.MULTIPLY, "y"), IR.get(6));
        assertEquals(new Instruction("temp-8", null, "temp-6", IROpcode.ADD, "temp-7"), IR.get(7));
        assertEquals(new Instruction("result", null, "temp-5", IROpcode.DIVIDE, "temp-8"), IR.get(8));
    }
}
