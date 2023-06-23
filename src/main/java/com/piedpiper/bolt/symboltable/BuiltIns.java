package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;
import com.piedpiper.bolt.semantic.EntityType;
import com.piedpiper.bolt.semantic.NodeType;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

public class BuiltIns {
    private static final EntityType intType = new EntityType(NodeType.INT);
    private static final EntityType floatType = new EntityType(NodeType.FLOAT);
    private static final EntityType booleanType = new EntityType(NodeType.BOOLEAN);
    private static final EntityType stringType = new EntityType(NodeType.STRING);
    private static final EntityType stringArrayType = new EntityType(NodeType.ARRAY, NodeType.STRING);
    private static final EntityType genericType = new EntityType(NodeType.GENERIC);
    private static final EntityType genericArrayType = new EntityType(NodeType.ARRAY, NodeType.GENERIC);
    public static final Map<String, List<Symbol>> Variables = Map.ofEntries(
        entry("INT MIN", List.of(new Symbol("INT_MIN", intType, new VariableToken(TokenType.NUMBER, String.valueOf(Integer.MIN_VALUE))))),
        entry("INT_MAX", List.of(new Symbol("INT_MAX", intType, new VariableToken(TokenType.NUMBER, String.valueOf(Integer.MAX_VALUE))))),
        entry("FLOAT_MIN", List.of(new Symbol("FLOAT_MIN", floatType, new VariableToken(TokenType.NUMBER, String.valueOf(Float.MIN_VALUE))))),
        entry("FLOAT_MAX", List.of(new Symbol("FLOAT_MAX", floatType, new VariableToken(TokenType.NUMBER, String.valueOf(Float.MAX_VALUE))))),
        entry("argc", List.of(new Symbol("argc", intType))),
        entry("argv", List.of(new Symbol("argv", stringArrayType)))
    );

    public static final Map<String, List<FunctionSymbol>> Functions = Map.ofEntries(
        // Define function bodies in IR phase of compiler
        entry("length", List.of(
            new FunctionSymbol("length", intType, new EntityType[]{stringType}, true),
            new FunctionSymbol("length", intType, new EntityType[]{genericArrayType}, true)
        )),
        entry("toString", List.of(
            new FunctionSymbol("toString", stringType, new EntityType[]{intType}, true),
            new FunctionSymbol("toString", stringType, new EntityType[]{floatType}, true),
            new FunctionSymbol("toString", stringType, new EntityType[]{booleanType}, true),
            new FunctionSymbol("toString", stringType, new EntityType[]{genericArrayType}, true)
        )),
        entry("toInt", List.of(
            new FunctionSymbol("toInt", intType, new EntityType[]{floatType}, true),
            new FunctionSymbol("toInt", intType, new EntityType[]{stringType}, true)
        )),
        entry("toFloat", List.of(
            new FunctionSymbol("toFloat", floatType, new EntityType[]{intType}, true),
            new FunctionSymbol("toFloat", floatType, new EntityType[]{stringType}, true)
        )),
        entry("max", List.of(
            new FunctionSymbol("max", intType, new EntityType[]{intType, intType}, true),
            new FunctionSymbol("max", floatType, new EntityType[]{floatType, floatType}, true),
            new FunctionSymbol("max", floatType, new EntityType[]{floatType, intType}, true),
            new FunctionSymbol("max", floatType, new EntityType[]{intType, floatType}, true)
        )),
        entry("min", List.of(
            new FunctionSymbol("min", intType, new EntityType[]{intType, intType}, true),
            new FunctionSymbol("min", floatType, new EntityType[]{floatType, floatType}, true),
            new FunctionSymbol("min", floatType, new EntityType[]{floatType, intType}, true),
            new FunctionSymbol("min", floatType, new EntityType[]{intType, floatType}, true)
            )),
        entry("contains", List.of(
            new FunctionSymbol("contains", booleanType, new EntityType[]{stringType, stringType}, true),
            new FunctionSymbol("contains", booleanType, new EntityType[]{genericArrayType, genericType}, true)
        )),
        entry("startsWith", List.of(new FunctionSymbol("startsWith", booleanType, new EntityType[]{stringType, stringType}, true))),
        entry("endsWith", List.of(new FunctionSymbol("endsWith", booleanType, new EntityType[]{stringType, stringType}, true))),
        entry("exit", List.of(
            new FunctionSymbol("exit", true),
            new FunctionSymbol("exit", new EntityType[]{intType}, true)
        )),
        entry("fileExists", List.of(new FunctionSymbol("fileExists", booleanType, new EntityType[]{stringType}, true))),
        entry("readFile", List.of(
            new FunctionSymbol("readFile", stringArrayType, new EntityType[]{stringType}, true)
        )),
        entry("writeFile", List.of(
            new FunctionSymbol("writeFile", new EntityType[]{stringType, stringType}, true)
        )),
        entry("appendToFile", List.of(
            new FunctionSymbol("appendFile", new EntityType[]{stringType, stringType}, true)
        )),
        entry("sleep", List.of(new FunctionSymbol("sleep", new EntityType[]{floatType}, true))),
        entry("slice", List.of(
            new FunctionSymbol("slice", stringType, new EntityType[]{stringType, intType}, true),
            new FunctionSymbol("slice", stringType, new EntityType[]{stringType, intType, intType}, true)
        )),
        entry("remove", List.of(
            new FunctionSymbol("remove", stringType, new EntityType[]{stringType}, true),
            new FunctionSymbol("remove", stringType, new EntityType[]{intType}, true)
        )),

        // at some point, will need to add type checking for these calls...
        entry("pop", List.of(new FunctionSymbol("pop", genericType, new EntityType[]{genericArrayType}, true))),
        entry("append", List.of(new FunctionSymbol("append", new EntityType[]{genericArrayType, genericType}, true))),
        entry("prepend", List.of(new FunctionSymbol("prepend", new EntityType[]{genericArrayType, genericType}, true))),
        entry("sort", List.of(new FunctionSymbol("sort", genericArrayType, new EntityType[]{genericArrayType}, true))),

        entry("search", List.of(new FunctionSymbol("search", intType, new EntityType[]{stringType, stringType}, true))),
        entry("reverse", List.of(new FunctionSymbol("reverse", stringType, new EntityType[]{stringType}, true))),
        entry("split", List.of(
            new FunctionSymbol("split", stringArrayType, new EntityType[]{stringType}, true),
            new FunctionSymbol("split", stringArrayType, new EntityType[]{stringType, stringType}, true)
        )),
        entry("join", List.of(
            new FunctionSymbol("join", stringType, new EntityType[]{stringArrayType, stringType}, true)
        )),
        entry("at", List.of(new FunctionSymbol("at", stringType, new EntityType[]{stringType, intType}, true))),
        entry("print", List.of(new FunctionSymbol("print", stringType, true)))

    );
}
