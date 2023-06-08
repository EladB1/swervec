package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.semantic.NodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


public class SymbolTable {
    private final Map<String, List<Symbol>> table = new HashMap<>();
    private final Map<String, List<FunctionSymbol>> functionTable = new HashMap<>();
    private final Stack<Integer> scopes = new Stack<>();
    private int scopeLevel = 1;

    private int scopeSerial = 1;

    public SymbolTable() {
        // TODO: add built-in functions and constants
        scopes.push(0); // built-in scope
        scopes.push(1); // global scope
    }

    public Integer getScopeLevel() {
        return scopeLevel;
    }

    public boolean isScopeOpen(int scope) {
        return scopes.search(scope) != -1;
    }

    public int enterScope() {
        scopeLevel = scopeSerial + 1;
        scopeSerial++;
        scopes.push(scopeLevel);
        return scopeLevel;
    }

    public int leaveScope() {
        if (scopeLevel <= 1)
            return scopeLevel;
        scopes.pop();
        scopeLevel = scopes.peek(); // go back to the last scope still on the stack
        // leave scopeSerial as is
        return scopeLevel;
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

    public void insert(FunctionSymbol fnSymbol) {
        String name = fnSymbol.getName();
        if (!functionTable.containsKey(name)) {
            functionTable.put(name, List.of(fnSymbol));
            return;
        }

        if (functionTable.get(name).get(0).getReturnType() != fnSymbol.getReturnType()) {
            String message = String.format(
                "Function '%s' cannot have return type %s because another definition returns type %s",
                name,
                fnSymbol.getReturnType(),
                functionTable.get(name).get(0).getReturnType()
            );
            throw new TypeError(message);
        }

        if (lookup(name, fnSymbol.getParamTypes()) != null)
            throw new NameError("Function '" + fnSymbol.formFnSignature() + "' is already defined");

        List<FunctionSymbol> functionSymbols = functionTable.get(name);

        functionSymbols = new ArrayList<>(functionSymbols);
        functionSymbols.add(fnSymbol);
        functionTable.put(name, functionSymbols);
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

    public FunctionSymbol lookup(String name, NodeType[] types) {
        if (!functionTable.containsKey(name))
            return null;
        List<FunctionSymbol> matchingFunctions = functionTable.get(name);
        for (FunctionSymbol fnSymbol : matchingFunctions) {
            System.out.println(Arrays.equals(fnSymbol.getParamTypes(), types));
            if (fnSymbol.getName().equals(name) && Arrays.equals(fnSymbol.getParamTypes(), types))
                return fnSymbol;
        }
        return null;
    }

    @Override
    public String toString() {
        if (table.isEmpty() && functionTable.isEmpty())
            return "";
        StringBuilder output = new StringBuilder("Current scope level: " + scopeLevel + "\nOpen scopes: " + scopes + "\n");
        output.append("table: {\n");
        for (Map.Entry<String, List<Symbol>> symbol : table.entrySet()) {
            output.append("\t").append(symbol.getKey()).append(": ").append(symbol.getValue()).append("\n");
        }
        output.append("}\nfunction table: {\n");
        for (Map.Entry<String, List<FunctionSymbol>> symbol : functionTable.entrySet()) {
            output.append("\t").append(symbol.getKey()).append(": ").append(symbol.getValue()).append("\n");
        }
        output.append("}");
        return output.toString();
    }
}
