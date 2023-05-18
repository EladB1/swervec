package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
}
