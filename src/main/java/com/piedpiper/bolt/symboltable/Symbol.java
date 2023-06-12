package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import com.piedpiper.bolt.semantic.EntityType;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
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
    private boolean isMutableArray = false;
    private List<Integer> arraySizes = List.of(0);
    private AbstractSyntaxTree valueNodes = null;

    public Symbol(AbstractSyntaxTree node, Integer scope) {
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
        else if (node.getLabel().equals("ARRAY-DECL")) {
            this.isMutableArray = children.get(offset).getName() == TokenType.KW_MUT;
            if (isMutableArray)
                offset++;
            this.type = new EntityType(children.get(offset));
            this.name = children.get(offset + 1).getValue();
            if (isConstant && !isMutableArray) {
                // handle potential dynamic array sizing
                if (children.get(offset + 2).getLabel().equals("ARRAY-INDEX")) {
                    AbstractSyntaxTree current = children.get(offset+2).getChildren().get(0);
                    List<Integer> sizes = new ArrayList<>();
                    while (current != null) {
                        sizes.add(Integer.valueOf(current.getValue()));
                        // TODO: type check each child and handle more complex nodes
                        current = current.getChildren().get(0);
                    }
                    offset += 2;
                }
                else {

                }
                this.valueNodes = children.get(offset);
            }
            else {
                // TODO: handle array sizing
                if (children.size() == offset + 4)
                    this.valueNodes = children.get(offset + 3);
            }
        }
        else if (node.getLabel().equals("FUNC-PARAM")) {
            this.type = new EntityType(children.get(0));
            this.name = children.get(1).getValue();
        }
    }
}
