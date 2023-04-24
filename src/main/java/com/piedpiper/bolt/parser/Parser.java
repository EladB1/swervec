package com.piedpiper.bolt.parser;

import java.util.List;

import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;

public class Parser {
    /*
     * STMNT ::= EXPR / COND / LOOP / FUNC / VARDECL
     * EXPR ::= "(" EXPR ")" / EXPR OP EXPR / UNARYOPUSE / VALUE
     * UNARYOPUSE ::= ( LEFTUNARYOP / UNARYOP ) (ID / BOOLEAN / NUMBER / ARRAYACCESS / FUNCCALL) / (ID / NUMBER / ARRAYACCESS / FUNCCALL) (UNARYOP)
     * LEFTUNARYOP ::= "!" / "-"
     * UNARYOP ::= "++" / "--"
     * VALUE ::=  FUNCCALL / ARRAYACCESS / STRINGLIT / NUMBER / BOOLEAN / ARRAYLIT 
     * FUNCCALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )?
     * ARRAYACCESS ::= ID ( ARRAYINDEX )*
     * ARRAYINDEX ::= "[" NUMBER "]"
     * ARRAYLIT ::= "{" (VALUE ("," VALUE)* )? "}"
     * COND ::= ( "if" / "else" ("if")? )"(" EXPR ")" "{" ( ( "return" )? EXPR / "return")* "}"
     * LOOP ::= ("for" / "while") "(" EXPR ")" "{" ( EXPR / CONTROLFLOW )* "}"
     * FUNC ::= "fn" ID "(" (EXPR ("," EXPR)* )? ")" ( ":" TYPE )? "{" (EXPR)* "}"
     * ARRAYTYPE ::= "Array" "<" TYPE ">"
     * IMMUTABLEARRAYDECL ::= "const" ARRAYTYPE ID ( ARRAYINDEX )? "=" ARRAYLIT
     * ARRAYDECL ::= ("const")? ("mut")? ARRAYTYPE ID ARRAYINDEX ( "=" ARRAYLIT )? / IMMUTABLEARRAYDECL
     * VARDECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAYDECL
     * TYPE ::= "int" / "string" / "float" / "boolean" / ARRAYTYPE
     * CONTROLFLOW ::= "return" ( EXPR )? / "continue" / "break"
    */
    private Token current;
    private Token next; // use this to look ahead
    
    private ParseTree parseTree; // the AST that will result from parsing

    private boolean isNumber(Token token) {
        return token.getName() == TokenType.NUMBER;
    }

    private boolean isOp(Token token) {
        return token.getName() == TokenType.OP;
    }

    private boolean isID(Token token) {
        return token.getName() == TokenType.ID;
    }

    private boolean isString(Token token) {
        return token.getName() == TokenType.STRING;
    }

    private boolean isBooleanLiteral(Token token) {
        return token.getName() == TokenType.KW_TRUE || token.getName() == TokenType.KW_FALSE;
    }

    private boolean isPrimitiveType(Token token) {
        return List.of(
            TokenType.KW_INT,
            TokenType.KW_FLOAT,
            TokenType.KW_STR,
            TokenType.KW_BOOL
        ).contains(token.getName());
    }

    // STMNT ::= EXPR / COND / LOOP / FUNC / VARDECL
    public void parseStatement() {
        //parseExpr() || parseCondition() || parseLoop() || parseFunc() || parseVariableDeclaration();
    }

    // "(" EXPR ")" / EXPR OP EXPR / UNARYOPUSE / VALUE
    public void parseExpr() {

    }

    // UNARYOPUSE ::= ( LEFTUNARYOP / UNARYOP ) (ID / BOOLEAN / NUMBER / ARRAYACCESS / FUNCCALL) / (ID / NUMBER / ARRAYACCESS / FUNCCALL) (UNARYOP)
    public void parseUnaryOpUse() {

    }

    // LEFTUNARYOP ::= "!" / "-"
    public void parseLeftUnaryOp() {

    }

    // UNARYOP ::= "++" / "--"
    public void parseUnaryOp() {

    }

    // VALUE ::=  FUNCCALL / ARRAYACCESS / STRINGLIT / NUMBER / BOOLEAN / ARRAYLIT 
    public void parseValue() {

    }

    // FUNCCALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )?
    public void parseFunctionCall() {

    }

    // ARRAYACCESS ::= ID ( ARRAYINDEX )*
    public void parseArrayAccess() {

    }

    // ARRAYINDEX ::= "[" NUMBER "]"
    public void parseArrayIndex() {

    }

    // ARRAYLIT ::= "{" (VALUE ("," VALUE)* )? "}"
    public void parseArrayLiteral() {

    }

    // COND ::= ( "if" / "else" ("if")? )"(" EXPR ")" "{" ( ( "return" )? EXPR / "return")* "}"
    public void parseConditional() {
    }

    // LOOP ::= ("for" / "while") "(" EXPR ")" "{" ( EXPR / CONTROLFLOW )* "}"
    public void parseLoop() {

    }

    // FUNC ::= "fn" ID "(" (EXPR ("," EXPR)* )? ")" ( ":" TYPE )? "{" (EXPR)* "}"
    public void parseFunctionDefinition() {

    }

    // ARRAYTYPE ::= "Array" "<" TYPE ">"
    public void parseArrayType() {

    }

    // IMMUTABLEARRAYDECL ::= "const" ARRAYTYPE ID ( ARRAYINDEX )? "=" ARRAYLIT
    public void parseImmutableArrayDeclaration() {

    }

    // ARRAYDECL ::= ("const")? ("mut")? ARRAYTYPE ID ARRAYINDEX ( "=" ARRAYLIT )? / IMMUTABLEARRAYDECL
    public void parseArrayDeclaration() {

    }

    // VARDECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAYDECL
    public void parseVariableDeclaration() {

    }

    // TYPE ::= "int" / "string" / "float" / "boolean" / ARRAYTYPE
    public void parseType() {

    }

    // CONTROLFLOW ::= "return" ( EXPR )? / "continue" / "break"
    public void parseControlFlow() {

    }

    public void parse(List<Token> tokens) {
        
    }


}
