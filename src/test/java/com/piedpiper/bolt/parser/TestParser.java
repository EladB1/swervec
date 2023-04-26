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
    void test_parseArrayAccess_succeedsWithArrayIndex() { // TODO: find a way to clean up testing trees
        List<Token> tokens = List.of(
            new Token(TokenType.ID, "x"),
            new Token(TokenType.LEFT_SQB, "["),
            new Token(TokenType.NUMBER, "12"),
            new Token(TokenType.RIGHT_SQB, "]")
        );
        Parser parser = new Parser(tokens);
        ParseTree node = parser.parseArrayAccess();
        assertEquals(2, node.getChildren().size());
        assertEquals("ARRAYACCESS", node.getType());
        assertEquals(tokens.get(0), node.getChildren().get(0).getToken());
        ParseTree arrayIndex = node.getChildren().get(1);
        assertEquals("ARRAYINDEX", arrayIndex.getType());
        List<ParseTree> leafNodes = arrayIndex.getChildren();
        assertEquals(3, leafNodes.size());
        for (int i = 1; i < leafNodes.size(); i++) { // start from 1 to skip over the first value of tokens
            assertEquals(tokens.get(i), leafNodes.get(i-1).getToken());
            assertEquals(0, leafNodes.get(i-1).getChildren().size());
        }
    }

    @Test
    void test_parseArrayAccess_succeedsWithoutArrayIndex() {
        List<Token> tokens = List.of(
            new Token(TokenType.ID, "x")
        );
        Parser parser = new Parser(tokens);
        ParseTree node = parser.parseArrayAccess();
        assertEquals(1, node.getChildren().size());
        assertEquals(tokens.get(0), node.getChildren().get(0).getToken());
        assertEquals(0, node.getChildren().get(0).getChildren().size());
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
        ParseTree node = parser.parseArrayIndex();
        assertEquals(3, node.getChildren().size());
        for (int i = 0; i < node.getChildren().size(); i++) {
            assertEquals(tokens.get(i), node.getChildren().get(i).getToken());
            assertEquals(0, node.getChildren().get(i).getChildren().size());
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
