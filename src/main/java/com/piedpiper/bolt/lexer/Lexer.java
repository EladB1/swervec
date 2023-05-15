package com.piedpiper.bolt.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.Map.entry;

import com.piedpiper.bolt.error.SyntaxError;

public class Lexer {
    private LexerState state = LexerState.DEFAULT;
    private final List<Token> tokens;
    private StringBuilder multiLineString = new StringBuilder();
    private final Map<String, Token> reservedWords = Map.ofEntries(
        entry("const", new StaticToken(TokenType.KW_CONST)),
        entry("int", new StaticToken(TokenType.KW_INT)),
        entry("float", new StaticToken(TokenType.KW_FLOAT)),
        entry("string", new StaticToken(TokenType.KW_STR)),
        entry("boolean", new StaticToken(TokenType.KW_BOOL)),
        entry("fn", new StaticToken(TokenType.KW_FN)),
        entry("return", new StaticToken(TokenType.KW_RET)),
        entry("for", new StaticToken(TokenType.KW_FOR)),
        entry("if", new StaticToken(TokenType.KW_IF)),
        entry("else", new StaticToken(TokenType.KW_ELSE)),
        entry("while", new StaticToken(TokenType.KW_WHILE)),
        entry("break", new StaticToken(TokenType.KW_BRK)),
        entry("continue", new StaticToken(TokenType.KW_CNT)),
        entry("true", new StaticToken(TokenType.KW_TRUE)),
        entry("false", new StaticToken(TokenType.KW_FALSE)),
        entry("Array", new StaticToken(TokenType.KW_ARR)),
        entry("mut", new StaticToken(TokenType.KW_MUT)),
        entry("null", new StaticToken(TokenType.KW_NULL))
    );

    public Lexer() {
        tokens = new ArrayList<>();
    }

    private void setState(final LexerState state) {
        this.state = state;
    }

    private void clearState() {
        setState(LexerState.DEFAULT);
    }

    public void printTokens() {
        if (tokens.isEmpty()) {
            System.out.println("No tokens");
            return;
        }
        System.out.println("Tokens: ");
        tokens.forEach(System.out::println);
        System.out.println("EOF");
    }

    public List<Token> lex(List<String> input) {
        int lineNumber = 1;
        if (!input.isEmpty()) {
            for (String line : input) {
                if (state == LexerState.IN_MULTILINE_STRING) {
                    if (!line.contains("\"/")) {
                        multiLineString.append(line.replace("\"", "\\\"").concat("\n"));
                    }
                    else {
                        analyzeLine(line, lineNumber);
                    }
                }
                else if (!line.isEmpty())
                    analyzeLine(line, lineNumber);
                lineNumber++;
            }
        }
        if (state == LexerState.IN_MULTILINE_COMMENT) {
            throw new SyntaxError("EOF while scanning multiline comment");
        }
        if (state == LexerState.IN_MULTILINE_STRING) {
            throw new SyntaxError("EOF while scanning multi-line string literal");
        }
        
        return tokens;
    }

    private boolean isNumber(char curr) {
        return String.valueOf(curr).matches("[0-9]");
    }

    private boolean isOperator(char curr) {
        return String.valueOf(curr).matches("([+\\-*/%!&^=?<>||&&])");
    }

    private boolean isLetter(char curr) {
        return String.valueOf(curr).matches("[a-zA-Z]");
    }

    private boolean isWhitespace(char curr) {
        return String.valueOf(curr).matches("\\s");
    }

    private Optional<Token> getReservedWord(String identifier) {
        // check if an identifier is a reserved word; return Optional.empty() if not
        if (reservedWords.containsKey(identifier)) {
            return Optional.of(reservedWords.get(identifier));
        }
        return Optional.empty();
    }

    private void addNumberToken(StringBuilder number, int lineNumber) {
        String num = number.toString();
        String numRegex = "[0-9]+(\\.[0-9]+)?";
        if (num.matches(numRegex)) {
            tokens.add(new VariableToken(TokenType.NUMBER, num, lineNumber));
        }
        else {
            throw new SyntaxError("Found invalid number '" + num + "'", lineNumber);
        }
    }

    private void addOperatorToken(StringBuilder operator, int lineNumber) {
        String op = operator.toString();
        String operatorRegex = "(\\+|-|\\*|/|%|!|&|\\^|=|\\?|\\+\\+|--|\\*\\*|&&|\\|\\||\\+=|<|>|<=|>=|==|!=|-=|\\*=|/=)";
        if (op.matches(operatorRegex))
            tokens.add(new VariableToken(TokenType.OP, op, lineNumber));
    }

    private void addIdentifierToken(StringBuilder identifier, int lineNumber) {
        String id = identifier.toString();
        String identifierRegex = "[a-zA-Z]([a-zA-Z0-9_])*";
        if (id.matches(identifierRegex)) {
            Optional<Token> reservedWord = getReservedWord(id);
            if (reservedWord.isEmpty()) 
                tokens.add(new VariableToken(TokenType.ID, id, lineNumber));
            else {
                Token keyword = reservedWord.get();
                tokens.add(keyword.withLineNumber(lineNumber));
            }
        }
    }

    private StringBuilder handleNumberStateChar(char currentChar, char nextChar, StringBuilder sequence, int lineNumber) {
        boolean currIsNumber = isNumber(currentChar) || currentChar == '.';
        boolean nextIsNumber = (isNumber(nextChar) || nextChar == '.') && nextChar != '\0';
        if (currIsNumber) {
            sequence.append(currentChar);
        }
        if (!nextIsNumber) {
            clearState();
            addNumberToken(sequence, lineNumber);
            return new StringBuilder(); // clear out the sequence
        }
        return sequence;
    }

    private StringBuilder handleOperatorStateChar(char currentChar, char nextChar, StringBuilder sequence, int lineNumber) {
        if (isOperator(currentChar)) {
            sequence.append(currentChar);
        }
        if (sequence.toString().equals("//"))
            return sequence;
        if (sequence.toString().equals("/*")) {
            setState(LexerState.IN_MULTILINE_COMMENT);
            return sequence;
        }
        if (!isOperator(nextChar) && nextChar != '\0') {
            clearState();
            addOperatorToken(sequence, lineNumber);
            return new StringBuilder();
        }
        if (nextChar == '\0')
            addOperatorToken(sequence, lineNumber);
        return sequence;
    }

    private StringBuilder handleIdentifierStateChar(char currentChar, char nextChar, StringBuilder sequence, int lineNumber) {
        boolean currIsID = isLetter(currentChar) || isNumber(currentChar) || currentChar == '_';
        boolean nextIsID = (isLetter(nextChar) || isNumber(nextChar) || nextChar == '_') && nextChar != '\0';
        if (currIsID) {
            sequence.append(currentChar);
        }
        if (!nextIsID) {
            clearState();
            addIdentifierToken(sequence, lineNumber);
            return new StringBuilder();
        }
        return sequence;
    }

    private int enterStringStateAndMovePosition(String line, int index, int lineNumber) {
        int length = line.length();
        String stringErrorMessage = "EOL while scanning string literal";
        if (length < 2)
            throw new SyntaxError(stringErrorMessage, lineNumber);
        int i = index  + 1;
        // already recognized the opening quote to get here
        StringBuilder sequence = new StringBuilder("\"");
        char currentChar, nextChar;
        while (i < length - 1) {
            currentChar = line.charAt(i);
            nextChar = line.charAt(i+1);
            if (currentChar == '\\' && nextChar == '"') {
                sequence.append("\\\"");
                i += 2;
                continue;
            }
            else if (currentChar == '"') {
                sequence.append('"');
                tokens.add(new VariableToken(TokenType.STRING, sequence.toString(), lineNumber));
                return i+1;
            }
            sequence.append(currentChar);
            i++;
        }
        if (line.charAt(length - 2) != '\\' && line.charAt(length -1) == '"') {
            sequence.append('"');
            tokens.add(new VariableToken(TokenType.STRING, sequence.toString(), lineNumber));
            return i+1;
        }
        throw new SyntaxError(stringErrorMessage, lineNumber);
    }

    public List<Token> analyzeLine(String line) {
        return analyzeLine(line, 0);
    }

    public List<Token> analyzeLine(String line, int lineNumber) {
        StringBuilder currentSequence = new StringBuilder();
        char currentChar, nextChar;
        int i = 0;
        while(i < line.length()) {
            currentChar = line.charAt(i);
            nextChar = i == line.length() - 1 ? '\0' : line.charAt(i+1); // look ahead to the next character and handle edge case if at the end
            if (currentChar == '/' && nextChar == '"' && state != LexerState.IN_MULTILINE_STRING) {
                setState(LexerState.IN_MULTILINE_STRING);
                multiLineString = new StringBuilder("\"");
                currentSequence = new StringBuilder(); // clear it out
                i += 2;
                continue;
            }
            if (currentChar == '"' && state != LexerState.IN_MULTILINE_STRING) {
                i = enterStringStateAndMovePosition(line, i, lineNumber);
                continue;
            }
            if (isWhitespace(currentChar) && state != LexerState.IN_MULTILINE_STRING) {
                i++;
                continue;
            }
            if (state == LexerState.DEFAULT) { // this needs to be outside the switch statement, so it can set the state
                if (isNumber(currentChar))
                    setState(LexerState.IN_NUMBER);
                else if (isOperator(currentChar))
                    setState(LexerState.IN_OPERATOR);
                else if (isLetter(currentChar))
                    setState(LexerState.IN_IDENTIFIER);
            }
            switch(state) {
                case IN_NUMBER:
                    currentSequence = handleNumberStateChar(currentChar, nextChar, currentSequence, lineNumber);
                    break;
                case IN_OPERATOR:
                    currentSequence = handleOperatorStateChar(currentChar, nextChar, currentSequence, lineNumber);
                    if (currentSequence.toString().equals("//")) {
                        clearState();
                        return tokens; // end the line at an inline comment
                    }
                    if (currentSequence.toString().equals("/*")) {
                        setState(LexerState.IN_MULTILINE_COMMENT);
                        if (!line.contains("*/"))
                            return tokens;
                    }
                    break;
                case IN_IDENTIFIER:
                    currentSequence = handleIdentifierStateChar(currentChar, nextChar, currentSequence, lineNumber);
                    break;
                case IN_MULTILINE_COMMENT:
                    if (!line.contains("*/"))
                        return tokens;
                    i = line.startsWith("*/") ? 2 : line.indexOf("*/") + 1;
                    if (i > 0) {
                        clearState();
                        continue;
                    }
                    break;
                case IN_MULTILINE_STRING:
                    if (currentChar != '"' && nextChar != '/') {
                        multiLineString.append(currentChar);
                        multiLineString.append(nextChar);
                    }
                    else if (currentChar == '"' && nextChar == '/') {
                        multiLineString.append('"');
                        tokens.add(new VariableToken(TokenType.STRING, multiLineString.toString(), lineNumber));
                        multiLineString = new StringBuilder(); // reset the value
                        clearState();
                    }
                    else if (currentChar == '"') {
                        multiLineString.append("\\\""); // escape quotes not followed by a slash
                        i++;
                        continue;
                    }
                    i += 2;
                    continue; // increment already done so skip rest of loop iteration
                case DEFAULT:
                    // handle any non-operator characters
                    switch(currentChar) {
                        case '{':
                            tokens.add(new StaticToken(TokenType.LEFT_CB, lineNumber));
                            break;
                        case '}':
                            tokens.add(new StaticToken(TokenType.RIGHT_CB, lineNumber));
                            break;
                        case '(':
                            tokens.add(new StaticToken(TokenType.LEFT_PAREN, lineNumber));
                            break;
                        case ')':
                            tokens.add(new StaticToken(TokenType.RIGHT_PAREN, lineNumber));
                            break;
                        case '[':
                            tokens.add(new StaticToken(TokenType.LEFT_SQB, lineNumber));
                            break;
                        case ']':
                            tokens.add(new StaticToken(TokenType.RIGHT_SQB, lineNumber));
                            break;
                        case ';':
                            tokens.add(new StaticToken(TokenType.SC, lineNumber));
                            break;
                        case ':':
                            tokens.add(new StaticToken(TokenType.COLON, lineNumber));
                            break;
                        case ',':
                            tokens.add(new StaticToken(TokenType.COMMA, lineNumber));
                            break;
                        default:
                            throw new SyntaxError("Unrecognized character '" + currentChar + "'", lineNumber);
                    }
                    break;
            }
            i++;
        }
        if (state == LexerState.IN_MULTILINE_STRING)
            multiLineString.append('\n');
        return tokens;
    }
}
