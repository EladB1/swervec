package com.piedpiper.bolt.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;
import static com.piedpiper.bolt.parser.ParseTree.createNestedTree;

public class TestParser {
    // commonly used tokens
    private final Token commaToken = new StaticToken(TokenType.COMMA);
    private final Token leftParenToken = new StaticToken(TokenType.LEFT_PAREN);
    private final Token rightParenToken = new StaticToken(TokenType.RIGHT_PAREN);
    private final Token leftCBToken = new StaticToken(TokenType.LEFT_CB);
    private final Token rightCBToken = new StaticToken(TokenType.RIGHT_CB);
    private final Token leftSQBToken = new StaticToken(TokenType.LEFT_SQB);
    private final Token rightSQBToken = new StaticToken(TokenType.RIGHT_SQB);
    

    // commonly used tree nodes
    private final ParseTree commaNode = new ParseTree(commaToken);
    private final ParseTree leftParenNode = new ParseTree(leftParenToken);
    private final ParseTree rightParenNode = new ParseTree(rightParenToken);
    private final ParseTree leftCBNode = new ParseTree(leftCBToken);
    private final ParseTree rightCBNode = new ParseTree(rightCBToken);


    private void assertSyntaxError(String expectedErrorMessage, Executable executable) {
        SyntaxError error = assertThrows(SyntaxError.class, executable);
        assertEquals(expectedErrorMessage, error.getMessage());
    }

    // top-level parse

    // parseStatement

    // parseExpr
    @Test
    void test_parseExpr_withUnary() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "++"),
            new VariableToken(TokenType.OP, "*"),
            new VariableToken(TokenType.NUMBER, "2")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("EXPR", List.of(
            new ParseTree("TERM", List.of(
                createNestedTree(tokens.subList(0, 2), "UNARY-OP"),
                new ParseTree(tokens.get(2)),
                new ParseTree(tokens.get(3))
            ))
        ));

        assertEquals(expectedParseTree, parser.parseExpr());
    }

    @Test
    void test_parseExpr_compound() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "+"),
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "<="),
            new VariableToken(TokenType.NUMBER, "4"),
            new VariableToken(TokenType.OP, "&&"),
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "!="),
            new VariableToken(TokenType.NUMBER, "0")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("EXPR", List.of(
            new ParseTree("LOGICAL-AND", List.of(
                new ParseTree("CMPR-EXPR", List.of(
                    createNestedTree(tokens.subList(0, 3), "ARITH-EXPR"),
                    new ParseTree(tokens.get(3)),
                    new ParseTree(tokens.get(4))
                )),
                new ParseTree(tokens.get(5)),
                createNestedTree(tokens.subList(6, 9), "CMPR-EXPR")
            ))
        ));

        assertEquals(expectedParseTree, parser.parseExpr());
    }

    @Test
    void test_parseExpr_complex() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "+"),
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "<="),
            new VariableToken(TokenType.NUMBER, "4"),
            new VariableToken(TokenType.OP, "||"),
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "%"),
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "=="),
            new VariableToken(TokenType.NUMBER, "0"),
            new VariableToken(TokenType.OP, "&&"),
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "!="),
            new VariableToken(TokenType.ID, "length")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("EXPR", List.of(
            new ParseTree("LOGICAL-OR", List.of(
                new ParseTree("CMPR-EXPR", List.of(
                    createNestedTree(tokens.subList(0, 3), "ARITH-EXPR"),
                    new ParseTree(tokens.get(3)),
                    new ParseTree(tokens.get(4))
                )),
                new ParseTree(tokens.get(5)),
                new ParseTree("LOGICAL-AND", List.of(
                    new ParseTree("CMPR-EXPR", List.of(
                        createNestedTree(tokens.subList(6, 9), "TERM"),
                        new ParseTree(tokens.get(9)),
                        new ParseTree(tokens.get(10))
                    )),
                    new ParseTree(tokens.get(11)),
                    createNestedTree(tokens.subList(12, 15), "CMPR-EXPR")
                ))
            ))
                
        ));

        assertEquals(expectedParseTree, parser.parseExpr());
    }

    // parseArithmeticExpression
    @Test
    void test_parseArithmeticExpression_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "length"),
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.ID, "i")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = createNestedTree(tokens, "ARITH-EXPR");
        assertEquals(expectedParseTree, parser.parseArithmeticExpression());
    }

    // parseTerm
    @Test
    void test_parseTerm_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "*"),
            new VariableToken(TokenType.ID, "j")
        );

        ParseTree expectedParseTree = createNestedTree(tokens, "TERM");
        Parser parser = new Parser(tokens);
        assertEquals(expectedParseTree, parser.parseTerm());
    }

    @Test
    void test_parseTerm_withParens() {
        // (i + 1) * j
        List<Token> tokens = List.of(
            leftParenToken,
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "+"),
            new VariableToken(TokenType.NUMBER, "1"),
            rightParenToken,
            new VariableToken(TokenType.OP, "*"),
            new VariableToken(TokenType.ID, "j")
        );

        ParseTree expectedParseTree = new ParseTree("TERM", List.of(
            new ParseTree("FACTOR", List.of(
                leftParenNode,
                createNestedTree(tokens.subList(1, 4), "EXPR", "ARITH-EXPR"),
                rightParenNode
            )),
            new ParseTree(tokens.get(5)),
            new ParseTree(tokens.get(6))
        ));
        Parser parser = new Parser(tokens);
        assertEquals(expectedParseTree, parser.parseTerm());
    }

    // parseExponent
    @Test
    void test_parseExpo_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "**"),
            new VariableToken(TokenType.ID, "n")
        );

        ParseTree expectedParseTree = createNestedTree(tokens, "EXPO");
        Parser parser = new Parser(tokens);
        assertEquals(expectedParseTree, parser.parseExponent());
    }

    // parseFactor
    @Test
    void test_parseFactor_value() {

    }

    @Test
    void test_parseFactor_parenExpr() {

    }

    @Test
    void test_parseFactor_unaryOp() {

    }

    // parseLogicalOr
    @Test
    void test_parseLogicalOr_simple() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_TRUE),
            new VariableToken(TokenType.OP, "||"),
            new StaticToken(TokenType.KW_FALSE)
        );
        Parser parser = new Parser(tokens);
        ParseTree expectedParseTree = createNestedTree(tokens, "LOGICAL-OR");

        assertEquals(expectedParseTree, parser.parseLogicalOr());
    }

    @Test
    void test_parseLogicalOr_compound() {
        List<Token> tokens = List.of(
            leftParenToken,
            new StaticToken(TokenType.KW_TRUE),
            new VariableToken(TokenType.OP, "||"),
            new StaticToken(TokenType.KW_FALSE),
            rightParenToken,
            new VariableToken(TokenType.OP, "&&"),
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "=="),
            new VariableToken(TokenType.NUMBER, "10")
        );
        Parser parser = new Parser(tokens);
        ParseTree expectedParseTree = new ParseTree("LOGICAL-AND", List.of(
            new ParseTree("FACTOR", List.of(
                leftParenNode,
                createNestedTree(tokens.subList(1, 4), "EXPR", "LOGICAL-OR"),
                rightParenNode
            )),
            new ParseTree(tokens.get(5)),
            createNestedTree(tokens.subList(6, 9), "CMPR-EXPR")
        ));
        assertEquals(expectedParseTree, parser.parseLogicalOr());
    }


    // parseLogicalAnd
    @Test
    void test_parseLogicalAnd_simple() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_TRUE),
            new VariableToken(TokenType.OP, "&&"),
            new StaticToken(TokenType.KW_FALSE)
        );
        Parser parser = new Parser(tokens);
        ParseTree expectedParseTree = createNestedTree(tokens, "LOGICAL-AND");

        assertEquals(expectedParseTree, parser.parseLogicalAnd());
    }

    @Test
    void test_parseLogicalAnd_compound() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_TRUE),
            new VariableToken(TokenType.OP, "&&"),
            new StaticToken(TokenType.KW_FALSE),
            new VariableToken(TokenType.OP, "&&"),
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "=="),
            new VariableToken(TokenType.NUMBER, "10")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = createNestedTree(tokens.subList(0, 4), "LOGICAL-AND");
        expectedParseTree.appendChildren(createNestedTree(tokens.subList(4, 7), "CMPR-EXPR"));

        assertEquals(expectedParseTree, parser.parseLogicalAnd());
    }


    // parseComparisonExpression
    @Test
    void test_parseComparisonExpression_compound() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "+"),
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "<="),
            new VariableToken(TokenType.NUMBER, "4")
        );
        Parser parser = new Parser(tokens);
        ParseTree expectedParseTree = new ParseTree("CMPR-EXPR", List.of(
            createNestedTree(tokens.subList(0, 3), "ARITH-EXPR"),
            new ParseTree(tokens.get(3)),
            new ParseTree(tokens.get(4))
        ));

        assertEquals(expectedParseTree, parser.parseComparisonExpression());
    }

    // parseUnaryOp
    @ParameterizedTest
    @ValueSource(strings = {"++", "--"})
    void test_parseUnaryOp(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, operator)
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = createNestedTree(tokens, "UNARY-OP");

        assertEquals(expectedParseTree, parser.parseUnaryOp());
    }

    @Test
    void test_parseUnaryOp_callsLeftUnaryOp() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.ID, "i")
            
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("UNARY-OP", List.of(
            createNestedTree(tokens, "LEFT-UNARY-OP")
        ));

        assertEquals(expectedParseTree, parser.parseUnaryOp());
    }

    // parseLeftUnaryOp
    @ParameterizedTest
    @ValueSource(strings = {"++", "--", "-", "!"})
    void test_parseLeftUnaryOp_succeedsID(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, operator),
            new VariableToken(TokenType.ID, "x")
        );

        ParseTree expectedParseTree = createNestedTree(tokens, "LEFT-UNARY-OP");

        Parser parser = new Parser(tokens);
        ParseTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedParseTree, tree);
    }

    @ParameterizedTest
    @ValueSource(strings = {"++", "--", "-"})
    void test_parseLeftUnaryOp_succeedsWithNumLiteral(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, operator),
            new VariableToken(TokenType.NUMBER, "5")
        );

        ParseTree expectedParseTree = createNestedTree(tokens, "LEFT-UNARY-OP");

        Parser parser = new Parser(tokens);
        ParseTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseLeftUnaryOp_succeedsWithBooleanLiteral() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new StaticToken(TokenType.KW_TRUE)
        );

        ParseTree expectedParseTree = createNestedTree(tokens, "LEFT-UNARY-OP");
        Parser parser = new Parser(tokens);
        ParseTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseLeftUnaryOp_succeedsWithFunctionCall() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new VariableToken(TokenType.ID, "isInt"),
            leftParenToken,
            rightParenToken
        );

        ParseTree expectedParseTree = new ParseTree("LEFT-UNARY-OP", List.of(
            new ParseTree(tokens.get(0)),
            createNestedTree(tokens.subList(1, 4), "FUNC-CALL")
        ));

        Parser parser = new Parser(tokens);
        ParseTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseLeftUnaryOp_succeedsWithArrayAccess() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.ID, "nums"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "0"),
            rightSQBToken
        );

        ParseTree expectedParseTree = new ParseTree("LEFT-UNARY-OP", List.of(
            new ParseTree(tokens.get(0)),
            new ParseTree("ARRAY-ACCESS", List.of(
                new ParseTree(tokens.get(1)),
                createNestedTree(tokens.subList(2, 5), "ARRAY-INDEX")
            ))
        ));

        Parser parser = new Parser(tokens);
        ParseTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseLeftUnaryOp_wrongToken() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new VariableToken(TokenType.STRING, "\"true\"")
        );

        Parser parser = new Parser(tokens);
        assertSyntaxError("Invalid unary operator on STRING", () -> parser.parseLeftUnaryOp());
    }

    // parseTernary
    @Test
    void test_parseTernary_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "isOpen"),
            new VariableToken(TokenType.OP, "?"),
            new VariableToken(TokenType.ID, "connect"),
            new StaticToken(TokenType.COLON),
            new VariableToken(TokenType.ID, "disconnect")
        );
        Parser parser = new Parser(tokens);
        ParseTree expectedParseTree = new ParseTree("TERNARY", List.of(
            new ParseTree(tokens.get(0)),
            new ParseTree(tokens.get(1)),
            new ParseTree("EXPR", List.of(new ParseTree(tokens.get(2)))),
            new ParseTree(tokens.get(3)),
            new ParseTree("EXPR", List.of(new ParseTree(tokens.get(4))))

        ));
        assertEquals(expectedParseTree, parser.parseTernary());
    }

    @Test
    void test_parseTernary_complex() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "%"),
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "=="),
            new VariableToken(TokenType.NUMBER, "0"),
            new VariableToken(TokenType.OP, "?"),
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "/"),
            new VariableToken(TokenType.NUMBER, "2"),
            new StaticToken(TokenType.COLON),
            leftParenToken,
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.NUMBER, "1"),
            rightParenToken,
            new VariableToken(TokenType.OP, "/"),
            new VariableToken(TokenType.NUMBER, "2")
        );
        Parser parser = new Parser(tokens);
        ParseTree expectedParseTree = new ParseTree("TERNARY", List.of(
            new ParseTree("CMPR-EXPR", List.of(
                createNestedTree(tokens.subList(0, 3), "TERM"),
                new ParseTree(tokens.get(3)),
                new ParseTree(tokens.get(4))
            )),
            new ParseTree(tokens.get(5)),
            new ParseTree("EXPR", List.of(
                createNestedTree(tokens.subList(6, 9), "TERM")
            )),
            new ParseTree(tokens.get(9)),
            new ParseTree("EXPR", List.of(
                new ParseTree("TERM", List.of(
                    new ParseTree("FACTOR", List.of(
                        new ParseTree(tokens.get(10)),
                        createNestedTree(tokens.subList(11, 14), "EXPR", "ARITH-EXPR"),
                        new ParseTree(tokens.get(14))
                    )),
                    new ParseTree(tokens.get(15)),
                    new ParseTree(tokens.get(16))
                ))
            ))
        ));
        assertEquals(expectedParseTree, parser.parseTernary());
    }

    // parseValue

    // parseFunctionCall
    @Test
    void test_parseFunctionCall_noParams() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "request"),
            leftParenToken,
            rightParenToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = createNestedTree(tokens, "FUNC-CALL");

        assertEquals(expectedParseTree, parser.parseFunctionCall());
    }

    @Test
    void test_parseFunctionCall_oneParam() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "request"),
            leftParenToken,
            new VariableToken(TokenType.STRING, "\"https://website.com/resource/\""),
            rightParenToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("FUNC-CALL", List.of(
            new ParseTree(tokens.get(0)),
            new ParseTree(tokens.get(1)),
            createNestedTree(new ParseTree(tokens.get(2)), "EXPR"),
            new ParseTree(tokens.get(3))
        ));

        assertEquals(expectedParseTree, parser.parseFunctionCall());
    }

    @Test
    void test_parseFunctionCall_multipleParams() {
        // request("https://website.com/resource", headers[application_json], getMethod(RESOURCE))
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "request"),
            leftParenToken,
            new VariableToken(TokenType.STRING, "\"https://website.com/resource/\""),
            commaToken,
            new VariableToken(TokenType.ID, "headers"),
            leftSQBToken,
            new VariableToken(TokenType.ID, "application_json"),
            rightSQBToken,
            commaToken,
            new VariableToken(TokenType.ID, "getMethod"),
            leftParenToken,
            new VariableToken(TokenType.ID, "RESOURCE"),
            rightParenToken,
            rightParenToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("FUNC-CALL", List.of(
            new ParseTree(tokens.get(0)),
            leftParenNode,
            new ParseTree("EXPR", List.of(
                new ParseTree(tokens.get(2))
            )),
            commaNode,
            new ParseTree("EXPR", List.of(
                new ParseTree("ARRAY-ACCESS", List.of(
                    new ParseTree(tokens.get(4)),
                    createNestedTree(tokens.subList(5, 8), "ARRAY-INDEX")
                ))
            )),
            commaNode,
            new ParseTree("EXPR", List.of(
                new ParseTree("FUNC-CALL", List.of(
                    new ParseTree(tokens.get(9)),
                    new ParseTree(tokens.get(10)),
                    createNestedTree(new ParseTree(tokens.get(11)), "EXPR"),
                    rightParenNode
                ))
            )),
            rightParenNode
        ));

        assertEquals(expectedParseTree, parser.parseFunctionCall());
    }

    // parseArrayAccess
    @Test
    void test_parseArrayAccess_succeedsWithArrayIndex() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "12"),
            rightSQBToken
        );

        ParseTree expectedParseTree = new ParseTree("ARRAY-ACCESS", List.of(
            new ParseTree(tokens.get(0)),
            createNestedTree(tokens.subList(1, 4), "ARRAY-INDEX")
        ));

        Parser parser = new Parser(tokens);
        ParseTree tree = parser.parseArrayAccess();

        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseArrayAccess_EOFError() {
        // x[
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftSQBToken
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected NUMBER but reached EOF", () -> parser.parseArrayAccess());
    }

    @Test
    void test_parseArrayAccess_wrongTokenError() {
        // x(
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftParenToken
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected LEFT_SQB but got LEFT_PAREN", () -> parser.parseArrayAccess());
    }

    // parseArrayIndex
    @Test
    void test_parseArrayIndex_succeeds() {
        // [5]
        List<Token> tokens = List.of(
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "5"),
            rightSQBToken
        );
        Parser parser = new Parser(tokens);
        ParseTree expectedParseTree = createNestedTree(tokens, "ARRAY-INDEX");
        ParseTree tree = parser.parseArrayIndex();
        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseArrayIndex_EOFError() {
        // [3
        List<Token> tokens = List.of(
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "3")
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected RIGHT_SQB but reached EOF", () -> parser.parseArrayIndex());
    }

    @Test
    void test_parseArrayIndex_wrongTokenError() {
        // ["value"]
        List<Token> tokens = List.of(
            leftSQBToken,
            new VariableToken(TokenType.STRING, "\"value\""),
            rightSQBToken
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected NUMBER but got STRING ('\"value\"')", () -> parser.parseArrayIndex());
    }

    // parseArrayLiteral
    @Test
    void test_parseArrayLiteral_empty() {
        List<Token> tokens = List.of(
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = createNestedTree(tokens, "ARRAY-LIT");

        assertEquals(expectedParseTree, parser.parseArrayLiteral());
    }

    @Test
    void test_parseArrayLiteral_one() {
        List<Token> tokens = List.of(
            leftCBToken,
            new VariableToken(TokenType.ID, "computedValue"),
            new VariableToken(TokenType.OP, "^"),
            new VariableToken(TokenType.NUMBER, "1"),
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("ARRAY-LIT", List.of(
            leftCBNode,
            createNestedTree(tokens.subList(1, 4), "EXPR", "ARITH-EXPR"),
            rightCBNode
        ));

        assertEquals(expectedParseTree, parser.parseArrayLiteral());
    }

    @Test
    void test_parseArrayLiteral_multiple() {
        // {computedValue ^ 1, someFunc(), someArr[0]}
        List<Token> tokens = List.of(
            leftCBToken,
            new VariableToken(TokenType.ID, "computedValue"),
            new VariableToken(TokenType.OP, "^"),
            new VariableToken(TokenType.NUMBER, "1"),
            commaToken,
            new VariableToken(TokenType.ID, "someFunc"),
            leftParenToken,
            rightParenToken,
            commaToken,
            new VariableToken(TokenType.ID, "someArr"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "0"),
            rightSQBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("ARRAY-LIT", List.of(
            leftCBNode,
            createNestedTree(tokens.subList(1, 4), "EXPR", "ARITH-EXPR"),
            commaNode,
            createNestedTree(tokens.subList(5, 8), "EXPR", "FUNC-CALL"),
            commaNode,
            new ParseTree("EXPR", List.of(
                new ParseTree("ARRAY-ACCESS", List.of(
                    new ParseTree(tokens.get(9)),
                    createNestedTree(tokens.subList(10, 13), "ARRAY-INDEX")
                ))
            )),
            rightCBNode
        ));

        assertEquals(expectedParseTree, parser.parseArrayLiteral());
    }

    @Test
    void test_parseArrayLiteral_nested() {
        // {{}, {"red"}}
        List<Token> tokens = List.of(
            leftCBToken,
            leftCBToken,
            rightCBToken,
            commaToken,
            leftCBToken,
            new VariableToken(TokenType.STRING, "\"red\""),
            rightCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("ARRAY-LIT", List.of(
            leftCBNode,
            new ParseTree("EXPR", List.of(
                new ParseTree("ARRAY-LIT", List.of(
                    leftCBNode,
                    rightCBNode
                ))
            )),
            commaNode,
            new ParseTree("EXPR", List.of(
                new ParseTree("ARRAY-LIT", List.of(
                    leftCBNode,
                    new ParseTree("EXPR", List.of(new ParseTree(tokens.get(5)))),
                    rightCBNode
                ))
            )),
            rightCBNode
        ));

        assertEquals(expectedParseTree, parser.parseArrayLiteral());
    }

    @Test
    void test_parseArrayLiteral_error() {

    }

    // parseConditional

    // parseIf

    // parseElse

    // parseConditionalBody

    // parseBlockBody

    // parseLoop

    // parseForLoop

    // parseWhileLoop

    // parseFunctionDefinition

    // parseFunctionParameter

    // parseArrayType
    @Test
    void test_parseArrayType_success() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.OP, ">")
        );
        ParseTree expectedParseTree = createNestedTree(tokens, "ARRAY-TYPE");
        Parser parser = new Parser(tokens);
        ParseTree tree = parser.parseArrayType();
        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseArrayType_wrongTokenError() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, ">"),
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.OP, "<")
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected < but got OP ('>')", () -> parser.parseArrayType());
    }

    @Test
    void test_parseArrayType_wrongTypeError() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new VariableToken(TokenType.ID, "integer"),
            new VariableToken(TokenType.OP, ">")
        );
        Parser parser = new Parser(tokens);
        SyntaxError error = assertThrows(SyntaxError.class, () -> parser.parseArrayType());
        assertEquals("Expected TYPE but got ID ('integer')", error.getMessage());
    }

    // parseImmutableArrayDeclaration

    // parseArrayDeclaration
    @Test
    void test_parseArrayDeclaration_regularNoAssignment() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "arr")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("ARRAY-DECL", List.of(
            createNestedTree(tokens.subList(0, 4), "ARRAY-TYPE"),
            new ParseTree(tokens.get(4))
        ));
        
        ParseTree tree = parser.parseArrayDeclaration();
        assertEquals(expectedParseTree, tree);
    }

    @Test
    void test_parseArrayDeclaration_regular() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "magnitudes"),
            new VariableToken(TokenType.OP, "="),
            leftCBToken,
            new VariableToken(TokenType.NUMBER, "0.00035"),
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("ARRAY-DECL", List.of(
            createNestedTree(tokens.subList(0, 4), "ARRAY-TYPE"),
            new ParseTree(tokens.get(4)),
            new ParseTree(tokens.get(5)),
            new ParseTree("ARRAY-LIT", List.of(
                leftCBNode,
                createNestedTree(new ParseTree(tokens.get(7)), "EXPR"),
                rightCBNode
            ))
        ));
        
        ParseTree tree = parser.parseArrayDeclaration();
        assertEquals(expectedParseTree, tree);
    }

    // parseVariableDeclaration
    @Test
    void test_parseVariableDeclaration_simple() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.ID, "count"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.NUMBER, "5")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("VAR-DECL", List.of(
            new ParseTree(tokens.get(0)),
            new ParseTree(tokens.get(1)),
            new ParseTree(tokens.get(2)),
            createNestedTree(tokens.subList(3, 5), "EXPR", "UNARY-OP", "LEFT-UNARY-OP")
        ));

        assertEquals(expectedParseTree, parser.parseVariableDeclaration());
    }

    @Test
    void test_parseVariableDeclaration_const() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_BOOL),
            new VariableToken(TokenType.ID, "isOpen"),
            new VariableToken(TokenType.OP, "="),
            new StaticToken(TokenType.KW_FALSE)
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("VAR-DECL", List.of(
            new ParseTree(tokens.get(0)),
            new ParseTree(tokens.get(1)),
            new ParseTree(tokens.get(2)),
            new ParseTree(tokens.get(3)),
            new ParseTree("EXPR", List.of(
                new ParseTree(tokens.get(4))
            ))
        ));

        assertEquals(expectedParseTree, parser.parseVariableDeclaration());
    }

    @Test
    void test_parseVariableDeclaration_missingType() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "count"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.NUMBER, "1")
        );
        Parser parser = new Parser(tokens);


        assertSyntaxError("Expected TYPE but got ID ('count')", () -> parser.parseVariableDeclaration());
    }

    @Test
    void test_parseVariableDeclaration_wrongOperator() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.ID, "count"),
            new VariableToken(TokenType.OP, "*="),
            new VariableToken(TokenType.NUMBER, "1")
        );
        Parser parser = new Parser(tokens);


        assertSyntaxError("Expected '=' but got OP ('*=')", () -> parser.parseVariableDeclaration());
    }

    @Test
    void test_parseVariableDeclaration_constMissingAssignment() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_STR),
            new VariableToken(TokenType.ID, "name")
        );
        Parser parser = new Parser(tokens);


        assertSyntaxError("Constant variable must be initialized", () -> parser.parseVariableDeclaration());
    }

    // parseVariableAssignment
    @ParameterizedTest
    @ValueSource(strings = {"=", "+=", "-=", "*=", "/="})
    void test_variableAssignment(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "count"),
            new VariableToken(TokenType.OP, operator),
            new VariableToken(TokenType.NUMBER, "1")
        );
        Parser parser = new Parser(tokens);

        ParseTree expectedParseTree = new ParseTree("VAR-ASSIGN", List.of(
            new ParseTree(tokens.get(0)),
            new ParseTree(tokens.get(1)),
            new ParseTree("EXPR", List.of(
                new ParseTree(tokens.get(2))
            ))
        ));

        assertEquals(expectedParseTree, parser.parseVariableAssignment());
    }

    // parseType
    @ParameterizedTest
    @EnumSource(value = TokenType.class, names = {"KW_INT", "KW_STR", "KW_BOOL", "KW_FLOAT"})
    void test_parseType_primitiveTypeSuccess(TokenType type) {
        List<Token> token = List.of(new StaticToken(type));
        Parser parser = new Parser(token);
        assertEquals(new ParseTree(token.get(0)), parser.parseType());
    }

    @Test
    void test_parseType_fails() {
        List<Token> token = List.of(new StaticToken(TokenType.KW_FN));
        Parser parser = new Parser(token);
        assertSyntaxError("Expected TYPE but got KW_FN", () -> parser.parseType());
    }

    // parseControlFlow
    @Test
    void test_parseControlFlow_successReturnOnly() {
        List<Token> token = List.of(new StaticToken(TokenType.KW_RET));
        ParseTree expectedParseTree = createNestedTree(token, "CONTROL-FLOW");
        Parser parser = new Parser(token);
        assertEquals(expectedParseTree, parser.parseControlFlow());
    }

    @Test
    void test_parseControlFlow_successReturnValue() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_RET),
            new VariableToken(TokenType.ID, "value"),
            new VariableToken(TokenType.OP, "%"),
            new VariableToken(TokenType.NUMBER, "2")
        );
        ParseTree expectedParseTree = new ParseTree("CONTROL-FLOW", List.of(
            new ParseTree(tokens.get(0)),
            createNestedTree(tokens.subList(1, 4), "EXPR", "TERM")
        ));
        Parser parser = new Parser(tokens);
        assertEquals(expectedParseTree, parser.parseControlFlow());
    }

    @ParameterizedTest
    @EnumSource(value = TokenType.class, names = {"KW_CNT", "KW_BRK"})
    void test_parseControlFlow_successNonReturn(TokenType type) {
        List<Token> tokens = List.of(new StaticToken(type));
        ParseTree expectedParseTree = new ParseTree("CONTROL-FLOW", tokens.get(0));
        Parser parser = new Parser(tokens);
        assertEquals(expectedParseTree, parser.parseControlFlow());
    }
}
