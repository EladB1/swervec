package com.piedpiper.bolt.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;

public class TestParser {
    @Test
    void test_parseArrayAccess_succeedsWithArrayIndex() {
        List<Token> tokens = List.of(
            new Token(TokenType.ID, "x"),
            new Token(TokenType.LEFT_SQB, "["),
            new Token(TokenType.NUMBER, "12"),
            new Token(TokenType.RIGHT_SQB, "]")
        );
        Parser parser = new Parser(tokens);
        List<ParseTree> nodes = parser.parseArrayAccess();
        assertEquals(4, nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            assertEquals(tokens.get(i), nodes.get(i).getToken());
            assertEquals(0, nodes.get(i).getChildren().size());
        }
    }

    @Test
    void test_parseArrayAccess_succeedsWithoutArrayIndex() {
        List<Token> tokens = List.of(
            new Token(TokenType.ID, "x")
        );
        Parser parser = new Parser(tokens);
        List<ParseTree> nodes = parser.parseArrayAccess();
        assertEquals(1, nodes.size());
        assertEquals(tokens.get(0), nodes.get(0).getToken());
        assertEquals(0, nodes.get(0).getChildren().size());
    }

    @Test
    void test_parseArrayAccess_EOFError() {
        List<Token> tokens = List.of(
            new Token(TokenType.ID, "x"),
            new Token(TokenType.LEFT_SQB, "[")
        );
        Parser parser = new Parser(tokens);
        SyntaxError exception = assertThrows(SyntaxError.class, () -> parser.parseArrayAccess());
        assertEquals("Expected NUMBER but reached EOF", exception.getMessage());
    }

    @Test
    void test_parseArrayAccess_wrongTokenError() {
        List<Token> tokens = List.of(
            new Token(TokenType.ID, "x"),
            new Token(TokenType.LEFT_PAREN, "(")
        );
        Parser parser = new Parser(tokens);
        SyntaxError exception = assertThrows(SyntaxError.class, () -> parser.parseArrayAccess());
        assertEquals("Expected LEFT_SQB but got LEFT_PAREN ('(')", exception.getMessage());
    }

    @Test
    void test_parseArrayIndex_succeeds() {
        List<Token> tokens = List.of(
            new Token(TokenType.LEFT_SQB, "["),
            new Token(TokenType.NUMBER, "5"),
            new Token(TokenType.RIGHT_SQB, "]")
        );
        Parser parser = new Parser(tokens);
        List<ParseTree> nodes = parser.parseArrayIndex();
        assertEquals(3, nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            assertEquals(tokens.get(i), nodes.get(i).getToken());
            assertEquals(0, nodes.get(i).getChildren().size());
        }
    }

    @Test
    void test_parseArrayIndex_EOFError() {
        List<Token> tokens = List.of(
            new Token(TokenType.LEFT_SQB, "["),
            new Token(TokenType.NUMBER, "3")
        );
        Parser parser = new Parser(tokens);
        SyntaxError exception = assertThrows(SyntaxError.class, () -> parser.parseArrayIndex());
        assertEquals("Expected RIGHT_SQB but reached EOF", exception.getMessage());
    }

    @Test
    void test_parseArrayIndex_wrongTokenError() {
        List<Token> tokens = List.of(
            new Token(TokenType.LEFT_SQB, "["),
            new Token(TokenType.STRING, "\"value\""),
            new Token(TokenType.RIGHT_SQB, "]")
        );
        Parser parser = new Parser(tokens);
        SyntaxError exception = assertThrows(SyntaxError.class, () -> parser.parseArrayIndex());
        assertEquals("Expected NUMBER but got STRING ('\"value\"')", exception.getMessage());
    }
}
