package com.piedpiper.bolt.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
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
import static com.piedpiper.bolt.parser.AbstractSyntaxTree.createNestedTree;

public class TestParser {
    // commonly used tokens
    private final Token commaToken = new StaticToken(TokenType.COMMA);
    private final Token leftParenToken = new StaticToken(TokenType.LEFT_PAREN);
    private final Token rightParenToken = new StaticToken(TokenType.RIGHT_PAREN);
    private final Token leftCBToken = new StaticToken(TokenType.LEFT_CB);
    private final Token rightCBToken = new StaticToken(TokenType.RIGHT_CB);
    private final Token leftSQBToken = new StaticToken(TokenType.LEFT_SQB);
    private final Token rightSQBToken = new StaticToken(TokenType.RIGHT_SQB);

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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(2), List.of(
            createNestedTree(tokens.subList(0, 2), "UNARY-OP"),
            new AbstractSyntaxTree(tokens.get(3))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseExpr());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(1), List.of(
                    new AbstractSyntaxTree(tokens.get(0)),
                    new AbstractSyntaxTree(tokens.get(2))
                )),
                new AbstractSyntaxTree(tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(7), List.of(
                new AbstractSyntaxTree(tokens.get(6)),
                new AbstractSyntaxTree(tokens.get(8))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseExpr());
    }

    @Test
    void test_parseExpr_complex() {
        // 2 + 2 <= 4 || i % 2 == 0 && i != length
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(1), List.of(
                    new AbstractSyntaxTree(tokens.get(0)),
                    new AbstractSyntaxTree(tokens.get(2))
                )),
                new AbstractSyntaxTree(tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(11), List.of(
                new AbstractSyntaxTree(tokens.get(9), List.of(
                    new AbstractSyntaxTree(tokens.get(7), List.of(
                        new AbstractSyntaxTree(tokens.get(6)),
                        new AbstractSyntaxTree(tokens.get(8))
                    )),
                    new AbstractSyntaxTree(tokens.get(10))
                )),
                new AbstractSyntaxTree(tokens.get(13), List.of(
                    new AbstractSyntaxTree(tokens.get(12)),
                    new AbstractSyntaxTree(tokens.get(14))
                ))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseExpr());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2))
        ));
        assertEquals(expectedAbstractSyntaxTree, parser.parseArithmeticExpression());
    }

    // parseTerm
    @Test
    void test_parseTerm_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "*"),
            new VariableToken(TokenType.ID, "j")
        );

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2))
        ));
        Parser parser = new Parser(tokens);
        assertEquals(expectedAbstractSyntaxTree, parser.parseTerm());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(2), List.of(
                new AbstractSyntaxTree(tokens.get(1)),
                new AbstractSyntaxTree(tokens.get(3))
            )),
            new AbstractSyntaxTree(tokens.get(6))
        ));
        Parser parser = new Parser(tokens);
        assertEquals(expectedAbstractSyntaxTree, parser.parseTerm());
    }

    // parseExponent
    @Test
    void test_parseExpo_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "**"),
            new VariableToken(TokenType.ID, "n")
        );

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2))
        ));
        Parser parser = new Parser(tokens);
        assertEquals(expectedAbstractSyntaxTree, parser.parseExponent());
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
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLogicalOr());
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
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(2), List.of(
                new AbstractSyntaxTree(tokens.get(1)),
                new AbstractSyntaxTree(tokens.get(3))
            )),
            new AbstractSyntaxTree(tokens.get(7), List.of(
                new AbstractSyntaxTree(tokens.get(6)),
                new AbstractSyntaxTree(tokens.get(8))
            ))
        ));
        assertEquals(expectedAbstractSyntaxTree, parser.parseLogicalOr());
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
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLogicalAnd());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(5), List.of(
                    new AbstractSyntaxTree(tokens.get(4)),
                    new AbstractSyntaxTree(tokens.get(6))
                ))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLogicalAnd());
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
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(3), List.of(
            new AbstractSyntaxTree(tokens.get(1), List.of(
                new AbstractSyntaxTree(tokens.get(0)),
                new AbstractSyntaxTree(tokens.get(2))
            )),
            new AbstractSyntaxTree(tokens.get(4))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseComparisonExpression());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(tokens, "UNARY-OP");

        assertEquals(expectedAbstractSyntaxTree, parser.parseUnaryOp());
    }

    @Test
    void test_parseUnaryOp_callsLeftUnaryOp() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.ID, "i")
            
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(tokens, "UNARY-OP");

        assertEquals(expectedAbstractSyntaxTree, parser.parseUnaryOp());
    }

    // parseLeftUnaryOp
    @ParameterizedTest
    @ValueSource(strings = {"++", "--", "-", "!"})
    void test_parseLeftUnaryOp_succeedsID(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, operator),
            new VariableToken(TokenType.ID, "x")
        );

        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(tokens, "UNARY-OP");

        Parser parser = new Parser(tokens);
        AbstractSyntaxTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedAbstractSyntaxTree, tree);
    }

    @ParameterizedTest
    @ValueSource(strings = {"++", "--", "-"})
    void test_parseLeftUnaryOp_succeedsWithNumLiteral(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, operator),
            new VariableToken(TokenType.NUMBER, "5")
        );

        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(tokens, "UNARY-OP");

        Parser parser = new Parser(tokens);
        AbstractSyntaxTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedAbstractSyntaxTree, tree);
    }

    @Test
    void test_parseLeftUnaryOp_succeedsWithBooleanLiteral() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new StaticToken(TokenType.KW_TRUE)
        );

        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(tokens, "UNARY-OP");
        Parser parser = new Parser(tokens);
        AbstractSyntaxTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedAbstractSyntaxTree, tree);
    }

    @Test
    void test_parseLeftUnaryOp_succeedsWithFunctionCall() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new VariableToken(TokenType.ID, "isInt"),
            leftParenToken,
            rightParenToken
        );

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree("FUNC-CALL", tokens.get(1))
        ));

        Parser parser = new Parser(tokens);
        AbstractSyntaxTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedAbstractSyntaxTree, tree);
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
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1), List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                    new AbstractSyntaxTree(tokens.get(3))
                ))
            ))
        ));

        AbstractSyntaxTree tree = parser.parseLeftUnaryOp();

        assertEquals(expectedAbstractSyntaxTree, tree);
    }

    @Test
    void test_parseLeftUnaryOp_wrongToken() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new VariableToken(TokenType.STRING, "\"true\"")
        );

        Parser parser = new Parser(tokens);
        assertSyntaxError("Invalid unary operator on STRING", parser::parseLeftUnaryOp);
    }

    // parseTernary
    @Test
    void test_parseTernary_simple() {
        // isOpen ? connect : disconnect
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "isOpen"),
            new VariableToken(TokenType.OP, "?"),
            new VariableToken(TokenType.ID, "connect"),
            new StaticToken(TokenType.COLON),
            new VariableToken(TokenType.ID, "disconnect")
        );
        Parser parser = new Parser(tokens);
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("TERNARY", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree(tokens.get(4))
        ));
        assertEquals(expectedAbstractSyntaxTree, parser.parseTernary());
    }

    @Test
    void test_parseTernary_complex() {
        // i % 2 == 0 ? i / 2 : (i - 1) / 2
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
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("TERNARY", List.of(
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(1), List.of(
                    new AbstractSyntaxTree(tokens.get(0)),
                    new AbstractSyntaxTree(tokens.get(2))
                )),
                new AbstractSyntaxTree(tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(7), List.of(
                new AbstractSyntaxTree(tokens.get(6)),
                new AbstractSyntaxTree(tokens.get(8))
            )),
            new AbstractSyntaxTree(tokens.get(15), List.of(
                new AbstractSyntaxTree(tokens.get(12), List.of(
                    new AbstractSyntaxTree(tokens.get(11)),
                    new AbstractSyntaxTree(tokens.get(13))
                )),
                new AbstractSyntaxTree(tokens.get(16))
            ))
        ));
        assertEquals(expectedAbstractSyntaxTree, parser.parseTernary());
    }

    // parseFunctionCall
    @Test
    void test_parseFunctionCall_noParams() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "request"),
            leftParenToken,
            rightParenToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("FUNC-CALL", tokens.get(0));

        assertEquals(expectedAbstractSyntaxTree, parser.parseFunctionCall());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("FUNC-CALL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree("FUNC-PARAMS", tokens.get(2))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseFunctionCall());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("FUNC-CALL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(4), List.of(
                    new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                        new AbstractSyntaxTree(tokens.get(6))
                    ))
                )),
                new AbstractSyntaxTree("FUNC-CALL", List.of(
                    new AbstractSyntaxTree(tokens.get(9)),
                    new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                        new AbstractSyntaxTree(tokens.get(11))
                    ))
                ))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseFunctionCall());
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
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(2))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseArrayAccess());
    }

    @Test
    void test_parseArrayAccess_multipleIndexes() {
        // arr[3][0]
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "arr"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "3"),
            rightSQBToken,
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "0"),
            rightSQBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                new AbstractSyntaxTree(tokens.get(2), List.of(
                    new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(5))
                ))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseArrayAccess());
    }

    @Test
    void test_parseArrayAccess_EOFError() {
        // x[
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftSQBToken
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected EXPR but reached EOF", parser::parseArrayAccess);
    }

    @Test
    void test_parseArrayAccess_wrongTokenError() {
        // x(
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftParenToken
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected LEFT_SQB but got LEFT_PAREN", parser::parseArrayAccess);
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
        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(tokens.get(1), "ARRAY-INDEX");
        AbstractSyntaxTree tree = parser.parseArrayIndex();
        assertEquals(expectedAbstractSyntaxTree, tree);
    }

    @Test
    void test_parseArrayIndex_EOFError() {
        // [3
        List<Token> tokens = List.of(
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "3")
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected RIGHT_SQB but reached EOF", parser::parseArrayIndex);
    }

    @Test
    void test_parseArrayIndex_EmptyError() {
        // []
        List<Token> tokens = List.of(
            leftSQBToken,
            rightSQBToken
        );
        Parser parser = new Parser(tokens);
        assertSyntaxError("Expected EXPR but got RIGHT_SQB", parser::parseArrayIndex);
    }

    // parseArrayLiteral
    @Test
    void test_parseArrayLiteral_empty() {
        List<Token> tokens = List.of(
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-LIT");

        assertEquals(expectedAbstractSyntaxTree, parser.parseArrayLiteral());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(tokens.get(2), List.of(
                new AbstractSyntaxTree(tokens.get(1)),
                new AbstractSyntaxTree(tokens.get(3))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseArrayLiteral());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(tokens.get(2), List.of(
                new AbstractSyntaxTree(tokens.get(1)),
                new AbstractSyntaxTree(tokens.get(3))
            )),
            new AbstractSyntaxTree("FUNC-CALL", tokens.get(5)),
            new AbstractSyntaxTree(tokens.get(9), List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(11))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseArrayLiteral());
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            createNestedTree(tokens.get(5), "ARRAY-LIT")
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseArrayLiteral());
    }

    @Test
    void test_parseArrayLiteral_missingComma() {
        // {1, 1 0} -> missing comma
        List<Token> tokens = List.of(
            leftCBToken,
            new VariableToken(TokenType.NUMBER, "1"),
            commaToken,
            new VariableToken(TokenType.NUMBER, "1"),
            new VariableToken(TokenType.NUMBER, "0"),
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        assertSyntaxError("Expected COMMA but got NUMBER ('0')", parser::parseArrayLiteral);
    }

    @Test
    void test_parseArrayLiteral_missingElement() {
        // {1, 1,} -> missing comma
        List<Token> tokens = List.of(
            leftCBToken,
            new VariableToken(TokenType.NUMBER, "1"),
            commaToken,
            new VariableToken(TokenType.NUMBER, "1"),
            commaToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        assertSyntaxError("Expected EXPR but got RIGHT_CB", parser::parseArrayLiteral);
    }

    @Test
    void test_parseArrayLiteral_missingClosingCB() {
        // {1, 1  -> missing closing CB
        List<Token> tokens = List.of(
            leftCBToken,
            new VariableToken(TokenType.NUMBER, "1"),
            commaToken,
            new VariableToken(TokenType.NUMBER, "1")
        );
        Parser parser = new Parser(tokens);

        assertSyntaxError("Expected RIGHT_CB but reached EOF", parser::parseArrayLiteral);
    }

    // parseConditional
    @Test
    void test_parseConditional_emptyIfBlock() {
        // if (condition) {}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_IF),
            leftParenToken,
            new VariableToken(TokenType.ID, "condition"),
            rightParenToken,
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("BLOCK-BODY")
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseConditional());
    }

    @Test
    void test_parseConditional_singleLineIfBlock() {
        // if (condition) { setValue(false) }
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_IF),
            leftParenToken,
            new VariableToken(TokenType.ID, "condition"),
            rightParenToken,
            leftCBToken,
            new VariableToken(TokenType.ID, "setValue"),
            leftParenToken,
            new StaticToken(TokenType.KW_FALSE),
            rightParenToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("FUNC-CALL", List.of(
                        new AbstractSyntaxTree(tokens.get(5)),
                        new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                            new AbstractSyntaxTree(tokens.get(7))
                        ))
                    ))
                ))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseConditional());
    }

    @Test
    void test_parseConditional_singleLineIfNoCB() {
        // if (condition) setValue(false)
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_IF),
            leftParenToken,
            new VariableToken(TokenType.ID, "condition"),
            rightParenToken,
            new VariableToken(TokenType.ID, "setValue"),
            leftParenToken,
            new StaticToken(TokenType.KW_FALSE),
            rightParenToken
        );
        Parser parser = new Parser(tokens);


        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("FUNC-CALL", List.of(
                        new AbstractSyntaxTree(tokens.get(4)),
                        new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                            new AbstractSyntaxTree(tokens.get(6))
                        ))
                    ))
                ))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseConditional());
    }

    @Test
    void test_parseConditional_parseIfElseSimple() {
        /*
            if (condition)
                return true
            else
                return false
        */
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_IF),
            leftParenToken,
            new VariableToken(TokenType.ID, "condition"),
            rightParenToken,
            new StaticToken(TokenType.KW_RET),
            new StaticToken(TokenType.KW_TRUE),
            new StaticToken(TokenType.KW_ELSE),
            new StaticToken(TokenType.KW_RET),
            new StaticToken(TokenType.KW_FALSE)
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                createNestedTree(tokens.subList(4, 6), "BLOCK-BODY", "CONTROL-FLOW")
            )),
            new AbstractSyntaxTree(tokens.get(6), List.of(
                createNestedTree(tokens.subList(7, 9), "BLOCK-BODY", "CONTROL-FLOW")
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseConditional());
    }

    @Test
    void test_parseConditional_parseIfElseEmpty() {
        // if (x < 3) {} else if (x > 3) {} else {}
        Token varToken = new VariableToken(TokenType.ID, "x");
        Token numToken = new VariableToken(TokenType.NUMBER, "3");
        Token ifToken = new StaticToken(TokenType.KW_IF);
        Token elseToken = new StaticToken(TokenType.KW_ELSE);

        List<Token> tokens = List.of(
            ifToken,
            leftParenToken,
            varToken,
            new VariableToken(TokenType.OP, "<"),
            numToken,
            rightParenToken,
            leftCBToken,
            rightCBToken,
            elseToken,
            ifToken,
            leftParenToken,
            varToken,
            new VariableToken(TokenType.OP, ">"),
            numToken,
            rightParenToken,
            leftCBToken,
            rightCBToken,
            elseToken,
            leftCBToken,
            rightCBToken

        );
        Parser parser = new Parser(tokens);


        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(3), List.of(
                    new AbstractSyntaxTree(tokens.get(2)),
                    new AbstractSyntaxTree(tokens.get(4))
                )),
                new AbstractSyntaxTree("BLOCK-BODY")
            )),
            new AbstractSyntaxTree("ELSE IF", List.of(
                new AbstractSyntaxTree(tokens.get(12), List.of(
                    new AbstractSyntaxTree(tokens.get(11)),
                    new AbstractSyntaxTree(tokens.get(13))
                )),
                new AbstractSyntaxTree("BLOCK-BODY")
            )),
            new AbstractSyntaxTree(tokens.get(17), List.of(
                new AbstractSyntaxTree("BLOCK-BODY")
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseConditional());
    }

    // parseLoop
    @Test
    void test_parseLoop_emptyWhileTrue() {
        // while(true) {}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_WHILE),
            leftParenToken,
            new StaticToken(TokenType.KW_TRUE),
            rightParenToken,
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(2))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLoop());
    }

    @Test
    void test_parseLoop_singleStatementWhileTrue() {
        // while(true) { break }
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_WHILE),
            leftParenToken,
            new StaticToken(TokenType.KW_TRUE),
            rightParenToken,
            leftCBToken,
            new StaticToken(TokenType.KW_BRK),
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree("CONTROL-FLOW", List.of(new AbstractSyntaxTree(tokens.get(5))))
            ))

        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLoop());
    }

    @Test
    void test_parseLoop_multiStatementWhileTrue() {
        /*
            while(true) {
                var *= 5
                break
            }
        */
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_WHILE),
            leftParenToken,
            new StaticToken(TokenType.KW_TRUE),
            rightParenToken,
            leftCBToken,
            new VariableToken(TokenType.ID, "var"),
            new VariableToken(TokenType.OP, "*="),
            new VariableToken(TokenType.NUMBER, "5"),
            new StaticToken(TokenType.KW_BRK),
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree(tokens.get(6), List.of(
                    new AbstractSyntaxTree(tokens.get(5)),
                    new AbstractSyntaxTree(tokens.get(7))
                )),
                createNestedTree(tokens.get(8), "CONTROL-FLOW")
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLoop());
    }

    @Test
    void test_parseLoop_emptyWhile() {
        // while (i < length) {}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_WHILE),
            leftParenToken,
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "<"),
            new VariableToken(TokenType.ID, "length"),
            rightParenToken,
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(4))
            ))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLoop());
    }

    @Test
    void test_parseLoop_emptyForEach() {
        // for (int element : array) {}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FOR),
            leftParenToken,
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.ID, "element"),
            new StaticToken(TokenType.COLON),
            new VariableToken(TokenType.ID, "array"),
            rightParenToken,
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree("VAR-DECL", List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(3))
            )),
            new AbstractSyntaxTree(tokens.get(5))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLoop());
    }

    @Test
    void test_parseLoop_emptyForDeclaration() {
        // for (int i = 0; i < length; i++) {}
        Token SCToken = new StaticToken(TokenType.SC);
        Token varToken = new VariableToken(TokenType.ID, "i");

        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FOR),
            leftParenToken,
            new StaticToken(TokenType.KW_INT),
            varToken,
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.NUMBER, "0"),
            SCToken,
            varToken,
            new VariableToken(TokenType.OP, "<"),
            new VariableToken(TokenType.ID, "length"),
            SCToken,
            varToken,
            new VariableToken(TokenType.OP, "++"),
            rightParenToken,
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);
        List<Token> varDecl = new ArrayList<>(tokens.subList(2, 6));
        varDecl.remove(2);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            createNestedTree(varDecl, "VAR-DECL"),
            new AbstractSyntaxTree(tokens.get(8), List.of(
                new AbstractSyntaxTree(tokens.get(7)),
                new AbstractSyntaxTree(tokens.get(9))
            )),
            createNestedTree(tokens.subList(11, 13), "UNARY-OP")
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseLoop());
    }

    // parseFunctionDefinition
    @Test
    void test_parseFunctionDefinition_voidAndEmpty() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FN),
            new VariableToken(TokenType.ID, "print"),
            leftParenToken,
            rightParenToken,
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(1))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseFunctionDefinition());
    }

    @Test
    void test_parseFunctionDefinition_nonVoidAndEmpty() {
        // fn reverse() : string {}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FN),
            new VariableToken(TokenType.ID, "reverse"),
            leftParenToken,
            rightParenToken,
            new StaticToken(TokenType.COLON),
            new StaticToken(TokenType.KW_STR),
            leftCBToken,
            rightCBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(1)),
            new AbstractSyntaxTree(tokens.get(5))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseFunctionDefinition());
    }

    // parseFunctionParameter
    @Test
    void test_parseFunctionParameter_noDefault() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_STR),
            new VariableToken(TokenType.ID, "param1")
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(tokens, "FUNC-PARAM");

        assertEquals(expectedAbstractSyntaxTree, parser.parseFunctionParameter());
    }

    @Test
    void test_parseFunctionParameter_default() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_STR),
            new VariableToken(TokenType.ID, "param1"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.STRING, "\"\"")
        );
        Parser parser = new Parser(tokens);

        List<Token> parsedTokens = new ArrayList<>(tokens);
        parsedTokens.remove(2);

        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(parsedTokens, "FUNC-PARAM");

        assertEquals(expectedAbstractSyntaxTree, parser.parseFunctionParameter());
    }

    // parseArrayType
    @Test
    void test_parseArrayType_success() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.OP, ">")
        );
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(0));
        expectedAbstractSyntaxTree.appendChildren(new AbstractSyntaxTree(tokens.get(2)));
        Parser parser = new Parser(tokens);
        AbstractSyntaxTree tree = parser.parseArrayType();
        assertEquals(expectedAbstractSyntaxTree, tree);
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
        assertSyntaxError("Expected < but got OP ('>')", parser::parseArrayType);
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
        SyntaxError error = assertThrows(SyntaxError.class, parser::parseArrayType);
        assertEquals("Expected TYPE but got ID ('integer')", error.getMessage());
    }

    // parseImmutableArrayDeclaration
    @Test
    void test_ImmutableArrayDeclaration_noSize() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "arr"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.ID, "someId")
        );
        Parser parser = new Parser(tokens);
        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(1));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(3)));

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            arrayType,
            new AbstractSyntaxTree(tokens.get(5)),
            //new ParseTree(tokens.get(6)),
            new AbstractSyntaxTree(tokens.get(7))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseImmutableArrayDeclaration());
    }

    @Test
    void test_ImmutableArrayDeclaration_withSize() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "arr"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "2"),
            rightSQBToken,
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.ID, "someId")
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1), List.of(
                new AbstractSyntaxTree(tokens.get(3))
            )),
            new AbstractSyntaxTree(tokens.get(5)),
            createNestedTree(tokens.get(7), "ARRAY-INDEX"),
            //new ParseTree(tokens.get(9)),
            new AbstractSyntaxTree(tokens.get(10))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseImmutableArrayDeclaration());
    }

    @Test
    void test_ImmutableArrayDeclaration_notInitialized() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "arr")
        );
        Parser parser = new Parser(tokens);


        assertSyntaxError("Constant array cannot be uninitialized", parser::parseImmutableArrayDeclaration);
    }

    // parseArrayDeclaration
    @Test
    void test_parseArrayDeclaration_regularNoAssignment() {
        // Array<int> arr[5]
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "arr"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "5"),
            rightSQBToken
        );
        Parser parser = new Parser(tokens);
        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(0));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(2)));

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            arrayType,
            new AbstractSyntaxTree(tokens.get(4)),
            createNestedTree(tokens.get(6), "ARRAY-INDEX")
        ));
        
        AbstractSyntaxTree tree = parser.parseArrayDeclaration();
        assertEquals(expectedAbstractSyntaxTree, tree);
    }

    @Test
    void test_parseArrayDeclaration_regular() {
        // Array<float> magnitudes = {0.00035}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "magnitudes"),
            leftSQBToken,
            new VariableToken(TokenType.ID, "capacity"),
            rightSQBToken,
            new VariableToken(TokenType.OP, "="),
            leftCBToken,
            new VariableToken(TokenType.NUMBER, "0.00035"),
            rightCBToken
        );
        Parser parser = new Parser(tokens);
        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(0));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(2)));

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            arrayType,
            new AbstractSyntaxTree(tokens.get(4)),
            createNestedTree(tokens.get(6), "ARRAY-INDEX"),
            //new ParseTree(tokens.get(8)),
            createNestedTree(tokens.get(10), "ARRAY-LIT")
        ));
        
        AbstractSyntaxTree tree = parser.parseArrayDeclaration();
        assertEquals(expectedAbstractSyntaxTree, tree);
    }

    @Test
    void test_parseArrayDeclaration_constMut() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_MUT),
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "arr"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "2"),
            rightSQBToken
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(2));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(4)));

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1)),
            arrayType,
            new AbstractSyntaxTree(tokens.get(6)),
            createNestedTree(tokens.get(8), "ARRAY-INDEX")
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseArrayDeclaration());
    }

    @Test
    void test_parseArrayDeclaration_noSizeError() {
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

        assertSyntaxError("Expected LEFT_SQB but got OP ('=')", parser::parseArrayDeclaration);
    }

    // parseVariableDeclaration
    @Test
    void test_parseVariableDeclaration_simple() {
        // int count = -5
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.ID, "count"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.NUMBER, "5")
        );
        Parser parser = new Parser(tokens);

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("VAR-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1)),
            //new ParseTree(tokens.get(2)),
            createNestedTree(tokens.subList(3, 5), "UNARY-OP")
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseVariableDeclaration());
    }

    @Test
    void test_parseVariableDeclaration_string() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_STR),
            new VariableToken(TokenType.ID, "str"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.STRING, "\"someValue\"")
        );
        Parser parser = new Parser(tokens);

        List<Token> parsingTokens = new ArrayList<>(tokens);
        parsingTokens.remove(2);
        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(parsingTokens, "VAR-DECL");

        assertEquals(expectedAbstractSyntaxTree, parser.parseVariableDeclaration());
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
        List<Token> parsingTokens = new ArrayList<>(tokens);
        parsingTokens.remove(3);
        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(parsingTokens, "VAR-DECL");

        assertEquals(expectedAbstractSyntaxTree, parser.parseVariableDeclaration());
    }

    @Test
    void test_parseVariableDeclaration_missingType() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "count"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.NUMBER, "1")
        );
        Parser parser = new Parser(tokens);


        assertSyntaxError("Expected TYPE but got ID ('count')", parser::parseVariableDeclaration);
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


        assertSyntaxError("Expected '=' but got OP ('*=')", parser::parseVariableDeclaration);
    }

    @Test
    void test_parseVariableDeclaration_constMissingAssignment() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_STR),
            new VariableToken(TokenType.ID, "name")
        );
        Parser parser = new Parser(tokens);


        assertSyntaxError("Constant variable must be initialized", parser::parseVariableDeclaration);
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

        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2))
        ));

        assertEquals(expectedAbstractSyntaxTree, parser.parseVariableAssignment());
    }

    // parseType
    @ParameterizedTest
    @EnumSource(value = TokenType.class, names = {"KW_INT", "KW_STR", "KW_BOOL", "KW_FLOAT"})
    void test_parseType_primitiveTypeSuccess(TokenType type) {
        List<Token> token = List.of(new StaticToken(type));
        Parser parser = new Parser(token);
        assertEquals(new AbstractSyntaxTree(token.get(0)), parser.parseType());
    }

    @Test
    void test_parseType_fails() {
        List<Token> token = List.of(new StaticToken(TokenType.KW_FN));
        Parser parser = new Parser(token);
        assertSyntaxError("Expected TYPE but got KW_FN", parser::parseType);
    }

    // parseControlFlow
    @Test
    void test_parseControlFlow_successReturnOnly() {
        List<Token> token = List.of(new StaticToken(TokenType.KW_RET));
        AbstractSyntaxTree expectedAbstractSyntaxTree = createNestedTree(token, "CONTROL-FLOW");
        Parser parser = new Parser(token);
        assertEquals(expectedAbstractSyntaxTree, parser.parseControlFlow());
    }

    @Test
    void test_parseControlFlow_successReturnValue() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_RET),
            new VariableToken(TokenType.ID, "value"),
            new VariableToken(TokenType.OP, "%"),
            new VariableToken(TokenType.NUMBER, "2")
        );
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("CONTROL-FLOW", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2), List.of(
                new AbstractSyntaxTree(tokens.get(1)),
                new AbstractSyntaxTree(tokens.get(3))
            ))
        ));
        Parser parser = new Parser(tokens);
        assertEquals(expectedAbstractSyntaxTree, parser.parseControlFlow());
    }

    @ParameterizedTest
    @EnumSource(value = TokenType.class, names = {"KW_CNT", "KW_BRK"})
    void test_parseControlFlow_successNonReturn(TokenType type) {
        List<Token> tokens = List.of(new StaticToken(type));
        AbstractSyntaxTree expectedAbstractSyntaxTree = new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(0));
        Parser parser = new Parser(tokens);
        assertEquals(expectedAbstractSyntaxTree, parser.parseControlFlow());
    }
}
