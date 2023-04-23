package com.piedpiper.bolt.parser;

import java.util.List;

import com.piedpiper.bolt.lexer.Token;

public class Parser {
    /*
     * PROG ::= (STMNT)+
     * STMNT ::= EXPR / COND / LOOP / FUNC / VARDECL
     * EXPR ::= "(" EXPR ")" / EXPR OP EXPR / VALUE
     * VALUE ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )? / ( ARRAYINDEX )* ) / STRINGLIT / NUMBER / BOOLEAN / ARRAYLIT 
     * ARRAYLIT ::= "{" (VALUE ("," VALUE)* )? "}"
     * COND ::= ( "if" / "else" ("if")? )"(" EXPR ")" "{" (EXPR)* "}"
     * LOOP ::= ("for" / "while") "(" EXPR ")" "{" (EXPR)* "}"
     * FUNC ::= "fn" ID "(" (EXPR ("," EXPR)* )? ")" "{" (EXPR)* "}"
     * ARRAYINDEX ::= "[" NUMBER "]"
     * ARRAYTYPE ::= "Array" "<" TYPE ">"
     * IMMUTABLEARRAYDECL ::= "const" ARRAYTYPE ID ( ARRAYINDEX )? "=" ARRAYLIT
     * ARRAYDECL ::= ("const")? ("mut")? ARRAYTYPE ID ARRAYINDEX ( "=" ARRAYLIT )? / IMMUTABLEARRAYDECL
     * VARDECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAYDECL
     * TYPE ::= "int" / "string" / "float" / "boolean" / ARRAYTYPE
    */

    private ParseTree parseTree;

    public void parse(List<Token> tokens) {
        
    }


}
