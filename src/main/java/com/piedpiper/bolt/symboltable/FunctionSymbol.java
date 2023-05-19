package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

/*
function cases:
  - fn name() {} => [name]
  - fn name(): type {} => [name, type]
  - fn name() {body} => [name, body]
  - fn name(): type {body} => [name, type, body]
  - fn name(params) {} => [name, params]
  - fn name(params) {body} => [name, params, body]
  - fn name(params): type {} => [name, params, type]
  - fn name(params): type {body} => [name, params, type, body]
*/

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class FunctionSymbol {
    @NonNull
    private String name;
    private AbstractSyntaxTree returnType = null; // need to handle complex return values like Array<Array<int>>
    private AbstractSyntaxTree[] paramTypes = {};

    private AbstractSyntaxTree fnBodyNode = null;


    public FunctionSymbol(String name, AbstractSyntaxTree node) { // node can be a return type or function body
        this.name = name;
        if (node.getLabel().equals("BLOCK-BODY"))
            this.fnBodyNode = node;
        else
            throw new TypeError("Function should return " + node.getName() + " but returns nothing");
    }

    public FunctionSymbol(String name, AbstractSyntaxTree returnType, AbstractSyntaxTree fnBodyNode) {
        this.name = name;
        this.fnBodyNode = fnBodyNode;
        this.returnType = returnType;
    }

    public FunctionSymbol(String name, AbstractSyntaxTree[] paramTypes) {
        this.name = name;
        this.paramTypes = paramTypes;
    }

    public FunctionSymbol(String name, AbstractSyntaxTree[] paramTypes, AbstractSyntaxTree node) { // node can be a return type or function body
        this.name = name;
        this.paramTypes = paramTypes;
        if (node.getLabel().equals("BLOCK-BODY"))
            this.fnBodyNode = node;
        else
            throw new TypeError("Function should return " + node.getName() + " but returns nothing");
    }

    public FunctionSymbol(List<AbstractSyntaxTree> fnDetails, SymbolTable table) {
        this.name = fnDetails.get(0).getValue();
        int size = fnDetails.size();
        if (size == 1)
            return;

        switch(size) {
            case 2:
                if (fnDetails.get(1).getLabel().equals("FUNC-PARAMS")) {
                    this.paramTypes = extractParamData(fnDetails.get(1), table);
                }
                else if (fnDetails.get(1).getLabel().equals("BLOCK-BODY")) {
                    this.fnBodyNode = fnDetails.get(1); // analyze the function body separately
                }
                else {
                    throw new TypeError("Function does not return expected type", fnDetails.get(0).getToken().getLineNumber());
                }
                break;
            case 3:
                if (fnDetails.get(1).getLabel().equals("FUNC-PARAMS")) {
                    this.paramTypes = extractParamData(fnDetails.get(1), table);
                    if (fnDetails.get(2).getLabel().equals("BLOCK-BODY")) {
                        this.fnBodyNode = fnDetails.get(2);
                    }
                    else {
                        throw new TypeError("Function does not return expected type", fnDetails.get(0).getToken().getLineNumber());
                    }
                }
                else { // contains type and body
                    this.returnType = fnDetails.get(1);
                    this.fnBodyNode = fnDetails.get(2);
                }
            case 4:
                this.paramTypes = extractParamData(fnDetails.get(1), table);
                this.returnType = fnDetails.get(2);
                this.fnBodyNode = fnDetails.get(3);

        }
    }

    private AbstractSyntaxTree[] extractParamData(AbstractSyntaxTree paramsNode, SymbolTable table) {
        List<AbstractSyntaxTree> params = paramsNode.getChildren();
        AbstractSyntaxTree[] paramData = new AbstractSyntaxTree[params.size()];
        int scope = table.getScopeLevel();
        AbstractSyntaxTree param;
        for (int i = 0; i < params.size(); i++) {
            param = params.get(i);
            paramData[i] = param.getChildren().get(0);
            table.insert(new Symbol(param, scope));
        }
        return paramData;
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
