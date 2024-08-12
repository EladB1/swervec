package com.piedpiper.swerve.symboltable;

import com.piedpiper.swerve.parser.AbstractSyntaxTree;
import com.piedpiper.swerve.semantic.EntityType;

public interface ProcedureSymbol {
    public String getName();
    public EntityType getReturnType();
    public EntityType[] getParamTypes();
    public boolean isBuiltIn();
    public AbstractSyntaxTree getFnBodyNode();
    public boolean hasGenericParam();
    public boolean returnsGeneric();
    public boolean hasCompatibleParams(EntityType[] params);
    public String formSignature();
}
