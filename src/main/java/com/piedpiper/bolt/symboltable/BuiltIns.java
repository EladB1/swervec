package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
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
            new FunctionSymbol("length", intType, new EntityType[]{stringType}, true)
        )),
        entry("toString", List.of(
            new FunctionSymbol("toString", stringType, new EntityType[]{intType}, true),
            new FunctionSymbol("toString", stringType, new EntityType[]{floatType}, true),
            new FunctionSymbol("toString", stringType, new EntityType[]{booleanType}, true)
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
            new FunctionSymbol("contains", booleanType, new EntityType[]{stringType, stringType}, true)
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
        entry("print", List.of(new FunctionSymbol("print", new EntityType[]{stringType}, true)))
    );

    /** Function body:
        if (array == null || length(array) == 0) {
            print("Cannot pop from empty array");
            exit(1);
        }
        generic top = array[0];
        remove(array, 0);
        return top;
    */
    private static AbstractSyntaxTree getPopBody() {
        AbstractSyntaxTree zeroNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "0"));
        AbstractSyntaxTree topNode = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "top"));
        AbstractSyntaxTree arrayNode = new AbstractSyntaxTree(new VariableToken(TokenType.ID, "array"));
        return new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("COND", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_IF, 2), List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.OP, "||", 2), List.of(
                        new AbstractSyntaxTree(new VariableToken(TokenType.OP, "==", 2), List.of(
                            arrayNode,
                            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL, 2))
                        )),
                        new AbstractSyntaxTree(new VariableToken(TokenType.OP, "==", 2), List.of(
                            new AbstractSyntaxTree("FUNC-CALL", List.of(
                                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length", 2)),
                                new AbstractSyntaxTree("FUNC-PARAMS", List.of(arrayNode))
                            )),
                            zeroNode
                        ))
                    )),
                    new AbstractSyntaxTree("BLOCK-BODY", List.of(
                        new AbstractSyntaxTree("FUNC-CALL", List.of(
                            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "print", 3)),
                            new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                                new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"Cannot pop from empty array\"", 3))
                            ))
                        )),
                        new AbstractSyntaxTree("FUNC-CALL", List.of(
                            new AbstractSyntaxTree(new VariableToken(TokenType.ID, "exit", 4)),
                            new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1", 4))
                            ))
                        ))
                    ))
                ))
            )),
            new AbstractSyntaxTree("VAR-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_GEN, 4)),
                topNode,
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "array" ,4), List.of(
                    new AbstractSyntaxTree("ARRAY-INDEX", 4, List.of(zeroNode))
                ))
            )),
            new AbstractSyntaxTree("FUNC-CALL", List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "remove", 5)),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    arrayNode,
                    zeroNode
                ))
            )),
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET, 6)),
                topNode
            ))
        ));
    }

    public static final Map<String, List<PrototypeSymbol>> Prototypes = Map.ofEntries(
        // Define function bodies in IR phase of compiler
        entry("length", List.of(
            new PrototypeSymbol("length", intType, new EntityType[]{genericArrayType}, new String[]{"array"}, true)
        )),
        entry("toString", List.of(
            new PrototypeSymbol("toString", stringType, new EntityType[]{genericArrayType}, new String[]{"array"}, true)
        )),
        entry("contains", List.of(
            new PrototypeSymbol("contains", booleanType, new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true)
        )),
        entry("remove", List.of(
            new PrototypeSymbol("remove", new EntityType[]{genericArrayType, intType}, new String[]{"array", "index"}, true)
        )),

        entry("pop", List.of(new PrototypeSymbol("pop", genericType, new EntityType[]{genericArrayType}, new String[]{"array"}, true, getPopBody()))),
        entry("append", List.of(new PrototypeSymbol("append", new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("prepend", List.of(new PrototypeSymbol("prepend", new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("sort", List.of(new PrototypeSymbol("sort", new EntityType[]{genericArrayType}, new String[]{"array"}, true))),

        entry("indexOf", List.of(new PrototypeSymbol("indexOf", intType, new EntityType[]{genericArrayType, genericType}, new String[]{"array", "element"}, true))),
        entry("reverse", List.of(new PrototypeSymbol("reverse", new EntityType[]{genericArrayType}, new String[]{"array"}, true))),
        entry("print", List.of(new PrototypeSymbol("print", new EntityType[]{genericArrayType}, new String[]{"array"}, true)))
    );
}
