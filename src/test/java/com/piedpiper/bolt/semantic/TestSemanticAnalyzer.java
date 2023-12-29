package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.IllegalStatementError;
import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.error.UnreachableCodeError;
import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSemanticAnalyzer {
    private final SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
    private final Token intTypeToken = new StaticToken(TokenType.KW_INT);
    private final Token varToken = new VariableToken(TokenType.ID, "var");

    @Test
    void test_varDecl_noValue() {
        // int var;
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken)
        ));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    @Test
    void test_varDecl_withValue() {
        // int var = 5;
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken, new VariableToken(TokenType.NUMBER, "5"))
        ));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    @Test
    void test_varDecl_withWrongTypeValue() {
        // int var = "5";
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken, new VariableToken(TokenType.STRING, "\"5\""))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Right hand side of variable is STRING but INT expected", error.getMessage());
    }

    @Test
    void test_varDecl_sameNameSameScope() {
        // int var; int var;
        AbstractSyntaxTree varDeclaration = new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken);
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(varDeclaration, varDeclaration));
        NameError error = assertThrows(NameError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Symbol 'var' is already defined in this scope", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "2.0"})
    void test_evaluateType_nonEqualityComparison_twoSameTypes(String num) {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, num));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<="), List.of(
            numNode,
            numNode
        ));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.BOOLEAN));
    }

    @ParameterizedTest
    @CsvSource({"2, 2.0", "2.0, 2"})
    void test_evaluateType_nonEqualityComparison_mixedTypes(String value1, String value2) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<="), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, value1)),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, value2))
        ));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.BOOLEAN));
    }

    @Test
    void test_evaluateType_nonEqualityComparison_wrongTypes() {
        // 2 > "2"
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, ">"), List.of(
            numNode,
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"2\""))
        ));

        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Cannot compare INT with STRING using >", error.getMessage());
    }

    private static Stream<Arguments> bitwiseProvider() {
        AbstractSyntaxTree num = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree bool = new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE));
        return Stream.of(
            Arguments.of(num, num),
            Arguments.of(bool, bool),
            Arguments.of(num, bool),
            Arguments.of(bool, num)
        );
    }
    @ParameterizedTest
    @MethodSource("bitwiseProvider")
    void test_evaluateType_bitwise_simple(AbstractSyntaxTree left, AbstractSyntaxTree right) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "^"), List.of(left, right));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.INT));
    }

    @Test
    void test_evaluateType_bitwise_complex() {
        // 2 ^ (2 < 2)
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "^"), List.of(
            numNode,
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, ">"), List.of(
                numNode,
                numNode
            ))
        ));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.INT));
    }

    @Test
    void test_evaluateType_bitwise_invalid() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "&"), List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FALSE)),
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"false\""))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Binary expression (&) with BOOLEAN and STRING is not valid", error.getMessage());
    }

    private static Stream<Arguments> multiplicationOperatorProvider() {
        AbstractSyntaxTree intNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree floatNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2.78"));
        AbstractSyntaxTree stringNode = new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"a\""));
        return Stream.of(
            Arguments.of(intNode, intNode, NodeType.INT),
            Arguments.of(floatNode, floatNode, NodeType.FLOAT),
            Arguments.of(intNode, floatNode, NodeType.FLOAT),
            Arguments.of(floatNode, intNode, NodeType.FLOAT),
            Arguments.of(stringNode, intNode, NodeType.STRING),
            Arguments.of(intNode, stringNode, NodeType.STRING)
        );
    }
    @ParameterizedTest
    @MethodSource("multiplicationOperatorProvider")
    void test_evaluateType_multiplicationOperator_simple(AbstractSyntaxTree left, AbstractSyntaxTree right, NodeType expectedType) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(left, right));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(expectedType));
    }

    @Test
    void test_evaluateType_multiplicationOperator_wrongMix() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3.14")),
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"a\""))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Cannot multiply FLOAT with STRING", error.getMessage());
    }

    @Test
    void test_evaluateType_multiplicationOperator_completelyWrongTypes() {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, ">"), List.of(
                numNode,
                numNode
            ))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Cannot multiply NULL with BOOLEAN", error.getMessage());
    }

    @Test
    void test_estimateArrayTypes_emptyArray() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT");
        assertTrue(semanticAnalyzer.estimateArrayTypes(AST).isType(NodeType.ARRAY));
    }

    @Test
    void test_estimateArrayTypes_simpleIntArray() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.INT), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleArrayContainsNull() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.INT), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleArrayContainsExpression() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "%"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2.78")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            )),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3.14"))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.FLOAT), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_nestedEmptyArrays() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT")
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.ARRAY), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_nestedArrays() {
        // { { {"Typescript"}, {"C", "C++"} }, { {"PostgreSQL", "MongoDB"} } }
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"Typescript\""))
                )),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"C\"")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"C++\""))
                ))
            )),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"PostgreSQL\"")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"MongoDB\""))
                ))
            ))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.ARRAY, NodeType.ARRAY, NodeType.STRING), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_mixedDepths() {
        // { {}, { {true, true}, {} }, { {}, {true} } }
        AbstractSyntaxTree trueNode = new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE));
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    trueNode,
                    trueNode
                )),
                new AbstractSyntaxTree("ARRAY-LIT")
            )),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT"),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(trueNode))
            ))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.ARRAY, NodeType.ARRAY, NodeType.BOOLEAN), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleArrayError() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"2\"")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.estimateArrayTypes(AST));
        assertEquals(error.getMessage(), "Cannot mix INT elements with STRING elements in array literal");
    }

    @Test
    void test_estimateArrayTypes_improperNestingError() {
        // {1, {1, 1}, 1}
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            numNode,
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                numNode,
                numNode
            )),
            numNode
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.estimateArrayTypes(AST));
        assertEquals(error.getMessage(), "Cannot mix non-array elements with nested array elements in array literal");
    }

    @Test
    void test_functionReturns_voidNoExplicitReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        assertFalse(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    @Test
    void test_functionReturns_voidExplicitReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            )),
            new AbstractSyntaxTree("CONTROL-FLOW", new StaticToken(TokenType.KW_RET))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        assertTrue(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    @Test
    void test_functionReturns_voidExplicitNullReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            )),
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Cannot return value from void function", error.getMessage());
    }

    @Test
    void test_functionReturns_voidReturnTypeError() {
        // { return 2 + 2 }, NONE
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
                ))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Cannot return value from void function", error.getMessage());
    }

    @Test
    void test_functionReturns_returnWrongPlace() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET))
            )),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        UnreachableCodeError error = assertThrows(UnreachableCodeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Unreachable statement following return", error.getMessage());
    }

    @Test
    void test_functionReturns_nonVoidProperReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
                ))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        assertTrue(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    @Test
    void test_functionReturns_nonVoidReturnTypeError() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"+\""))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Expected INT to be returned but got STRING", error.getMessage());
    }

    @Test
    void test_functionReturns_nonVoidMissingReturnValue() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Expected return type INT but didn't return a value", error.getMessage());
    }

    @Test
    void test_functionReturns_nonVoidMissingReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", new StaticToken(TokenType.KW_TRUE));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        assertFalse(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    @Test
    void test_conditionalBlockReturns_simple_true() {
        AbstractSyntaxTree conditionalBlock = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_IF), List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE))
                    ))
                ))
            )),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_ELSE), List.of(
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_FALSE))
                    ))
                ))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.BOOLEAN);
        assertTrue(semanticAnalyzer.conditionalBlockReturns(conditionalBlock, expectedReturnType));
    }

    @Test
    void test_conditionalBlockReturns_simple_false() {
        AbstractSyntaxTree conditionalBlock = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_IF), List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE))
                    ))
                ))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.BOOLEAN);
        assertFalse(semanticAnalyzer.conditionalBlockReturns(conditionalBlock, expectedReturnType));
    }

    @Test
    void test_generic_var_outside_function() {
        AbstractSyntaxTree declaration = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL", new StaticToken(TokenType.KW_GEN), new VariableToken(TokenType.ID, "var"))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(declaration));
        assertEquals("Cannot use generic variable outside of function definition", error.getMessage());
    }

    @Test
    void test_generic_return_fn_call() {
        // Array<int> nums[2] = {2, 4}; pop(nums);
        AbstractSyntaxTree fnCall = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "nums")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.NUMBER, "4"))
            )),
            new AbstractSyntaxTree("FUNC-CALL", List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "pop")),
                new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "nums"))
            ))
        ));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(fnCall));
    }

    @Test
    void test_generic_return_fn_call_assignment() {
        // Array<int> nums[2] = {2, 4}; int num = pop(nums);
        AbstractSyntaxTree fnCall = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "nums")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.NUMBER, "4"))
            )),
            new AbstractSyntaxTree("VAR-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "num")),
                new AbstractSyntaxTree("FUNC-CALL", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.ID, "pop")),
                    new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "nums"))
                ))
            ))

        ));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(fnCall));
    }
}
