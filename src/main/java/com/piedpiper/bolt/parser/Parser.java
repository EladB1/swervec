package com.piedpiper.bolt.parser;

import java.util.List;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;

public class Parser {
    /*
     * PROGRAM ::= ( STMNT / FUNC-DEF )+
     * STMNT ::= EXPR / COND / LOOP / VAR-DECL / VAR-ASSIGN
     * EXPR ::= ( ARITH-EXPR / LOGICAL-OR / TERNARY ) ( ";" )?
     * ARITH-EXPR ::= TERM (ADD-OP TERM)*
     * TERM ::= FACTOR (MULT-OP TERM)*
     * FACTOR ::= VALUE / "(" EXPR ")" / UNARY-OP
     * LOGICAL-OR ::=  ( LOGICAL-OR "||" )* LOGICAL-AND
     * LOGICAL-AND ::= ( LOGICAL-AND "&&" )* CMPR-EXPR
     * CMPR-EXPR ::= EXPR ( CMPR-OP EXPR )?
     * UNARY-OP ::= LEFT-UNARY-OP / ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) ( "++" / "--" )
     * LEFT-UNARY-OP ::= ( "++" / "--" / "-" ) ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) / "!" (BOOLEAN / ID / ARRAY-ACCESS / FUNC-CALL)
     * ADD-OP ::= "+" / "-" / "^" / "&"
     * MULT-OP ::= "*" / "/" / "%"
     * CMPR-OP ::= "<" / ">" / "<=" / ">=" / "!=" / "=="
     * TERNARY ::= EXPR ? VALUE : VALUE
     * VALUE ::=  FUNC-CALL / ARRAY-ACCESS / ID / STRING-LIT / NUMBER / BOOLEAN / ARRAY-LIT 
     * FUNC-CALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
     * ARRAY-ACCESS ::= ID ( ARRAY-INDEX )+
     * ARRAY-INDEX ::= "[" NUMBER / ARRAY-ACCESS / FUNC-CALL / ID "]"
     * ARRAY-LIT ::= "{" (EXPR ("," EXPR)* )? "}"
     * COND ::= IF ( "else" IF )* ( ELSE )?
     * IF ::= "if" "(" EXPR ")" COND-BODY
     * ELSE ::= "else" COND-BODY
     * COND-BODY ::= ( ( "{" ( BLOCK-BODY )* "}" ) / BLOCK-BODY )
     * BLOCK-BODY ::= CONTROL-FLOW / STMNT
     * LOOP ::= ( WHILE-LOOP / FOR-LOOP ) "{" ( BLOCK-BODY )* "}"
     * WHILE-LOOP ::= "while" "(" EXPR ")"
     * FOR-LOOP ::= "for" "(" EXPR ( ";" EXPR ";" EXPR )? ")"
     * FUNC-DEF ::= "fn" ID "(" (FUNC-PARAM ("," FUNC-PARAM)* )? ")" ( ":" TYPE )? "{" ( BLOCK-BODY )* "}"
     * FUNC-PARAM ::= TYPE ID ("=" EXPR )?
     * ARRAY-TYPE ::= "Array" "<" TYPE ">"
     * IMMUTABLE-ARRAY-DECL ::= "const" ARRAY-TYPE ID ( ARRAY-INDEX )? "=" ARRAY-LIT / ID
     * ARRAY-DECL ::= ("const" "mut")? ARRAY-TYPE ID ARRAY-INDEX ( "=" ARRAY-LIT / ID )? / IMMUTABLE-ARRAY-DECL
     * VAR-DECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAY-DECL
     * VAR-ASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = VALUE 
     * TYPE ::= "int" / "string" / "float" / "boolean" / ARRAY-TYPE
     * CONTROL-FLOW ::= "return" ( EXPR )? / "continue" / "break"
    */
    private List<Token> tokens;
    private int position = 0;
    private Token current;
    private Token next; // use this to look ahead
    List<String> leftUnaryOps = List.of("-", "!", "++", "--");
    List<String> rightUnaryOps = leftUnaryOps.subList(2, 4);
    List<String> assignmentOps = List.of("=", "+=", "-=", "*=", "/=");
    List<String> comparisonOps = List.of("<", ">", "<=", ">=", "!=", "==");
    List<String> addOps = List.of("+", "-", "^", "&");
    List<String> multOps = List.of("*", "/", "%");
    
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

    // PROGRAM ::= ( STMNT / FUNCDEF )+
    public ParseTree parse() {
        ParseTree node = new ParseTree("PROGRAM");
        while (!atEnd()) {
            if (current.getName() == TokenType.KW_FN) {
                node.appendChildren(parseFunctionDefinition());
            }
            else {
                node.appendChildren(parseStatement());
            }
        }
        return node;
    }

    // STMNT ::= EXPR / COND / LOOP / VAR-DECL / VAR-ASSIGN
    public ParseTree parseStatement() {
        ParseTree node = new ParseTree("STMT");
        
        if (current.getName() == TokenType.KW_CONST || isPrimitiveType(current) || current.getName() == TokenType.KW_ARR) {
            node.appendChildren(parseVariableDeclaration());
        }
        else if (current.getName() == TokenType.LEFT_PAREN || current.getName() == TokenType.LEFT_CB || leftUnaryOps.contains(current.getValue()) || isBooleanLiteral(current) || isString(current)) {
            node.appendChildren(parseExpr());
        }
        else if (current.getName() == TokenType.KW_FOR || current.getName() == TokenType.KW_WHILE) {
            node.appendChildren(parseLoop());
        }
        else if (current.getName() == TokenType.KW_IF || current.getName() == TokenType.KW_ELSE) {
            node.appendChildren(parseConditional());
        }
        else if (isID(current)) {
            if (isOp(next) && assignmentOps.contains(next.getValue()))
                node.appendChildren(parseVariableAssignment());
            else if (next.getName() == TokenType.LEFT_PAREN)
                node.appendChildren(parseFunctionCall());
            else if (next.getName() == TokenType.LEFT_SQB)
                node.appendChildren(parseArrayAccess());
        }
        return node;
    }

    // EXPR ::= ( ARITH-EXPR / LOGICAL-OR / TERNARY ) ( ";" )?
    public ParseTree parseExpr() {
        ParseTree node = new ParseTree("EXPR");

        if (current.getName() == TokenType.LEFT_PAREN) {
            return new ParseTree("EXPR", List.of(
                parseExpectedToken(TokenType.LEFT_PAREN, current),
                parseExpr(),
                parseExpectedToken(TokenType.RIGHT_PAREN, current)
            ));
        }

        List<String> unaryOps = List.of("!", "++", "--");
        if (unaryOps.contains(current.getValue()) || current.getValue().equals("-")) {
            node.appendChildren(parseUnaryOp());
            return node;
        }
        
        node.appendChildren(parseValue());
        while (isOp(current) && !unaryOps.contains(current.getValue())) {
            node.appendChildren(parseExpectedToken(TokenType.OP, current), parseExpr());
        }
        if (unaryOps.contains(current.getValue()))
            throw new SyntaxError("Unary operator used where binary operator expected", current.getLineNumber());
        return node;
    }

    // UNARY-OP ::= LEFT-UNARY-OP / ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) ( "++" / "--" )
    public ParseTree parseUnaryOp() {
        ParseTree node = new ParseTree("UNARY-OP");
        if (leftUnaryOps.contains(current.getValue()))
            node.appendChildren(parseLeftUnaryOp());
        else {
            if (isID(current)) {
                if (next.getName() == TokenType.LEFT_PAREN)
                    node.appendChildren(parseFunctionCall());
                else if (next.getName() == TokenType.LEFT_SQB)
                    node.appendChildren(parseArrayAccess());
                else
                    node.appendChildren(parseExpectedToken(TokenType.ID, current));
            }
            if (rightUnaryOps.contains(current.getValue()))
                node.appendChildren(parseExpectedToken(TokenType.OP, current));
        }
        return node;
    }

    // LEFT-UNARY-OP ::= ( "++" / "--" / "-" ) ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) / "!" (BOOLEAN / ID / ARRAY-ACCESS / FUNC-CALL)
    public ParseTree parseLeftUnaryOp() {
        ParseTree node = new ParseTree("LEFT-UNARY-OP");
        if (current.getValue().equals("++") || current.getValue().equals("--") || current.getValue().equals("-")) {
            node.appendChildren(parseExpectedToken(TokenType.OP, current, current.getValue()));
            if (isNumber(current))
                node.appendChildren(parseExpectedToken(TokenType.NUMBER, current));
            else if (isID(current)) {
                if (next != null && next.getName() == TokenType.LEFT_PAREN)
                    node.appendChildren(parseFunctionCall());
                else if (next != null && next.getName() == TokenType.LEFT_SQB)
                    node.appendChildren(parseArrayAccess());
                else
                    node.appendChildren(parseExpectedToken(TokenType.ID, current));
            }
            else {
                throw new SyntaxError("Invalid unary operator on " + current.getName());
            }
        }
        else if (current.getValue().equals("!")) {
            node.appendChildren(parseExpectedToken(TokenType.OP, current, current.getValue()));
            if (isBooleanLiteral(current))
                node.appendChildren(parseExpectedToken(current.getName(), current));
            else if (isID(current)) {
                if (next != null && next.getName() == TokenType.LEFT_PAREN)
                    node.appendChildren(parseFunctionCall());
                else if (next != null && next.getName() == TokenType.LEFT_SQB)
                    node.appendChildren(parseArrayAccess());
                else
                    node.appendChildren(parseExpectedToken(TokenType.ID, current));
            }
            else {
                throw new SyntaxError("Invalid unary operator on " + current.getName());
            }
        }
        else
            throw new SyntaxError("Expected LEFTUNARYOP but got " + current.getName() + " ('" + current.getValue() +"')", current.getLineNumber());
        return node;
    }

    // VALUE ::=  FUNC-CALL / ARRAY-ACCESS / ID / STRING-LIT / NUMBER / BOOLEAN / ARRAY-LIT 
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
            if (next != null && next.getName() == TokenType.LEFT_SQB)
                node.appendChildren(parseArrayAccess());
            else if (next != null && next.getName() == TokenType.LEFT_PAREN)
                node.appendChildren(parseFunctionCall());
            else
                node.appendChildren(parseExpectedToken(TokenType.ID, current));
        }
        return node;
    }

    // FUNC-CALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
    public ParseTree parseFunctionCall() {
        ParseTree node = new ParseTree("FUNC-CALL", List.of(
            parseExpectedToken(TokenType.ID, current),
            parseExpectedToken(TokenType.LEFT_PAREN, current))
        );
        if (current.getName() != TokenType.RIGHT_PAREN) {
            node.appendChildren(parseExpr());
            while (current.getName() != TokenType.RIGHT_PAREN) {
                node.appendChildren(parseExpectedToken(TokenType.COMMA, current), parseExpr());
            }
        }
        node.appendChildren(parseExpectedToken(TokenType.RIGHT_PAREN, current));
        return node;
    }

    // ARRAY-ACCESS ::= ID ( ARRAY-INDEX )+
    public ParseTree parseArrayAccess() {
        ParseTree node = new ParseTree("ARRAY-ACCESS");
        boolean hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
        if (!hasLeftSQB) {
            throw new SyntaxError("Expected LEFT_SQB but got " + next.getName() + " ('" + next.getValue() + "')", next.getLineNumber());
        }
        node.appendChildren(parseExpectedToken(TokenType.ID, current));
        while (hasLeftSQB) {
            hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
            node.appendChildren(parseArrayIndex());
        }
        return node;
    }

    // ARRAY-INDEX ::= "[" NUMBER / ARRAY-ACCESS / FUNC-CALL / ID "]"
    public ParseTree parseArrayIndex() {
        return new ParseTree("ARRAY-INDEX", List.of(
            parseExpectedToken(TokenType.LEFT_SQB, current),
            parseExpectedToken(TokenType.NUMBER, current),
            parseExpectedToken(TokenType.RIGHT_SQB, current)
        ));
    }

    // ARRAY-LIT ::= "{" (EXPR ("," EXPR)* )? "}"
    public ParseTree parseArrayLiteral() {
        ParseTree node = new ParseTree("ARRAY-LIT");
        node.appendChildren(parseExpectedToken(TokenType.LEFT_CB, current));
        if (current.getName() != TokenType.RIGHT_CB) {
            node.appendChildren(parseExpr());
            while (current.getName() != TokenType.RIGHT_CB) {
                node.appendChildren(parseExpectedToken(TokenType.COMMA, current), parseExpr());
            }
        }
        node.appendChildren(parseExpectedToken(TokenType.RIGHT_CB, current));
        return node;
    }

    // COND ::= IF ( "else" IF )* ( ELSE )?
    public ParseTree parseConditional() {
        ParseTree node = new ParseTree("COND", List.of(parseIf()));
        if (current.getName() == TokenType.KW_ELSE && next != null && next.getName() == TokenType.KW_IF) {
            while(current.getName() == TokenType.KW_ELSE && next != null && next.getName() == TokenType.KW_IF) {
                node.appendChildren(parseExpectedToken(TokenType.KW_ELSE, current), parseIf());
            }
        }
        if (current.getName() == TokenType.KW_ELSE)
            node.appendChildren(parseElse());
        return node;
    }

    // IF ::= "if" "(" EXPR ")" COND-BODY
    public ParseTree parseIf() {
        ParseTree node = new ParseTree("IF", List.of(
            parseExpectedToken(TokenType.KW_IF, current),
            parseExpectedToken(TokenType.LEFT_PAREN, current),
            parseExpr(),
            parseExpectedToken(TokenType.RIGHT_PAREN, current),
            parseConditionalBody()
        ));
        return node;
    }

    // ELSE ::= "else" COND-BODY
    public ParseTree parseElse() {
        return new ParseTree("ELSE", List.of(
            parseExpectedToken(TokenType.KW_ELSE, current),
            parseConditionalBody()
        ));
    }

    // COND-BODY = ( ( "{" ( BLOCK-BODY )* "}" ) / BLOCK-BODY )
    public ParseTree parseConditionalBody() {
        ParseTree node = new ParseTree("BLOCK-BODY");
        if (current.getName() == TokenType.LEFT_CB) {
            node.appendChildren(parseExpectedToken(TokenType.LEFT_CB, current));
            if (current.getName() != TokenType.RIGHT_CB) {
                while (current.getName() != TokenType.RIGHT_CB) {
                    node.appendChildren(parseBlockBody());
                }
            }
            node.appendChildren(parseExpectedToken(TokenType.RIGHT_CB, current));
        }
        else
            return parseBlockBody();
        return node;
    }

    // BLOCK-BODY = CONTROLFLOW / STMNT
    public ParseTree parseBlockBody() {
        return List.of(TokenType.KW_RET, TokenType.KW_CNT, TokenType.KW_BRK).contains(current.getName()) ? parseControlFlow() : parseStatement();
    }

    // LOOP ::= ( WHILE-LOOP / FOR-LOOP ) "{" ( BLOCK-BODY )* "}"
    public ParseTree parseLoop() {
        TokenType loopType = current.getName();
        ParseTree node = new ParseTree("LOOP", List.of(
            parseExpectedToken(loopType, current),
            parseExpectedToken(TokenType.LEFT_PAREN, current),
            parseExpr(),
            parseExpectedToken(TokenType.RIGHT_PAREN, current),
            parseExpectedToken(TokenType.LEFT_CB, current)
        ));
        while (current.getName() != TokenType.RIGHT_CB) {
            node.appendChildren(parseBlockBody());
        }
        node.appendChildren(parseExpectedToken(TokenType.RIGHT_CB, current));
        return node;
    }

    //  FUNC-DEF ::= "fn" ID "(" (FUNC-PARAM ("," FUNC-PARAM)* )? ")" ( ":" TYPE )? "{" ( BLOCK-BODY )* "}"
    public ParseTree parseFunctionDefinition() {
        ParseTree node = new ParseTree("FUNC-DEF", List.of(
            parseExpectedToken(TokenType.KW_FN, current),
            parseExpectedToken(TokenType.ID, current),
            parseExpectedToken(TokenType.LEFT_PAREN, current)
        ));
        if (current.getName() != TokenType.RIGHT_PAREN)
            node.appendChildren(parseFunctionParameter());
        while (current.getName() != TokenType.RIGHT_PAREN) {
            node.appendChildren(parseExpectedToken(TokenType.COMMA, current), parseFunctionParameter());
        }
        node.appendChildren(parseExpectedToken(TokenType.RIGHT_PAREN, current));
        if (current.getValue().equals(":"))
            node.appendChildren(parseExpectedToken(TokenType.COLON, current), parseType());
        node.appendChildren(parseExpectedToken(TokenType.LEFT_CB, current));
        if (current.getName() != TokenType.RIGHT_CB) {
            while(current.getName() != TokenType.RIGHT_CB) {
                node.appendChildren(parseBlockBody());
            }
        }
        node.appendChildren(parseExpectedToken(TokenType.RIGHT_CB, current));
        return node;
    }

    // FUNC-PARAM ::= TYPE ID ("=" EXPR )?
    public ParseTree parseFunctionParameter() {
        ParseTree node = new ParseTree("FUNC-PARAM", List.of(
            parseType(),
            parseExpectedToken(TokenType.ID, current)
        ));
        if (current.getValue().equals("=")) {
            node.appendChildren(parseExpectedToken(TokenType.OP, current, "="), parseExpr());
        }
        return node;
    }

    // ARRAY-TYPE ::= "Array" "<" TYPE ">"
    public ParseTree parseArrayType() {
        ParseTree node = new ParseTree("ARRAY-TYPE");
        node.appendChildren(
            parseExpectedToken(TokenType.KW_ARR, current),
            parseExpectedToken(TokenType.OP, current, "<"),
            parseType(),
            parseExpectedToken(TokenType.OP, current, ">")

        );
        return node;
    }

    // IMMUTABLE-ARRAY-DECL ::= "const" ARRAY-TYPE ID ( ARRAY-INDEX )? "=" ARRAY-LIT / ID
    public ParseTree parseImmutableArrayDeclaration() {
        ParseTree node = new ParseTree("IMMUTABLE-ARRAY-DECL", List.of(
            parseExpectedToken(TokenType.KW_CONST, current),
            parseArrayType(),
            parseExpectedToken(TokenType.ID, current)
        ));
        if (current.getName() == TokenType.RIGHT_SQB)
            node.appendChildren(parseArrayIndex());
        
        node.appendChildren(parseExpectedToken(TokenType.OP, current, "="), parseArrayLiteral());
        return node;
    }

    // ARRAY-DECL ::= ("const" "mut")? ARRAY-TYPE ID ARRAY-INDEX ( "=" ARRAY-LIT / ID )? / IMMUTABLE-ARRAY-DECL
    public ParseTree parseArrayDeclaration() {
        ParseTree node = new ParseTree("ARRAY-DECL");
        if (current.getName() == TokenType.KW_CONST) {
            if (next != null && next.getName() == TokenType.KW_MUT) {
                node.appendChildren(parseExpectedToken(TokenType.KW_CONST, current), parseExpectedToken(TokenType.KW_MUT, current));
            }
            else {
                node.appendChildren(parseImmutableArrayDeclaration());
                return node;
            }
        }
        node.appendChildren(parseArrayType(), parseExpectedToken(TokenType.ID, current));
        if (current.getValue().equals("=")) {
            if (next != null && next.getName() == TokenType.LEFT_CB)
                node.appendChildren(parseExpectedToken(TokenType.OP, current, "="), parseArrayLiteral());
            else if (next != null && next.getName() == TokenType.ID)
                node.appendChildren(parseExpectedToken(TokenType.OP, current, "="), parseExpectedToken(TokenType.ID, current));
        }
        return node;
    }

    // VAR-DECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAY-DECL
    public ParseTree parseVariableDeclaration() {
        ParseTree node = new ParseTree("VAR-DECL");
        if (current.getName() == TokenType.KW_CONST) {
            if (next != null && (next.getName() == TokenType.KW_MUT || next.getName() == TokenType.KW_ARR)) {
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

    // VAR-ASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = VALUE 
    public ParseTree parseVariableAssignment() { // reassignment of already declared variable
        ParseTree node = new ParseTree("VAR-ASSIGN", List.of(parseExpectedToken(TokenType.ID, current)));
        if (current.getName().equals(TokenType.OP)) {
            if (assignmentOps.contains(current.getValue()))
                node.appendChildren(parseExpectedToken(TokenType.OP, current, current.getValue()));
            else
                throw new SyntaxError("Unexpected character '" + current.getValue() + "' in variable assignment", current.getLineNumber());
            node.appendChildren(parseValue());            
        }
        else
            throw new SyntaxError("Expected equality operator but got " + current.getName() + "('" + current.getValue() + "')", current.getLineNumber());
        return node;
    }

    // TYPE ::= "int" / "string" / "float" / "boolean" / ARRAY-TYPE
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

    // CONTROL-FLOW ::= "return" ( EXPR )? / "continue" / "break"
    public ParseTree parseControlFlow() {
        ParseTree node = new ParseTree("CONTROL-FLOW");
        if (current.getName() == TokenType.KW_CNT || current.getName() == TokenType.KW_BRK) {
            node.appendChildren(parseExpectedToken(current.getName(), current));
        }
            
        else if (current.getName() == TokenType.KW_RET) {
            node.appendChildren(parseExpectedToken(current.getName(), current));
            if (current.getName() == TokenType.LEFT_PAREN || current.getName() == TokenType.LEFT_CB || leftUnaryOps.contains(current.getValue()) || isID(current) ||  isBooleanLiteral(current) || isString(current)) {
                node.appendChildren(parseExpr());
            }
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
                message = actualToken.getValue().isBlank() // is this a StaticToken or VariableToken
                ? String.format("Expected %s but got %s", expectedToken, actualToken.getName())
                : String.format("Expected %s but got %s ('%s')", expectedToken, actualToken.getName(), actualToken.getValue());
                
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
                message = String.format("Expected %s but got %s ('%s')", value, actualToken.getName(), actualToken.getValue());
                
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
