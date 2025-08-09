package com.piedpiper.swerve;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.piedpiper.swerve.error.CompilerError;
import com.piedpiper.swerve.ir.FunctionBlock;
import com.piedpiper.swerve.ir.IRGenerator;
import com.piedpiper.swerve.lexer.Lexer;
import com.piedpiper.swerve.lexer.Token;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;
import com.piedpiper.swerve.parser.Parser;
import com.piedpiper.swerve.semantic.SemanticAnalyzer;
import com.piedpiper.swerve.symboltable.SymbolTable;

public class Compiler {
    public static final List<String> assignmentOperators = List.of("=", "+=", "-=", "*=", "/=");
    public static final List<String> comparisonOperators = List.of("<", ">", "<=", ">=", "!=", "==");

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("A file path is required");
            System.exit(1);
        }
        Path filePath = Paths.get(args[0]).toAbsolutePath();
        try {
            if (Files.exists(filePath)) {
                    List<String> lines = Files.readAllLines(filePath);
                    if (lines.isEmpty())
                        throw new CompilerError("Cannot compile empty file.");
                    Lexer lexer = new Lexer();
                    List<Token> tokens = lexer.lex(lines);
                    lexer.printTokens();
                    System.out.println("Number of tokens: " + tokens.size());
                    Parser parser = new Parser(tokens);
                    AbstractSyntaxTree ast = parser.parse();
                    System.out.println(ast);
                    SymbolTable symbolTable = new SymbolTable();
                    SemanticAnalyzer sa = new SemanticAnalyzer(symbolTable);
                    sa.analyze(ast);
                    IRGenerator irGenerator = new IRGenerator(symbolTable);
                    List<FunctionBlock> IR = irGenerator.generateIR(ast);
                    for (FunctionBlock block : IR) {
                        System.out.println(block);
                    }
            }
            else {
                throw new FileNotFoundException("Could not find file '" + filePath + "'");
            }
        } catch (AccessDeniedException exception) {
            System.out.println("Access to file '" + filePath + "' was denied. Please check permissions.");
            System.exit(2);
        } catch (FileNotFoundException exception) {
            System.out.println(exception.getMessage());
            System.exit(2);
        } catch (IOException exception) {
            System.out.println("Failed to open file with error: " + exception.getClass());
            exception.printStackTrace();
            System.exit(2);
        } 
    }
}
