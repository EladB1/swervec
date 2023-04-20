package com.piedpiper.bolt.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.Map.entry;

import com.piedpiper.bolt.error.SyntaxError;

import lombok.SneakyThrows;

public class Lexer {
    private LexerState state = LexerState.DEFAULT;
    private List<Token> tokens;
    private final Map<String, Token> reservedWords = Map.ofEntries(
        entry("const", new Token("KW_CONST", "const")),
        entry("int", new Token("KW_INT", "int")),
        entry("float", new Token("KW_FLOAT", "float")),
        entry("string", new Token("KW_STR", "string")),
        entry("boolean", new Token("KW_BOOL", "boolean")),
        entry("fn", new Token("KW_FN", "fn")),
        entry("return", new Token("KW_RET", "return")),
        entry("for", new Token("KW_FOR", "for")),
        entry("if", new Token("KW_IF", "if")),
        entry("else", new Token("KW_ELSE", "else")),
        entry("while", new Token("KW_WHILE", "while")),
        entry("break", new Token("KW_BRK", "break")),
        entry("continue", new Token("KW_CNT", "continue")),
        entry("true", new Token("KW_TRUE", "true")),
        entry("false", new Token("KW_FALSE", "false"))
    );
    private final String numRegex = "[0-9]+(\\.[0-9]+)?";
    private final String operatorRegex = "(\\+|-|\\*|/|%|\\!|&|\\^|=|\\?|\\+\\+|--|\\*\\*|&&|\\|\\||\\+=|<|>|<=|>=|==)";
    private final String identifierRegex = "[a-zA-Z]([a-zA-Z0-9_])*";

    public Lexer() {
        tokens = new ArrayList<>();
    }

    public List<Token> getTokens() {
        return tokens;
    }

    private void setState(final LexerState state) {
        this.state = state;
    }

    private void clearState() {
        setState(LexerState.DEFAULT);
    }

    public List<Token> lex(List<String> input) {
        int lineNumber = 1;
        if (!input.isEmpty()) {
            for (String line : input) {
                if (!line.isEmpty())
                    analyzeLine(line, lineNumber);
                lineNumber++;
            }
        }
        System.out.println("Tokens: " + tokens);
        return tokens;
    }

    private boolean isNumber(char curr) {
        return String.valueOf(curr).matches("[0-9]");
    }

    private boolean isOperator(char curr) {
        return String.valueOf(curr).matches("(\\+|-|\\*|/|%|!|&|\\^|=|\\?|<|>)");
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


    @SneakyThrows
    private void addNumberToken(StringBuilder number, int lineNumber) {
        String num = number.toString();
        if (num.matches(numRegex)) {
            tokens.add(new Token("NUMBER", num, lineNumber));
        }
        else {
            throw new SyntaxError("Found invalid number '" + num + "'", lineNumber);
        }
    }

    private void addOperatorToken(StringBuilder operator, int lineNumber) {
        String op = operator.toString();
        if (op.matches(operatorRegex))
            tokens.add(new Token("OP", op, lineNumber));
    }

    private void addIdentifierToken(StringBuilder identifier, int lineNumber) {
        String id = identifier.toString();
        if (id.matches(identifierRegex)) {
            Optional<Token> reservedWord = getReservedWord(id);
            if (reservedWord.isEmpty()) 
                tokens.add(new Token("ID", id, lineNumber));
            else {
                Token keyword = reservedWord.get();
                if (lineNumber != 0)
                    keyword.setLineNumber(lineNumber);
                tokens.add(keyword);
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

    @SneakyThrows
    private int enterStringStateAndMovePosition(String line, int index, int lineNumber) {
        // TODO: Handle escape string sequences
        int relativeClosingPosition = line.substring(index + 1).indexOf('"');
        if (relativeClosingPosition < 0) // did not find the closing quote
            throw new SyntaxError("EOL while scanning string literal", lineNumber);
        int absoluteClosingPosition = index + relativeClosingPosition + 1;
        StringBuilder sequence = new StringBuilder();

        for (int i = index; i <= absoluteClosingPosition; i++) {
            sequence.append(line.charAt(i));
        }
        tokens.add(new Token("STRING", sequence.toString()));
        return absoluteClosingPosition + 1; // Move to the position after the closing quotation mark
    }

    @SneakyThrows
    public List<Token> analyzeLine(String line) {
        return analyzeLine(line, 0);
    }

    @SneakyThrows
    public List<Token> analyzeLine(String line, int lineNumber) {
        StringBuilder currentSequence = new StringBuilder();
        char currentChar, nextChar;
        int i = 0;
        while(i < line.length()) {
            currentChar = line.charAt(i);
            nextChar = i == line.length() - 1 ? '\0' : line.charAt(i+1); // look ahead to the next character and handle edge case if at the end
            if (currentChar == '"') {
                i = enterStringStateAndMovePosition(line, i, lineNumber);
                continue;
            }
            if (isWhitespace(currentChar)) {
                i++;
                continue;
            }
            if (state == LexerState.DEFAULT) { // this needs to be outside the switch statement so it can set the state
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
                        return tokens; // end the line at an inline comment
                    }
                    break;
                case IN_IDENTIFIER:
                    currentSequence = handleIdentifierStateChar(currentChar, nextChar, currentSequence, lineNumber);
                    break;
                case DEFAULT:
                    // handle any non-operator characters
                    switch(currentChar) {
                        case '{':
                            tokens.add(new Token("LEFT_CB", String.valueOf(currentChar)));
                            break;
                        case '}':
                            tokens.add(new Token("RIGHT_CB", String.valueOf(currentChar)));
                            break;
                        case '(':
                            tokens.add(new Token("LEFT_PAREN", String.valueOf(currentChar)));
                            break;
                        case ')':
                            tokens.add(new Token("RIGHT_PAREN", String.valueOf(currentChar)));
                            break;
                        case ';':
                            tokens.add(new Token("SC", String.valueOf(currentChar)));
                            break;
                        case ':':
                            tokens.add(new Token("COLON", String.valueOf(currentChar)));
                            break;
                        case ',':
                            tokens.add(new Token("COMMA", String.valueOf(currentChar)));
                            break;
                        default:
                            throw new SyntaxError("Unrecognized character '" + currentChar + "'", lineNumber);
                    }
                    break;
            }
            i++;
        }
        return tokens;
    }
}
