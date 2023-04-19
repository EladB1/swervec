package com.piedpiper.bolt.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.piedpiper.error.SyntaxError;

public class TestLexer {
    private Lexer lexer = new Lexer();

    void assertZeroTokens(String line) {
        List<Token> tokens = lexer.analyzeLine(line);
        assertEquals(0, tokens.size());
    }

    void assertOneToken(String line, Token expectedToken) {
        List<Token> tokens = lexer.analyzeLine(line);
        assertEquals(1, tokens.size());
        assertEquals(expectedToken, tokens.get(0));

    }

    void assertManyTokens(String line, List<Token> expectedTokens) {
        List<Token> tokens = lexer.analyzeLine(line);
        assertEquals(expectedTokens.size(), tokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i), tokens.get(i));
        }
    }

    @Test
    void analyzeLine_shouldRecognizeInteger() {
        Token token = new Token("NUMBER", "10");
        assertOneToken("10", token);
    }

    @Test
    void analyzeLine_shouldRecognizeFloat() {
        Token token = new Token("NUMBER", "1.0");
        assertOneToken("1.0", token);
    }

    @Test
    void analyzeLine_shouldOnlyRecognizeInteger() {
        Token token = new Token("NUMBER", "2");
        assertOneToken("(2)", token);
    }

    @Test
    void analyzeLine_shouldSkipWhitespaceOnLastChar() {
        Token token = new Token("NUMBER", "2");
        assertOneToken("2 ", token);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.", "15. + 3.9", "0..5"})
    void analyzeLine_shouldNotThrowErrorOnBrokenFloat(String input) {
        assertThrows(SyntaxError.class, () -> lexer.analyzeLine(input));
    }

    @Test
    void analyzeLine_shouldRecognizeNumberAddition() {
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "14"), 
            new Token("OP", "+"),
            new Token("NUMBER", "19")
        );
        assertManyTokens("14 + 19", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeNumberDivision() {
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "144"), 
            new Token("OP", "/"),
            new Token("NUMBER", "12")
        );
        assertManyTokens("144/12", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeNumberIncrement() {
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "5"), 
            new Token("OP", "++")
        );
        assertManyTokens("5++", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeString() {
        Token token = new Token("STRING", "\"Hello\"");
        assertOneToken("\"Hello\"", token);
    }

    @Test
    void analyzeLine_shouldNotRecognizeIncompleteString() {
        assertThrows(SyntaxError.class, () -> lexer.analyzeLine("\"Hello"));
    }

    @Test
    void analyzeLine_shouldRecognizeIdentifierAndInteger() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "print"),
            new Token("NUMBER", "2")
        );
        assertManyTokens("print(2)", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeExpression() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "const"),
            new Token("ID", "float"),
            new Token("ID", "PI"),
            new Token("OP", "="),
            new Token("NUMBER", "3.14")
        );
        assertManyTokens("const float PI = 3.14", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeComplexExpression() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "float"),
            new Token("ID", "y"),
            new Token("OP", "="),
            new Token("ID", "someInteger"),
            new Token("OP", "*"),
            new Token("ID", "PI"),
            new Token("OP", "+"),
            new Token("NUMBER", "1")
        );
        assertManyTokens("float y = someInteger * PI + 1", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeExpressionWithoutSpace() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "int"),
            new Token("ID", "y"),
            new Token("OP", "="),
            new Token("NUMBER", "2"),
            new Token("OP", "+"),
            new Token("NUMBER", "3")
        );
        assertManyTokens("int y=2+3", expectedTokens);
    }

    @Test 
    void analyzeLine_shouldRecognizeExpressionWithStrings() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "languages"),
            new Token("OP", "+="),
            new Token("STRING", "\"Korean\""),
            new Token("OP", "+"),
            new Token("STRING", "\"Portuguese\"")
        );
        assertManyTokens("languages += \"Korean\" + \"Portuguese\"", expectedTokens);
    }

    @Test
    void analyzeLine_shouldIgnoreStandaloneInlineComment() {
        assertZeroTokens("// this is a comment");
    }

    @Test
    void analyzeLine_shouldIgnoreInlineComment() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "y"),
            new Token("OP", "++")
        );
        assertManyTokens("y++ // y+= 1", expectedTokens);
    }
}
