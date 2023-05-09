package com.piedpiper.bolt.parser;

import java.util.List;
import java.util.Objects;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;

public class Parser {
    /*
     * PROGRAM ::= ( STMNT / FUNC-DEF )+
     * STMNT ::= EXPR / COND / LOOP / VAR-DECL / VAR-ASSIGN
     * EXPR ::= ( LOGICAL-OR / TERNARY )
     * ARITH-EXPR ::= TERM (ADD-OP ARITH-EXPR)*
     * TERM ::= EXPO (MULT-OP TERM)*
     * EXPO ::= FACTOR ( "**" EXPO )*
     * FACTOR ::= VALUE / "(" EXPR ")" / UNARY-OP
     * LOGICAL-OR ::=  ( LOGICAL-OR "||" )* LOGICAL-AND
     * LOGICAL-AND ::= ( LOGICAL-AND "&&" )* CMPR-EXPR
     * CMPR-EXPR ::= EXPR ( CMPR-OP EXPR )?
     * UNARY-OP ::= LEFT-UNARY-OP / ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) ( "++" / "--" )
     * LEFT-UNARY-OP ::= ( "++" / "--" / "-" ) ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) / "!" (BOOLEAN / ID / ARRAY-ACCESS / FUNC-CALL)
     * ADD-OP ::= "+" / "-" / "^" / "&"
     * MULT-OP ::= "*" / "/" / "%"
     * CMPR-OP ::= "<" / ">" / "<=" / ">=" / "!=" / "=="
     * TERNARY ::= LOGICAL-OR "?" EXPR ":" EXPR
     * VALUE ::=  FUNC-CALL / ARRAY-ACCESS / ID / STRING-LIT / NUMBER / BOOLEAN / ARRAY-LIT
     * FUNC-CALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
     * ARRAY-ACCESS ::= ID ( ARRAY-INDEX )+
     * ARRAY-INDEX ::= "[" EXPR "]"
     * ARRAY-LIT ::= "{" (EXPR ("," EXPR)* )? "}"
     * COND ::= IF ( "else" IF )* ( ELSE )?
     * IF ::= "if" "(" EXPR ")" COND-BODY
     * ELSE ::= "else" COND-BODY
     * COND-BODY ::= ( ( "{" ( BLOCK-BODY )* "}" ) / BLOCK-BODY )
     * BLOCK-BODY ::= CONTROL-FLOW / STMNT
     * LOOP ::= ( WHILE-LOOP / FOR-LOOP ) "{" ( BLOCK-BODY )* "}"
     * WHILE-LOOP ::= "while" "(" EXPR ")"
     * FOR-LOOP ::= "for" "(" ( ( ( VAR-DECL / VAR-ASSIGN ) ( ";" EXPR ";" EXPR )? ) / TYPE ID : EXPR ) ")"
     * FUNC-DEF ::= "fn" ID "(" (FUNC-PARAM ("," FUNC-PARAM)* )? ")" ( ":" TYPE )? "{" ( BLOCK-BODY )* "}"
     * FUNC-PARAM ::= TYPE ID ("=" EXPR )?
     * ARRAY-TYPE ::= "Array" "<" TYPE ">"
     * IMMUTABLE-ARRAY-DECL ::= "const" ARRAY-TYPE ID ( ARRAY-INDEX )? "=" EXPR
     * ARRAY-DECL ::= ("const" "mut")? ARRAY-TYPE ID ARRAY-INDEX ( "=" EXPR )? / IMMUTABLE-ARRAY-DECL
     * VAR-DECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAY-DECL
     * VAR-ASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = VALUE 
     * TYPE ::= "int" / "string" / "float" / "boolean" / ARRAY-TYPE
     * CONTROL-FLOW ::= "return" ( EXPR )? / "continue" / "break"
    */
    private final List<Token> tokens;
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

    private SyntaxError formComplaint(String expected, Token token) {
        String messageStart = "Expected " + expected + " but ";
        String messageEnd = atEnd() || token == null
            ? "reached EOF" 
            : "got " + token.getName() + ( 
                token.getValue().isBlank()
                ? ""
                : " ('" + token.getValue() + "')"
            );
        return new SyntaxError(messageStart + messageEnd, token == null ? 0 : token.getLineNumber());
    }

    private SyntaxError formComplaint(TokenType expected, Token token) {
        return formComplaint(expected.toString(), token);
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
        if (current.getName() == TokenType.KW_CONST || isPrimitiveType(current) || current.getName() == TokenType.KW_ARR) {
            return parseVariableDeclaration();
        } else if (
            current.getName() == TokenType.LEFT_PAREN
                || current.getName() == TokenType.LEFT_CB
                || leftUnaryOps.contains(current.getValue())
                || isBooleanLiteral(current)
                || isString(current)
        ) {
            return parseExpr();
        } else if (current.getName() == TokenType.KW_FOR || current.getName() == TokenType.KW_WHILE) {
            return parseLoop();
        } else if (current.getName() == TokenType.KW_IF || current.getName() == TokenType.KW_ELSE) {
            return parseConditional();
        } else if (isID(current)) {
            if (isOp(next) && assignmentOps.contains(next.getValue()))
                return parseVariableAssignment();
            else if (next.getName() == TokenType.LEFT_PAREN)
                return parseFunctionCall();
            else if (next.getName() == TokenType.LEFT_SQB)
                return parseArrayAccess();
            else
                return parseExpr();
        }
        else
            throw new SyntaxError("Invalid STMT", current.getLineNumber());
    }

    // EXPR ::= ( LOGICAL-OR / TERNARY )
    public ParseTree parseExpr() {
        if (!(current.getName() == TokenType.LEFT_PAREN
            || current.getName() == TokenType.LEFT_CB
            || current.getName() == TokenType.ID
            || leftUnaryOps.contains(current.getValue())
            || isNumber(current)
            || isBooleanLiteral(current)
            || isString(current)))
            throw formComplaint("EXPR", current);
        ParseTree node = parseLogicalOr();
        if (current.getValue().equals("?"))
            node = parseTernary();
        return node;
    }

    // ARITH-EXPR ::= TERM (ADD-OP ARITH-EXPR)*
    public ParseTree parseArithmeticExpression() {
        ParseTree term = parseTerm();
        if (atEnd() || !addOps.contains(current.getValue()))
            return term;
        else if (!atEnd() && addOps.contains(current.getValue())) {
            ParseTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(term, parseArithmeticExpression());
            return node;
        }
        return null;
    }

    // TERM ::= EXPO (MULT-OP TERM)*
    public ParseTree parseTerm() {
        ParseTree exponent = parseExponent();
        if (atEnd() || !multOps.contains(current.getValue()))
            return exponent;
        else if (!atEnd() && multOps.contains(current.getValue())) {
            ParseTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(exponent, parseTerm());
            return node;
        }
        return null;
    }

    // EXPO ::= FACTOR ( "**" EXPO )*
    public ParseTree parseExponent() {
        ParseTree factor = parseFactor();
        if (atEnd() || !current.getValue().equals("**"))
            return factor;
        else if (!atEnd() && current.getValue().equals("**")) {
            ParseTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(factor, parseExponent());
            return node;
        }
        return null;
    }

    // FACTOR ::= VALUE / "(" EXPR ")" / UNARY-OP
    public ParseTree parseFactor() {
        if (next == null)
            return parseValue();
        if (leftUnaryOps.contains(current.getValue()) || (isID(current) && rightUnaryOps.contains(next.getValue())))
            return parseUnaryOp();
        else if (current.getName() == TokenType.LEFT_PAREN) {
            parseExpectedToken(TokenType.LEFT_PAREN, current);
            ParseTree expr = parseExpr();
            parseExpectedToken(TokenType.RIGHT_PAREN, current);
            return expr;
        }
        else
            return parseValue();
    }

    // LOGICAL-OR ::=  ( LOGICAL-OR "||" )* LOGICAL-AND
    public ParseTree parseLogicalOr() {
        ParseTree logicalAnd = parseLogicalAnd();
        if (atEnd() || !current.getValue().equals("||"))
            return logicalAnd;

        if (!atEnd() && current.getValue().equals("||")) {
            ParseTree node = parseExpectedToken("||", current);
            node.appendChildren(logicalAnd, parseLogicalOr());
            return node;
        }
        return null;
    }

    // LOGICAL-AND ::= ( LOGICAL-AND "&&" )* CMPR-EXPR
    public ParseTree parseLogicalAnd() {
        ParseTree compare = parseComparisonExpression();
        if (atEnd() || !current.getValue().equals("&&"))
            return compare;

        if (!atEnd() && current.getValue().equals("&&")) {
            ParseTree node = parseExpectedToken("&&", current);
            node.appendChildren(compare, parseLogicalAnd());
            return node;
        }
        return null;
    }

    // CMPR-EXPR ::= ARITH-EXPR ( CMPR-OP ARITH-EXPR )?
    public ParseTree parseComparisonExpression() {
        ParseTree arithExpr = parseArithmeticExpression();
        if (atEnd() || !comparisonOps.contains(current.getValue()))
            return arithExpr;
        //ParseTree node = new ParseTree("CMPR-EXPR", List.of(arithExpr));
        if (comparisonOps.contains(current.getValue())) {
            ParseTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(arithExpr, parseArithmeticExpression());
            return node;
        }
        return null; //node;

    }

    // UNARY-OP ::= LEFT-UNARY-OP / ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) ( "++" / "--" )
    public ParseTree parseUnaryOp() {
        ParseTree node = new ParseTree("UNARY-OP");
        if (leftUnaryOps.contains(current.getValue()))
            return parseLeftUnaryOp();
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
        ParseTree node = new ParseTree("UNARY-OP");
        if (current.getValue().equals("++") || current.getValue().equals("--") || current.getValue().equals("-")) {
            node.appendChildren(parseExpectedToken(current.getValue(), current));
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
                throw new SyntaxError("Invalid unary operator on " + current.getName(), current.getLineNumber());
            }
        }
        else if (current.getValue().equals("!")) {
            node.appendChildren(parseExpectedToken(current.getValue(), current));
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
                throw new SyntaxError("Invalid unary operator on " + current.getName(), current.getLineNumber());
            }
        }
        else
            throw formComplaint("LEFTUNARYOP", current);
        return node;
    }

    // TERNARY ::= LOGICAL-OR "?" EXPR ":" EXPR
    public ParseTree parseTernary() {
        ParseTree node = new ParseTree("TERNARY", List.of(parseLogicalOr()));
        parseExpectedToken("?", current);
        node.appendChildren(parseExpr());
        parseExpectedToken(TokenType.COLON, current);
        node.appendChildren(parseExpr());
        return node;
    }

    // VALUE ::=  FUNC-CALL / ARRAY-ACCESS / ID / STRING-LIT / NUMBER / BOOLEAN / ARRAY-LIT 
    public ParseTree parseValue() {
        if (isNumber(current))
            return parseExpectedToken(TokenType.NUMBER, current);
        else if (isString(current))
            return parseExpectedToken(TokenType.STRING, current);
        else if (isBooleanLiteral(current))
            return parseExpectedToken(current.getName(), current);
        else if (current.getName() == TokenType.LEFT_CB)
            return parseArrayLiteral();
        else if (isID(current)) {
            if (next != null && next.getName() == TokenType.LEFT_SQB)
                return parseArrayAccess();
            else if (next != null && next.getName() == TokenType.LEFT_PAREN)
                return parseFunctionCall();
            else
                return parseExpectedToken(TokenType.ID, current);
        }
        return new ParseTree(current);
        //else
            //throw formComplaint("VALUE", current);
    }

    // FUNC-CALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
    public ParseTree parseFunctionCall() {
        ParseTree node = new ParseTree("FUNC-CALL", List.of(parseExpectedToken(TokenType.ID, current)));
        parseExpectedToken(TokenType.LEFT_PAREN, current);
        ParseTree params = new ParseTree("FUNC-PARAMS");
        if (current.getName() != TokenType.RIGHT_PAREN) {
            params.appendChildren(parseExpr());
            while (current.getName() != TokenType.RIGHT_PAREN) {
                parseExpectedToken(TokenType.COMMA, current);
                params.appendChildren(parseExpr());
            }
        }
        if (!params.getChildren().isEmpty())
            node.appendChildren(params);
        parseExpectedToken(TokenType.RIGHT_PAREN, current);
        return node;
    }

    // ARRAY-ACCESS ::= ID ( ARRAY-INDEX )+
    public ParseTree parseArrayAccess() {
        boolean hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
        if (!hasLeftSQB) {
            throw formComplaint("LEFT_SQB", next);
        }
        ParseTree node = parseExpectedToken(TokenType.ID, current);
        ParseTree currNode = node;
        ParseTree index;
        while (hasLeftSQB) {
            index = parseArrayIndex();
            currNode.appendChildren(index);
            currNode = index.getChildren().get(0); // array[ind1][ind2] => array->ARRAY-INDEX->ind1->ARRAY-INDEX->ind2
            hasLeftSQB = !atEnd() && current.getName() == TokenType.LEFT_SQB;
        }
        return node;
    }

    // ARRAY-INDEX ::= "[" EXPR "]"
    public ParseTree parseArrayIndex() {
        parseExpectedToken(TokenType.LEFT_SQB, current);
        ParseTree node = new ParseTree("ARRAY-INDEX", List.of(
            parseExpr()
        ));
        parseExpectedToken(TokenType.RIGHT_SQB, current);
        return node;
    }

    // ARRAY-LIT ::= "{" (EXPR ("," EXPR)* )? "}"
    public ParseTree parseArrayLiteral() {
        ParseTree node = new ParseTree("ARRAY-LIT");
        parseExpectedToken(TokenType.LEFT_CB, current);
        if (current.getName() != TokenType.RIGHT_CB && current.getName() != TokenType.COMMA) {
            node.appendChildren(parseExpr());
            while (!atEnd() && current.getName() != TokenType.RIGHT_CB) {
                parseExpectedToken(TokenType.COMMA, current);
                node.appendChildren(parseExpr());
            }
        }
        parseExpectedToken(TokenType.RIGHT_CB, current);
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
        return new ParseTree("IF", List.of(
            parseExpectedToken(TokenType.KW_IF, current),
            parseExpectedToken(TokenType.LEFT_PAREN, current),
            parseExpr(),
            parseExpectedToken(TokenType.RIGHT_PAREN, current),
            parseConditionalBody()
        ));
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
        ParseTree node = new ParseTree("COND-BODY");
        if (current.getName() == TokenType.LEFT_CB) {
            node.appendChildren(parseExpectedToken(TokenType.LEFT_CB, current));
            while (current.getName() != TokenType.RIGHT_CB) {
                node.appendChildren(parseBlockBody());
            }
            node.appendChildren(parseExpectedToken(TokenType.RIGHT_CB, current));
        }
        else
            return parseBlockBody();
        return node;
    }

    // BLOCK-BODY ::= CONTROLFLOW / STMNT
    public ParseTree parseBlockBody() {
        return isControlFlow(current) ? parseControlFlow() : parseStatement();
    }

    // LOOP ::= ( WHILE-LOOP / FOR-LOOP ) "{" ( BLOCK-BODY )* "}"
    public ParseTree parseLoop() {
        TokenType loopType = current.getName();
        ParseTree node = new ParseTree("LOOP", List.of(
            //parseExpectedToken(loopType, current),
            loopType == TokenType.KW_FOR ? parseForLoop() : parseWhileLoop(),
            parseExpectedToken(TokenType.LEFT_CB, current)
        ));
        while (current.getName() != TokenType.RIGHT_CB) {
            node.appendChildren(parseBlockBody());
        }
        node.appendChildren(parseExpectedToken(TokenType.RIGHT_CB, current));
        return node;
    }

    //WHILE-LOOP ::= "while" "(" EXPR ")"
    public ParseTree parseWhileLoop() {
        return new ParseTree("WHILE-LOOP", List.of(
            parseExpectedToken(TokenType.KW_WHILE, current),
            parseExpectedToken(TokenType.LEFT_PAREN, current),
            parseExpr(),
            parseExpectedToken(TokenType.RIGHT_PAREN, current)
        ));
    }

    // FOR-LOOP ::= "for" "(" ( ( ( VAR-DECL / VAR-ASSIGN ) ( ";" EXPR ";" EXPR )? ) / TYPE ID : EXPR ) ")"
    public ParseTree parseForLoop() {
        ParseTree node = new ParseTree("FOR-LOOP", List.of(
            parseExpectedToken(TokenType.KW_FOR, current),
            parseExpectedToken(TokenType.LEFT_PAREN, current)
        ));
        // look ahead to find out what type of for loop this is
        int lookAheadPosition = position;
        boolean isForEach = false;
        while (lookAheadPosition != tokens.size()) {
            if (tokens.get(lookAheadPosition).getName() == TokenType.COLON) {
                isForEach = true;
                break;
            }
            if (tokens.get(lookAheadPosition).getName() == TokenType.SC)
                break;
            lookAheadPosition++;
        }
        if (isForEach) {
            node.appendChildren(
                parseType(),
                parseExpectedToken(TokenType.ID, current),
                parseExpectedToken(TokenType.COLON, current),
                parseExpr()
            );
        }
        else {
           node.appendChildren(
               isPrimitiveType(current)
               ? parseVariableDeclaration()
               : parseVariableAssignment()
           );
            if (current.getName() == TokenType.SC) {
                node.appendChildren(
                    parseExpectedToken(TokenType.SC, current),
                    parseExpr(),
                    parseExpectedToken(TokenType.SC, current),
                    parseExpr()
                );
            }
        }

        node.appendChildren(parseExpectedToken(TokenType.RIGHT_PAREN, current));
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
        if (current.getName() == TokenType.COLON)
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
            node.appendChildren(parseExpectedToken("=", current), parseExpr());
        }
        return node;
    }

    // ARRAY-TYPE ::= "Array" "<" TYPE ">"
    public ParseTree parseArrayType() {
        ParseTree node = parseExpectedToken(TokenType.KW_ARR, current);
        parseExpectedToken("<", current);
        node.appendChildren(parseType());
        parseExpectedToken(">", current);
        return node;
    }

    // IMMUTABLE-ARRAY-DECL ::= "const" ARRAY-TYPE ID ( ARRAY-INDEX )? "=" EXPR
    public ParseTree parseImmutableArrayDeclaration() {
        ParseTree node = new ParseTree("ARRAY-DECL", List.of(
            parseExpectedToken(TokenType.KW_CONST, current),
            parseArrayType(),
            parseExpectedToken(TokenType.ID, current)
        ));
        if (current.getName() == TokenType.LEFT_SQB)
            node.appendChildren(parseArrayIndex());

        if (current.getValue().equals("=")) {
            parseExpectedToken("=", current);
            node.appendChildren(parseExpr());
        }
        else
            throw new SyntaxError("Constant array cannot be uninitialized", current.getLineNumber());
        return node;
    }

    // ARRAY-DECL ::= ("const" "mut")? ARRAY-TYPE ID ARRAY-INDEX ( "=" EXPR )? / IMMUTABLE-ARRAY-DECL
    public ParseTree parseArrayDeclaration() {
        ParseTree node = new ParseTree("ARRAY-DECL");
        if (current.getName() == TokenType.KW_CONST) {
            if (next != null && next.getName() == TokenType.KW_MUT) {
                node.appendChildren(
                    parseExpectedToken(TokenType.KW_CONST, current),
                    parseExpectedToken(TokenType.KW_MUT, current)
                );
            }
            else if (next != null)
                return parseImmutableArrayDeclaration();
        }
        node.appendChildren(parseArrayType(), parseExpectedToken(TokenType.ID, current), parseArrayIndex());
        if (current.getValue().equals("=")) {
            parseExpectedToken("=", current);
            node.appendChildren(parseExpr());
        }
        return node;
    }

    // VAR-DECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAY-DECL
    public ParseTree parseVariableDeclaration() {
        ParseTree node = new ParseTree("VAR-DECL");
        boolean isConst = false;
        if (current.getName() == TokenType.KW_CONST) {
            isConst = true;
            if (
                (current.getName() == TokenType.KW_MUT || current.getName() == TokenType.KW_ARR)
                || (next != null && (next.getName() == TokenType.KW_MUT || next.getName() == TokenType.KW_ARR))
            )
                 return parseArrayDeclaration(); // let array declaration do the rest
            else
                node.appendChildren(parseExpectedToken(TokenType.KW_CONST, current));
        }
        if (
            (current.getName() == TokenType.KW_MUT || current.getName() == TokenType.KW_ARR)
            || (next != null && (next.getName() == TokenType.KW_MUT || next.getName() == TokenType.KW_ARR))
        )
            return parseArrayDeclaration(); // let array declaration do the rest
        node.appendChildren(parseType());
        if (next != null && next.getValue().equals("=")) {
            node.appendChildren(parseExpectedToken(TokenType.ID, current));
            parseExpectedToken("=", current);
            node.appendChildren(parseExpr());
        }
        else {
            System.out.println(current);
            System.out.println(next);
            if (isConst)
                throw new SyntaxError("Constant variable must be initialized", current.getLineNumber());
            else if (next != null)
                throw formComplaint("'='", next);
        }
        return node;
    }

    // VAR-ASSIGN ::= ID ( "+" / "-" / "*" / "/" )? = EXPR 
    public ParseTree parseVariableAssignment() { // reassignment of already declared variable
        ParseTree IdNode = parseExpectedToken(TokenType.ID, current);
        if (current.getName() == TokenType.OP && assignmentOps.contains(current.getValue())) {
            ParseTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(IdNode, parseExpr());
            return node;
        }
        else
            throw new SyntaxError("Unexpected character '" + current.getValue() + "' in variable assignment", current.getLineNumber());
    }

    // TYPE ::= "int" / "string" / "float" / "boolean" / ARRAY-TYPE
    public ParseTree parseType() {
        if (current.getName() == TokenType.KW_ARR)
            return parseArrayType();
        if (isPrimitiveType(current)) {
            return parseExpectedToken(current.getName(), current);
        }
        throw formComplaint("TYPE", current);
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
            throw formComplaint("KW_CNT, KW_BRK, or KW_RET EXPR", current);
        }
        return node;
    }

    /* Utility parsing method */
    private ParseTree parseExpectedToken(TokenType expectedToken, Token actualToken) {
        ParseTree node;
        SyntaxError error;
        if (atEnd())
            error = formComplaint(expectedToken, actualToken);
        else {
            if (actualToken.getName() == expectedToken) {
                node = new ParseTree(actualToken);
                move();
                return node;
            }
            else {
                error = formComplaint(expectedToken, actualToken);
                
            }
        }
        throw error;
    }

      /* Utility parsing method */
      private ParseTree parseExpectedToken(String value, Token actualToken) {
        ParseTree node;
        SyntaxError error;
        if (atEnd())
            error = formComplaint(TokenType.OP + " ('" + value + "')", actualToken);
        else {
            if (actualToken.getName() == TokenType.OP && actualToken.getValue().equals(value)) {
                node = new ParseTree(actualToken);
                move();
                return node;
            }
            else {
                error = formComplaint(value, actualToken);
            }
        }
        throw error;
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

    private boolean isControlFlow(Token token) {
        return List.of(
            TokenType.KW_RET,
            TokenType.KW_CNT,
            TokenType.KW_BRK
        ).contains(token.getName());
    }
}
