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
        Token token = new Token(TokenType.NUMBER, "10");
        assertOneToken("10", token);
    }

    @Test
    void analyzeLine_shouldRecognizeFloat() {
        Token token = new Token(TokenType.NUMBER, "1.0");
        assertOneToken("1.0", token);
    }

    @Test
    void analyzeLine_shouldOnlyRecognizeInteger() {
        Token token = new Token(TokenType.NUMBER, "2");
        assertOneToken(" 2\t", token);
    }

    @Test
    void analyzeLine_shouldSkipWhitespaceOnLastChar() {
        Token token = new Token(TokenType.NUMBER, "2");
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
            new Token(TokenType.NUMBER, "14"), 
            new Token(TokenType.OP, "+"),
            new Token(TokenType.NUMBER, "19")
        );
        assertManyTokens("14 + 19", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeNumberDivision() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.NUMBER, "144"), 
            new Token(TokenType.OP, "/"),
            new Token(TokenType.NUMBER, "12")
        );
        assertManyTokens("144/12", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeNumberIncrement() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.NUMBER, "5"), 
            new Token(TokenType.OP, "++")
        );
        assertManyTokens("5++", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeString() {
        Token token = new Token(TokenType.STRING, "\"Hello\"");
        assertOneToken("\"Hello\"", token);
    }

    @Test
    void analyzeLine_shouldRecognizeEmptyString() {
        Token token = new Token(TokenType.STRING, "\"\"");
        assertOneToken("\"\"", token);
    }

    @Test
    void analyzeLine_shouldRecognizeStringWithEscape() {
        Token token = new Token(TokenType.STRING, "\"{ \\\"a\\\": 10 }\"");
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
            new Token(TokenType.ID, "print"),
            new Token(TokenType.LEFT_PAREN, "("),
            new Token(TokenType.NUMBER, "2"),
            new Token(TokenType.RIGHT_PAREN, ")")
        );
        assertManyTokens("print(2)", expectedTokens);
    }

    @Test
    void analyzeLine_shouldReconizeKeyword() {
        Token token = new Token(TokenType.KW_FN, "fn");
        assertOneToken("fn", token);
    }

    @Test
    void analyzeLine_shouldRecognizeExpression() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_CONST, "const"),
            new Token(TokenType.KW_FLOAT, "float"),
            new Token(TokenType.ID, "PI"),
            new Token(TokenType.OP, "="),
            new Token(TokenType.NUMBER, "3.14")
        );
        assertManyTokens("const float PI = 3.14", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeComplexExpression() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_FLOAT, "float"),
            new Token(TokenType.ID, "y"),
            new Token(TokenType.OP, "="),
            new Token(TokenType.ID, "someInteger"),
            new Token(TokenType.OP, "*"),
            new Token(TokenType.ID, "PI"),
            new Token(TokenType.OP, "+"),
            new Token(TokenType.NUMBER, "1")
        );
        assertManyTokens("float y = someInteger * PI + 1", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeExpressionWithoutSpace() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_INT, "int"),
            new Token(TokenType.ID, "y"),
            new Token(TokenType.OP, "="),
            new Token(TokenType.NUMBER, "2"),
            new Token(TokenType.OP, "+"),
            new Token(TokenType.NUMBER, "3")
        );
        assertManyTokens("int y=2+3", expectedTokens);
    }

    @Test 
    void analyzeLine_shouldRecognizeExpressionWithStrings() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.ID, "languages"),
            new Token(TokenType.OP, "+="),
            new Token(TokenType.STRING, "\"Korean\""),
            new Token(TokenType.OP, "+"),
            new Token(TokenType.STRING, "\"Portuguese\"")
        );
        assertManyTokens("languages += \"Korean\" + \"Portuguese\"", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeArrayDeclaration() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_CONST, "const"),
            new Token(TokenType.KW_ARR, "array"),
            new Token(TokenType.LEFT_SQB, "["),
            new Token(TokenType.KW_INT, "int"),
            new Token(TokenType.RIGHT_SQB, "]"),
            new Token(TokenType.ID, "myList"),
            new Token(TokenType.OP, "="),
            new Token(TokenType.LEFT_CB, "{"),
            new Token(TokenType.NUMBER, "10"),
            new Token(TokenType.COMMA, ","),
            new Token(TokenType.NUMBER, "20"),
            new Token(TokenType.COMMA, ","),
            new Token(TokenType.NUMBER, "30"),
            new Token(TokenType.RIGHT_CB, "}")
        );
        assertManyTokens("const array[int] myList = {10, 20, 30}", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeArrayIndex() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_CONST, "const"),
            new Token(TokenType.KW_INT, "int"),
            new Token(TokenType.ID, "value"),
            new Token(TokenType.OP, "="),
            new Token(TokenType.ID, "myList"),
            new Token(TokenType.LEFT_SQB, "["),
            new Token(TokenType.NUMBER, "0"),
            new Token(TokenType.RIGHT_SQB, "]")
        );
        assertManyTokens("const int value = myList[0]", expectedTokens);
    }

    @Test
    void analyzeLine_shouldIgnoreStandaloneInlineComment() {
        assertZeroTokens("// this is a comment");
    }

    @Test
    void analyzeLine_shouldIgnoreInlineComment() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.ID, "y"),
            new Token(TokenType.OP, "++")
        );
        assertManyTokens("y++ // y+= 1", expectedTokens);
    }

    @Test
    void analyzeLine_shouldTakeLineNumber() {
        List<Token> token = lexer.analyzeLine("main()", 5);
        List<Token> expectedToken = List.of(new Token(TokenType.ID, "main", 5));
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
            new Token(TokenType.LEFT_PAREN, "("),
            new Token(TokenType.NUMBER, "2"),
            new Token(TokenType.RIGHT_PAREN, ")")
        );
        assertManyTokens("(2)", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeBooleanAssignment() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_BOOL, "boolean"),
            new Token(TokenType.ID, "isFree"),
            new Token(TokenType.OP, "="),
            new Token(TokenType.ID, "state"),
            new Token(TokenType.OP, "=="),
            new Token(TokenType.STRING, "\"free\"")
        );
        assertManyTokens("boolean isFree = state == \"free\"", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeVoidFunctionDefinition() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_FN, "fn"),
            new Token(TokenType.ID, "reverse"),
            new Token(TokenType.LEFT_PAREN, "("),
            new Token(TokenType.KW_STR, "string"),
            new Token(TokenType.ID, "param"),
            new Token(TokenType.RIGHT_PAREN, ")"),
            new Token(TokenType.LEFT_CB, "{")
        );
        assertManyTokens("fn reverse(string param) {", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeReturningFunctionDefinition() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_FN, "fn"),
            new Token(TokenType.ID, "concat"),
            new Token(TokenType.LEFT_PAREN, "("),
            new Token(TokenType.KW_STR, "string"),
            new Token(TokenType.ID, "first"),
            new Token(TokenType.COMMA, ","),
            new Token(TokenType.KW_STR, "string"),
            new Token(TokenType.ID, "second"),
            new Token(TokenType.RIGHT_PAREN, ")"),
            new Token(TokenType.COLON, ":"),
            new Token(TokenType.KW_STR, "string"),
            new Token(TokenType.LEFT_CB, "{")
        );
        assertManyTokens("fn concat(string first, string second) : string {", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeEmptyIf() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_IF, "if"),
            new Token(TokenType.LEFT_PAREN, "("),
            new Token(TokenType.ID, "x"),
            new Token(TokenType.OP, "<"),
            new Token(TokenType.NUMBER, "5"),
            new Token(TokenType.RIGHT_PAREN, ")"),
            new Token(TokenType.LEFT_CB, "{"),
            new Token(TokenType.RIGHT_CB, "}")
        );
        assertManyTokens("if (x < 5) {}", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeElseIf() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.KW_ELSE, "else"),
            new Token(TokenType.KW_IF, "if"),
            new Token(TokenType.LEFT_PAREN, "("),
            new Token(TokenType.ID, "var"),
            new Token(TokenType.RIGHT_PAREN, ")"),
            new Token(TokenType.LEFT_CB, "{")
        );
        assertManyTokens("else if (var) {", expectedTokens);
    }

    @Test
    void analyzeLine_shouldRecognizeReturn() {
        Token token = new Token(TokenType.KW_RET, "return");
        assertOneToken("return", token);
    }

    @Test
    void analyzeLine_shouldRecognizeSemicolon() {
        Token token = new Token(TokenType.SC, ";");
        assertOneToken(";", token);
    }

    @Test
    void analyzeLine_shouldThrowErrorOnUnrecognizedChar() {
        SyntaxError error = assertThrows(SyntaxError.class, () -> lexer.analyzeLine("x = ~5", 10));
        assertEquals("Line 10\n\tUnrecognized character '~'", error.getMessage());
    }

    @Test
    void analyzeLine_shouldIngoreMultilineComment() {
        assertZeroTokens("/*This is a comment*/");
    }

    @Test
    void analyzeLine_shouldIngoreMultilineCommentButHandleToken() {
        Token token = new Token(TokenType.NUMBER, "5");
        assertOneToken("/*This is a comment*/5", token);
    }

    @Test
    void analyzeLine_shouldIngoreMultilineCommentButHandleSeveralTokens() {
        List<Token> expectedTokens = List.of(
            new Token(TokenType.ID, "y"),
            new Token(TokenType.OP, "="),
            new Token(TokenType.ID, "PI"),
            new Token(TokenType.OP, "*"),
            new Token(TokenType.ID, "radius"),
            new Token(TokenType.OP, "**"),
            new Token(TokenType.NUMBER, "2")
        );
        assertManyTokens("y = /*multiply(PI, radius ** 2)*/ PI * radius ** 2", expectedTokens);
    }

    @Test
    void lex_shouldHandleEmptyMultilineString() {
        List<String> source = List.of("/\"\"/");
        Token token = new Token(TokenType.STRING, "\"\"", 1);
        List<Token> tokens = lexer.lex(source);
        assertEquals(1, tokens.size());
        assertEquals(token, tokens.get(0));
    }

    @Test
    void lex_shouldHandleInlineMultilineString() {
        List<String> source = List.of("/\" some inline string \"/");
        Token token = new Token(TokenType.STRING, "\" some inline string \"", 1);
        List<Token> tokens = lexer.lex(source);
        assertEquals(1, tokens.size());
        assertEquals(token, tokens.get(0));
    }

    @Test
    void lex_shouldHandleInlineMultilineStringWithQuotes() {
        List<String> source = List.of("/\" some \"inline\" string \"/");
        Token token = new Token(TokenType.STRING, "\" some \\\"inline\\\" string \"", 1);
        List<Token> tokens = lexer.lex(source);
        assertEquals(1, tokens.size());
        assertEquals(token, tokens.get(0));
    }

    @Test
    void lex_shouldHandleTrueMultilineString() {
        List<String> source = List.of("/\"", "\tfn test(int x) {", "\t\tx ** 2", "\t}", "\"/");
        String tokenValue = "\"\n" +
            "\tfn test(int x) {\n" + 
            "\t\tx ** 2\n" +
            "\t}\n" +
            "\"";
        Token token = new Token(TokenType.STRING, tokenValue, 5);
        List<Token> tokens = lexer.lex(source);
        assertEquals(1, tokens.size());
        assertEquals(token, tokens.get(0));
    }

    @Test
    void lex_shouldHandleQuotesInMultilineString() {
        List<String> source = List.of("/\"", "\t{", "\t\t\"key\": [", "\t\t\t\"value1\",", "\t\t\t\"value2\"", "\t\t]", "\t}", "\"/");
        String tokenValue = "\"\n" +
            "\t{\n" + 
            "\t\t\\\"key\\\": [\n" +
            "\t\t\t\\\"value1\\\",\n" +
            "\t\t\t\\\"value2\\\"\n" +
            "\t\t]\n" +
            "\t}\n" +
            "\"";
        Token token = new Token(TokenType.STRING, tokenValue, 8);
        List<Token> tokens = lexer.lex(source);
        assertEquals(1, tokens.size());
        assertEquals(token, tokens.get(0));
    }

    @Test
    void lex_shouldThrowExceptionOnUnterminatedMultilineString() {
        List<String> source = List.of("/\"", "\tfn test(int x) {", "\t\tx ** 2", "\t}");
        SyntaxError error = assertThrows(SyntaxError.class, () -> lexer.lex(source));
        assertEquals("EOF while scanning multi-line string literal", error.getMessage());
    }
}
