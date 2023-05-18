package com.piedpiper.bolt.symboltable;

import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
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
    private AbstractSyntaxTree type;
    @NonNull
    private Integer scope;
    private boolean isConstant = false;
    private boolean isMutableArray = false;
    private Integer arraySize = 0;
    private AbstractSyntaxTree valueNodes = null;

    public Symbol(AbstractSyntaxTree node, Integer scope) {
        this.scope = scope;
        List<AbstractSyntaxTree> children = node.getChildren();
        this.isConstant = children.get(0).getToken().getName() == TokenType.KW_CONST;
        int offset = isConstant ? 1 : 0;
        if (node.getType().equals("VAR-DECL")) {
            this.type = children.get(offset);
            this.name = children.get(offset + 1).getToken().getValue();
            if (children.size() == offset + 3)
                this.valueNodes = children.get(offset + 2);
        }
        else if (node.getType().equals("ARRAY-DECL")) {
            this.isMutableArray = children.get(offset).getToken().getName() == TokenType.KW_MUT;
            if (isMutableArray)
                offset++;
            this.type = children.get(offset);
            this.name = children.get(offset + 1).getToken().getValue();
            if (isConstant && !isMutableArray) {
                // handle potential dynamic array sizing
                if (children.get(offset + 2).getType().equals("ARRAY-INDEX")) {
                    // TODO: handle array size
                    offset += 2;
                }
                this.valueNodes = children.get(offset);
            }
            else {
                // TODO: handle array sizing
                if (children.size() == offset + 4)
                    this.valueNodes = children.get(offset + 3);
            }
        }
        else if (node.getType().equals("FUNC-PARAM")) {
            this.type = children.get(0);
            this.name = children.get(1).getToken().getValue();
            if (children.size() == 3)
                this.valueNodes = children.get(2);
        }
    }
}
