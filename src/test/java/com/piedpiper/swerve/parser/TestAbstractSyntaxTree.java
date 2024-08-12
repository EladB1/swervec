package com.piedpiper.swerve.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.piedpiper.swerve.lexer.StaticToken;
import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.lexer.VariableToken;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;

public class TestAbstractSyntaxTree {
    @Test
    void test_appendChildren() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree("FUNC-CALL");
        assertFalse(tree.hasChildren());
        tree.appendChildren(new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length")));
        assertTrue(tree.hasChildren());
        assertEquals(tree.countChildren(), 1);
    }

    @Test
    void test_matchesLabel_true() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree("FUNC-CALL");
        assertTrue(tree.matchesLabel("FUNC-CALL"));
    }

    @Test
    void test_matchesLabel_false() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length"));
        assertFalse(tree.matchesLabel("FUNC-CALL"));
    }

    @Test
    void test_matchesStaticToken_true() {
        StaticToken token = new StaticToken(TokenType.KW_TRUE);
        AbstractSyntaxTree tree = new AbstractSyntaxTree(token);
        assertTrue(tree.matchesStaticToken(TokenType.KW_TRUE));
    }

    @Test
    void test_matchesStaticToken_false() {
        StaticToken token = new StaticToken(TokenType.KW_TRUE);
        AbstractSyntaxTree tree = new AbstractSyntaxTree(token);
        assertFalse(tree.matchesStaticToken(TokenType.KW_FALSE));
    }

    @Test
    void test_matchesValue_true() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length"));
        assertTrue(tree.matchesValue("length"));
    }

    @Test
    void test_matchesValue_false() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree("FUNC-CALL");
        assertFalse(tree.matchesValue("length"));
    }

    @Test
    void test_isTypeLabel_true() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new StaticToken(TokenType.KW_BOOL));
        assertTrue(tree.isTypeLabel());
    }

    @Test
    void test_isTypeLabel_false() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new StaticToken(TokenType.KW_FOR));
        assertFalse(tree.isTypeLabel());
    }

    @Test
    void test_isTypeLabel_false_label() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree("ARRAY-LIT");
        assertFalse(tree.isTypeLabel());
    }

    @Test
    void test_toString_singleNodeNonTerminalTree() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree("STMNT");
        String treeString = tree.toString();
        assertEquals("AST => STMNT", treeString);
    }

    @Test
    void test_toString_singleNodeStaticTerminalTree() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new StaticToken(TokenType.LEFT_PAREN));
        String treeString = tree.toString();
        assertEquals("AST => Token: LEFT_PAREN", treeString);
    }

    @Test
    void test_toString_singleNodeVariableTerminalTree() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "isOpen"));
        String treeString = tree.toString();
        assertEquals("AST => Token: ID ('isOpen')", treeString);
    }

    @Test
    void test_toString_singleNodeStaticTerminalTreeWithLineNum() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new StaticToken(TokenType.LEFT_PAREN, 10));
        String treeString = tree.toString();
        assertEquals("AST => Token: LEFT_PAREN, line: 10", treeString);
    }

    @Test
    void test_toString_singleNodeVariableTerminalTreeWithLineNum() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "isOpen", 4));
        String treeString = tree.toString();
        assertEquals("AST => Token: ID ('isOpen'), line: 4", treeString);
    }

    @Test
    void test_toString_multiLevelTree() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree("ARRAYINDEX", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.LEFT_SQB)),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "51")),
            new AbstractSyntaxTree(new StaticToken(TokenType.RIGHT_SQB))
        ));
        String treeString = tree.toString();
        String expectedString = 
            "AST => ARRAYINDEX, children: [\n" +
            "\tAST => Token: LEFT_SQB\n" +
            "\tAST => Token: NUMBER ('51')\n" +
            "\tAST => Token: RIGHT_SQB\n" +
            "]";
        
        assertEquals(expectedString, treeString);
    }

    @Test
    void test_toString_multiLevelTreeWithLineNumbers() {
        AbstractSyntaxTree tree = new AbstractSyntaxTree("ARRAYACCESS", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "someVarName", 1)),
            new AbstractSyntaxTree("ARRAYINDEX", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.LEFT_SQB, 1)),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "51", 2)),
                new AbstractSyntaxTree(new StaticToken(TokenType.RIGHT_SQB, 3))
            )
        )));
        String treeString = tree.toString();
        String expectedString =
            "AST => ARRAYACCESS, children: [\n" +
            "\tAST => Token: ID ('someVarName'), line: 1\n" +
            "\tAST => ARRAYINDEX, children: [\n" +
            "\t\tAST => Token: LEFT_SQB, line: 1\n" +
            "\t\tAST => Token: NUMBER ('51'), line: 2\n" +
            "\t\tAST => Token: RIGHT_SQB, line: 3\n" +
            "\t]\n" +
            "]";
        
        assertEquals(expectedString, treeString);
    }
}
