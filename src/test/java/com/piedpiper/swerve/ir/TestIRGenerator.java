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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestIRGenerator {
    private final SymbolTable table = new SymbolTable();
    private final IRGenerator subject = new IRGenerator(table);
    private final List<Instruction> mainCall = List.of(
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
        assertEquals(
            Instruction.builder()
                .operand1("0")
                .operator(IROpcode.RETURN)
                .build(),
            IR.get(1).getInstructions().get(0)
        );
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
        List<Instruction> entryInstructions = new ArrayList<>(List.of(
            Instruction.builder()
                .global(true)
                .result("x")
                .operand1("42")
                .build()
        ));
        entryInstructions.addAll(mainCall);
        assertEquals(2, IR.size());

        assertEquals("_entry", IR.get(0).getName());
        assertNotEquals(mainCall, IR.get(0).getInstructions());
        assertEquals(entryInstructions, IR.get(0).getInstructions());

        assertEquals("main", IR.get(1).getName());
        assertEquals(1, IR.get(1).getInstructions().size(), 1);
        assertEquals(
            Instruction.builder()
                .operand1("0")
                .operator(IROpcode.RETURN)
                .build(),
            IR.get(1).getInstructions().get(0)
        );
    }

    @Test
    void generateIREmptyFunctionWithParams() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "test")),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
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
        assertEquals(
            Instruction.builder()
                .operand1("0")
                .operator(IROpcode.RETURN)
                .build(),
            IR.get(2).getInstructions().get(0)
        );
    }

    @Test
    void testSimpleArithmetic() {
        // result = y + 1;
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), new VariableToken(TokenType.ID, "y"), new VariableToken(TokenType.NUMBER, "1"));
        List<Instruction> IR = subject.generateBinaryExpression(AST, "result");

        assertEquals(1, IR.size());
        assertEquals(
            Instruction.builder()
                .result("result")
                .operand1("y")
                .operator(IROpcode.ADD)
                .operand2("1")
                .build(),
            IR.get(0)
        );
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
        assertEquals(
            Instruction.builder()
                .result("temp-1")
                .operand1("y")
                .operator(IROpcode.DIVIDE)
                .operand2("2")
                .build(),
            IR.get(0)
        );
        assertEquals(
            Instruction.builder()
                .result("result")
                .operand1("temp-1")
                .operator(IROpcode.ADD)
                .operand2("1")
                .build(),
            IR.get(1)
        );
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
        assertEquals(
            Instruction.builder()
                .result("temp-1")
                .operand1("2")
                .operator(IROpcode.MULTIPLY)
                .operand2("x")
                .build(),
            IR.get(0)
        );
        assertEquals(
            Instruction.builder()
                .result("result")
                .operand1("y")
                .operator(IROpcode.ADD)
                .operand2("temp-1")
                .build(),
            IR.get(1)
        );
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
        assertEquals(
            Instruction.builder()
                .result("temp-1")
                .operand1("y")
                .operator(IROpcode.DIVIDE)
                .operand2("2")
                .build(),
            IR.get(0)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-2")
                .operand1("2")
                .operator(IROpcode.MULTIPLY)
                .operand2("x")
                .build(),
            IR.get(1)
        );
        assertEquals(
            Instruction.builder()
                .result("result")
                .operand1("temp-1")
                .operator(IROpcode.ADD)
                .operand2("temp-2")
                .build(),
            IR.get(2)
        );
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
        assertEquals(
            Instruction.builder()
                .result("temp-1")
                .operand1("i")
                .operator(IROpcode.ADD)
                .operand2("1")
                .build(),
            IR.get(0)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-2")
                .operand1("k")
                .operator(IROpcode.ADD)
                .operand2("1")
                .build(),
            IR.get(1)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-3")
                .operand1("2")
                .operator(IROpcode.MULTIPLY)
                .operand2("temp-2")
                .build(),
            IR.get(2)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-4")
                .operand1("j")
                .operator(IROpcode.SUB)
                .operand2("temp-3")
                .build(),
            IR.get(3)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-5")
                .operand1("temp-1")
                .operator(IROpcode.MULTIPLY)
                .operand2("temp-4")
                .build(),
            IR.get(4)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-6")
                .operand1("2")
                .operator(IROpcode.MULTIPLY)
                .operand2("x")
                .build(),
            IR.get(5)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-7")
                .operand1("2")
                .operator(IROpcode.MULTIPLY)
                .operand2("y")
                .build(),
            IR.get(6)
        );
        assertEquals(
            Instruction.builder()
                .result("temp-8")
                .operand1("temp-6")
                .operator(IROpcode.ADD)
                .operand2("temp-7")
                .build(),
            IR.get(7)
        );
        assertEquals(
            Instruction.builder()
                .result("result")
                .operand1("temp-5")
                .operator(IROpcode.DIVIDE)
                .operand2("temp-8")
                .build(),
            IR.get(8)
        );
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

        assertEquals(
            Instruction.builder()
                .result("x")
                .operand1(defaultVal)
                .build(),
            instructions.get(0)
        );
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

        assertEquals(
            Instruction.builder()
                .result("x")
                .operand1("3")
                .build(),
            instructions.get(0)
        );
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

        assertEquals(
            Instruction.builder()
                .result("x")
                .operand1("3")
                .operator(IROpcode.DIVIDE)
                .operand2("i")
                .build(),
            instructions.get(0)
        );
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

        assertEquals(
            Instruction.builder()
                .result("temp-1")
                .operand1("i")
                .operator(IROpcode.MULTIPLY)
                .operand2("2")
                .build(),
            instructions.get(0)
        );
        assertEquals(
            Instruction.builder()
                .result("x")
                .operand1("3")
                .operator(IROpcode.DIVIDE)
                .operand2("temp-1")
                .build(),
            instructions.get(1)
        );
    }

    @Test
    void testSimpleUnaryMinus() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "-")),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "i"))
        ));

        List<Instruction> instructions = subject.generateUnaryExpression(AST);

        assertEquals(1, instructions.size());
        assertEquals(
            Instruction.builder()
                .operand1("0")
                .operator(IROpcode.SUB)
                .operand2("i")
                .build(),
            instructions.get(0)
        );
    }

    @Test
    void testSimpleUnaryNot() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "!")),
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "isOpen"))
        ));

        List<Instruction> instructions = subject.generateUnaryExpression(AST);

        assertEquals(1, instructions.size());
        assertEquals(
            Instruction.builder()
                .operand1("isOpen")
                .operator(IROpcode.NOT)
                .build(),
            instructions.get(0)
        );
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
        assertEquals(
            Instruction.builder()
                .result("temp-1")
                .operand1("i")
                .operator(IROpcode.SUB)
                .operand2("1")
                .build(),
            instructions.get(0)
        );
        assertEquals(
            Instruction.builder()
                .operand1("0")
                .operator(IROpcode.SUB)
                .operand2("temp-1")
                .build(),
            instructions.get(1)
        );
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

        assertEquals(
            Instruction.builder()
                .result("temp-1")
                .operand1("isOpen")
                .operator(IROpcode.AND)
                .operand2("true")
                .build(),
            instructions.get(0)
        );
        assertEquals(
            Instruction.builder()
                .operand1("temp-1")
                .operator(IROpcode.NOT)
                .build(),
            instructions.get(1)
        );
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
        table.insert(new Symbol(declaration, List.of(new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3"))), new EntityType(NodeType.ARRAY, NodeType.BOOLEAN), 4, 1));
        List<Instruction> IR = subject.generateArrayDeclaration(declaration.getChildren());
        assertEquals(5, IR.size());
        assertEquals(List.of(
            Instruction.builder()
                .result("arr")
                .operand1("3")
                .operator(IROpcode.MALLOC)
                .build(),
            Instruction.builder()
                .result("temp-1")
                .operand1("arr")
                .operator(IROpcode.OFFSET)
                .operand2("0")
                .build(),
            Instruction.builder()
                .result("*temp-1")
                .operand1("true")
                .build(),
            Instruction.builder()
                .result("temp-2")
                .operand1("arr")
                .operator(IROpcode.OFFSET)
                .operand2("1")
                .build(),
            Instruction.builder()
                .result("*temp-2")
                .operand1("false")
                .build()
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
        table.insert(new Symbol(declaration, List.of(new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "4"))), new EntityType(NodeType.ARRAY, NodeType.INT), 3, 1));
        Symbol constants = new Symbol(
            "constants",
            new EntityType(NodeType.ARRAY, NodeType.INT),
            true,
            new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "5")),
            1
        );
        constants.setArraySizes(List.of(new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "4"))));
        table.insert(constants);
        List<Instruction> IR = subject.generateArrayDeclaration(declaration.getChildren());
        assertEquals(10, IR.size());
        assertEquals(List.of(
            Instruction.builder()
                .result("arr")
                .operand1("4")
                .operator(IROpcode.MALLOC)
                .build(),
            Instruction.builder()
                .result("temp-1")
                .operand1("i")
                .operator(IROpcode.ADD)
                .operand2("1")
                .build(),
            Instruction.builder()
                .result("temp-2")
                .operand1("arr")
                .operator(IROpcode.OFFSET)
                .operand2("0")
                .build(),
            Instruction.builder()
                .result("*temp-2")
                .operand1("temp-1")
                .build(),
            Instruction.builder()
                .result("temp-3")
                .operand1("arr")
                .operator(IROpcode.OFFSET)
                .operand2("1")
                .build(),
            Instruction.builder()
                .result("*temp-3")
                .operand1("-5")
                .build(),
            Instruction.builder()
                .result("temp-4")
                .operand1("constants")
                .operator(IROpcode.OFFSET)
                .operand2("0")
                .build(),
            Instruction.builder()
                .result("temp-5")
                .operand1("*0")
                .build(),
            Instruction.builder()
                .result("temp-6")
                .operand1("arr")
                .operator(IROpcode.OFFSET)
                .operand2("2")
                .build(),
            Instruction.builder()
                .result("*temp-6")
                .operand1("temp-5")
                .build()
        ), IR);
    }

    @Test
    void generateArrayIndexOneDimension() {
        AbstractSyntaxTree index = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "constants"), List.of(
            new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "0"))
        ));
        Symbol constants = new Symbol(
            "constants",
            new EntityType(NodeType.ARRAY, NodeType.INT),
            true,
            new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "5")),
            1
        );
        constants.setArraySizes(List.of(new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "4"))));
        table.insert(constants);

        List<Instruction> IR = subject.generateArrayIndex(index.getValue(), index.getChildren().get(0).getChildren());
        assertEquals(2, IR.size());

        assertEquals(List.of(
            Instruction.builder()
                .result("temp-1")
                .operand1("constants")
                .operator(IROpcode.OFFSET)
                .operand2("0")
                .build(),
            Instruction.builder()
                .result("temp-2")
                .operand1("*0")
                .build()
        ), IR);
    }

    @Test
    void generateArrayIndexMultiDimension() {
        // arr[0][1][i]
        AbstractSyntaxTree index = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "arr"), List.of(
            new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "0")),
                new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
                    new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.ID, "i")))
                ))
            ))
        );

        Symbol constants = new Symbol(
            "arr",
            new EntityType(NodeType.ARRAY, NodeType.ARRAY, NodeType.ARRAY, NodeType.INT),
            true,
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree("ARRAY-LIT")
                )),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "5"))))
            )),
            1
        );
        VariableToken size = new VariableToken(TokenType.NUMBER, "4");
        constants.setArraySizes(List.of(new AbstractSyntaxTree(size), new AbstractSyntaxTree(size), new AbstractSyntaxTree(size)));
        table.insert(constants);

        List<Instruction> IR = subject.generateArrayIndex(index.getValue(), index.getChildren().get(0).getChildren());
        assertEquals(6, IR.size());

        assertEquals(List.of(
            Instruction.builder()
                .result("temp-1")
                .operand1("arr")
                .operator(IROpcode.OFFSET)
                .operand2("0")
                .build(),
            Instruction.builder()
                .result("temp-2")
                .operand1("*0")
                .build(),
            Instruction.builder()
                .result("temp-3")
                .operand1("temp-2")
                .operator(IROpcode.OFFSET)
                .operand2("1")
                .build(),
            Instruction.builder()
                .result("temp-4")
                .operand1("*1")
                .build(),
            Instruction.builder()
                .result("temp-5")
                .operand1("temp-4")
                .operator(IROpcode.OFFSET)
                .operand2("i")
                .build(),
            Instruction.builder()
                .result("temp-6")
                .operand1("*i")
                .build()
        ), IR);
    }
}
