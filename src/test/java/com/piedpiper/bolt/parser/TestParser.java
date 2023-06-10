package com.piedpiper.bolt.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;

public class TestParser {
    // commonly used tokens
    private final Token commaToken = new StaticToken(TokenType.COMMA);
    private final Token leftParenToken = new StaticToken(TokenType.LEFT_PAREN);
    private final Token rightParenToken = new StaticToken(TokenType.RIGHT_PAREN);
    private final Token leftCBToken = new StaticToken(TokenType.LEFT_CB);
    private final Token rightCBToken = new StaticToken(TokenType.RIGHT_CB);
    private final Token leftSQBToken = new StaticToken(TokenType.LEFT_SQB);
    private final Token rightSQBToken = new StaticToken(TokenType.RIGHT_SQB);

    private void assertSyntaxError(String expectedErrorMessage, List<Token> tokens) {
        Parser parser = new Parser(tokens);
        SyntaxError error = assertThrows(SyntaxError.class, parser::parse);
        assertEquals(expectedErrorMessage, error.getMessage());
    }

    private void assertAST(AbstractSyntaxTree expectedAST, List<Token> tokens) {
        Parser parser = new Parser(tokens);
        AbstractSyntaxTree rootAST = new AbstractSyntaxTree("PROGRAM");
        rootAST.appendChildren(expectedAST);
        assertEquals(rootAST, parser.parse());
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
        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(2), List.of(
            new AbstractSyntaxTree("UNARY-OP", tokens.get(0), tokens.get(1)),
            new AbstractSyntaxTree(tokens.get(3))
        ));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(7), tokens.get(6), tokens.get(8))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(11), List.of(
                new AbstractSyntaxTree(tokens.get(9), List.of(
                    new AbstractSyntaxTree(tokens.get(7), tokens.get(6), tokens.get(8)),
                    new AbstractSyntaxTree(tokens.get(10))
                )),
                new AbstractSyntaxTree(tokens.get(13), tokens.get(12), tokens.get(14))
            ))
        ));

        assertAST(expectedAST, tokens);
    }

    // parseArithmeticExpression
    @Test
    void test_parseArithmeticExpression_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "length"),
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.ID, "i")
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2));
        
        assertAST(expectedAST, tokens);
    }

    // parseTerm
    @Test
    void test_parseTerm_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, "*"),
            new VariableToken(TokenType.ID, "j")
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(2), tokens.get(1), tokens.get(3)),
            new AbstractSyntaxTree(tokens.get(6))
        ));

        assertAST(expectedAST, tokens);
    }

    // parseExponent
    @Test
    void test_parseExpo_simple() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.NUMBER, "2"),
            new VariableToken(TokenType.OP, "**"),
            new VariableToken(TokenType.ID, "n")
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2));

        assertAST(expectedAST, tokens);
    }

    // parseLogicalOr
    @Test
    void test_parseLogicalOr_simple() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_TRUE),
            new VariableToken(TokenType.OP, "||"),
            new StaticToken(TokenType.KW_FALSE)
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(5), List.of(
            new AbstractSyntaxTree(tokens.get(2), tokens.get(1), tokens.get(3)),
            new AbstractSyntaxTree(tokens.get(7), tokens.get(6), tokens.get(8))
        ));
        assertAST(expectedAST, tokens);
    }


    // parseLogicalAnd
    @Test
    void test_parseLogicalAnd_simple() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_TRUE),
            new VariableToken(TokenType.OP, "&&"),
            new StaticToken(TokenType.KW_FALSE)
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(1), List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(5), tokens.get(4), tokens.get(6))
            ))
        ));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(3), List.of(
            new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2)),
            new AbstractSyntaxTree(tokens.get(4))
        ));

        assertAST(expectedAST, tokens);
    }

    // parseUnaryOp
    @ParameterizedTest
    @ValueSource(strings = {"++", "--"})
    void test_parseUnaryOp(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "i"),
            new VariableToken(TokenType.OP, operator)
        );


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("UNARY-OP", tokens.get(0), tokens.get(1));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseUnaryOp_callsLeftUnaryOp() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "-"),
            new VariableToken(TokenType.ID, "i")

        );


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("UNARY-OP", tokens.get(0), tokens.get(1));

        assertAST(expectedAST, tokens);
    }

    // parseLeftUnaryOp
    @ParameterizedTest
    @ValueSource(strings = {"++", "--", "-", "!"})
    void test_parseLeftUnaryOp_succeedsID(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, operator),
            new VariableToken(TokenType.ID, "x")
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("UNARY-OP", tokens.get(0), tokens.get(1));

        assertAST(expectedAST, tokens);
    }

    @ParameterizedTest
    @ValueSource(strings = {"++", "--", "-"})
    void test_parseLeftUnaryOp_succeedsWithNumLiteral(String operator) {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, operator),
            new VariableToken(TokenType.NUMBER, "5")
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("UNARY-OP", tokens.get(0), tokens.get(1));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseLeftUnaryOp_succeedsWithBooleanLiteral() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new StaticToken(TokenType.KW_TRUE)
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("UNARY-OP", tokens.get(0), tokens.get(1));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseLeftUnaryOp_succeedsWithFunctionCall() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new VariableToken(TokenType.ID, "isInt"),
            leftParenToken,
            rightParenToken
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree("FUNC-CALL", tokens.get(1))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("UNARY-OP", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1), List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(3))
            ))
        ));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseLeftUnaryOp_wrongToken() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.OP, "!"),
            new VariableToken(TokenType.STRING, "\"true\"")
        );

        assertSyntaxError("Invalid unary operator on STRING", tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("TERNARY", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree(tokens.get(4))
        ));
        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("TERNARY", List.of(
            new AbstractSyntaxTree(tokens.get(3), List.of(
                new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(7), tokens.get(6), tokens.get(8)),
            new AbstractSyntaxTree(tokens.get(15), List.of(
                new AbstractSyntaxTree(tokens.get(12), tokens.get(11), tokens.get(13)),
                new AbstractSyntaxTree(tokens.get(16))
            ))
        ));
        assertAST(expectedAST, tokens);
    }

    // parseFunctionCall
    @Test
    void test_parseFunctionCall_noParams() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "request"),
            leftParenToken,
            rightParenToken
        );


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("FUNC-CALL", tokens.get(0));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseFunctionCall_oneParam() {
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "request"),
            leftParenToken,
            new VariableToken(TokenType.STRING, "\"https://website.com/resource/\""),
            rightParenToken
        );


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("FUNC-CALL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree("FUNC-PARAMS", tokens.get(2))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("FUNC-CALL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree(tokens.get(4), List.of(
                    new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(6))
                )),
                new AbstractSyntaxTree("FUNC-CALL", List.of(
                    new AbstractSyntaxTree(tokens.get(9)),
                    new AbstractSyntaxTree("FUNC-PARAMS", tokens.get(11))
                ))
            ))
        ));

        assertAST(expectedAST, tokens);
    }

    // parseArrayAccess
    @Test
    void test_parseArrayAccess_succeedsWithArrayIndex() {
        // x[12]
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "12"),
            rightSQBToken
        );


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(2))
        ));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(5))
            ))
        ));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseArrayAccess_EOFError() {
        // x[
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftSQBToken
        );

        assertSyntaxError("Expected EXPR but reached EOF", tokens);
    }

    @Test
    void test_parseArrayAccess_EmptyIndexError() {
        // x[]
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "x"),
            leftSQBToken,
            rightSQBToken
        );

        assertSyntaxError("Expected EXPR but got RIGHT_SQB", tokens);
    }

    // parseArrayLiteral
    @Test
    void test_parseArrayLiteral_empty() {
        List<Token> tokens = List.of(
            leftCBToken,
            rightCBToken
        );
        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-LIT");

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(tokens.get(2), tokens.get(1), tokens.get(3))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(tokens.get(2), tokens.get(1), tokens.get(3)),
            new AbstractSyntaxTree("FUNC-CALL", tokens.get(5)),
            new AbstractSyntaxTree(tokens.get(9), List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(11))
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT", tokens.get(5))
        ));

        assertAST(expectedAST, tokens);
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


        assertSyntaxError("Expected COMMA but got NUMBER ('0')", tokens);
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


        assertSyntaxError("Expected EXPR but got RIGHT_CB", tokens);
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


        assertSyntaxError("Expected RIGHT_CB but reached EOF", tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("BLOCK-BODY")
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("FUNC-CALL", List.of(
                        new AbstractSyntaxTree(tokens.get(5)),
                        new AbstractSyntaxTree("FUNC-PARAMS", tokens.get(7))
                    ))
                ))
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("FUNC-CALL", List.of(
                        new AbstractSyntaxTree(tokens.get(4)),
                        new AbstractSyntaxTree("FUNC-PARAMS", tokens.get(6))
                    ))
                ))
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(4), tokens.get(5))
                ))
            )),
            new AbstractSyntaxTree(tokens.get(6), List.of(
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(7), tokens.get(8))
                ))
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(3), tokens.get(2), tokens.get(4)),
                new AbstractSyntaxTree("BLOCK-BODY")
            )),
            new AbstractSyntaxTree("ELSE IF", List.of(
                new AbstractSyntaxTree(tokens.get(12), tokens.get(11), tokens.get(13)),
                new AbstractSyntaxTree("BLOCK-BODY")
            )),
            new AbstractSyntaxTree(tokens.get(17), List.of(
                new AbstractSyntaxTree("BLOCK-BODY")
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree("BLOCK-BODY")
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(5))
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree(tokens.get(6), tokens.get(5), tokens.get(7)),
                new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(8))
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(3), tokens.get(2), tokens.get(4)),
            new AbstractSyntaxTree("BLOCK-BODY")
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree("VAR-DECL", tokens.get(2), tokens.get(3)),
            new AbstractSyntaxTree(tokens.get(5)),
            new AbstractSyntaxTree("BLOCK-BODY")
        ));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree("VAR-DECL", tokens.get(2), tokens.get(3), tokens.get(5)),
            new AbstractSyntaxTree(tokens.get(8), tokens.get(7), tokens.get(9)),
            new AbstractSyntaxTree("UNARY-OP", tokens.get(11), tokens.get(12)),
            new AbstractSyntaxTree("BLOCK-BODY")
        ));

        assertAST(expectedAST, tokens);
    }

    // parseControlFlow
    @ParameterizedTest
    @EnumSource(value = TokenType.class, names = {"KW_CNT", "KW_BRK"})
    void test_parseControlFlow_successNonReturn(TokenType type) {
        // while (true) { <type> }
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_WHILE),
            leftParenToken,
            new StaticToken(TokenType.KW_TRUE),
            rightParenToken,
            leftCBToken,
            new StaticToken(type),
            rightCBToken
        );
        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(2)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(5))
            ))
        ));
        assertAST(expectedAST, tokens);
    }

    // parseFunctionDefinition
    @Test
    void test_parseFunctionDefinition_voidAndEmpty() {
        // fn print() {}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FN),
            new VariableToken(TokenType.ID, "print"),
            leftParenToken,
            rightParenToken,
            leftCBToken,
            rightCBToken
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), tokens.get(1));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), tokens.get(1), tokens.get(5));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseFunctionDefinition_oneParam() {
        // fn doNothing (string str) : string { return str }
        Token varToken = new VariableToken(TokenType.ID, "str");

        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FN),
            new VariableToken(TokenType.ID, "doNothing"),
            leftParenToken,
            new StaticToken(TokenType.KW_STR),
            varToken,
            rightParenToken,
            new StaticToken(TokenType.COLON),
            new StaticToken(TokenType.KW_STR),
            leftCBToken,
            new StaticToken(TokenType.KW_RET),
            varToken,
            rightCBToken
        );
        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(1)),
            new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                new AbstractSyntaxTree("FUNC-PARAM", tokens.get(3), tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(7)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(9), tokens.get(10))
            ))
        ));
        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseFunctionDefinition_multiParams() {
        // fn concat (string var1, string var2) : string { return var1 + var2 }
        Token var1Token = new VariableToken(TokenType.ID, "var1");
        Token var2Token = new VariableToken(TokenType.ID, "var2");

        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FN),
            new VariableToken(TokenType.ID, "concat"),
            leftParenToken,
            new StaticToken(TokenType.KW_STR),
            var1Token,
            new StaticToken(TokenType.COMMA),
            new StaticToken(TokenType.KW_STR),
            var2Token,
            rightParenToken,
            new StaticToken(TokenType.COLON),
            new StaticToken(TokenType.KW_STR),
            leftCBToken,
            new StaticToken(TokenType.KW_RET),
            var1Token,
            new VariableToken(TokenType.OP, "+"),
            var2Token,
            rightCBToken
        );
        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(1)),
            new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                new AbstractSyntaxTree("FUNC-PARAM", tokens.get(3), tokens.get(4)),
                new AbstractSyntaxTree("FUNC-PARAM", tokens.get(6), tokens.get(7))
            )),

            new AbstractSyntaxTree(tokens.get(10)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                    new AbstractSyntaxTree(tokens.get(12)),
                    new AbstractSyntaxTree(tokens.get(14), tokens.get(13), tokens.get(15))
                ))
            ))
        ));
        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseFunctionDefinition_voidReturn() {
        // fn test() { return }
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FN),
            new VariableToken(TokenType.ID, "test"),
            leftParenToken,
            rightParenToken,
            leftCBToken,
            new StaticToken(TokenType.KW_RET),
            rightCBToken
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(0), List.of(
            new AbstractSyntaxTree(tokens.get(1)),
            new AbstractSyntaxTree("BLOCK-BODY", List.of(
                new AbstractSyntaxTree("CONTROL-FLOW", tokens.get(5))
            ))
        ));

        assertAST(expectedAST, tokens);
    }

    // parseArrayType
    @Test
    void test_parseArrayType_wrongTokenError() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, ">"),
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.OP, "<")
        );

        assertSyntaxError("Expected < but got OP ('>')", tokens);
    }

    @Test
    void test_parseArrayType_wrongTypeError() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new VariableToken(TokenType.ID, "integer"),
            new VariableToken(TokenType.OP, ">")
        );

        assertSyntaxError("Expected TYPE but got ID ('integer')", tokens);
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

        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(1));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(3)));

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            arrayType,
            new AbstractSyntaxTree(tokens.get(5)),
            new AbstractSyntaxTree(tokens.get(7))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1), tokens.get(3)),
            new AbstractSyntaxTree(tokens.get(5)),
            new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(7)),
            new AbstractSyntaxTree(tokens.get(10))
        ));

        assertAST(expectedAST, tokens);
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


        assertSyntaxError("Constant array cannot be uninitialized", tokens);
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

        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(0));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(2)));

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            arrayType,
            new AbstractSyntaxTree(tokens.get(4)),
            new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(6))
        ));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(0));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(2)));

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            arrayType,
            new AbstractSyntaxTree(tokens.get(4)),
            new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(6)),
            new AbstractSyntaxTree("ARRAY-LIT", tokens.get(10))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(2));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(4)));

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1)),
            arrayType,
            new AbstractSyntaxTree(tokens.get(6)),
            new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(8))
        ));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseArrayDeclaration_noSizeError() {
        // Array<float> magnitudes = {0.00035}
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


        assertSyntaxError("Expected LEFT_SQB but got OP ('=')", tokens);
    }

    @Test
    void test_parseArrayDeclaration_Nested() {
        // Array<Array<int>> values[2][3] = {{4}, {1}}
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_ARR),
            new VariableToken(TokenType.OP, "<"),
            new StaticToken(TokenType.KW_INT),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.OP, ">"),
            new VariableToken(TokenType.ID, "values"),
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "2"),
            rightSQBToken,
            leftSQBToken,
            new VariableToken(TokenType.NUMBER, "3"),
            rightSQBToken,
            new VariableToken(TokenType.OP, "="),
            leftCBToken,
            leftCBToken,
            new VariableToken(TokenType.NUMBER, "4"),
            rightCBToken,
            commaToken,
            leftCBToken,
            new VariableToken(TokenType.NUMBER, "1"),
            rightCBToken,
            rightCBToken
        );

        AbstractSyntaxTree arrayType = new AbstractSyntaxTree(tokens.get(0));
        arrayType.appendChildren(new AbstractSyntaxTree(tokens.get(2)));

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("ARRAY-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree(tokens.get(2), tokens.get(4))
            )),
            new AbstractSyntaxTree(tokens.get(7)),
            new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                new AbstractSyntaxTree(tokens.get(9)),
                new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(12))
            )),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", tokens.get(17)),
                new AbstractSyntaxTree("ARRAY-LIT", tokens.get(21))
            ))
        ));

        assertAST(expectedAST, tokens);
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


        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("VAR-DECL", List.of(
            new AbstractSyntaxTree(tokens.get(0)),
            new AbstractSyntaxTree(tokens.get(1)),
            new AbstractSyntaxTree("UNARY-OP", tokens.get(3), tokens.get(4))
        ));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseVariableDeclaration_string() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_STR),
            new VariableToken(TokenType.ID, "str"),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.STRING, "\"someValue\"")
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("VAR-DECL", tokens.get(0), tokens.get(1), tokens.get(3));

        assertAST(expectedAST, tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree("VAR-DECL", tokens.get(0), tokens.get(1), tokens.get(2), tokens.get(4));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_parseVariableDeclaration_wrongOperator() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_FLOAT),
            new VariableToken(TokenType.ID, "count"),
            new VariableToken(TokenType.OP, "*="),
            new VariableToken(TokenType.NUMBER, "1")
        );


        assertSyntaxError("Expected '=' but got OP ('*=')", tokens);
    }

    @Test
    void test_parseVariableDeclaration_constMissingAssignment() {
        List<Token> tokens = List.of(
            new StaticToken(TokenType.KW_CONST),
            new StaticToken(TokenType.KW_STR),
            new VariableToken(TokenType.ID, "name")
        );


        assertSyntaxError("Constant variable must be initialized", tokens);
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

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(1), tokens.get(0), tokens.get(2));

        assertAST(expectedAST, tokens);
    }

    @Test
    void test_variableAssignmentArrayIndex() {
        // arr[0][2] = 0.001
        List<Token> tokens = List.of(
            new VariableToken(TokenType.ID, "arr"),
            new StaticToken(TokenType.LEFT_SQB),
            new VariableToken(TokenType.NUMBER, "0"),
            new StaticToken(TokenType.RIGHT_SQB),
            new StaticToken(TokenType.LEFT_SQB),
            new VariableToken(TokenType.NUMBER, "2"),
            new StaticToken(TokenType.RIGHT_SQB),
            new VariableToken(TokenType.OP, "="),
            new VariableToken(TokenType.NUMBER, "0.001")
        );

        AbstractSyntaxTree expectedAST = new AbstractSyntaxTree(tokens.get(7), List.of(
            new AbstractSyntaxTree(tokens.get(0), List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                    new AbstractSyntaxTree(tokens.get(2)),
                    new AbstractSyntaxTree("ARRAY-INDEX", tokens.get(5))
                ))
            )),
            new AbstractSyntaxTree(tokens.get(8))
        ));

        assertAST(expectedAST, tokens);
    }
}