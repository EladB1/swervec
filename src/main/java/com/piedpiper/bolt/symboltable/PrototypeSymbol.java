package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.semantic.EntityType;
import com.piedpiper.bolt.semantic.NodeType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/*
valid prototype cases:
  - fn name(params) {} => [name, params]
  - fn name(params) {body} => [name, params, body]
  - fn name(params): type {body} => [name, params, type, body]
*/

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class PrototypeSymbol implements ProcedureSymbol {
    @NonNull
    private String name;
    private EntityType returnType = null; // need to handle complex return values like Array<Array<int>>
    private EntityType[] paramTypes = {};
    private String[] paramNames = {};
    private Boolean builtIn = false;
    private AbstractSyntaxTree fnBodyNode = null;

    public PrototypeSymbol(@NonNull String name, EntityType[] paramTypes, String[] paramNames, Boolean builtIn) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.paramNames = paramNames;
        this.builtIn = builtIn;
    }

    public PrototypeSymbol(@NonNull String name, EntityType returnType, EntityType[] paramTypes, String[] paramNames, Boolean builtIn) {
        this.name = name;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        this.paramNames = paramNames;
        this.builtIn = builtIn;
    }

    public PrototypeSymbol(@NonNull String name, EntityType returnType, Boolean builtIn) {
        this.name = name;
        this.returnType = returnType;
        this.builtIn = builtIn;
    }

    public PrototypeSymbol(@NonNull String name, EntityType[] paramTypes, String[] paramNames) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.paramNames = paramNames;
    }

    public PrototypeSymbol(@NonNull String name, EntityType[] paramTypes, String[] paramNames, AbstractSyntaxTree fnBodyNode) {
        this.name = name;
        this.paramTypes = paramTypes;
        this.paramNames = paramNames;
        this.fnBodyNode = fnBodyNode;
    }

    public PrototypeSymbol(@NonNull String name, EntityType returnType, EntityType[] paramTypes, String[] paramNames, AbstractSyntaxTree fnBodyNode) {
        this.name = name;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        this.paramNames = paramNames;
        this.fnBodyNode = fnBodyNode;
    }

    public PrototypeSymbol(@NonNull String name, EntityType returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public boolean hasGenericParam() {
        for (EntityType paramType : paramTypes) {
            if (paramType.isType(NodeType.GENERIC) || paramType.containsSubType(NodeType.GENERIC))
                return true;
        }
        return false;
    }

    public boolean returnsGeneric() {
        return returnType != null && (returnType.isType(NodeType.GENERIC) || returnType.containsSubType(NodeType.GENERIC));
    }
    
    public boolean hasCompatibleParams(EntityType[] params) {
        if (params.length != paramTypes.length)
            return false; // skip the ones with a different number of params
        for (int i = 0; i < params.length; i++) {
            if (!paramTypes[i].equals(params[i])) {
                if (!(paramTypes[i].isType(NodeType.GENERIC) || (params[i].startsWith(NodeType.ARRAY) && paramTypes[i].containsSubType(NodeType.GENERIC)))) {
                    return false;
                }
            }
        }
        return true;
    }

    public String formSignature() {
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
