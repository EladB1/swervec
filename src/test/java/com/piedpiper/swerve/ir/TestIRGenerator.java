package com.piedpiper.swerve.ir;

import com.piedpiper.swerve.lexer.StaticToken;
import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.lexer.VariableToken;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;
import com.piedpiper.swerve.semantic.EntityType;
import com.piedpiper.swerve.semantic.NodeType;
import com.piedpiper.swerve.symboltable.Symbol;
import com.piedpiper.swerve.symboltable.SymbolTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestIRGenerator {
    private final SymbolTable table = new SymbolTable();
    private final IRGenerator subject = new IRGenerator(table);
    private final List<Instruction> mainCall = List.of(
        new Instruction("temp-0", IROpcode.CALL, "main"),
        new Instruction(IROpcode.RETURN, "temp-0")
    );

    @Test
    void generateIREmptyMain() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), new VariableToken(TokenType.ID, "main"))
        ));
        List<FunctionBlock> IR = subject.generateIR(AST);
        assertEquals(2, IR.size());

        assertEquals("_entry", IR.get(0).getName());
        assertEquals(mainCall, IR.get(0).getInstructions());

        assertEquals("main", IR.get(1).getName());
        assertEquals(1, IR.get(1).getInstructions().size());
        assertEquals(new Instruction(IROpcode.RETURN, "0"), IR.get(1).getInstructions().get(0));
    }

    @Test
    void generateIRGlobalVariableSimple() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL",
                new StaticToken(TokenType.KW_CONST),
                new StaticToken(TokenType.KW_INT),
                new VariableToken(TokenType.ID, "x"),
                new VariableToken(TokenType.NUMBER, "42")
            ),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), new VariableToken(TokenType.ID, "main"))
        ));
        List<FunctionBlock> IR = subject.generateIR(AST);
        List<Instruction> entryInstructions = List.of(
            new Instruction("x", "42").withGlobal(true),
            new Instruction("temp-0", IROpcode.CALL, "main"),
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
    void generateIREmptyFunctionWithParams() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "test")),
                new AbstractSyntaxTree("PARAMS", List.of(
                    new AbstractSyntaxTree("PARAM", new StaticToken(TokenType.KW_INT), new VariableToken(TokenType.ID, "i"))
                ))
            )),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), new VariableToken(TokenType.ID, "main"))

        ));

        List<FunctionBlock> IR = subject.generateIR(AST);
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
        List<Instruction> IR = subject.generateBinaryExpression(AST, "result");

        assertEquals(1, IR.size());
        assertEquals(new Instruction("result", IROpcode.ADD, "y", "1"), IR.get(0));
    }

    @Test
    void testLeftSubExpressionArithmetic() {
        // result = y / 2 + 1;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "/"), new VariableToken(TokenType.ID, "y"), new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        List<Instruction> IR = subject.generateBinaryExpression(AST, "result");

        assertEquals(2, IR.size());
        assertEquals(new Instruction("temp-1", IROpcode.DIVIDE, "y", "2"), IR.get(0));
        assertEquals(new Instruction("result", IROpcode.ADD, "temp-1", "1"), IR.get(1));
    }

    @Test
    void testRightSubExpressionArithmetic() {
        // result = y + 2 * x;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "y")),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.ID, "x"))
        ));
        List<Instruction> IR = subject.generateBinaryExpression(AST, "result");

        assertEquals(2, IR.size());
        assertEquals(new Instruction("temp-1", IROpcode.MULTIPLY, "2", "x"), IR.get(0));
        assertEquals(new Instruction("result", IROpcode.ADD, "y", "temp-1"), IR.get(1));
    }

    @Test
    void testMultipleSubExpressionsArithmetic() {
        // result = y / 2 + 2 * x;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "/"), new VariableToken(TokenType.ID, "y"), new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.ID, "x"))
        ));
        List<Instruction> IR = subject.generateBinaryExpression(AST, "result");

        assertEquals(3, IR.size());
        assertEquals(new Instruction("temp-1", IROpcode.DIVIDE, "y", "2"), IR.get(0));
        assertEquals(new Instruction("temp-2", IROpcode.MULTIPLY, "2", "x"), IR.get(1));
        assertEquals(new Instruction("result", IROpcode.ADD, "temp-1", "temp-2"), IR.get(2));
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
        List<Instruction> IR = subject.generateBinaryExpression(AST, "result");
        assertEquals(9, IR.size());
        assertEquals(new Instruction("temp-1", IROpcode.ADD, "i", "1"), IR.get(0));
        assertEquals(new Instruction("temp-2", IROpcode.ADD, "k", "1"), IR.get(1));
        assertEquals(new Instruction("temp-3", IROpcode.MULTIPLY, "2", "temp-2"), IR.get(2));
        assertEquals(new Instruction("temp-4", IROpcode.SUB, "j", "temp-3"), IR.get(3));
        assertEquals(new Instruction("temp-5", IROpcode.MULTIPLY, "temp-1", "temp-4"), IR.get(4));
        assertEquals(new Instruction("temp-6", IROpcode.MULTIPLY, "2", "x"), IR.get(5));
        assertEquals(new Instruction("temp-7", IROpcode.MULTIPLY, "2", "y"), IR.get(6));
        assertEquals(new Instruction("temp-8", IROpcode.ADD, "temp-6", "temp-7"), IR.get(7));
        assertEquals(new Instruction("result", IROpcode.DIVIDE, "temp-5", "temp-8"), IR.get(8));
    }

    private static Stream<Arguments> provideVarDeclarationNoValue() {
        return Stream.of(
            Arguments.of(TokenType.KW_INT, "0"),
            Arguments.of(TokenType.KW_DOUBLE, "0"),
            Arguments.of(TokenType.KW_BOOL, "false"),
            Arguments.of(TokenType.KW_STR, "\"\"")
        );
    }

    @ParameterizedTest
    @MethodSource("provideVarDeclarationNoValue")
    void testVarDeclarationNoValue(TokenType type, String defaultVal) {
        List<AbstractSyntaxTree> varDeclaration = List.of(
            new AbstractSyntaxTree(new StaticToken(type)),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "x"))
        );

        List<Instruction> instructions = subject.generateVarDeclaration(varDeclaration);
        assertEquals(1, instructions.size());

        assertEquals(new Instruction("x", defaultVal), instructions.get(0));
    }

    @Test
    void testVarDeclarationWithSimpleValue() {
        List<AbstractSyntaxTree> varDeclaration = List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_INT)),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "x")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3"))
        );

        List<Instruction> instructions = subject.generateVarDeclaration(varDeclaration);
        assertEquals(1, instructions.size());

        assertEquals(new Instruction("x", "3"), instructions.get(0));
    }

    @Test
    void testVarDeclarationWithSimpleExpressionValue() {
        List<AbstractSyntaxTree> varDeclaration = List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_INT)),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "x")),
            new AbstractSyntaxTree(
                new VariableToken(TokenType.OP, "/"),
                new VariableToken(TokenType.NUMBER, "3"),
                new VariableToken(TokenType.NUMBER, "i")
            )
        );

        List<Instruction> instructions = subject.generateVarDeclaration(varDeclaration);
        assertEquals(1, instructions.size());

        assertEquals(new Instruction("x", IROpcode.DIVIDE, "3", "i"), instructions.get(0));
    }

    @Test
    void testVarDeclarationWithComplexExpressionValue() {
        List<AbstractSyntaxTree> varDeclaration = List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_INT)),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "x")),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "/"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3")),
                new AbstractSyntaxTree(
                    new VariableToken(TokenType.OP, "*"),
                    new VariableToken(TokenType.NUMBER, "i"),
                    new VariableToken(TokenType.NUMBER, "2")
                )
            ))
        );

        List<Instruction> instructions = subject.generateVarDeclaration(varDeclaration);
        assertEquals(2, instructions.size());

        assertEquals(new Instruction("temp-1", IROpcode.MULTIPLY, "i", "2"), instructions.get(0));
        assertEquals(new Instruction("x", IROpcode.DIVIDE, "3", "temp-1"), instructions.get(1));
    }

    @Test
    void testSimpleUnaryMinus() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "-")),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "i"))
        ));

        List<Instruction> instructions = subject.generateUnaryExpression(AST);

        assertEquals(1, instructions.size());
        assertEquals(new Instruction(IROpcode.SUB, "0", "i"), instructions.get(0));
    }

    @Test
    void testSimpleUnaryNot() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "!")),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "isOpen"))
        ));

        List<Instruction> instructions = subject.generateUnaryExpression(AST);

        assertEquals(1, instructions.size());
        assertEquals(new Instruction(null, null, IROpcode.NOT, "isOpen"), instructions.get(0));
    }

    @Test
    void testComplexUnaryMinus() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "-")),
            new AbstractSyntaxTree(
                new VariableToken(TokenType.OP, "-"),
                new VariableToken(TokenType.ID, "i"),
                new VariableToken(TokenType.NUMBER, "1")
            )
        ));

        List<Instruction> instructions = subject.generateUnaryExpression(AST);

        assertEquals(2, instructions.size());
        assertEquals(new Instruction("temp-1", IROpcode.SUB, "i", "1"), instructions.get(0));
        assertEquals(new Instruction(IROpcode.SUB, "0", "temp-1"), instructions.get(1));
    }

    @Test
    void testComplexUnaryNot() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "!")),
            new AbstractSyntaxTree(
                new VariableToken(TokenType.OP, "&&"),
                new VariableToken(TokenType.ID, "isOpen"),
                new StaticToken(TokenType.KW_TRUE)
            )
        ));

        List<Instruction> instructions = subject.generateUnaryExpression(AST);

        assertEquals(2, instructions.size());
        assertEquals(new Instruction("temp-1", IROpcode.AND, "isOpen", "true"), instructions.get(0));
        assertEquals(new Instruction(IROpcode.NOT, "temp-1"), instructions.get(1));
    }

    @Test
    void testGenerateArrayDeclaration() {
        AbstractSyntaxTree literal = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE)),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FALSE))
        ));
        AbstractSyntaxTree declaration = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_BOOL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "arr")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3")),
            literal
        ));
        table.insert(new Symbol(declaration, List.of(3), new EntityType(NodeType.ARRAY, NodeType.BOOLEAN), 4, 1));
        List<Instruction> IR = subject.generateArrayDeclaration(declaration.getChildren());
        assertEquals(7, IR.size());
        assertEquals(List.of(
            new Instruction("arr", IROpcode.MALLOC, "3"),
            new Instruction("temp-1", IROpcode.OFFSET, "arr", "0"),
            new Instruction("*temp-1", "true"),
            new Instruction("temp-2", IROpcode.OFFSET, "arr","1"),
            new Instruction("*temp-2", "false"),
            new Instruction("temp-3", IROpcode.OFFSET, "arr", "2"),
            new Instruction("*temp-3", "false")
        ), IR);

    }

    @Test
    void testGenerateArrayDeclarationInts() {
        AbstractSyntaxTree literal = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(
                new VariableToken(TokenType.OP, "+"),
                new VariableToken(TokenType.ID, "i"),
                new VariableToken(TokenType.NUMBER, "1")
            ),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "-5")),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "constants"), List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "0"))
            ))
        )); // {i+1, -5, constants[0]}
        AbstractSyntaxTree declaration = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "arr")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3")),
            literal
        ));
        table.insert(new Symbol(declaration, List.of(4), new EntityType(NodeType.ARRAY, NodeType.INT), 3, 1));
        Symbol constants = new Symbol(
            "constants",
            new EntityType(NodeType.ARRAY, NodeType.INT),
            true,
            new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "5")),
            1
        );
        constants.setArraySizes(List.of(4));
        table.insert(constants);
        List<Instruction> IR = subject.generateArrayDeclaration(declaration.getChildren());
        assertEquals(12, IR.size());
        assertEquals(List.of(
            new Instruction("arr", IROpcode.MALLOC, "16"),
            new Instruction("temp-1", IROpcode.ADD, "i", "1"),
            new Instruction("temp-2", IROpcode.OFFSET, "arr", "0"),
            new Instruction("*temp-2", "temp-1"),
            new Instruction("temp-3", IROpcode.OFFSET, "arr", "4"),
            new Instruction("*temp-3", "-5"),
            new Instruction("temp-4", IROpcode.MULTIPLY, "0", "4"),
            new Instruction("temp-5", IROpcode.OFFSET, "constants", "temp-4"),
            new Instruction("temp-6", IROpcode.OFFSET, "arr", "8"),
            new Instruction("*temp-6", "*temp-5"),
            new Instruction("temp-7", IROpcode.OFFSET, "arr", "12"),
            new Instruction("*temp-7", "0")
        ), IR);
    }
}
