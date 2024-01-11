package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.error.IllegalStatementError;
import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.semantic.EntityType;
import com.piedpiper.bolt.semantic.NodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;


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
        insert(fnSymbol, false);
    }

    public void insert(FunctionSymbol fnSymbol, boolean fromPrototypeTranslation) {
        String name = fnSymbol.getName();
        if (!functionTable.containsKey(name)) {
            if (fnSymbol.getReturnType() != null) {
                if (!fromPrototypeTranslation && (fnSymbol.getReturnType().isType(NodeType.GENERIC) || fnSymbol.getReturnType().containsSubType(NodeType.GENERIC) || fnSymbol.hasGenericParam())) {
                    throw new IllegalStatementError("Generic parameter found in function definition; generics can only be used in prototype");
                }
            }
            functionTable.put(name, List.of(fnSymbol));
            return;
        }
        boolean isBuiltIn = functionTable.get(name).get(0).isBuiltIn();

        if (isBuiltIn && !fromPrototypeTranslation)
            throw new NameError("Function '" + name + "' is builtin so its name cannot be reused");

        EntityType storedReturnType = functionTable.get(name).get(0).getReturnType();
        EntityType returnType = fnSymbol.getReturnType();
        if (storedReturnType != null) {
            if (!fromPrototypeTranslation && (fnSymbol.getReturnType().isType(NodeType.GENERIC) || fnSymbol.getReturnType().containsSubType(NodeType.GENERIC) || fnSymbol.hasGenericParam())) {
                throw new IllegalStatementError("Generic parameter found in function definition; generics can only be used in prototype");
            }
        }

        if (!Objects.equals(storedReturnType, returnType) && !fromPrototypeTranslation) {
            String message = String.format(
                "Function '%s' cannot have return type %s because another definition returns type %s",
                name,
                fnSymbol.getReturnType(),
                storedReturnType
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

    public void insert(PrototypeSymbol prototype) {
        String name = prototype.getName();
        if (!prototypesTable.containsKey(name)) {
            if (prototype.getParamTypes().length == 0 || !prototype.hasGenericParam())
                throw new IllegalStatementError("Prototype definition must contain at least one generic parameter");
            prototypesTable.put(name, List.of(prototype));
            return;
        }
        boolean isBuiltIn = prototypesTable.get(name).get(0).isBuiltIn();

        if (isBuiltIn)
            throw new NameError("Prototype '" + name + "' is builtin so its name cannot be reused");

        EntityType storedReturnType = prototypesTable.get(name).get(0).getReturnType();
        EntityType returnType = prototype.getReturnType();
        if (prototype.getParamTypes().length == 0 || !prototype.hasGenericParam())
            throw new IllegalStatementError("Prototype definition must contain at least one generic parameter");

        if (!Objects.equals(storedReturnType, returnType)) {
            String message = String.format(
                "Prototype '%s' cannot have return type %s because another definition returns type %s",
                name,
                prototype.getReturnType(),
                storedReturnType
            );
            throw new TypeError(message);
        }

        if (lookup(name, prototype.getParamTypes()) != null)
            throw new NameError("Prototype '" + prototype.formFnSignature() + "' is already defined");

        List<PrototypeSymbol> prototypeSymbols = prototypesTable.get(name);

        prototypeSymbols = new ArrayList<>(prototypeSymbols);
        prototypeSymbols.add(prototype);
        prototypesTable.put(name, prototypeSymbols);
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
        List<Symbol> matchingSymbols = table.get(name);
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
