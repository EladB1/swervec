package com.piedpiper.bolt.parser;

import java.util.List;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;

public class Parser {
    /*
     * PROGRAM ::= ( STMNT / FUNC-DEF )+
     * STMNT ::= ( ( VAR-DECL / VAR-ASSIGN / EXPR / CONTROL-FLOW ) SC ) / COND / LOOP
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
     * VALUE ::=  FUNC-CALL / ARRAY-ACCESS / ID / STRING-LIT / NUMBER / BOOLEAN / ARRAY-LIT / null
     * FUNC-CALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
     * ARRAY-ACCESS ::= ID ARRAY-INDEX
     * ARRAY-INDEX ::= ( "[" EXPR "]" )+
     * ARRAY-LIT ::= "{" (EXPR ("," EXPR)* )? "}"
     * COND ::= IF ( ELSEIF )* ( ELSE )?
     * IF ::= "if" "(" EXPR ")" COND-BODY
     * ELSEIF ::= "else" "if" "(" EXPR ")" COND-BODY
     * ELSE ::= "else" COND-BODY
     * COND-BODY ::= ( ( "{" ( STMNT )* "}" ) / STMNT )
     * LOOP ::= ( WHILE-LOOP / FOR-LOOP ) "{" ( STMNT )* "}"
     * WHILE-LOOP ::= "while" "(" EXPR ")"
     * FOR-LOOP ::= "for" "(" ( ( ( VAR-DECL / VAR-ASSIGN ) ";" EXPR ";" EXPR ) / TYPE ID : EXPR ) ")"
     * FUNC-DEF ::= "fn" ID "(" (FUNC-PARAM ("," FUNC-PARAM)* )? ")" ( ":" TYPE )? "{" ( STMNT )* "}"
     * FUNC-PARAM ::= TYPE ID
     * ARRAY-TYPE ::= "Array" "<" TYPE ">"
     * IMMUTABLE-ARRAY-DECL ::= "const" ARRAY-TYPE ID ( ARRAY-INDEX )? "=" EXPR
     * ARRAY-DECL ::= ("const" "mut")? ARRAY-TYPE ID ARRAY-INDEX ( "=" EXPR )? / IMMUTABLE-ARRAY-DECL
     * VAR-DECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAY-DECL
     * VAR-ASSIGN ::= ( ID / ARRAY-ACCESS )  ( "+" / "-" / "*" / "/" )? = EXPR
     * TYPE ::= "int" / "string" / "float" / "boolean" / "generic" / ARRAY-TYPE
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
    public AbstractSyntaxTree parse() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("PROGRAM");

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


    // STMNT ::= ( ( VAR-DECL / VAR-ASSIGN / EXPR / CONTROL-FLOW ) SC ) / COND / LOOP
    private AbstractSyntaxTree parseStatement() {
        if (current.getName() == TokenType.KW_FOR || current.getName() == TokenType.KW_WHILE) {
            return parseLoop();
        }
        else if (current.getName() == TokenType.KW_IF || current.getName() == TokenType.KW_ELSE) {
            return parseConditional();
        }
        else {
            AbstractSyntaxTree node = null;
            if (
                current.getName() == TokenType.KW_CONST
                || current.getName() == TokenType.KW_MUT
                || isPrimitiveType(current)
                || current.getName() == TokenType.KW_ARR
            ) {
                node = parseVariableDeclaration();
            }
            else if (
                current.getName() == TokenType.LEFT_PAREN
                || current.getName() == TokenType.LEFT_CB
                || leftUnaryOps.contains(current.getValue())
                || isBooleanLiteral(current)
                || isString(current)
                || isNumber(current)
                || current.getName() == TokenType.KW_NULL
            ) {
                node = parseExpr();
            }
            else if (isControlFlow(current))
                node = parseControlFlow();
            else if (isID(current)) {
                if (isOp(next) && assignmentOps.contains(next.getValue()))
                    node = parseVariableAssignment();
                else if (next.getName() == TokenType.LEFT_PAREN)
                    node = parseFunctionCall();
                else if (next.getName() == TokenType.LEFT_SQB) {
                    if (position >= tokens.size() - 2)
                        throw new SyntaxError("Expected EXPR but reached EOF", current.getLineNumber());
                    int line = current.getLineNumber();
                    int currPos = position + 2; // skip next
                    Token lookahead = tokens.get(currPos);
                    while (currPos != tokens.size() - 1 && lookahead.getLineNumber() == line) { // look for assignment operator
                        if (assignmentOps.contains(lookahead.getValue())) {
                            node = parseVariableAssignment();
                            break;
                        }
                        currPos++;
                        lookahead = tokens.get(currPos);
                    }
                    if (currPos == tokens.size() - 1 || lookahead.getLineNumber() != line)
                        node = parseArrayAccess();
                }
                else
                    node = parseExpr();
            }
            else
                throw new SyntaxError("Invalid STMT", current.getLineNumber());
            if (atEnd() || current.getName() != TokenType.SC)
                throw new SyntaxError("Missing semicolon", current.getLineNumber());
            else
                parseExpectedToken(TokenType.SC, current);
            return node;
        }

    }

    // EXPR ::= ( LOGICAL-OR / TERNARY )
    private AbstractSyntaxTree parseExpr() {
        if (!(current.getName() == TokenType.LEFT_PAREN
            || current.getName() == TokenType.LEFT_CB
            || current.getName() == TokenType.ID
            || leftUnaryOps.contains(current.getValue())
            || isNumber(current)
            || isBooleanLiteral(current)
            || isString(current)
            || current.getName() == TokenType.KW_NULL
        ))
            throw formComplaint("EXPR", current);
        AbstractSyntaxTree node = parseLogicalOr();
        if (current.getValue().equals("?"))
            node = parseTernary(node);
        return node;
    }

    // ARITH-EXPR ::= TERM (ADD-OP ARITH-EXPR)*
    private AbstractSyntaxTree parseArithmeticExpression() {
        AbstractSyntaxTree term = parseTerm();
        if (atEnd() || !addOps.contains(current.getValue()))
            return term;
        else if (!atEnd() && addOps.contains(current.getValue())) {
            AbstractSyntaxTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(term, parseArithmeticExpression());
            return node;
        }
        return null;
    }

    // TERM ::= EXPO (MULT-OP TERM)*
    private AbstractSyntaxTree parseTerm() {
        AbstractSyntaxTree exponent = parseExponent();
        if (atEnd() || !multOps.contains(current.getValue()))
            return exponent;
        else if (!atEnd() && multOps.contains(current.getValue())) {
            AbstractSyntaxTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(exponent, parseTerm());
            return node;
        }
        return null;
    }

    // EXPO ::= FACTOR ( "**" EXPO )*
    private AbstractSyntaxTree parseExponent() {
        AbstractSyntaxTree factor = parseFactor();
        if (atEnd() || !current.getValue().equals("**"))
            return factor;
        else if (!atEnd() && current.getValue().equals("**")) {
            AbstractSyntaxTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(factor, parseExponent());
            return node;
        }
        return null;
    }

    // FACTOR ::= VALUE / "(" EXPR ")" / UNARY-OP
    private AbstractSyntaxTree parseFactor() {
        if (next == null)
            return parseValue();
        if (leftUnaryOps.contains(current.getValue()) || (isID(current) && rightUnaryOps.contains(next.getValue())))
            return parseUnaryOp();
        else if (current.getName() == TokenType.LEFT_PAREN) {
            parseExpectedToken(TokenType.LEFT_PAREN, current);
            AbstractSyntaxTree expr = parseExpr();
            parseExpectedToken(TokenType.RIGHT_PAREN, current);
            return expr;
        }
        else
            return parseValue();
    }

    // LOGICAL-OR ::=  ( LOGICAL-OR "||" )* LOGICAL-AND
    private AbstractSyntaxTree parseLogicalOr() {
        AbstractSyntaxTree logicalAnd = parseLogicalAnd();
        if (atEnd() || !current.getValue().equals("||"))
            return logicalAnd;

        if (!atEnd() && current.getValue().equals("||")) {
            AbstractSyntaxTree node = parseExpectedToken("||", current);
            node.appendChildren(logicalAnd, parseLogicalOr());
            return node;
        }
        return null;
    }

    // LOGICAL-AND ::= ( LOGICAL-AND "&&" )* CMPR-EXPR
    private AbstractSyntaxTree parseLogicalAnd() {
        AbstractSyntaxTree compare = parseComparisonExpression();
        if (atEnd() || !current.getValue().equals("&&"))
            return compare;

        if (!atEnd() && current.getValue().equals("&&")) {
            AbstractSyntaxTree node = parseExpectedToken("&&", current);
            node.appendChildren(compare, parseLogicalAnd());
            return node;
        }
        return null;
    }

    // CMPR-EXPR ::= ARITH-EXPR ( CMPR-OP ARITH-EXPR )?
    private AbstractSyntaxTree parseComparisonExpression() {
        AbstractSyntaxTree arithExpr = parseArithmeticExpression();
        if (atEnd() || !comparisonOps.contains(current.getValue()))
            return arithExpr;

        if (comparisonOps.contains(current.getValue())) {
            AbstractSyntaxTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(arithExpr, parseArithmeticExpression());
            return node;
        }
        return null;

    }

    // UNARY-OP ::= LEFT-UNARY-OP / ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) ( "++" / "--" )
    private AbstractSyntaxTree parseUnaryOp() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("UNARY-OP", current.getLineNumber());
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
    private AbstractSyntaxTree parseLeftUnaryOp() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("UNARY-OP", current.getLineNumber());
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
    private AbstractSyntaxTree parseTernary(AbstractSyntaxTree parsedLogicalOr) {
        AbstractSyntaxTree node = new AbstractSyntaxTree("TERNARY", current.getLineNumber(), List.of(parsedLogicalOr));
        parseExpectedToken("?", current);
        node.appendChildren(parseExpr());
        parseExpectedToken(TokenType.COLON, current);
        node.appendChildren(parseExpr());
        return node;
    }

    // VALUE ::=  FUNC-CALL / ARRAY-ACCESS / ID / STRING-LIT / NUMBER / BOOLEAN / ARRAY-LIT / null
    private AbstractSyntaxTree parseValue() {
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
        else if (current.getName() == TokenType.KW_NULL) {
            return parseExpectedToken(TokenType.KW_NULL, current);
        }
        return new AbstractSyntaxTree(current);
        //else
            //throw formComplaint("VALUE", current);
    }

    // FUNC-CALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
    private AbstractSyntaxTree parseFunctionCall() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("FUNC-CALL", current.getLineNumber(), List.of(parseExpectedToken(TokenType.ID, current)));
        parseExpectedToken(TokenType.LEFT_PAREN, current);
        AbstractSyntaxTree params = new AbstractSyntaxTree("FUNC-PARAMS", current.getLineNumber());
        if (current.getName() != TokenType.RIGHT_PAREN) {
            params.appendChildren(parseExpr());
            while (current.getName() != TokenType.RIGHT_PAREN) {
                parseExpectedToken(TokenType.COMMA, current);
                params.appendChildren(parseExpr());
            }
        }
        if (params.hasChildren())
            node.appendChildren(params);
        parseExpectedToken(TokenType.RIGHT_PAREN, current);
        return node;
    }

    // ARRAY-ACCESS ::= ID ARRAY-INDEX
    private AbstractSyntaxTree parseArrayAccess() {
        boolean hasLeftSQB = next != null && next.getName() == TokenType.LEFT_SQB;
        if (!hasLeftSQB) {
            throw formComplaint("LEFT_SQB", next);
        }
        AbstractSyntaxTree node = parseExpectedToken(TokenType.ID, current);
        node.appendChildren(parseArrayIndex());
        return node;
    }

    /* 
            code: array[ind1][ind2]
            AST:
               array
                 |
             ARRAY-INDEX
            /           \
           ind1     ARRAY-INDEX
                         |
                        ind2
    */ 

    // ARRAY-INDEX ::= ( "[" EXPR "]" )+
    private AbstractSyntaxTree parseArrayIndex() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("ARRAY-INDEX", current.getLineNumber());
        if (!atEnd() && current.getName() != TokenType.LEFT_SQB)
            throw formComplaint("LEFT_SQB", current);
        parseExpectedToken(TokenType.LEFT_SQB, current);
        node.appendChildren(parseExpr());
        parseExpectedToken(TokenType.RIGHT_SQB, current);
        if (!atEnd() && current.getName() == TokenType.LEFT_SQB)
            node.appendChildren(parseArrayIndex());
        return node;
    }

    // ARRAY-LIT ::= "{" (EXPR ("," EXPR)* )? "}"
    private AbstractSyntaxTree parseArrayLiteral() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("ARRAY-LIT", current.getLineNumber());
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

    // COND ::= IF ( ELSEIF )* ( ELSE )?
    private AbstractSyntaxTree parseConditional() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("COND", current.getLineNumber(), List.of(parseIf()));
        if (current.getName() == TokenType.KW_ELSE && next != null && next.getName() == TokenType.KW_IF) {
            while(current.getName() == TokenType.KW_ELSE && next != null && next.getName() == TokenType.KW_IF) {
                node.appendChildren(parseElseIf());
            }
        }
        if (current.getName() == TokenType.KW_ELSE)
            node.appendChildren(parseElse());
        return node;
    }

    // IF ::= "if" "(" EXPR ")" COND-BODY
    private AbstractSyntaxTree parseIf() {
        AbstractSyntaxTree node = parseExpectedToken(TokenType.KW_IF, current);
        parseExpectedToken(TokenType.LEFT_PAREN, current);
        node.appendChildren(parseExpr());
        parseExpectedToken(TokenType.RIGHT_PAREN, current);
        node.appendChildren(parseConditionalBody());
        return node;
    }

    // ELSEIF ::= "else" "if" "(" EXPR ")" COND-BODY
    private AbstractSyntaxTree parseElseIf() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("ELSE IF", current.getLineNumber());
        parseExpectedToken(TokenType.KW_ELSE, current);
        parseExpectedToken(TokenType.KW_IF, current);
        parseExpectedToken(TokenType.LEFT_PAREN, current);
        node.appendChildren(parseExpr());
        parseExpectedToken(TokenType.RIGHT_PAREN, current);
        node.appendChildren(parseConditionalBody());
        return node;
    }

    // ELSE ::= "else" COND-BODY
    private AbstractSyntaxTree parseElse() {
        AbstractSyntaxTree node = parseExpectedToken(TokenType.KW_ELSE, current);
        node.appendChildren(parseConditionalBody());
        return node;
    }

    // COND-BODY ::= ( ( "{" ( STMNT )* "}" ) / STMNT )
    private AbstractSyntaxTree parseConditionalBody() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("BLOCK-BODY", current.getLineNumber());
        if (current.getName() == TokenType.LEFT_CB) {
            parseExpectedToken(TokenType.LEFT_CB, current);
            while (current.getName() != TokenType.RIGHT_CB) {
                node.appendChildren(parseStatement());
            }
            parseExpectedToken(TokenType.RIGHT_CB, current);
        }
        else
            return new AbstractSyntaxTree("BLOCK-BODY", current.getLineNumber(), List.of(parseStatement()));
        return node;
    }

    // LOOP ::= ( WHILE-LOOP / FOR-LOOP ) "{" ( STMNT )* "}"
    private AbstractSyntaxTree parseLoop() {
        AbstractSyntaxTree node = current.getName() == TokenType.KW_FOR ? parseForLoop() : parseWhileLoop();
        AbstractSyntaxTree bodyNode = new AbstractSyntaxTree("BLOCK-BODY", current.getLineNumber());
        parseExpectedToken(TokenType.LEFT_CB, current);
        while (current.getName() != TokenType.RIGHT_CB) {
            bodyNode.appendChildren(parseStatement());
        }
        node.appendChildren(bodyNode);
        parseExpectedToken(TokenType.RIGHT_CB, current);
        return node;
    }

    //WHILE-LOOP ::= "while" "(" EXPR ")"
    private AbstractSyntaxTree parseWhileLoop() {
        AbstractSyntaxTree node = parseExpectedToken(TokenType.KW_WHILE, current);
        parseExpectedToken(TokenType.LEFT_PAREN, current);
        node.appendChildren(parseExpr());
        parseExpectedToken(TokenType.RIGHT_PAREN, current);
        return node;
    }

    // FOR-LOOP ::= "for" "(" ( ( ( VAR-DECL / VAR-ASSIGN ) ";" EXPR ";" EXPR ) / TYPE ID : EXPR ) ")"
    private AbstractSyntaxTree parseForLoop() {
        AbstractSyntaxTree node = parseExpectedToken(TokenType.KW_FOR, current);
        parseExpectedToken(TokenType.LEFT_PAREN, current);
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

            if (
                lookAheadPosition != tokens.size() - 1
                && tokens.get(lookAheadPosition).getName() == TokenType.RIGHT_PAREN
                && tokens.get(lookAheadPosition + 1).getName() == TokenType.LEFT_CB
            )
                break; // found the end of the for loop header, break out of this loop
            lookAheadPosition++;
        }
        if (isForEach) {
            AbstractSyntaxTree varNode = new AbstractSyntaxTree("VAR-DECL", current.getLineNumber(), List.of(
                parseType(),
                parseExpectedToken(TokenType.ID, current)
            ));
            parseExpectedToken(TokenType.COLON, current);
            node.appendChildren(
                varNode,
                parseExpr()
            );
        }
        else {
            node.appendChildren(
                isPrimitiveType(current)
                ? parseVariableDeclaration()
                : parseVariableAssignment()
            );

            parseExpectedToken(TokenType.SC, current);
            node.appendChildren(parseExpr());
            parseExpectedToken(TokenType.SC, current);
            node.appendChildren(parseExpr());

        }
        parseExpectedToken(TokenType.RIGHT_PAREN, current);
        return node;
    }

    //  FUNC-DEF ::= "fn" ID "(" (FUNC-PARAM ("," FUNC-PARAM)* )? ")" ( ":" TYPE )? "{" ( BLOCK-BODY )* "}"
    private AbstractSyntaxTree parseFunctionDefinition() {
        AbstractSyntaxTree node = parseExpectedToken(TokenType.KW_FN, current);
        AbstractSyntaxTree functionNameNode = parseExpectedToken(TokenType.ID, current);
        node.appendChildren(functionNameNode);
        parseExpectedToken(TokenType.LEFT_PAREN, current);
        AbstractSyntaxTree paramsNode = new AbstractSyntaxTree("FUNC-PARAMS", current.getLineNumber());
        if (current.getName() != TokenType.RIGHT_PAREN) {
            paramsNode.appendChildren(parseFunctionParameter());
        }
        while (current.getName() != TokenType.RIGHT_PAREN) {
            parseExpectedToken(TokenType.COMMA, current);
            paramsNode.appendChildren(parseFunctionParameter());
        }
        parseExpectedToken(TokenType.RIGHT_PAREN, current);
        if (paramsNode.hasChildren())
            node.appendChildren(paramsNode);
        if (current.getName() == TokenType.COLON) {
            parseExpectedToken(TokenType.COLON, current);
            node.appendChildren(parseType());
        }
        parseExpectedToken(TokenType.LEFT_CB, current);
        if (current.getName() != TokenType.RIGHT_CB) {
            AbstractSyntaxTree bodyNode = new AbstractSyntaxTree("BLOCK-BODY", current.getLineNumber());
            while(current.getName() != TokenType.RIGHT_CB) {
                bodyNode.appendChildren(parseStatement());
            }
            if (bodyNode.hasChildren())
                node.appendChildren(bodyNode);
        }
        parseExpectedToken(TokenType.RIGHT_CB, current);
        return node;
    }

    // FUNC-PARAM ::= TYPE ID
    private AbstractSyntaxTree parseFunctionParameter() {
        return new AbstractSyntaxTree("FUNC-PARAM", current.getLineNumber(), List.of(
            parseType(),
            parseExpectedToken(TokenType.ID, current)
        ));
    }

    // ARRAY-TYPE ::= "Array" "<" TYPE ">"
    private AbstractSyntaxTree parseArrayType() {
        AbstractSyntaxTree node = parseExpectedToken(TokenType.KW_ARR, current);
        parseExpectedToken("<", current);
        node.appendChildren(parseType());
        parseExpectedToken(">", current);
        return node;
    }

    // IMMUTABLE-ARRAY-DECL ::= "const" ARRAY-TYPE ID ( ARRAY-INDEX )? "=" EXPR
    private AbstractSyntaxTree parseImmutableArrayDeclaration() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("ARRAY-DECL", current.getLineNumber(), List.of(
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
    private AbstractSyntaxTree parseArrayDeclaration() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("ARRAY-DECL", current.getLineNumber());
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
        else if (current.getName() == TokenType.KW_MUT)
            node.appendChildren(parseExpectedToken(TokenType.KW_MUT, current));
        node.appendChildren(parseArrayType(), parseExpectedToken(TokenType.ID, current));
        if (current.getName() != TokenType.LEFT_SQB)
            throw new SyntaxError("Array size required for arrays that are not constant and immutable", current.getLineNumber());
        node.appendChildren(parseArrayIndex());
        if (current.getValue().equals("=")) {
            parseExpectedToken("=", current);
            node.appendChildren(parseExpr());
        }
        return node;
    }

    // VAR-DECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAY-DECL
    private AbstractSyntaxTree parseVariableDeclaration() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("VAR-DECL", current.getLineNumber());
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
        if (current.getName() == TokenType.KW_MUT && next.getName() != TokenType.KW_ARR)
            throw new SyntaxError("Cannot use modifier 'mut' on non-array variables", current.getLineNumber());
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
            if (isConst)
                throw new SyntaxError("Constant variable must be initialized", current.getLineNumber());
            node.appendChildren(parseExpectedToken(TokenType.ID, current));
        }
        return node;
    }

    // VAR-ASSIGN ::= ( ID / ARRAY-ACCESS )  ( "+" / "-" / "*" / "/" )? = EXPR
    private AbstractSyntaxTree parseVariableAssignment() { // reassignment of already declared variable
        AbstractSyntaxTree left = next.getName() == TokenType.LEFT_SQB ? parseArrayAccess() : parseExpectedToken(TokenType.ID, current);

        if (current.getName() == TokenType.OP && assignmentOps.contains(current.getValue())) {
            AbstractSyntaxTree node = parseExpectedToken(current.getValue(), current);
            node.appendChildren(left, parseExpr());
            return node;
        }
        else
            throw new SyntaxError("Unexpected character '" + current.getValue() + "' in variable assignment", current.getLineNumber());
    }

    // TYPE ::= "int" / "string" / "float" / "boolean" / ARRAY-TYPE
    private AbstractSyntaxTree parseType() {
        if (current.getName() == TokenType.KW_ARR)
            return parseArrayType();
        if (isPrimitiveType(current)) {
            return parseExpectedToken(current.getName(), current);
        }
        throw formComplaint("TYPE", current);
    }

    // CONTROL-FLOW ::= "return" ( EXPR )? / "continue" / "break"
    private AbstractSyntaxTree parseControlFlow() {
        AbstractSyntaxTree node = new AbstractSyntaxTree("CONTROL-FLOW", current.getLineNumber());
        if (current.getName() == TokenType.KW_CNT || current.getName() == TokenType.KW_BRK) {
            node.appendChildren(parseExpectedToken(current.getName(), current));
        }
            
        else if (current.getName() == TokenType.KW_RET) {
            node.appendChildren(parseExpectedToken(current.getName(), current));
            if (
                current.getName() == TokenType.LEFT_PAREN
                || current.getName() == TokenType.LEFT_CB
                || current.getName() == TokenType.KW_NULL
                || leftUnaryOps.contains(current.getValue())
                || isID(current)
                ||  isBooleanLiteral(current)
                || isString(current)
                || isNumber(current)
            ) {
                node.appendChildren(parseExpr());
            }
        }
        else {
            throw formComplaint("KW_CNT, KW_BRK, or KW_RET EXPR", current);
        }
        return node;
    }

    /* Utility parsing method */
    private AbstractSyntaxTree parseExpectedToken(TokenType expectedToken, Token actualToken) {
        AbstractSyntaxTree node;
        SyntaxError error;
        if (atEnd())
            error = formComplaint(expectedToken, actualToken);
        else {
            if (actualToken.getName() == expectedToken) {
                node = new AbstractSyntaxTree(actualToken);
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
      private AbstractSyntaxTree parseExpectedToken(String value, Token actualToken) {
        AbstractSyntaxTree node;
        SyntaxError error;
        if (atEnd())
            error = formComplaint(TokenType.OP + " ('" + value + "')", actualToken);
        else {
            if (actualToken.getName() == TokenType.OP && actualToken.getValue().equals(value)) {
                node = new AbstractSyntaxTree(actualToken);
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
            TokenType.KW_BOOL,
            TokenType.KW_GEN
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
