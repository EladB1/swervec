package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.error.NameError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SymbolTable {
    private Map<String, List<Symbol>> table = new HashMap<>();
    private Stack<Integer> scopes = new Stack<>();
    private int scopeLevel = 1;

    private int scopeSerial = 1;

    public SymbolTable() {
        // TODO: add built-in methods
        scopes.push(0); // built-in scope
        scopes.push(1); // global scope
    }

    public Integer getScopeLevel() {
        return scopeLevel;
    }

    public boolean isScopeOpen(int scope) {
        return scopes.search(scope) != -1;
    }

    public void enterScope() {
        scopeLevel = scopeSerial + 1;
        scopeSerial++;
        scopes.push(scopeLevel);
    }

    public void leaveScope() {
        if (scopeLevel <= 1)
            return;
        scopes.pop();
        scopeLevel = scopes.peek(); // go back to the last scope still on the stack
        // leave scopeSerial as is
    }

    public void insert(Symbol symbol) {
        String name = symbol.getName();
        if (!table.containsKey(name)) {
            table.put(name, List.of(symbol));
            return;
        }
        List<Symbol> symbols = table.get(name);

        if (symbols.size() == 1 && symbols.get(0).getScope() == 0)
            throw new NameError("Symbol '" + name + "' is already defined in the built-in scope");

        for (Symbol sym : symbols) {
            if (sym.getScope() == scopeLevel)
                throw new NameError("Symbol '" + name + "' is already defined in this scope");
        }
        symbols = new ArrayList<>(symbols);
        symbols.add(symbol);
        table.put(name, symbols);
    }

    public Symbol lookup(String symbolName) {
        if (!table.containsKey(symbolName))
            return null;
        List<Symbol> matchingSymbols = table.get(symbolName);
        if (matchingSymbols.size() == 1 && isScopeOpen(matchingSymbols.get(0).getScope()))
            return matchingSymbols.get(0);
        int limit = Math.min(scopeLevel, matchingSymbols.size() - 1);
        for (int i = limit; i >= 0; i--) {
            if (isScopeOpen(i))
                return matchingSymbols.get(i); // return the first matching valid scope
        }
        return null;
    }
}
