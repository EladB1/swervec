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

    @Test
    void test_semanticAnalyzer_intAddition() {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
              numNode,
              numNode
            ))
        ));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    @Test
    void test_semanticAnalyzer_additionError() {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                numNode,
                new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"2\""))
            ))
        ));
        NameError error = assertThrows(NameError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Some error", error.getMessage());
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

    private static Stream<Arguments> binaryOperatorProvider() {
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
    @MethodSource("binaryOperatorProvider")
    void test_evaluateType_binaryOperator_simple(AbstractSyntaxTree left, AbstractSyntaxTree right) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "^"), List.of(left, right));
        assertEquals(NodeType.INT, semanticAnalyzer.evaluateType(AST));
    }

    @Test
    void test_evaluateType_binaryOperator_complex() {
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
    void test_evaluateType_binaryOperator_invalid() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "&"), List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FALSE)),
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"false\""))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Binary expression (&) with BOOLEAN and STRING is not valid", error.getMessage());
    }
}
