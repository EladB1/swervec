package com.piedpiper.bolt.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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



    public List<Token> analyzeLine(String line) {
        List<Token> lineTokens = new ArrayList<>();
        StringBuilder currentSequence = new StringBuilder();
        char currentChar;
        int i = 0;
        int closingPosition;
        while(i < line.length()) {
            currentChar = line.charAt(i);
            if (currentChar == '"') {
                // TODO: Handle escape string sequences
                closingPosition = line.substring(i+1).indexOf('"'); // look for the closing quote
                if (closingPosition < 0) // did not find the closing quote
                    return lineTokens;
                for (int j = i; j <= i + closingPosition + 1; j++) {
                    currentSequence.append(line.charAt(j));
                }
                lineTokens.add(new Token("STRING", currentSequence.toString()));
                currentSequence = new StringBuilder();
                i = i + closingPosition + 2;
                continue;
            }
            if (currentSequence.toString().equals("//")) {
                return lineTokens; // end the line at an inline comment
            }
            if (state == LexerState.IN_NUMBER) {
                if (isNumber(currentChar) || currentChar == '.') {
                    currentSequence.append(currentChar);
                }
                else {
                    clearState();
                    addNumberToken(lineTokens, currentSequence);
                    currentSequence = new StringBuilder(); // clear out the sequence
                }
            }
            if (isNumber(currentChar) && state != LexerState.IN_NUMBER) {
                setState(LexerState.IN_NUMBER);
                currentSequence.append(currentChar);
            }
            if (state == LexerState.IN_OPERATOR) {
                if (isOperator(currentChar)) {
                    currentSequence.append(currentChar);
                }
                else {
                    clearState();
                    addOperatorToken(lineTokens, currentSequence);
                    currentSequence = new StringBuilder();
                }
            }
            if (state != LexerState.IN_NUMBER && state != LexerState.IN_OPERATOR && isOperator(currentChar)) {
                setState(LexerState.IN_OPERATOR);
                currentSequence.append(currentChar);
            }

            if (state == LexerState.IN_IDENTIFIER) {
                if (isLetter(currentChar) || isNumber(currentChar) || currentChar == '_') {
                    currentSequence.append(currentChar);
                }
                else {
                    clearState();
                    addIdentifierToken(lineTokens, currentSequence);
                    currentSequence = new StringBuilder();
                }
            }

            if (state == LexerState.DEFAULT && isLetter(currentChar)) {
                setState(LexerState.IN_IDENTIFIER);
                currentSequence.append(currentChar);
            }

            
            
            i++;
        }
        // handle last character in string matching any of these patterns
        switch(state) {
            case IN_NUMBER:
                addNumberToken(lineTokens, currentSequence);
                break;
            case IN_OPERATOR:
                addOperatorToken(lineTokens, currentSequence);
                break;
            case IN_IDENTIFIER:
                addIdentifierToken(lineTokens, currentSequence);
                break;
            case DEFAULT:
                break;
        }
        
        return lineTokens;
    }
}
