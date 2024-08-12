package com.piedpiper.swerve.semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.piedpiper.swerve.error.ArrayBoundsError;
import com.piedpiper.swerve.lexer.TokenType;
import com.piedpiper.swerve.lexer.VariableToken;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;
import com.piedpiper.swerve.semantic.ArrayChecks;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static Stream<Arguments> getMaxDepthProvider() {
        return Stream.of(
            // {}
            Arguments.of(asts[0], 1),
            // {5, 10}
            Arguments.of(asts[1], 1),
            // {{5, 10}}
            Arguments.of(asts[2], 2),
            // {{{2, 3}, {4}, {}}, {}, {{1}, {2, 3, 4}}}
            Arguments.of(asts[3], 3),
            // {{}, {{}, {{}}}, {}, {{{{}}}}}
            Arguments.of(asts[4], 5)
        );
    }

    @ParameterizedTest
    @MethodSource("getMaxDepthProvider")
    void test_getMaxDepth(AbstractSyntaxTree array, int expectedResult) {
        assertEquals(expectedResult, ArrayChecks.getMaxDepth(array));
    }

    private static Stream<Arguments> getAllArraySizesProvider() {
        return Stream.of(
            // {}
            Arguments.of(asts[0], List.of(List.of(0))),
            // {5, 10}
            Arguments.of(asts[1], List.of(List.of(2))),
            // {{5, 10}}
            Arguments.of(asts[2], List.of(List.of(1), List.of(2))),
            // {{{2, 3}, {4}, {}}, {}, {{1}, {2, 3, 4}}}
            Arguments.of(asts[3], List.of(
                List.of(3),
                List.of(3, 0, 2),
                List.of(2, 1, 0, 1, 3)
            )),
            // {{}, {{}, {{}}}, {}, {{{{}}}}}
            Arguments.of(asts[4], List.of(
                List.of(4),
                List.of(0, 2, 0, 1),
                List.of(0, 1, 1),
                List.of(0, 1),
                List.of(0)
            ))
        );
    }

    @ParameterizedTest
    @MethodSource("getAllArraySizesProvider")
    void test_getAllArrayDepths(AbstractSyntaxTree array, List<List<Integer>> expectedResults) {
        List<List<Integer>> sizes = ArrayChecks.getAllArraySizes(array);
        assertEquals(expectedResults, sizes);
    }

    private static Stream<Arguments> estimateArraySizesProvider() {
        return Stream.of(
            // {}
            Arguments.of(asts[0], List.of(0)),
            // {5, 10}
            Arguments.of(asts[1], List.of(2)),
            // {{5, 10}}
            Arguments.of(asts[2], List.of(1, 2)),
            // {{{2, 3}, {4}, {}}, {}, {{1}, {2, 3, 4}}}
            Arguments.of(asts[3], List.of(3, 3, 3)),
            // {{}, {{}, {{}}}, {}, {{{{}}}}}
            Arguments.of(asts[4], List.of(4, 2, 1, 1, 0))
        );
    }

    @ParameterizedTest
    @MethodSource("estimateArraySizesProvider")
    void test_estimateArraySizes(AbstractSyntaxTree array, List<Integer> expectedResults) {
        List<Integer> sizes = ArrayChecks.estimateArraySizes(array);
        assertEquals(expectedResults, sizes);
    }

    private static Stream<Arguments> checkArraySizeMatchesExpectationNoErrorProvider() {
        return Stream.of(
            // {}
            Arguments.of(asts[0], List.of(1)),
            // {5, 10}
            Arguments.of(asts[1], List.of(4)),
            // {{5, 10}}
            Arguments.of(asts[2], List.of(1, 2)),
            // {{{2, 3}, {4}, {}}, {}, {{1}, {2, 3, 4}}}
            Arguments.of(asts[3], List.of(3, 3, 3)),
            // {{}, {{}, {{}}}, {}, {{{{}}}}}
            Arguments.of(asts[4], List.of(4, 2, 1, 1, 1))
        );
    }

    @ParameterizedTest
    @MethodSource("checkArraySizeMatchesExpectationNoErrorProvider")
    void test_checkArraySizeMatchesExpectation_doesNotThrow(AbstractSyntaxTree array, List<Integer> expectedSize) {
        assertDoesNotThrow(() -> ArrayChecks.checkArraySizeMatchesExpectation(array, expectedSize));
    }

    private static Stream<Arguments> checkArraySizeMatchesExpectationErrorProvider() {
        return Stream.of(
            // {5, 10}
            Arguments.of(asts[1], List.of(1)),
            // {{5, 10}}
            Arguments.of(asts[2], List.of(1, 1)),
            // {{{2, 3}, {4}, {}}, {}, {{1}, {2, 3, 4}}}
            Arguments.of(asts[3], List.of(3, 2, 3)),
            // {{}, {{}, {{}}}, {}, {{{{}}}}}
            Arguments.of(asts[4], List.of(4, 1, 1, 1, 1))
        );
    }

    @ParameterizedTest
    @MethodSource("checkArraySizeMatchesExpectationErrorProvider")
    void test_checkArraySizeMatchesExpectation_throws(AbstractSyntaxTree array, List<Integer> expectedSize) {
        ArrayBoundsError error = assertThrows(ArrayBoundsError.class, () -> ArrayChecks.checkArraySizeMatchesExpectation(array, expectedSize));
        assertEquals("Array value size exceeds declared size", error.getMessage());
    }

    @Test
    void test_checkArraySizeMatchesExpectation_sizesMismatch() {
        AbstractSyntaxTree array = asts[2]; // {{5, 10}}
        List<Integer> expectedSize = List.of(2);
        ArrayBoundsError error = assertThrows(ArrayBoundsError.class, () -> ArrayChecks.checkArraySizeMatchesExpectation(array, expectedSize));
        assertEquals("Array value does not match declaration type", error.getMessage());
    }
}
