package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.semantic.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/*
valid function cases:
  - fn name() {} => [name]
  - fn name() {body} => [name, body]
  - fn name(): type {body} => [name, type, body]
  - fn name(params) {} => [name, params]
  - fn name(params) {body} => [name, params, body]
  - fn name(params): type {body} => [name, params, type, body]
*/

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class FunctionSymbol {
    @NonNull
    private String name;
    private NodeType returnType = null; // need to handle complex return values like Array<Array<int>>
    private NodeType[] paramTypes = {};

    private AbstractSyntaxTree fnBodyNode = null;


    public FunctionSymbol(String name, AbstractSyntaxTree fnBodyNode) {
        this.name = name;
        this.fnBodyNode = fnBodyNode;
    }

    public FunctionSymbol(String name, NodeType returnType, AbstractSyntaxTree fnBodyNode) {
        this.name = name;
        this.returnType = returnType;
        this.fnBodyNode = fnBodyNode;
    }

    public FunctionSymbol(String name, NodeType[] paramTypes) {
        this.name = name;
        this.paramTypes = paramTypes;
    }

    public FunctionSymbol(String name, NodeType[] paramTypes, AbstractSyntaxTree fnBodyNode) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.fnBodyNode = fnBodyNode;
    }

    public FunctionSymbol(String name, NodeType[] paramTypes, NodeType returnType, AbstractSyntaxTree fnBodyNode) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.fnBodyNode = fnBodyNode;
    }

    public String formFnSignature() {
        StringBuilder output = new StringBuilder(name);
        output.append("(");
        for (int i = 0; i < paramTypes.length; i++) {
            output.append(paramTypes[i]);
            if (i < paramTypes.length - 1)
                output.append(",");
        }
        output.append(")");
        return output.toString();
    }
}
