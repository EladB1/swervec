package com.piedpiper.bolt.symboltable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.VariableToken;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.lexer.TokenType;

import java.util.List;

public class TestSymbolTable {
    private SymbolTable table;

    private final AbstractSyntaxTree typeNode = new AbstractSyntaxTree(new StaticToken(TokenType.KW_STR));

    @BeforeEach
    void setUp() {
        table = new SymbolTable();
    }

    @Test
    void test_getScopeLevel_default() {
        assertEquals(1, table.getScopeLevel());
    }

    @Test
    void test_enterScope_incrementsScope() {
        assertEquals(1, table.getScopeLevel());
        table.enterScope();
        assertEquals(2, table.getScopeLevel());
    }

    @Test
    void test_leaveScope_doesNothingOnGlobalScope() {
        table.leaveScope();
        assertEquals(1, table.getScopeLevel());
    }

    @Test
    void test_leaveScope_decrementsScope() {
        table.enterScope();
        assertEquals(2, table.getScopeLevel());
        table.leaveScope();
        assertEquals(1, table.getScopeLevel());
    }

    @Test
    void test_lookup_notFound() {
        assertNull(table.lookup("var"));
    }

    @Test
    void test_insertAndLookup_basic() {
        Symbol symbol = new Symbol("var", typeNode, 1);
        table.insert(symbol);
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(symbol, storedSymbol);
    }

    @Test
    void test_insertAndLookup_previousScope() {
        Symbol symbol = new Symbol("var", typeNode, 1);
        table.insert(symbol);
        table.enterScope();
        table.enterScope();
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(symbol, storedSymbol);
    }

    @Test
    void test_insertAndLookup_sameNameDifferentScopes() {
        table.insert(new Symbol("var", typeNode, 1));
        table.enterScope();
        table.insert(new Symbol("var", typeNode, 2));
        table.enterScope();
        Symbol lastVar = new Symbol("var", typeNode, 3);
        table.insert(lastVar);
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(lastVar, storedSymbol);
    }

    @Test
    void test_insert_sameNameSameScope() {
        Symbol var = new Symbol("var", typeNode, 1);
        table.insert(var);
        NameError error = assertThrows(NameError.class, () -> table.insert(var));
        assertEquals("Symbol 'var' is already defined in this scope", error.getMessage());
    }

    @Test
    void test_insertAndLookup_functionFound() {
        AbstractSyntaxTree funcBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"\""))
            ))
        ));
        AbstractSyntaxTree[] params = {};
        FunctionSymbol function = new FunctionSymbol("test", typeNode, funcBody);
        table.insert(function);
        FunctionSymbol storedSymbol = table.lookup("test", params);
        assertNotNull(storedSymbol);
        assertEquals(function, storedSymbol);
    }

    @Test
    void test_insertAndLookup_wrongParamTypesReturnNull() {
        AbstractSyntaxTree funcBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"\""))
            ))
        ));
        AbstractSyntaxTree[] params = {typeNode};
        FunctionSymbol function = new FunctionSymbol("test", typeNode, funcBody);
        table.insert(function);
        FunctionSymbol storedSymbol = table.lookup("test", params);
        assertNull(storedSymbol);
    }

    @Test
    void test_lookup_functionNotFound() {
        AbstractSyntaxTree[] params = {typeNode};
        FunctionSymbol storedSymbol = table.lookup("test", params);
        assertNull(storedSymbol);
    }

    @Test
    void test_insertAndLookup_functionWithParams() {
        AbstractSyntaxTree[] params = {typeNode, typeNode};
        FunctionSymbol fnSymbol = new FunctionSymbol("concat", params);
        table.insert(fnSymbol);
        FunctionSymbol symbol = table.lookup("concat", params);
        assertNotNull(symbol);
        assertEquals(fnSymbol, symbol);
    }

    @Test
    void test_insert_multipleFunctions() {
        AbstractSyntaxTree[] params1 = {typeNode, typeNode};
        AbstractSyntaxTree[] params2 = {typeNode, typeNode, typeNode};
        FunctionSymbol fnSymbol1 = new FunctionSymbol("concat", params1);
        FunctionSymbol fnSymbol2 = new FunctionSymbol("concat", params2);
        table.insert(fnSymbol1);
        table.insert(fnSymbol2);
        FunctionSymbol symbol1 = table.lookup("concat", params1);
        assertNotNull(symbol1);
        assertEquals(fnSymbol1, symbol1);
        FunctionSymbol symbol2 = table.lookup("concat", params2);
        assertNotNull(symbol2);
        assertEquals(fnSymbol2, symbol2);
    }

    @Test
    void test_insert_redefineFunction_noParams() {
        table.insert(new FunctionSymbol("test"));
        NameError error = assertThrows(NameError.class, () -> table.insert(new FunctionSymbol("test")));
        assertEquals("Function 'test()' is already defined", error.getMessage());
    }
}
