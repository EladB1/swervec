package com.piedpiper.swerve.symboltable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import com.piedpiper.swerve.error.IllegalStatementError;
import com.piedpiper.swerve.error.NameError;
import com.piedpiper.swerve.error.TypeError;
import com.piedpiper.swerve.semantic.EntityType;


public class SymbolTable {
    private final Map<String, List<Symbol>> table = new HashMap<>(BuiltIns.Variables);
    private final Map<String, List<FunctionSymbol>> functionTable = new HashMap<>(BuiltIns.Functions);
    private final Map<String, List<PrototypeSymbol>> prototypesTable = new HashMap<>(BuiltIns.Prototypes);
    private final Stack<Integer> scopes = new Stack<>();
    private int scopeLevel = 1;
    private int scopeSerial = 1;

    public SymbolTable() {
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

    private void appendToExistingDefinition(ProcedureSymbol symbol, List matchingSymbols) {
        if (symbol instanceof FunctionSymbol) {
            List<FunctionSymbol> symbols = new ArrayList<FunctionSymbol>(matchingSymbols);
            symbols.add((FunctionSymbol) symbol);
            functionTable.put(symbol.getName(), symbols);
        }
        else {
            List<PrototypeSymbol> symbols = new ArrayList<PrototypeSymbol>(matchingSymbols);
            symbols.add((PrototypeSymbol) symbol);
            prototypesTable.put(symbol.getName(), symbols);
        }
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

    public void insert(ProcedureSymbol symbol) {
        insert(symbol, false);
    }

    public void insert(ProcedureSymbol symbol, boolean fromPrototypeTranslation) {
        String name = symbol.getName();
        boolean isFunction = symbol instanceof FunctionSymbol;
        String entity;
        ProcedureSymbol existingSymbol;
        List matchingSymbols;
        if (isFunction) {
            if (!fromPrototypeTranslation && symbol.hasGenericParam())
                throw new IllegalStatementError("Generic parameter found in function definition; generics can only be used in prototype");
            if (!fromPrototypeTranslation && symbol.returnsGeneric())
                throw new IllegalStatementError("Generic return found in function definition; generics can only be used in prototype");
            if (!functionTable.containsKey(name)) {
                functionTable.put(name, List.of((FunctionSymbol) symbol));
                return;
            }
            entity = "Function";
            matchingSymbols = functionTable.get(name);
            existingSymbol = (ProcedureSymbol) matchingSymbols.get(0);
            if (lookup(name, symbol.getParamTypes()) != null)
                throw new NameError("Function '" + symbol.formSignature() + "' is already defined");
        }
        else {
            if (!symbol.hasGenericParam())
                throw new IllegalStatementError("Prototype definition must contain at least one generic parameter");
            if (!prototypesTable.containsKey(name)) {
                prototypesTable.put(name, List.of((PrototypeSymbol) symbol));
                return;
            }
            entity = "Prototype";
            matchingSymbols = prototypesTable.get(name);
            existingSymbol = (ProcedureSymbol) matchingSymbols.get(0);
            if (lookup(name, symbol.getParamTypes()) != null)
                throw new NameError("Prototype '" + symbol.formSignature() + "' is already defined");
        }
        if (existingSymbol.isBuiltIn() && !fromPrototypeTranslation)
            throw new NameError(entity + " '" + name + "' is builtin so its name cannot be reused");
        EntityType storedReturnType = existingSymbol.getReturnType();
        EntityType returnType = symbol.getReturnType();

        if (!Objects.equals(storedReturnType, returnType) && !fromPrototypeTranslation) {
            String message = String.format(
                "%s '%s' cannot have return type %s because another definition returns type %s",
                entity,
                name,
                returnType,
                storedReturnType
            );
            throw new TypeError(message);
        }
        appendToExistingDefinition(symbol, matchingSymbols);

    }

    public Symbol lookup(String symbolName) {
        if (!table.containsKey(symbolName))
            return null;
        List<Symbol> matchingSymbols = table.get(symbolName);
        if (matchingSymbols.size() == 1 && isScopeOpen(matchingSymbols.get(0).getScope()))
                return matchingSymbols.get(0);
        for (int i = matchingSymbols.size() - 1; i >= 0; i--) {
            if (isScopeOpen(matchingSymbols.get(i).getScope()))
                return matchingSymbols.get(i); // return the first matching valid scope
        }
        return null;
    }

    public FunctionSymbol lookup(String name, EntityType[] types) {
        if (!functionTable.containsKey(name))
            return null;
        List<FunctionSymbol> matchingFunctions = functionTable.get(name);
        for (FunctionSymbol fnSymbol : matchingFunctions) {
            if (fnSymbol.hasCompatibleParams(types))
                return fnSymbol;
        }
        return null;
    }

    public PrototypeSymbol lookupPrototype(String name, EntityType[] types) {
        if (!prototypesTable.containsKey(name))
            return null;
        List<PrototypeSymbol> matchingPrototypes = prototypesTable.get(name);
        for (PrototypeSymbol prototype : matchingPrototypes) {
            if (prototype.hasCompatibleParams(types))
                return prototype;
        }
        return null;
    }

    public void replace(String name, Symbol newSymbol) {
        List<Symbol> matchingSymbols = new ArrayList<>(table.get(name));
        matchingSymbols.remove(matchingSymbols.size() - 1);
        matchingSymbols.add(newSymbol);
        table.replace(name, matchingSymbols);
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
        output.append("}\nprototypes table: {\n");
        for (Map.Entry<String, List<PrototypeSymbol>> symbol : prototypesTable.entrySet()) {
            output.append("\t").append(symbol.getKey()).append(": ").append(symbol.getValue()).append("\n");
        }
        output.append("}");
        return output.toString();
    }
}
