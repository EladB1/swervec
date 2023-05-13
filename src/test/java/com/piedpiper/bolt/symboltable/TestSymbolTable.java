package com.piedpiper.bolt.symboltable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.lexer.TokenType;

public class TestSymbolTable {
    private SymbolTable table = new SymbolTable();

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
        Symbol symbol = new Symbol(1, "var", TokenType.KW_STR);
        table.insert(symbol);
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(symbol, storedSymbol);
    }

    @Test
    void test_insertAndLookup_previousScope() {
        Symbol symbol = new Symbol(1, "var", TokenType.KW_STR);
        table.insert(symbol);
        table.enterScope();
        table.enterScope();
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(symbol, storedSymbol);
    }

    @Test
    void test_insertAndLookup_sameNameDifferentScopes() {
        table.insert(new Symbol(1, "var", TokenType.KW_STR));
        table.enterScope();
        table.insert(new Symbol(2, "var", TokenType.KW_STR));
        table.enterScope();
        Symbol lastVar = new Symbol(3, "var", TokenType.KW_STR);
        table.insert(lastVar);
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(lastVar, storedSymbol);
    }

    @Test
    void test_insert_sameNameSameScope() {
        Symbol var = new Symbol(1, "var", TokenType.KW_INT);
        table.insert(var);
        NameError error = assertThrows(NameError.class, () -> table.insert(var));
        assertEquals("Symbol 'var' is already defined in this scope", error.getMessage());
    }

    @Test
    void test_insert_redefineBuiltinScope() {
        table.insert(new Symbol(0, "print", TokenType.KW_FN));
        NameError error = assertThrows(NameError.class, () -> table.insert(new Symbol(2, "print", TokenType.KW_FN)));
        assertEquals("Symbol 'print' is already defined in the built-in scope", error.getMessage());
    }
}
