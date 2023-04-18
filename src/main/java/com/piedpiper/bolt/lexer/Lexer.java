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
        if (!input.isEmpty()) {
            for (String line : input) {
                if (!line.isEmpty())
                    tokens.addAll(analyzeLine(line));
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

    private Optional<Token> getReservedWord(StringBuilder identfier) {
        // check if an identifier is a reserved word; return Optional.empty() if not
        String id = identfier.toString();
        if (reservedWords.containsKey(id)) {
            return Optional.of(reservedWords.get(id));
        }
        return Optional.empty();
    }

    private void addIdentifierToken(List<Token> tokens, StringBuilder identifier) {
        String id = identifier.toString();
        if (id.matches(identifierRegex)) {
            Optional<Token> reservedWord = getReservedWord(identifier);
            if (reservedWord.isEmpty()) 
                tokens.add(new Token("ID", id));
            else
                tokens.add(reservedWord.get());
        }
    }

    private void addNumberToken(List<Token> tokens, StringBuilder number) {
        String num = number.toString();
        if (num.matches(numRegex)) 
            tokens.add(new Token("NUMBER", num));
    }

    private void addOperatorToken(List<Token> tokens, StringBuilder operator) {
        String op = operator.toString();
        if (op.matches(operatorRegex))
            tokens.add(new Token("OP", op));
    }


    @SneakyThrows
    public List<Token> analyzeLine(String line) {
        List<Token> lineTokens = new ArrayList<>();
        StringBuilder currentSequence = new StringBuilder();
        char currentChar, nextChar;
        int i = 0;
        int closingPosition;
        while(i < line.length() - 1) {
            currentChar = line.charAt(i);
            nextChar = line.charAt(i+1); // look ahead to the next character
            if (currentChar == '"') {
                // TODO: Handle escape string sequences
                closingPosition = line.substring(i+1).indexOf('"'); // look for the closing quote
                if (closingPosition < 0) // did not find the closing quote
                    throw new SyntaxError("Failed to find closing quote", 0, i);
                for (int j = i; j <= i + closingPosition + 1; j++) {
                    currentSequence.append(line.charAt(j));
                }
                lineTokens.add(new Token("STRING", currentSequence.toString()));
                currentSequence = new StringBuilder();
                i = i + closingPosition + 2;
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
                    if (isNumber(currentChar) || currentChar == '.') {
                        currentSequence.append(currentChar);
                    }
                    if (!isNumber(nextChar) && nextChar != '.') {
                        clearState();
                        addNumberToken(lineTokens, currentSequence);
                        currentSequence = new StringBuilder(); // clear out the sequence
                    }
                    break;
                case IN_OPERATOR:
                    if (isOperator(currentChar)) {
                        currentSequence.append(currentChar);
                    }
                    if (currentSequence.toString().equals("//")) {
                        return lineTokens; // end the line at an inline comment
                    }
                    if (!isOperator(nextChar)) {
                        clearState();
                        addOperatorToken(lineTokens, currentSequence);
                        currentSequence = new StringBuilder();
                    }
                    
                    break;
                case IN_IDENTIFIER:
                    if (isLetter(currentChar) || isNumber(currentChar) || currentChar == '_') {
                        currentSequence.append(currentChar);
                    }
                    if (!isLetter(nextChar) && !isNumber(nextChar) && nextChar != '_') {
                        clearState();
                        addIdentifierToken(lineTokens, currentSequence);
                        currentSequence = new StringBuilder();
                    }
                    break;
            }
            i++;
        }
        // handle last two characters in string matching any of these patterns
        currentChar = line.charAt(line.length() - 1);
        StringBuilder nextSequence = new StringBuilder();
        switch(state) {
            case IN_NUMBER:
                if (isNumber(currentChar)) {
                    currentSequence.append(currentChar);
                    addNumberToken(lineTokens, currentSequence);
                }
                else {
                    nextSequence.append(currentChar);
                    if (isLetter(currentChar) || isOperator(currentChar)) { // only add the current sequence if it's followed by a valid character
                        addNumberToken(lineTokens, currentSequence);
                    }
                    if (isOperator(currentChar))
                        addOperatorToken(lineTokens, nextSequence);
                    else if (isLetter(currentChar))
                        addIdentifierToken(lineTokens, nextSequence);
                }
                break;
            case IN_OPERATOR:
            if (isOperator(currentChar)) {
                currentSequence.append(currentChar);
                addOperatorToken(lineTokens, currentSequence);
            }
            else {
                nextSequence.append(currentChar);
                if (isLetter(currentChar) || isNumber(currentChar)) { // only add the current sequence if it's followed by a valid character
                    addOperatorToken(lineTokens, currentSequence);
                }
                if (isNumber(currentChar))
                    addNumberToken(lineTokens, nextSequence);
                else if (isLetter(currentChar))
                    addIdentifierToken(lineTokens, nextSequence);
            }
                break;
            case IN_IDENTIFIER:
            if (isLetter(currentChar) || isNumber(currentChar) || currentChar == '_') {
                currentSequence.append(currentChar);
                addNumberToken(lineTokens, currentSequence);
            }
            else {
                nextSequence.append(currentChar);
                if (isOperator(currentChar)) { // only add the current sequence if it's followed by a valid character
                    addNumberToken(lineTokens, currentSequence);
                }
                if (isOperator(currentChar))
                    addOperatorToken(lineTokens, nextSequence);
            }
                break;
            case DEFAULT:
                if (isNumber(currentChar)) {
                    addNumberToken(lineTokens, new StringBuilder(currentChar));
                }
                else if (isOperator(currentChar)) {
                    addOperatorToken(lineTokens, new StringBuilder(currentChar));
                }
                else if (isLetter(currentChar)) {
                    addIdentifierToken(lineTokens, new StringBuilder(currentChar));
                }
                break;
        }
            
        return lineTokens;
    }
}
