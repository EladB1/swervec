package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.error.TypeError;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSemanticAnalyzer {
    private final SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
    private final Token intTypeToken = new StaticToken(TokenType.KW_INT);
    private final Token genericVarToken = new VariableToken(TokenType.ID, "var");

    @Test
    void test_semanticAnalyzer_varDecl_noValue() {
        AbstractSyntaxTree AST = AbstractSyntaxTree.createNestedTree(List.of(intTypeToken, genericVarToken), "PROGRAM", "VAR-DECL");
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    @Test
    void test_semanticAnalyzer_varDecl_sameNameSameScope() {
        AbstractSyntaxTree varDeclaration = AbstractSyntaxTree.createNestedTree(List.of(intTypeToken, genericVarToken), "VAR-DECL");
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
            numNode //new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"2\""))
        ));
        assertEquals(NodeType.BOOLEAN, semanticAnalyzer.evaluateType(AST));
    }

    @ParameterizedTest
    @CsvSource({"2, 2.0", "2.0, 2"})
    void test_evaluateType_nonEqualityComparison_mixedTypes(String value1, String value2) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<="), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, value1)),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, value2))
        ));
        assertEquals(NodeType.BOOLEAN, semanticAnalyzer.evaluateType(AST));
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
        assertEquals(NodeType.INT, semanticAnalyzer.evaluateType(AST));
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
        assertEquals(NodeType.INT, semanticAnalyzer.evaluateType(AST));
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
        assertEquals(expectedType, semanticAnalyzer.evaluateType(AST));
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
        assertEquals(List.of(NodeType.ARRAY), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleIntArray() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        assertEquals(List.of(NodeType.ARRAY, NodeType.INT), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleArrayContainsNull() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        assertEquals(List.of(NodeType.ARRAY, NodeType.INT), semanticAnalyzer.estimateArrayTypes(AST));
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
        assertEquals(List.of(NodeType.ARRAY, NodeType.FLOAT), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_nestedEmptyArrays() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT")
        ));
        assertEquals(List.of(NodeType.ARRAY, NodeType.ARRAY), semanticAnalyzer.estimateArrayTypes(AST));
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
        assertEquals(List.of(NodeType.ARRAY, NodeType.ARRAY, NodeType.ARRAY, NodeType.STRING), semanticAnalyzer.estimateArrayTypes(AST));
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
        assertEquals(List.of(NodeType.ARRAY, NodeType.ARRAY, NodeType.ARRAY, NodeType.BOOLEAN), semanticAnalyzer.estimateArrayTypes(AST));
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
}
