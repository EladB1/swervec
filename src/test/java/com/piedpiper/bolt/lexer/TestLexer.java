package com.piedpiper.bolt.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.piedpiper.bolt.error.SyntaxError;

public class TestLexer {
    private Lexer lexer = new Lexer();

    void assertZeroTokens(String line) {
        List<Token> tokens = lexer.analyzeLine(line);
        assertEquals(0, tokens.size());
    }

    void assertOneToken(String line, Token expectedToken) {
        List<Token> tokens = lexer.analyzeLine(line);
        assertEquals(1, tokens.size());
        assertEquals(expectedToken, tokens.get(0));

    }

    void assertManyTokens(String line, List<Token> expectedTokens) {
        List<Token> tokens = lexer.analyzeLine(line);
        assertEquals(expectedTokens.size(), tokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            assertEquals(expectedTokens.get(i), tokens.get(i));
        }
    }

    private static Stream<Arguments> provideInvalidNumberParameters() {
        return Stream.of(
            Arguments.of("5.", "5."),
            Arguments.of("15. + 3.9", "15."), 
            Arguments.of("0..5", "0..5")
        );
    }

    @Test
    void analyzeLine_shouldRecognizeInteger() {
        Token token = new Token("NUMBER", "10");
        assertOneToken("10", token);
    }

    @Test
    void analyzeLine_shouldRecognizeFloat() {
        Token token = new Token("NUMBER", "1.0");
        assertOneToken("1.0", token);
    }

    @Test
    void analyzeLine_shouldOnlyRecognizeInteger() {
        Token token = new Token("NUMBER", "2");
        assertOneToken(" 2\t", token);
    }

    @Test
    void analyzeLine_shouldSkipWhitespaceOnLastChar() {
        Token token = new Token("NUMBER", "2");
        assertOneToken("2 ", token);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidNumberParameters")
    void analyzeLine_shouldNotThrowErrorOnBrokenFloat(String input, String badNumberFragment) {
        SyntaxError error = assertThrows(SyntaxError.class, () -> lexer.analyzeLine(input));
        String expectedExceptionMessage = String.format("Found invalid number '%s'", badNumberFragment);
        assertEquals(expectedExceptionMessage, error.getMessage());
    }

    @Test
    void analyzeLine_shouldRecognizeNumberAddition() {
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "14"), 
            new Token("OP", "+"),
            new Token("NUMBER", "19")
        );
        assertManyTokens("14 + 19", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeNumberDivision() {
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "144"), 
            new Token("OP", "/"),
            new Token("NUMBER", "12")
        );
        assertManyTokens("144/12", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeNumberIncrement() {
        List<Token> expectedTokens = List.of(
            new Token("NUMBER", "5"), 
            new Token("OP", "++")
        );
        assertManyTokens("5++", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeString() {
        Token token = new Token("STRING", "\"Hello\"");
        assertOneToken("\"Hello\"", token);
    }

    @Test
    void analyzeLine_shouldRecognizeEmptyString() {
        Token token = new Token("STRING", "\"\"");
        assertOneToken("\"\"", token);
    }

    @Test
    void analyzeLine_shouldRecognizeStringWithEscape() {
        Token token = new Token("STRING", "\"{ \\\"a\\\": 10 }\"");
        assertOneToken("\"{ \\\"a\\\": 10 }\"", token);
    }

    @Test
    void analyzeLine_shouldNotRecognizeIncompleteString() {
        SyntaxError error = assertThrows(SyntaxError.class, () -> lexer.analyzeLine("\"Hello"));
        assertEquals("EOL while scanning string literal", error.getMessage());
    }

    @Test
    void analyzeLine_shouldRecognizeFunctionCall() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "print"),
            new Token("LEFT_PAREN", "("),
            new Token("NUMBER", "2"),
            new Token("RIGHT_PAREN", ")")
        );
        assertManyTokens("print(2)", expectedTokens);
    }

    @Test
    void analyzeLine_shouldReconizeKeyword() {
        Token token = new Token("KW_FN", "fn");
        assertOneToken("fn", token);
    }

    @Test
    void analyzeLine_shouldRecognizeExpression() {
        List<Token> expectedTokens = List.of(
            new Token("KW_CONST", "const"),
            new Token("KW_FLOAT", "float"),
            new Token("ID", "PI"),
            new Token("OP", "="),
            new Token("NUMBER", "3.14")
        );
        assertManyTokens("const float PI = 3.14", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeComplexExpression() {
        List<Token> expectedTokens = List.of(
            new Token("KW_FLOAT", "float"),
            new Token("ID", "y"),
            new Token("OP", "="),
            new Token("ID", "someInteger"),
            new Token("OP", "*"),
            new Token("ID", "PI"),
            new Token("OP", "+"),
            new Token("NUMBER", "1")
        );
        assertManyTokens("float y = someInteger * PI + 1", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeExpressionWithoutSpace() {
        List<Token> expectedTokens = List.of(
            new Token("KW_INT", "int"),
            new Token("ID", "y"),
            new Token("OP", "="),
            new Token("NUMBER", "2"),
            new Token("OP", "+"),
            new Token("NUMBER", "3")
        );
        assertManyTokens("int y=2+3", expectedTokens);
    }

    @Test 
    void analyzeLine_shouldRecognizeExpressionWithStrings() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "languages"),
            new Token("OP", "+="),
            new Token("STRING", "\"Korean\""),
            new Token("OP", "+"),
            new Token("STRING", "\"Portuguese\"")
        );
        assertManyTokens("languages += \"Korean\" + \"Portuguese\"", expectedTokens);
    }

    @Test
    void analyzeLine_shouldIgnoreStandaloneInlineComment() {
        assertZeroTokens("// this is a comment");
    }

    @Test
    void analyzeLine_shouldIgnoreInlineComment() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "y"),
            new Token("OP", "++")
        );
        assertManyTokens("y++ // y+= 1", expectedTokens);
    }

    @Test
    void analyzeLine_shouldTakeLineNumber() {
        List<Token> token = lexer.analyzeLine("main()", 5);
        List<Token> expectedToken = List.of(new Token("ID", "main", 5));
        assertEquals(expectedToken.get(0), token.get(0));
    }

    @Test
    void analyzeLine_shouldUseLineNumberInError() {
        SyntaxError error = assertThrows(SyntaxError.class, () -> lexer.analyzeLine("\"", 321));
        assertEquals("Line 321\n\tEOL while scanning string literal", error.getMessage());
    }

    @Test
    void analyzeLine_shouldRecognizeIntegerAndParens() {
        List<Token> expectedTokens = List.of(
            new Token("LEFT_PAREN", "("),
            new Token("NUMBER", "2"),
            new Token("RIGHT_PAREN", ")")
        );
        assertManyTokens("(2)", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeBooleanAssignment() {
        List<Token> expectedTokens = List.of(
            new Token("KW_BOOL", "boolean"),
            new Token("ID", "isFree"),
            new Token("OP", "="),
            new Token("ID", "state"),
            new Token("OP", "=="),
            new Token("STRING", "\"free\"")
        );
        assertManyTokens("boolean isFree = state == \"free\"", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeVoidFunctionDefinition() {
        List<Token> expectedTokens = List.of(
            new Token("KW_FN", "fn"),
            new Token("ID", "reverse"),
            new Token("LEFT_PAREN", "("),
            new Token("KW_STR", "string"),
            new Token("ID", "param"),
            new Token("RIGHT_PAREN", ")"),
            new Token("LEFT_CB", "{")
        );
        assertManyTokens("fn reverse(string param) {", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeReturningFunctionDefinition() {
        List<Token> expectedTokens = List.of(
            new Token("KW_FN", "fn"),
            new Token("ID", "concat"),
            new Token("LEFT_PAREN", "("),
            new Token("KW_STR", "string"),
            new Token("ID", "first"),
            new Token("COMMA", ","),
            new Token("KW_STR", "string"),
            new Token("ID", "second"),
            new Token("RIGHT_PAREN", ")"),
            new Token("COLON", ":"),
            new Token("KW_STR", "string"),
            new Token("LEFT_CB", "{")
        );
        assertManyTokens("fn concat(string first, string second) : string {", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeEmptyIf() {
        List<Token> expectedTokens = List.of(
            new Token("KW_IF", "if"),
            new Token("LEFT_PAREN", "("),
            new Token("ID", "x"),
            new Token("OP", "<"),
            new Token("NUMBER", "5"),
            new Token("RIGHT_PAREN", ")"),
            new Token("LEFT_CB", "{"),
            new Token("RIGHT_CB", "}")
        );
        assertManyTokens("if (x < 5) {}", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeElseIf() {
        List<Token> expectedTokens = List.of(
            new Token("KW_ELSE", "else"),
            new Token("KW_IF", "if"),
            new Token("LEFT_PAREN", "("),
            new Token("ID", "var"),
            new Token("RIGHT_PAREN", ")"),
            new Token("LEFT_CB", "{")
        );
        assertManyTokens("else if (var) {", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeReturn() {
        Token token = new Token("KW_RET", "return");
        assertOneToken("return", token);
    }

    @Test
    void analyzeLine_shouldRecognizeSemicolon() {
        Token token = new Token("SC", ";");
        assertOneToken(";", token);
    }

    @Test
    void analyzeLine_shouldThrowErrorOnUnrecognizedChar() {
        SyntaxError error = assertThrows(SyntaxError.class, () -> lexer.analyzeLine("x = ~5", 10));
        assertEquals("Line 10\n\tUnrecognized character '~'", error.getMessage());
    }

    @Test
    void analyze_shouldIngoreMultilineComment() {
        assertZeroTokens("/*This is a comment*/");
    }

    @Test
    void analyze_shouldIngoreMultilineCommentButHandleToken() {
        Token token = new Token("NUMBER", "5");
        assertOneToken("/*This is a comment*/5", token);
    }

    @Test
    void analyze_shouldIngoreMultilineCommentButHandleSeveralTokens() {
        List<Token> expectedTokens = List.of(
            new Token("ID", "y"),
            new Token("OP", "="),
            new Token("ID", "PI"),
            new Token("OP", "*"),
            new Token("ID", "radius"),
            new Token("OP", "**"),
            new Token("NUMBER", "2")
        );
        assertManyTokens("y = /*multiply(PI, radius ** 2)*/ PI * radius ** 2", expectedTokens);
    }
}
