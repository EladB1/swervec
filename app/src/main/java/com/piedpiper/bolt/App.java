package com.piedpiper.bolt;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.piedpiper.bolt.lexer.Lexer;
import com.piedpiper.bolt.lexer.Token;

public class App {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("A file path is required");
            return;
        }
        Path filePath = Paths.get(args[0]).toAbsolutePath();
        if (Files.exists(filePath)) {
            try {
                List<String> lines = Files.readAllLines(filePath);
                Lexer lexer = new Lexer();
                List<Token> tokens = lexer.lex(lines);
                System.out.println("Number of tokens: " + tokens.size());
            } catch (AccessDeniedException exception) {
                System.out.println("Access to file '" + filePath + "' was denied. Please check permissions.");
            } catch (IOException exception) {
                System.out.println("Failed to open file with error: " + exception.getClass());
                exception.printStackTrace();
            }
        }
        
    }
}
