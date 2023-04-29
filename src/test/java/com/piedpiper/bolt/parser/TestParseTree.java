package com.piedpiper.bolt.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;

public class TestParseTree {
    @Test
    void test_toString_singleNodeNonTerminalTree() {
        ParseTree tree = new ParseTree("STMNT");
        String treeString = tree.toString();
        assertEquals("ParseTree => STMNT", treeString);
    }

    @Test
    void test_toString_singleNodeStaticTerminalTree() {
        ParseTree tree = new ParseTree(new StaticToken(TokenType.LEFT_PAREN));
        String treeString = tree.toString();
        assertEquals("ParseTree => Token: LEFT_PAREN", treeString);
    }

    @Test
    void test_toString_singleNodeVariableTerminalTree() {
        ParseTree tree = new ParseTree(new VariableToken(TokenType.ID, "isOpen"));
        String treeString = tree.toString();
        assertEquals("ParseTree => Token: ID ('isOpen')", treeString);
    }

    @Test
    void test_toString_singleNodeStaticTerminalTreeWithLineNum() {
        ParseTree tree = new ParseTree(new StaticToken(TokenType.LEFT_PAREN, 10));
        String treeString = tree.toString();
        assertEquals("ParseTree => Token: LEFT_PAREN, line: 10", treeString);
    }

    @Test
    void test_toString_singleNodeVariableTerminalTreeWithLineNum() {
        ParseTree tree = new ParseTree(new VariableToken(TokenType.ID, "isOpen", 4));
        String treeString = tree.toString();
        assertEquals("ParseTree => Token: ID ('isOpen'), line: 4", treeString);
    }

    @Test
    void test_toString_multiLevelTree() {
        ParseTree tree = new ParseTree("ARRAYINDEX", List.of(
            new ParseTree(new StaticToken(TokenType.LEFT_SQB)),
            new ParseTree(new VariableToken(TokenType.NUMBER, "51")),
            new ParseTree(new StaticToken(TokenType.RIGHT_SQB))
        ));
        String treeString = tree.toString();
        String expectedString = 
            "ParseTree => ARRAYINDEX, children: [\n" +
            "\tParseTree => Token: LEFT_SQB\n" +
            "\tParseTree => Token: NUMBER ('51')\n" +
            "\tParseTree => Token: RIGHT_SQB\n" +
            "]";
        
        assertEquals(expectedString, treeString);
    }

    @Test
    void test_toString_multiLevelTreeWithLineNumbers() {
        ParseTree tree = new ParseTree("ARRAYACCESS", List.of(
            new ParseTree(new VariableToken(TokenType.ID, "someVarName", 1)),
            new ParseTree("ARRAYINDEX", List.of(
                new ParseTree(new StaticToken(TokenType.LEFT_SQB, 1)),
                new ParseTree(new VariableToken(TokenType.NUMBER, "51", 2)),
                new ParseTree(new StaticToken(TokenType.RIGHT_SQB, 3))
            )
        )));
        String treeString = tree.toString();
        String expectedString =
            "ParseTree => ARRAYACCESS, children: [\n" + 
            "\tParseTree => Token: ID ('someVarName'), line: 1\n" +
            "\tParseTree => ARRAYINDEX, children: [\n" +
            "\t\tParseTree => Token: LEFT_SQB, line: 1\n" +
            "\t\tParseTree => Token: NUMBER ('51'), line: 2\n" +
            "\t\tParseTree => Token: RIGHT_SQB, line: 3\n" +
            "\t]\n" +
            "]";
        
        assertEquals(expectedString, treeString);
    }
}
