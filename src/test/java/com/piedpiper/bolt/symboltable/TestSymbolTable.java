package com.piedpiper.bolt.symboltable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.piedpiper.bolt.error.IllegalStatementError;
import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.VariableToken;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.semantic.EntityType;
import com.piedpiper.bolt.semantic.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.lexer.TokenType;

import java.util.List;

public class TestSymbolTable {
    private SymbolTable table;

    private final EntityType stringType = new EntityType(NodeType.STRING);
    private final EntityType intType = new EntityType(NodeType.INT);

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
        assertEquals(2, table.enterScope());
    }

    @Test
    void test_leaveScope_doesNothingOnGlobalScope() {
        assertEquals(1, table.leaveScope());
    }

    @Test
    void test_leaveScope_decrementsScope() {
        assertEquals(2, table.enterScope());
        assertEquals(1, table.leaveScope());
    }

    @Test
    void test_lookup_notFound() {
        assertNull(table.lookup("var"));
    }

    @Test
    void test_insertAndLookup_basic() {
        Symbol symbol = new Symbol("var", stringType, 1);
        table.insert(symbol);
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(symbol, storedSymbol);
    }

    @Test
    void test_insertAndLookup_previousScope() {
        Symbol symbol = new Symbol("var", stringType, 1);
        table.insert(symbol);
        table.enterScope();
        table.enterScope();
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(symbol, storedSymbol);
    }

    @Test
    void test_insertAndLookup_sameNameDifferentScopes() {
        table.insert(new Symbol("var", stringType, 1));
        table.enterScope();
        table.insert(new Symbol("var", stringType, 2));
        table.enterScope();
        Symbol lastVar = new Symbol("var", stringType, 3);
        table.insert(lastVar);
        Symbol storedSymbol = table.lookup("var");
        assertNotNull(storedSymbol);
        assertEquals(lastVar, storedSymbol);
    }

    @Test
    void test_insert_sameNameSameScope() {
        Symbol var = new Symbol("var", stringType, 1);
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
        EntityType[] params = {};
        FunctionSymbol function = new FunctionSymbol("test", stringType, funcBody);
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
        EntityType[] params = {stringType};
        FunctionSymbol function = new FunctionSymbol("test", stringType, funcBody);
        table.insert(function);
        FunctionSymbol storedSymbol = table.lookup("test", params);
        assertNull(storedSymbol);
    }

    @Test
    void test_lookup_functionNotFound() {
        EntityType[] params = {stringType};
        FunctionSymbol storedSymbol = table.lookup("test", params);
        assertNull(storedSymbol);
    }

    @Test
    void test_insertAndLookup_functionWithParams() {
        EntityType[] params = {stringType, stringType};
        FunctionSymbol fnSymbol = new FunctionSymbol("concat", params);
        table.insert(fnSymbol);
        FunctionSymbol symbol = table.lookup("concat", params);
        assertNotNull(symbol);
        assertEquals(fnSymbol, symbol);
    }

    @Test
    void test_insert_multipleFunctions() {
        EntityType[] params1 = {stringType, stringType};
        EntityType[] params2 = {stringType, stringType, stringType};
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

    @Test
    void test_get_builtin_function() {
        FunctionSymbol lengthFunction = table.lookup("length", new EntityType[]{stringType});
        assertNotNull(lengthFunction);
        assertTrue(lengthFunction.isBuiltIn());
        assertEquals(intType, lengthFunction.getReturnType());
    }

    @Test
    void test_builtin_name_reuse() {
        EntityType[] params = {stringType, stringType};
        FunctionSymbol lengthFunction = new FunctionSymbol("length", intType, params, false);
        NameError error = assertThrows(NameError.class, () -> table.insert(lengthFunction));
        assertEquals("Function 'length' is builtin so its name cannot be reused", error.getMessage());
    }

    @Test
    void test_calling_generic_function() {
        EntityType[] paramTypes = new EntityType[] {new EntityType(NodeType.ARRAY, NodeType.STRING)};
        FunctionSymbol popFn = table.lookup("pop", paramTypes);
        assertNotNull(popFn);
        assertTrue(popFn.isBuiltIn());
        assertTrue(popFn.hasGenericParam());
        assertTrue(popFn.hasCompatibleParams(paramTypes));
    }

    @Test
    void test_generic_return_no_params() {
        FunctionSymbol symbol = new FunctionSymbol("test", new EntityType(NodeType.GENERIC), false);
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> table.insert(symbol));
        assertEquals("Cannot return generic from function with no parameters", error.getMessage());
    }

    @Test
    void test_generic_array_return_no_params() {
        FunctionSymbol symbol = new FunctionSymbol("test", new EntityType(NodeType.ARRAY, NodeType.GENERIC), false);
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> table.insert(symbol));
        assertEquals("Cannot return generic from function with no parameters", error.getMessage());
    }
}
