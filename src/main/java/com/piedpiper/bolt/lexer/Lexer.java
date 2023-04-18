package com.piedpiper.bolt.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.piedpiper.error.SyntaxError;

import lombok.SneakyThrows;

public class Lexer {
    private LexerState state = LexerState.DEFAULT;
    private List<Token> tokens;
    private final Map<String, Token> reservedWords = Map.of(
        
    );
    private final String numRegex = "[0-9]+(\\.[0-9]+)?";
    private final String operatorRegex = "(\\+|-|\\*|/|%|\\!|&|\\^|=|:|\\?|\\+\\+|--|\\*\\*|==|&&|\\|\\||\\+=)";
    private final String identifierRegex = "[a-zA-Z]([a-zA-Z0-9_])*";

    public Lexer() {
        tokens = new ArrayList<>();
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
                System.out.println(lineNumber);
                if (!line.isEmpty())
                    tokens.addAll(analyzeLine(line));
                lineNumber++;
            }
        }
        for (Token token : tokens) {
            System.out.println(token);
        }
        return tokens;
    }

    private boolean isNumber(char curr) {
        return String.valueOf(curr).matches("[0-9]");
    }

    private boolean isOperator(char curr) {
        return String.valueOf(curr).matches("(\\+|-|\\*|/|%|!|&|\\^|=|:|\\?)");
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
    private void addNumberToken(StringBuilder number) {
        String num = number.toString();
        System.out.println(num);
        System.out.println(num.matches(numRegex));
        if (num.matches(numRegex)) {
            tokens.add(new Token("NUMBER", num));
        }
        else {
            throw new SyntaxError("Found invalid number '" + num + "'");
        }
    }

    private void addNumberToken(char number) {
        addNumberToken(new StringBuilder().append(number));
    }


    private void addOperatorToken(StringBuilder operator) {
        String op = operator.toString();
        if (op.matches(operatorRegex))
            tokens.add(new Token("OP", op));
    }

    private void addOperatorToken(char operator) {
        addOperatorToken(new StringBuilder().append(operator));
    }

    private void addIdentifierToken(StringBuilder identifier) {
        String id = identifier.toString();
        if (id.matches(identifierRegex)) {
            Optional<Token> reservedWord = getReservedWord(id);
            if (reservedWord.isEmpty()) 
                tokens.add(new Token("ID", id));
            else
                tokens.add(reservedWord.get());
        }
    }

    private void addIdentifierToken(char identifier) {
        // No one character keywords, so no need to check keywords
        String id = String.valueOf(identifier);
        if (id.matches(identifierRegex))
            tokens.add(new Token("ID", id));
    }

    private StringBuilder handleNumberStateChar(char currentChar, char nextChar, StringBuilder sequence) {
        boolean currIsNumber = isNumber(currentChar) || currentChar == '.';
        boolean nextIsNumber = isNumber(nextChar) || nextChar == '.';
        if (currIsNumber) {
            sequence.append(currentChar);
        }
        if (!nextIsNumber) {
            clearState();
            addNumberToken(sequence);
            return new StringBuilder(); // clear out the sequence
        }
        return sequence;
    }

    @SneakyThrows
    private void handleLastCharNumberState(char lastChar, StringBuilder sequence) {        
        if (isNumber(lastChar)) {
            sequence.append(lastChar);
            addNumberToken(sequence);
        }
        if (lastChar == '.')
            throw new SyntaxError("Unterminated floating point number");
        else {
            if (isLetter(lastChar) || isOperator(lastChar)) { // only add the current sequence if it's followed by a valid character
                addNumberToken(sequence);
            }
            if (isOperator(lastChar))
                addOperatorToken(lastChar);
            else if (isLetter(lastChar))
                addIdentifierToken(lastChar);
        }
    }

    private StringBuilder handleOperatorStateChar(char currentChar, char nextChar, StringBuilder sequence) {
        if (isOperator(currentChar)) {
            sequence.append(currentChar);
        }
        if (sequence.toString().equals("//"))
            return sequence;
        if (!isOperator(nextChar)) {
            clearState();
            addOperatorToken(sequence);
            return new StringBuilder();
        }
        return sequence;
    }

    private void handleLastCharOperatorState(char lastChar, StringBuilder sequence) {
        if (isOperator(lastChar)) {
            sequence.append(lastChar);
            addOperatorToken(sequence);
        }
        else {
            if (isLetter(lastChar) || isNumber(lastChar)) { // only add the current sequence if it's followed by a valid character
                addOperatorToken(sequence);
            }
            if (isNumber(lastChar))
                addNumberToken(lastChar);
            else if (isLetter(lastChar))
                addIdentifierToken(lastChar);
        }
    }

    private StringBuilder handleIdentifierStateChar(char currentChar, char nextChar, StringBuilder sequence) {
        if (isLetter(currentChar) || isNumber(currentChar) || currentChar == '_') {
            sequence.append(currentChar);
        }
        if (!isLetter(nextChar) && !isNumber(nextChar) && nextChar != '_') {
            clearState();
            addIdentifierToken(sequence);
            return new StringBuilder();
        }
        return sequence;
    }

    private void handleLastCharIdentifierState(char lastChar, StringBuilder sequence) {
        if (isLetter(lastChar) || isNumber(lastChar) || lastChar == '_') {
            sequence.append(lastChar);
            addIdentifierToken(sequence);
        }
        else {
            if (isOperator(lastChar)) {
                addIdentifierToken(sequence);
                addOperatorToken(lastChar);
            }
        }
    }

    @SneakyThrows
    private int enterStringStateAndMovePosition(String line, int index) {
        // TODO: Handle escape string sequences
        int relativeClosingPosition = line.substring(index + 1).indexOf('"');
        if (relativeClosingPosition < 0) // did not find the closing quote
            throw new SyntaxError("Failed to find closing quote", 0, index);
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
        StringBuilder currentSequence = new StringBuilder();
        char currentChar, nextChar;
        int i = 0;
        while(i < line.length() - 1) {
            currentChar = line.charAt(i);
            nextChar = line.charAt(i+1); // look ahead to the next character
            if (currentChar == '"') {
                i = enterStringStateAndMovePosition(line, i);
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
                    currentSequence = handleNumberStateChar(currentChar, nextChar, currentSequence);
                    break;
                case IN_OPERATOR:
                    currentSequence = handleOperatorStateChar(currentChar, nextChar, currentSequence);
                    if (currentSequence.toString().equals("//")) {
                        return tokens; // end the line at an inline comment
                    }
                    break;
                case IN_IDENTIFIER:
                    currentSequence = handleIdentifierStateChar(currentChar, nextChar, currentSequence);
                    break;
                case DEFAULT:
                    break; // already did the work before the switch
            }
            i++;
        }
        // handle last two characters in string matching any of these patterns
        currentChar = line.charAt(line.length() - 1);
        StringBuilder nextSequence = new StringBuilder();
        switch(state) {
            case IN_NUMBER:
                handleLastCharNumberState(currentChar, currentSequence);
                break;
            case IN_OPERATOR:
                handleLastCharOperatorState(currentChar, currentSequence);
                break;
            case IN_IDENTIFIER:
                handleLastCharIdentifierState(currentChar, nextSequence);
                break;
            case DEFAULT:
                if (isNumber(currentChar)) {
                    addNumberToken(new StringBuilder(currentChar));
                }
                else if (isOperator(currentChar)) {
                    addOperatorToken(new StringBuilder(currentChar));
                }
                else if (isLetter(currentChar)) {
                    addIdentifierToken(new StringBuilder(currentChar));
                }
                break;
        }
        clearState(); // reset the state when finished
        return tokens;
    }
}
