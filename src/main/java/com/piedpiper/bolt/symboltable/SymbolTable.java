package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.error.NameError;

import java.util.List;
import java.util.Map;

public class SymbolTable {
    private Map<String, List<Symbol>> table;
    private List<Scope> scopeList = List.of(new Scope("built-in"), new Scope("global")); // use the index as the serial number
    private int scopeLevel = 1;

    private int maxScopeLevel = 1;

    public boolean isScopeOpen(int scope) {
        if (scope < 0 || scope >= scopeList.size())
            return false;
        return !scopeList.get(scope).getIsClosed();
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

    public void enterScope() {
        enterScope("");

    }

    public void enterScope(String name) {
        scopeLevel = maxScopeLevel + 1;
        maxScopeLevel++;
        if (name.isEmpty())
            scopeList.add(new Scope());
        else
            scopeList.add(new Scope(name));
    }

    public void leaveScope() {
        scopeList.get(scopeLevel).leaveScope();
        while (scopeList.get(scopeLevel).getIsClosed()) {
            scopeLevel--;
        }
        // leave maxScopeLevel as is
    }
}
