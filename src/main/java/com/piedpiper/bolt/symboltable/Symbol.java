package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.semantic.EntityType;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Data
public class Symbol {
    @NonNull
    private String name;
    @NonNull
    private EntityType type;
    @NonNull
    private Integer scope;
    private boolean isConstant = false;
    private List<Integer> arraySizes = List.of(0);
    private AbstractSyntaxTree valueNodes = null;

    // use this to add builtin variables
    public Symbol(@NonNull String name, @NonNull EntityType type, Token value) {
        this.name = name;
        this.type = type;
        this.scope = 0;
        this.isConstant = true;
        this.valueNodes = new AbstractSyntaxTree(value);
    }

    // use this to add builtin variables without values
    public Symbol(@NonNull String name, @NonNull EntityType type) {
        this.name = name;
        this.type = type;
        this.scope = 0;
        this.isConstant = true;
    }

    // Array declaration?
    public Symbol(AbstractSyntaxTree arrayDeclaration, List<Integer> arraySizes, @NonNull EntityType type, Integer valueIndex, @NonNull Integer scope) {
        this.scope = scope;
        this.arraySizes = arraySizes;
        this.type = type;
        List<AbstractSyntaxTree> children = arrayDeclaration.getChildren();
        this.isConstant = children.get(0).getName() == TokenType.KW_CONST;
        int offset = isConstant ? 1 : 0;
        this.name = children.get(offset + 1).getValue();
        if (valueIndex != 0)
            this.valueNodes = children.get(valueIndex);
    }

    public Symbol(AbstractSyntaxTree node, @NonNull Integer scope) {
        this.scope = scope;
        List<AbstractSyntaxTree> children = node.getChildren();
        this.isConstant = children.get(0).getName() == TokenType.KW_CONST;
        int offset = isConstant ? 1 : 0;
        if (node.getLabel().equals("VAR-DECL")) {
            this.type = new EntityType(children.get(offset));
            this.name = children.get(offset + 1).getValue();
            if (children.size() == offset + 3)
                this.valueNodes = children.get(offset + 2);
        }
        else if (node.getLabel().equals("FUNC-PARAM")) {
            this.type = new EntityType(children.get(0));
            this.name = children.get(1).getValue();
        }
    }

    public Symbol(@NonNull String name, @NonNull EntityType type, boolean isConstant, AbstractSyntaxTree value, @NonNull Integer scope) {
        this.name = name;
        this.type = type;
        this.isConstant = isConstant;
        this.valueNodes = value;
        this.scope = scope;
    }
}
