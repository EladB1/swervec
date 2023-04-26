package com.piedpiper.bolt.parser;

import java.util.ArrayList;
import java.util.List;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;

public class Parser {
    /*
     * STMNT ::= EXPR / GROUP / COND / LOOP / FUNC / VARDECL / VARASSIGN
     * GROUP ::= "( EXPR )"
     * EXPR ::=  VALUE ( OP EXPR )* / UNARYOPUSE
     * UNARYOPUSE ::= ( LEFTUNARYOP / UNARYOP ) ( ID / BOOLEAN / NUMBER / ARRAYACCESS / FUNCCALL ) / ( ID / NUMBER / ARRAYACCESS / FUNCCALL ) (UNARYOP)
     * LEFTUNARYOP ::= "!" / "-"
     * UNARYOP ::= "++" / "--"
     * VALUE ::=  FUNCCALL / ARRAYACCESS / STRINGLIT / NUMBER / BOOLEAN / ARRAYLIT 
     * FUNCCALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )?
     * ARRAYACCESS ::= ID ( ARRAYINDEX )*
     * ARRAYINDEX ::= "[" NUMBER "]"
     * ARRAYLIT ::= "{" (VALUE ("," VALUE)* )? "}"
     * COND ::= ( "else" ("if")? )"(" EXPR ")" ( ( "{" ( ( "return" )? EXPR / "return")* "}" )? / ( ( "return" )? EXPR / "return" ) ) / ELSE
     * ELSE ::= "else" ( ( "{" ( ("return")? EXPR / "return )*" "}" ) / ( ( "return" )? EXPR / "return" ) )
     * LOOP ::= ("for" / "while") "(" EXPR ")" "{" ( EXPR / CONTROLFLOW )* "}"
     * FUNC ::= "fn" ID "(" (EXPR ("," EXPR)* )? ")" ( ":" TYPE )? "{" (EXPR)* "}"
     * ARRAYTYPE ::= "Array" "<" TYPE ">"
     * IMMUTABLEARRAYDECL ::= "const" ARRAYTYPE ID ( ARRAYINDEX )? "=" ARRAYLIT
     * ARRAYDECL ::= ("const")? ("mut")? ARRAYTYPE ID ARRAYINDEX ( "=" ARRAYLIT )? / IMMUTABLEARRAYDECL
     * VARDECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAYDECL
     * VARASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = VALUE 
     * TYPE ::= "int" / "string" / "float" / "boolean" / ARRAYTYPE
     * CONTROLFLOW ::= "return" ( EXPR )? / "continue" / "break"
    */
    private List<Token> tokens;
    private int position = 0;
    private Token current;
    private Token next; // use this to look ahead
    
    private ParseTree root; // the AST that will result from parsing

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        current = tokens.get(position);
        next = tokens.size() == 1 ? null : tokens.get(position + 1);
    }

    private boolean atEnd() {
        return position >= tokens.size();
    }

    private void move() {
        position++;
        if (!atEnd()) {
            current = tokens.get(position);
            next = position == tokens.size() - 1 ? null : tokens.get(position + 1);
        }
    }

    
    public void parse() {
        
    }

    // STMNT ::= EXPR / GROUP / COND / LOOP / FUNC / VARDECL / VARASSIGN
    public void parseStatement() {

    }

    // GROUP ::= "( EXPR )"
    public void parseGroup() {

    }

    // EXPR ::=  VALUE ( OP EXPR )* / UNARYOPUSE
    public void parseExpr() {

    }

    // UNARYOPUSE ::= ( LEFTUNARYOP / UNARYOP ) ( ID / BOOLEAN / NUMBER / ARRAYACCESS / FUNCCALL ) / ( ID / NUMBER / ARRAYACCESS / FUNCCALL ) (UNARYOP)
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
    public List<ParseTree> parseArrayAccess() {
        List<ParseTree> nodes = new ArrayList<>();
        boolean hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
        if (next != null && next.getName() != TokenType.LEFT_SQB) {
            throw new SyntaxError("Expected LEFT_SQB but got " + next.getName() + " ('" + next.getValue() + "')", next.getLineNumber());
        }
        nodes.add(parseExpectedToken(TokenType.ID, current));
        while (hasLeftSQB) {
            hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
            nodes.addAll(parseArrayIndex());
        }
        return nodes;
    }

    // ARRAYINDEX ::= "[" NUMBER "]"
    public List<ParseTree> parseArrayIndex() {
        return List.of(
            parseExpectedToken(TokenType.LEFT_SQB, current),
            parseExpectedToken(TokenType.NUMBER, current),
            parseExpectedToken(TokenType.RIGHT_SQB, current)
        );
    }

    // ARRAYLIT ::= "{" (VALUE ("," VALUE)* )? "}"
    public void parseArrayLiteral() {

    }

    // COND ::= ( "else" ("if")? )"(" EXPR ")" ( ( "{" ( ( "return" )? EXPR / "return")* "}" )? / ( ( "return" )? EXPR / "return" ) ) / ELSE
    public void parseConditional() {

    }

    // ELSE ::= "else" ( ( "{" ( ("return")? EXPR / "return )*" "}" ) / ( ( "return" )? EXPR / "return" ) )
    public void parseElse() {

    }

    // LOOP ::= ("for" / "while") "(" EXPR ")" "{" ( EXPR / CONTROLFLOW )* "}"
    public void parseLoop() {

    }

    // FUNC ::= "fn" ID "(" (EXPR ("," EXPR)* )? ")" ( ":" TYPE )? "{" (EXPR)* "}"
    public void parseFunctionDefinition() {

    }

    // ARRAYTYPE ::= "Array" "<" TYPE ">"
    public List<ParseTree> parseArrayType() {
        List<ParseTree> nodes = List.of(
            parseExpectedToken(TokenType.KW_ARR, current),
            parseExpectedToken(TokenType.OP, current, "<")
        );
        nodes.addAll(parseType());
        nodes.add(parseExpectedToken(TokenType.OP, current, ">"));
        return nodes;
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

    // VARASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = VALUE 
    public void parseVariableAssignment() { // reassignment of already declared variable

    }

    // TYPE ::= "int" / "string" / "float" / "boolean" / ARRAYTYPE
    public List<ParseTree> parseType() {
        if (current.getName() == TokenType.KW_ARR)
            return parseArrayType();
        if (isPrimitiveType(current)) {
            List<ParseTree> nodes = List.of(new ParseTree(current));
            move();
            return nodes;
        }
        throw new SyntaxError("Expected TYPE but got " + current.getName() + " ('" + current.getValue() + "')", current.getLineNumber());
    }

    // CONTROLFLOW ::= "return" ( EXPR )? / "continue" / "break"
    public List<ParseTree> parseControlFlow() {
        List<ParseTree> nodes = new ArrayList<>();
        if (current.getName() == TokenType.KW_CNT || current.getName() == TokenType.KW_BRK) {
            nodes.add(new ParseTree(current));
            move();
        }
            
        else if (current.getName() == TokenType.KW_RET) {
            nodes.add(new ParseTree(current));
            move();
            // TODO: parseExpr()
        }
        else {
            throw new SyntaxError("Expected KW_CNT, KW_BRK, or KW_RET EXPR but got " + current.getName() + "('" + current.getValue() + "')", current.getLineNumber());
        }
        return nodes;
    }

    /* Utility parsing method */
    private ParseTree parseExpectedToken(TokenType expectedToken, Token actualToken) {
        ParseTree node;
        String message = "";
        if (atEnd())
            message = "Expected " + expectedToken + " but reached EOF";
        else {
            if (actualToken.getName() == expectedToken) {
                node = new ParseTree(actualToken);
                move();
                return node;
            }
            else {
                message = String.format("Expected %s but got %s ('%s')", expectedToken, actualToken.getName(), actualToken.getValue());
                
            }
        }
        throw new SyntaxError(message, actualToken.getLineNumber());
    }

      /* Utility parsing method */
      private ParseTree parseExpectedToken(TokenType expectedToken, Token actualToken, String value) {
        ParseTree node;
        String message = "";
        if (atEnd())
            message = "Expected " + expectedToken + " but reached EOF";
        else {
            if (actualToken.getName() == expectedToken && actualToken.getValue() == value) {
                node = new ParseTree(actualToken);
                move();
                return node;
            }
            else {
                message = String.format("Expected %s but got %s ('%s')", expectedToken, actualToken.getName(), actualToken.getValue());
                
            }
        }
        throw new SyntaxError(message, actualToken.getLineNumber());
    }

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

    


}
