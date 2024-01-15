package com.piedpiper.bolt.semantic;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import static java.util.Map.entry;

import java.util.ArrayList;

import com.piedpiper.bolt.error.ArrayBoundsError;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;

import lombok.Data;

/**
 * Container class to make type system uniform across simple types (int, float, etc.) and complex types (arrays)
 */
@Data
public class EntityType {
    private final Map<TokenType, NodeType> typeMappings = Map.ofEntries(
        entry(TokenType.KW_GEN, NodeType.GENERIC),
        entry(TokenType.KW_BOOL, NodeType.BOOLEAN),
        entry(TokenType.KW_INT, NodeType.INT),
        entry(TokenType.KW_FLOAT, NodeType.FLOAT),
        entry(TokenType.KW_STR, NodeType.STRING),
        entry(TokenType.KW_ARR, NodeType.ARRAY),
        entry(TokenType.KW_NULL, NodeType.NULL)
    );
    private List<NodeType> type;

    private EntityType() {}

    public EntityType(NodeType type) {
        this.type = List.of(type);
    }

    public EntityType(NodeType... types) {
        this.type = Arrays.asList(types);
    }

    public EntityType(NodeType type, EntityType entityType) {
        this.type = new ArrayList<>();
        this.type.add(type);
        this.type.addAll(entityType.getType());
    }

    public EntityType(AbstractSyntaxTree AST) {
        if (typeMappings.containsKey(AST.getName())) {
            if (!AST.hasChildren())
                this.type = List.of(typeMappings.get(AST.getName()));
            else {
                List<NodeType> types = new ArrayList<>();
                AbstractSyntaxTree node = AST;
                while (node.hasChildren()) {
                    types.add(typeMappings.get(node.getName()));
                    node = node.getChildren().get(0);
                }
                types.add(typeMappings.get(node.getName()));
                this.type = types;
            }
        }
    }

    public boolean containsSubType(NodeType nodeType) {
        return containsSubType(new EntityType(nodeType));
    }

    public boolean containsSubType(EntityType entityType) {
        List<NodeType> typeList = entityType.getType();
        if (type.size() == typeList.size())
            return type.equals(typeList);
        if (type.size() < typeList.size())
            return false;
        if (typeList.size() == 1) {
            if (typeList.get(0) == NodeType.ARRAY)
                return type.get(0) == NodeType.ARRAY;
            return type.get(type.size() - 1) == typeList.get(0);
        }
        int offset = type.size() - typeList.size();
        for (int i = offset; i < typeList.size(); i++) {
            if (typeList.get(i - offset) != type.get(i))
                return false;
        }
        return true;
    }

    public boolean isType(NodeType type) {
        if (this.type.size() != 1)
            return false;
        return this.type.get(0) == type;
    }

    public boolean startsWith(NodeType type) {
        if (this.type.isEmpty())
            return false;
        return this.type.get(0) == type;
    }

    private void setType(List<NodeType> type) {
        this.type = type;
    }

    public EntityType index(int depth, int lineNumber) {
        List<NodeType> types = type;
        EntityType entityType = new EntityType();
        if (depth < types.size()) {
            entityType.setType(types.subList(depth, types.size()));
            return entityType;
        }
        throw new ArrayBoundsError("Array index depth " + depth + " is greater than array depth " + (types.size() - 1), lineNumber);
    }

    @Override
    public String toString() {
        if (type.isEmpty())
            return "";
        if (type.size() == 1)
            return this.type.get(0).toString();
        return this.type.toString();
    }
}
