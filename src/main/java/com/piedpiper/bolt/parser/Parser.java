package com.piedpiper.bolt.parser;

import java.util.List;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;

public class Parser {
    /*
     * STMNT ::= EXPR / GROUP / COND / LOOP / FUNCDEF / VARDECL / VARASSIGN
     * GROUP ::= "( EXPR )"
     * EXPR ::=  VALUE ( OP ( EXPR / GROUP ) )* / UNARYOPUSE
     * UNARYOPUSE ::= ( LEFTUNARYOP / UNARYOP ) ( ID / BOOLEAN / NUMBER / ARRAYACCESS / FUNCCALL ) / ( ID / NUMBER / ARRAYACCESS / FUNCCALL ) (UNARYOP)
     * LEFTUNARYOP ::= "!" / "-"
     * UNARYOP ::= "++" / "--"
     * VALUE ::=  FUNCCALL / ARRAYACCESS / ID / STRINGLIT / NUMBER / BOOLEAN / ARRAYLIT 
     * FUNCCALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
     * ARRAYACCESS ::= ID ( ARRAYINDEX )+
     * ARRAYINDEX ::= "[" NUMBER "]"
     * ARRAYLIT ::= "{" (VALUE ("," VALUE)* )? "}"
     * COND ::= IF ( "else" IF )* ( ELSE )?
     * IF ::= "if" "(" EXPR ")" ( ( "{" (CONTROLFLOW / EXPR / GROUP / VALUE) "}") / (CONTROLFLOW / EXPR / GROUP / VALUE) ) 
     * ELSE ::= "else" ( ( "{" (CONTROLFLOW / EXPR / GROUP / VALUE) "}") / (CONTROLFLOW / EXPR / GROUP / VALUE) ) 
     * LOOP ::= ("for" / "while") "(" EXPR ")" "{" ( EXPR / CONTROLFLOW )* "}"
     * FUNCDEF ::= "fn" ID "(" (EXPR ("," EXPR)* )? ")" ( ":" TYPE )? "{" (EXPR)* "}"
     * ARRAYTYPE ::= "Array" "<" TYPE ">"
     * IMMUTABLEARRAYDECL ::= "const" ARRAYTYPE ID ( ARRAYINDEX )? "=" ARRAYLIT
     * ARRAYDECL ::= ("const")? ("mut")? ARRAYTYPE ID ARRAYINDEX ( "=" ARRAYLIT )? / IMMUTABLEARRAYDECL
     * VARDECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAYDECL
     * VARASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = VALUE 
     * TYPE ::= "int" / "string" / "float" / "boolean" / ARRAYTYPE
     * CONTROLFLOW ::= "return" ( EXPR / GROUP )? / "continue" / "break"
    */
    private List<Token> tokens;
    private int position = 0;
    private Token current;
    private Token next; // use this to look ahead
    
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

    
    public ParseTree parse() {
        ParseTree node = new ParseTree("PROGRAM");
        while (!atEnd()) {
            node.appendChildren(parseStatement());
        }
        return node;
    }

    // STMNT ::= EXPR / GROUP / COND / LOOP / FUNCDEF / VARDECL / VARASSIGN
    public ParseTree parseStatement() {
        ParseTree node = new ParseTree("STMT");
        if (current.getName() == TokenType.KW_CONST || isPrimitiveType(current) || current.getName() == TokenType.KW_ARR) {
            node.appendChildren(parseVariableDeclaration());
        }
        else if (current.getName() == TokenType.LEFT_PAREN) {
            node.appendChildren(parseGroup());
        }
        else if (current.getName() == TokenType.KW_FOR || current.getName() == TokenType.KW_WHILE) {
            node.appendChildren(parseLoop());
        }
        else if (current.getName() == TokenType.KW_IF || current.getName() == TokenType.KW_ELSE) {
            node.appendChildren(parseConditional());
        }
        else if (current.getName() == TokenType.KW_FN) {
            node.appendChildren(parseFunctionDefinition());
        }
        else if (isID(current)) {
            
        }
        return node;
    }

    // GROUP ::= "( EXPR )"
    public ParseTree parseGroup() {
        return new ParseTree("GROUP", List.of(
            parseExpectedToken(TokenType.LEFT_PAREN, current),
            parseExpr(),
            parseExpectedToken(TokenType.RIGHT_PAREN, current)
        ));
    }

    // EXPR ::=  VALUE ( OP ( EXPR / GROUP ) )* / UNARYOPUSE
    public ParseTree parseExpr() {
        ParseTree node = new ParseTree("EXPR");
        List<String> unaryOps = List.of("!", "++", "--");
        if (unaryOps.contains(current.getValue()) || current.getValue().equals("-")) {
            node.appendChildren(parseUnaryOpUse());
            return node;
        }
        node.appendChildren(parseValue());
        while (isOp(current) && !unaryOps.contains(current.getValue())) {
            node.appendChildren(new ParseTree(current));
            move();
            if (current.getName() == TokenType.LEFT_PAREN)
                node.appendChildren(parseGroup());
            else
                node.appendChildren(parseExpr());
        }
        if (unaryOps.contains(current.getValue()))
            throw new SyntaxError("Unary operator used where binary operator expected", current.getLineNumber());
        return node;
    }

    // UNARYOPUSE ::= ( LEFTUNARYOP / UNARYOP ) ( ID / BOOLEAN / NUMBER / ARRAYACCESS / FUNCCALL ) / ( ID / NUMBER / ARRAYACCESS / FUNCCALL ) (UNARYOP)
    public ParseTree parseUnaryOpUse() {
        ParseTree node = new ParseTree("UNARYOPUSE");
        return node;
    }

    // LEFTUNARYOP ::= "!" / "-"
    public ParseTree parseLeftUnaryOp() {
        if (current.getValue().equals("!") || current.getValue().equals("-"))
            return parseExpectedToken(TokenType.OP, current, current.getValue());
        else
            throw new SyntaxError("Expected LEFTUNARYOP but got " + current.getName() + " ('" + current.getValue() +"')", current.getLineNumber());
    }

    // UNARYOP ::= "++" / "--"
    public ParseTree parseUnaryOp() {
        if (current.getValue().equals("++") || current.getValue().equals("--"))
            return parseExpectedToken(TokenType.OP, current, current.getValue());
        else
            throw new SyntaxError("Expected LEFTUNARYOP but got " + current.getName() + " ('" + current.getValue() +"')", current.getLineNumber());
    }

    // VALUE ::=  FUNCCALL / ARRAYACCESS / ID / STRINGLIT / NUMBER / BOOLEAN / ARRAYLIT 
    public ParseTree parseValue() {
        ParseTree node = new ParseTree("VALUE");
        if (isNumber(current))
            node.appendChildren(parseExpectedToken(TokenType.NUMBER, current));
        else if (isString(current))
            node.appendChildren(parseExpectedToken(TokenType.STRING, current));
        else if (isBooleanLiteral(current))
            node.appendChildren(parseExpectedToken(current.getName(), current));
        else if (current.getName() == TokenType.LEFT_CB)
            node.appendChildren(parseArrayLiteral());
        else if (isID(current)) {
            if (next.getName() == TokenType.LEFT_SQB)
                node.appendChildren(parseArrayAccess());
            else if (next.getName() == TokenType.LEFT_PAREN)
                node.appendChildren(parseFunctionCall());
            else
                node.appendChildren(parseExpectedToken(TokenType.ID, current));
        }
        return node;
    }

    // FUNCCALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
    public ParseTree parseFunctionCall() {
        ParseTree node = new ParseTree("FUNCCALL");
        node.appendChildren(parseExpectedToken(TokenType.LEFT_PAREN, current));
        // TODO: params
        node.appendChildren(parseExpectedToken(TokenType.RIGHT_PAREN, current));
        return node;
    }

    // ARRAYACCESS ::= ID ( ARRAYINDEX )*
    public ParseTree parseArrayAccess() {
        ParseTree node = new ParseTree("ARRAYACCESS");
        boolean hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
        if (next != null && next.getName() != TokenType.LEFT_SQB) {
            throw new SyntaxError("Expected LEFT_SQB but got " + next.getName() + " ('" + next.getValue() + "')", next.getLineNumber());
        }
        node.appendChildren(parseExpectedToken(TokenType.ID, current));
        while (hasLeftSQB) {
            hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
            node.appendChildren(parseArrayIndex());
        }
        return node;
    }

    // ARRAYINDEX ::= "[" NUMBER "]"
    public ParseTree parseArrayIndex() {
        return new ParseTree("ARRAYINDEX", List.of(
            parseExpectedToken(TokenType.LEFT_SQB, current),
            parseExpectedToken(TokenType.NUMBER, current),
            parseExpectedToken(TokenType.RIGHT_SQB, current)
        ));
    }

    // ARRAYLIT ::= "{" (VALUE ("," VALUE)* )? "}"
    public ParseTree parseArrayLiteral() {
        ParseTree node = new ParseTree("ARRAYLIT");
        return node;
    }

    // COND ::= IF ( "else" IF )* ( ELSE )?
    public ParseTree parseConditional() {
        ParseTree node = new ParseTree("COND");
        return node;
    }

    // "if" "(" EXPR ")" ( ( "{" (CONTROLFLOW / EXPR / GROUP / VALUE) "}") / (CONTROLFLOW / EXPR / GROUP / VALUE) )
    public ParseTree parseIf() {
        ParseTree node = new ParseTree("IF");
        return node;
    }

    // ELSE ::= "else" ( ( "{" (CONTROLFLOW / EXPR / GROUP / VALUE) "}") / (CONTROLFLOW / EXPR / GROUP / VALUE) ) 
    public ParseTree parseElse() {
        ParseTree node = new ParseTree("ELSE");
        return node;
    }

    // LOOP ::= ("for" / "while") "(" EXPR ")" "{" ( EXPR / CONTROLFLOW )* "}"
    public ParseTree parseLoop() {
        ParseTree node = new ParseTree("LOOP");
        return node;
    }

    // FUNCDEF ::= "fn" ID "(" (EXPR ("," EXPR)* )? ")" ( ":" TYPE )? "{" (EXPR)* "}"
    public ParseTree parseFunctionDefinition() {
        ParseTree node = new ParseTree("FUNC");
        return node;
    }

    // ARRAYTYPE ::= "Array" "<" TYPE ">"
    public ParseTree parseArrayType() {
        ParseTree node = new ParseTree("ARRAYTYPE");
        node.appendChildren(
            parseExpectedToken(TokenType.KW_ARR, current),
            parseExpectedToken(TokenType.OP, current, "<"),
            parseType(),
            parseExpectedToken(TokenType.OP, current, ">")

        );
        return node;
    }

    // IMMUTABLEARRAYDECL ::= "const" ARRAYTYPE ID ( ARRAYINDEX )? "=" ARRAYLIT
    public ParseTree parseImmutableArrayDeclaration() {
        ParseTree node = new ParseTree("IMMUTABLEARRAYDECL");
        return node;
    }

    // ARRAYDECL ::= ("const")? ("mut")? ARRAYTYPE ID ARRAYINDEX ( "=" ARRAYLIT )? / IMMUTABLEARRAYDECL
    public ParseTree parseArrayDeclaration() {
        ParseTree node = new ParseTree("ARRAYDECL");
        return node;
    }

    // VARDECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAYDECL
    public ParseTree parseVariableDeclaration() {
        ParseTree node = new ParseTree("VARDECL");
        if (current.getName() == TokenType.KW_CONST) {
            if (next.getName() == TokenType.KW_MUT || next.getName() == TokenType.KW_ARR) {
                node.appendChildren(parseArrayDeclaration());
                return node; // let array declaraction do the rest
            }
            else
                node.appendChildren(parseExpectedToken(TokenType.KW_CONST, current));
        }
        else {
            if (current.getName() == TokenType.KW_MUT || current.getName() == TokenType.KW_ARR) {
                node.appendChildren(parseArrayDeclaration());
                return node; // let array declaraction do the rest
            }
            node.appendChildren(parseType());
            if (current.getValue().equals("="))
                node.appendChildren(parseExpectedToken(TokenType.OP, current, "="), parseExpr());
        }
        return node;
    }

    // VARASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = VALUE 
    public ParseTree parseVariableAssignment() { // reassignment of already declared variable
        ParseTree node = new ParseTree("VARASSIGN", List.of(parseExpectedToken(TokenType.ID, current)));
        List<String> assignmentOperators = List.of("+=", "-=", "*=", "/=", "=");
        if (current.getName().equals(TokenType.OP)) {
            if (assignmentOperators.contains(current.getValue()))
                node.appendChildren(parseExpectedToken(TokenType.OP, current, current.getValue()));
            else
                throw new SyntaxError("Unexpected character '" + current.getValue() + "' in variable assignment", current.getLineNumber());
            node.appendChildren(parseValue());            
        }
        else
            throw new SyntaxError("Expected equality operator but got " + current.getName() + "('" + current.getValue() + "')", current.getLineNumber());
        return node;
    }

    // TYPE ::= "int" / "string" / "float" / "boolean" / ARRAYTYPE
    public ParseTree parseType() {
        if (current.getName() == TokenType.KW_ARR)
            return parseArrayType();
        if (isPrimitiveType(current)) {
            ParseTree node = new ParseTree(current);
            move();
            return node;
        }
        throw new SyntaxError("Expected TYPE but got " + current.getName() + " ('" + current.getValue() + "')", current.getLineNumber());
    }

    // CONTROLFLOW ::= "return" ( EXPR / GROUP )? / "continue" / "break"
    public ParseTree parseControlFlow() {
        ParseTree node = new ParseTree("CONTROLFLOW");
        if (current.getName() == TokenType.KW_CNT || current.getName() == TokenType.KW_BRK) {
            node.appendChildren(parseExpectedToken(current.getName(), current));
        }
            
        else if (current.getName() == TokenType.KW_RET) {
            node.appendChildren(parseExpectedToken(current.getName(), current));
            // TODO: parseExpr()
        }
        else {
            throw new SyntaxError("Expected KW_CNT, KW_BRK, or KW_RET EXPR but got " + current.getName() + "('" + current.getValue() + "')", current.getLineNumber());
        }
        return node;
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
