package com.piedpiper.swerve.semantic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.lexer.VariableToken;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestArrayChecks {
    private static final AbstractSyntaxTree[] asts = {
        // {}
        new AbstractSyntaxTree("ARRAY-LIT"),
        // {5, 10}
        new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "5")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "10"))
        )),
        // {{5, 10}}
        new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "5")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "10"))
            ))
        )),
        // {{{2, 3}, {4}, {}}, {}, {{1}, {2, 3, 4}}}
        new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3"))
                )),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "4"))
                )),
                new AbstractSyntaxTree("ARRAY-LIT")
            )),
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
                )),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "4"))
                ))
            ))
        )),
        // {{}, {{}, {{}}}, {}, {{{{}}}}}
        new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT"),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree("ARRAY-LIT")
                ))
            )),
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree("ARRAY-LIT", List.of(
                        new AbstractSyntaxTree("ARRAY-LIT")
                    ))
                ))
            ))
        ))
    };

    private static Stream<Arguments> estimateArraySizesProvider() {
        AbstractSyntaxTree zero = intToNumberToken(0);
        AbstractSyntaxTree one = intToNumberToken(1);
        AbstractSyntaxTree two = intToNumberToken(2);
        AbstractSyntaxTree three = intToNumberToken(3);
        return Stream.of(
            // {}
            Arguments.of(asts[0], List.of(zero)),
            // {5, 10}
            Arguments.of(asts[1], List.of(two)),
            // {{5, 10}}
            Arguments.of(asts[2], List.of(one, two)),
            // {{{2, 3}, {4}, {}}, {}, {{1}, {2, 3, 4}}}
            Arguments.of(asts[3], List.of(three, three, three)),
            // {{}, {{}, {{}}}, {}, {{{{}}}}}
            Arguments.of(asts[4], List.of(intToNumberToken(4), two, one, one, zero))
        );
    }

    @ParameterizedTest
    @MethodSource("estimateArraySizesProvider")
    void test_estimateArraySizes(AbstractSyntaxTree array, List<AbstractSyntaxTree> expectedResults) {
        List<AbstractSyntaxTree> sizes = ArrayChecks.estimateArraySizes(array);
        assertEquals(expectedResults, sizes);
    }

    private static AbstractSyntaxTree intToNumberToken(int value) {
        return new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, String.valueOf(value)));
    }
}