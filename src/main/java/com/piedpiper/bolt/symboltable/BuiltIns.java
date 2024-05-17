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
    private static final EntityType doubleType = new EntityType(NodeType.DOUBLE);
    private static final EntityType booleanType = new EntityType(NodeType.BOOLEAN);
    private static final EntityType stringType = new EntityType(NodeType.STRING);
    private static final EntityType stringArrayType = new EntityType(NodeType.ARRAY, NodeType.STRING);
    private static final EntityType genericType = new EntityType(NodeType.GENERIC);
    private static final EntityType genericArrayType = new EntityType(NodeType.ARRAY, NodeType.GENERIC);
    public static final Map<String, List<Symbol>> Variables = Map.ofEntries(
        entry("INT MIN", List.of(new Symbol("INT_MIN", intType, new VariableToken(TokenType.NUMBER, String.valueOf(Integer.MIN_VALUE))))),
        entry("INT_MAX", List.of(new Symbol("INT_MAX", intType, new VariableToken(TokenType.NUMBER, String.valueOf(Integer.MAX_VALUE))))),
        entry("DOUBLE_MIN", List.of(new Symbol("DOUBLE_MIN", doubleType, new VariableToken(TokenType.NUMBER, String.valueOf(Double.MIN_VALUE))))),
        entry("DOUBLE_MAX", List.of(new Symbol("DOUBLE_MAX", doubleType, new VariableToken(TokenType.NUMBER, String.valueOf(Double.MAX_VALUE)))))
    );

    public static final Map<String, List<FunctionSymbol>> Functions = Map.ofEntries(
        // Function bodies defined in language VM
        entry("printerr", List.of(
            new FunctionSymbol("printerr", new EntityType[]{stringType}, true),
            new FunctionSymbol("printerr", new EntityType[]{stringType, intType}, true)
        )),
        entry("length", List.of(
            new FunctionSymbol("length", intType, new EntityType[]{stringType}, true)
        )),
        entry("max", List.of(
            new FunctionSymbol("max", intType, new EntityType[]{intType, intType}, true),
            new FunctionSymbol("max", doubleType, new EntityType[]{doubleType, doubleType}, true),
            new FunctionSymbol("max", doubleType, new EntityType[]{doubleType, intType}, true),
            new FunctionSymbol("max", doubleType, new EntityType[]{intType, doubleType}, true)
        )),
        entry("min", List.of(
            new FunctionSymbol("min", intType, new EntityType[]{intType, intType}, true),
            new FunctionSymbol("min", doubleType, new EntityType[]{doubleType, doubleType}, true),
            new FunctionSymbol("min", doubleType, new EntityType[]{doubleType, intType}, true),
            new FunctionSymbol("min", doubleType, new EntityType[]{intType, doubleType}, true)
        )),
        entry("replace", List.of(
            new FunctionSymbol("replace", stringType, new EntityType[]{stringType, stringType, stringType}, true)
        )),
        entry("replaceAll", List.of(
            new FunctionSymbol("replaceAll", stringType, new EntityType[]{stringType, stringType, stringType}, true)
        )),
        entry("split", List.of(
            new FunctionSymbol("split", stringArrayType, new EntityType[]{stringType}, true),
            new FunctionSymbol("split", stringArrayType, new EntityType[]{stringType, stringType}, true)
        )),
        entry("slice", List.of(
            new FunctionSymbol("slice", stringType, new EntityType[]{stringType, intType}, true),
            new FunctionSymbol("slice", stringType, new EntityType[]{stringType, intType, intType}, true)
        )),
        entry("contains", List.of(
            new FunctionSymbol("contains", booleanType, new EntityType[]{stringType, stringType}, true)
        )),
        entry("toInt", List.of(
            new FunctionSymbol("toInt", intType, new EntityType[]{doubleType}, true),
            new FunctionSymbol("toInt", intType, new EntityType[]{stringType}, true)
        )),
        entry("toDouble", List.of(
            new FunctionSymbol("toDouble", doubleType, new EntityType[]{intType}, true),
            new FunctionSymbol("toDouble", doubleType, new EntityType[]{stringType}, true)
        )),
        entry("at", List.of(new FunctionSymbol("at", stringType, new EntityType[]{stringType, intType}, true))),
        entry("join", List.of(
            new FunctionSymbol("join", stringType, new EntityType[]{stringArrayType, stringType}, true)
        )),
        entry("reverse", List.of(new FunctionSymbol("reverse", stringType, new EntityType[]{stringType}, true))),
        entry("startsWith", List.of(new FunctionSymbol("startsWith", booleanType, new EntityType[]{stringType, stringType}, true))),
        entry("endsWith", List.of(new FunctionSymbol("endsWith", booleanType, new EntityType[]{stringType, stringType}, true))),
        entry("sleep", List.of(new FunctionSymbol("sleep", new EntityType[]{doubleType}, true))),
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
        entry("renameFile", List.of(
            new FunctionSymbol("renameFile", new EntityType[]{stringType, stringType}, true)
        )),
        entry("deleteFile", List.of(
            new FunctionSymbol("deleteFile", new EntityType[]{stringType}, true)
        )),
        entry("getEnv", List.of(
            new FunctionSymbol("deleteFile", stringType, new EntityType[]{stringType}, true)
        )),
        entry("setEnv", List.of(
            new FunctionSymbol("renameFile", new EntityType[]{stringType, stringType}, true)
        ))
    );

    public static final Map<String, List<PrototypeSymbol>> Prototypes = Map.ofEntries(
        // Function bodies defined in language VM
        entry("print", List.of(new PrototypeSymbol("print", new EntityType[]{genericType}, new String[]{"value"}, true))),
        entry("println", List.of(new PrototypeSymbol("println", new EntityType[]{genericType}, new String[]{"value"}, true))),
        entry("getType", List.of(new PrototypeSymbol("getType", stringType, new EntityType[]{genericType}, new String[]{"value"}, true))),
        entry("length", List.of(
            new PrototypeSymbol("length", intType, new EntityType[]{genericArrayType}, new String[]{"array"}, true)
        )),
        entry("capacity", List.of(
            new PrototypeSymbol("capacity", intType, new EntityType[]{genericArrayType}, new String[]{"array"}, true)
        )),
        entry("slice", List.of(
            new PrototypeSymbol("slice", genericArrayType, new EntityType[]{genericArrayType, intType}, new String[]{"array, start"}, true),
            new PrototypeSymbol("slice", genericArrayType, new EntityType[]{genericArrayType, intType, intType}, new String[]{"array, start, end"}, true)
        )),
        entry("contains", List.of(
            new PrototypeSymbol("contains", booleanType, new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true)
        )),
        entry("append", List.of(new PrototypeSymbol("append", new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("prepend", List.of(new PrototypeSymbol("prepend", new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("insert", List.of(new PrototypeSymbol("insert", new EntityType[]{genericArrayType, genericType, intType}, new String[]{"array", "element", "index"}, true))),
        entry("removeIndex", List.of(new PrototypeSymbol("removeIndex", new EntityType[]{genericArrayType, intType}, new String[]{"array", "index"}, true))),
        entry("remove", List.of(new PrototypeSymbol("removeIndex", new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("removeAll", List.of(new PrototypeSymbol("removeIndex", new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("indexOf", List.of(new PrototypeSymbol("indexOf", intType, new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("toString", List.of(new PrototypeSymbol("toString", stringType, new EntityType[]{genericType}, new String[]{"value"}, true))),
        entry("reverse", List.of(new PrototypeSymbol("reverse", new EntityType[]{genericArrayType}, new String[]{"array"}, true))),
        entry("sort", List.of(new PrototypeSymbol("sort", new EntityType[]{genericArrayType}, new String[]{"array"}, true)))
    );
}
