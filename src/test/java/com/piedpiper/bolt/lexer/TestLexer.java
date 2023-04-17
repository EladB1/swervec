package com.piedpiper.bolt.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TestLexer {
    private Lexer lexer = new Lexer();

    @Test
    void analyzeLine_shouldRecognizeInteger() {
        List<Token> tokens = lexer.analyzeLine("10");
        Token token = new Token("NUMBER", "10");
        assertEquals(1, tokens.size());
        assertEquals(token.getName(), tokens.get(0).getName());
        assertEquals(token.getValue(), tokens.get(0).getValue());
    }

    @Test
    void analyzeLine_shouldRecognizeFloat() {
        List<Token> tokens = lexer.analyzeLine("1.0");
        Token token = new Token("NUMBER", "1.0");
        assertEquals(1, tokens.size());
        assertEquals(token.getName(), tokens.get(0).getName());
        assertEquals(token.getValue(), tokens.get(0).getValue());
    }

    @Test
    void analyzeLine_shouldOnlyRecognizeInteger() {
        List<Token> tokens = lexer.analyzeLine("(2)");
        Token token = new Token("NUMBER", "2");
        assertEquals(1, tokens.size());
        assertEquals(token.getName(), tokens.get(0).getName());
        assertEquals(token.getValue(), tokens.get(0).getValue());
    }

    @Test
    void analyzeLine_shouldNotRecognizeBrokenFloat() {
        List<Token> tokens = lexer.analyzeLine("5.");
        assertEquals(0, tokens.size());
    }

    @Test
    void analyzeLine_shouldRecognizeNumberAddition() {
        List<Token> tokens = lexer.analyzeLine("14 + 19");
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "14"), 
            new Token("OP", "+"),
            new Token("NUMBER", "19")
        );
        assertEquals(3, tokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i).getName(), tokens.get(i).getName());
            assertEquals(expectedTokens.get(i).getValue(), tokens.get(i).getValue());
        }
    }

    @Test
    void analyzeLine_shouldRecognizeNumberIncrement() {
        List<Token> tokens = lexer.analyzeLine("5++");
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "5"), 
            new Token("OP", "++")
        );
        assertEquals(2, tokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i).getName(), tokens.get(i).getName());
            assertEquals(expectedTokens.get(i).getValue(), tokens.get(i).getValue());
        }
    }

    @Test
    void analyzeLine_shouldRecognizeString() {
        List<Token> tokens = lexer.analyzeLine("\"Hello\"");
        Token token = new Token("STRING", "\"Hello\"");
        assertEquals(1, tokens.size());
        assertEquals(token.getName(), tokens.get(0).getName());
        assertEquals(token.getValue(), tokens.get(0).getValue());
    }

    @Test
    void analyzeLine_shouldNotRecognizeIncompleteString() {
        List<Token> tokens = lexer.analyzeLine("\"Hello");
        assertEquals(0, tokens.size());
    }

    @Test
    void analyzeLine_shouldRecognizeIdentifierAndInteger() {
        List<Token> tokens = lexer.analyzeLine("print(2)");
        List<Token> expectedTokens = List.of(
            new Token("ID", "print"),
            new Token("NUMBER", "2")
        );
        assertEquals(2, tokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i).getName(), tokens.get(i).getName());
            assertEquals(expectedTokens.get(i).getValue(), tokens.get(i).getValue());
        }
    }

    @Test
    void analyzeLine_shouldRecognizeExpression() {
        List<Token> tokens = lexer.analyzeLine("const float PI = 3.14");
        List<Token> expectedTokens = List.of(
            new Token("ID", "const"),
            new Token("ID", "float"),
            new Token("ID", "PI"),
            new Token("OP", "="),
            new Token("NUMBER", "3.14")
        );
        assertEquals(5, tokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i).getName(), tokens.get(i).getName());
            assertEquals(expectedTokens.get(i).getValue(), tokens.get(i).getValue());
        }
    }

    @Test 
    void analyzeLine_shouldRecognizeExpressionWithStrings() {
        List<Token> tokens = lexer.analyzeLine("languages += \"Korean\" + \"Portuguese\"");
        List<Token> expectedTokens = List.of(
            new Token("ID", "languages"),
            new Token("OP", "+="),
            new Token("STRING", "\"Korean\""),
            new Token("OP", "+"),
            new Token("STRING", "\"Portuguese\"")
        );
        assertEquals(5, tokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i).getName(), tokens.get(i).getName());
            assertEquals(expectedTokens.get(i).getValue(), tokens.get(i).getValue());
        }
    }
}
