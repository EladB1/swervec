package com.piedpiper.bolt.semantic;

import com.piedpiper.bolt.error.IllegalStatementError;
import com.piedpiper.bolt.error.NameError;
import com.piedpiper.bolt.error.ReferenceError;
import com.piedpiper.bolt.error.TypeError;
import com.piedpiper.bolt.error.UnreachableCodeError;
import com.piedpiper.bolt.lexer.StaticToken;
import com.piedpiper.bolt.lexer.Token;
import com.piedpiper.bolt.lexer.TokenType;
import com.piedpiper.bolt.lexer.VariableToken;
import com.piedpiper.bolt.parser.AbstractSyntaxTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSemanticAnalyzer {
    private final SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
    private final Token intTypeToken = new StaticToken(TokenType.KW_INT);
    private final Token varToken = new VariableToken(TokenType.ID, "var");

    private AbstractSyntaxTree createASTOfMainBody(AbstractSyntaxTree... nodes) {
        return new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main")),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(nodes))
            ))
        ));
    }

    private AbstractSyntaxTree createASTOfFunctionDefinitionAndMainBody(AbstractSyntaxTree functionDef, AbstractSyntaxTree... mainBody) {
        return new AbstractSyntaxTree("PROGRAM", List.of(
            functionDef,
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main")),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(mainBody))
            ))
        ));
    }

    private AbstractSyntaxTree createASTOfFunctionDefinition(AbstractSyntaxTree functionDef) {
        return new AbstractSyntaxTree("PROGRAM", List.of(
            functionDef,
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main"))
            ))
        ));
    }

    private AbstractSyntaxTree createASTWithEmptyMain(AbstractSyntaxTree... nodes) {
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(nodes));
        source.appendChildren(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main"))
            ))
        );
        return source;
    }

    /**
     * Source Code:
     *  int var;
     */
    @Test
    void test_variableDeclaration_noValue() {
        AbstractSyntaxTree AST = createASTOfMainBody(new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    /**
     * Source Code:
     *  int var = 5;
     */
    @Test
    void test_variableDeclaration_withValue() {
        AbstractSyntaxTree AST = createASTOfMainBody(new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken, new VariableToken(TokenType.NUMBER, "5")));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    /**
     * Source Code:
     *  int var = "5";
     */
    @Test
    void test_variableDeclaration_withWrongTypeValue() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken, new VariableToken(TokenType.STRING, "\"5\""))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Right hand side of variable is STRING but INT expected", error.getMessage());
    }

    /**
     * Source Code:
     *  int var;
     *  int var;
     */
    @Test
    void test_variableDeclaration_sameNameSameScope() {
        AbstractSyntaxTree varDeclaration = new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken);
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(varDeclaration, varDeclaration));
        NameError error = assertThrows(NameError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Symbol 'var' is already defined in this scope", error.getMessage());
    }

    /**
     * Source Code:
     *  int var;
     */
    @Test
    void test_missingMain() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken)));
        ReferenceError error = assertThrows(ReferenceError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Could not find entry point function 'main()' or 'main(int, Array<string>)'", error.getMessage());
    }

    /**
     * Source Code:
     *  fn main() {}
     *  fn main(int argc, Array<string> argv) {}
     */
    @Test
    void test_multipleMains() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main"))
            )),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main")),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", new StaticToken(TokenType.KW_INT), new VariableToken(TokenType.ID, "argc")),
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_STR)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "argv"))
                    ))
                ))
            ))
        ));
        ReferenceError error = assertThrows(ReferenceError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Multiple entry point functions 'main' found. Could not resolve proper entry point.", error.getMessage());
    }

    /**
     * Source Code:
     *  fn main(int argc, Array<string> argv):int {
     *      return 0;
     *  }
     */
    @Test
    void test_main_full() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main")),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", new StaticToken(TokenType.KW_INT), new VariableToken(TokenType.ID, "argc")),
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_STR)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "argv"))
                    ))
                )),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", new StaticToken(TokenType.KW_RET), new VariableToken(TokenType.NUMBER, "0"))
                ))
            ))
        ));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    /**
     * Source Code:
     *  fn main(): boolean {
     *      return true;
     *  }
     */
    @Test
    void test_wrongReturnTypeMain() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "main")),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_BOOL)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", new StaticToken(TokenType.KW_RET), new StaticToken(TokenType.KW_TRUE))
                ))
            ))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Entry point function 'main' must return INT or not return at all", error.getMessage());
    }

    /**
     * Source:
     *  int var;
     *  fn main() {}
     */
    @Test
    void test_globalVariableDeclaration() {
        AbstractSyntaxTree AST = createASTWithEmptyMain(new AbstractSyntaxTree("VAR-DECL", intTypeToken, varToken));
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    /**
     * Source:
     *  const Array<int> var[1] = {1};
     *  fn main() {}
     */
    @Test
    void test_globalArrayDeclaration() {
        Token numToken = new VariableToken(TokenType.NUMBER, "1");
        AbstractSyntaxTree AST = createASTWithEmptyMain(
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), intTypeToken),
                new AbstractSyntaxTree(varToken),
                new AbstractSyntaxTree("ARRAY-INDEX", numToken),
                new AbstractSyntaxTree("ARRAY-LIT", numToken)
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(AST));
    }

    /**
     * Source:
     *  while (true) {}
     *  fn main() {}
     */
    @Test
    void test_while_outsideOfFunction() {
        AbstractSyntaxTree AST = createASTWithEmptyMain(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_WHILE), new StaticToken(TokenType.KW_TRUE))
        );
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Outside of a function body, can only declare variables", error.getMessage());
    }

    /**
     * Source:
     *  for (int i = 0; i < 2; i++) {}
     *  fn main() {}
     */
    @Test
    void test_for_outsideOfFunction() {
        Token iterator = new VariableToken(TokenType.ID, "i");
        AbstractSyntaxTree AST = createASTWithEmptyMain(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FOR), List.of(
                new AbstractSyntaxTree("VAR-DECL", intTypeToken, iterator, new VariableToken(TokenType.NUMBER, "0")),
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<"), iterator, new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree("UNARY-OP", iterator, new VariableToken(TokenType.OP, "++"))
            ))
        );
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Outside of a function body, can only declare variables", error.getMessage());
    }

    /**
     * Source:
     *  if (4 < 4)
     *      4;
     *  fn main() {}
     */
    @Test
    void test_conditional_outsideOfFunction() {
        Token num = new VariableToken(TokenType.NUMBER, "4");
        AbstractSyntaxTree AST = createASTWithEmptyMain(
            new AbstractSyntaxTree("COND", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_IF), List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<"), num, num),
                    new AbstractSyntaxTree("BLOCK-BODY", num)
                ))
            ))
        );
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Outside of a function body, can only declare variables", error.getMessage());
    }

    /**
     * Source:
     *  4 < 4
     *  fn main() {}
     */
    @Test
    void test_expression_outsideOfFunction() {
        Token num = new VariableToken(TokenType.NUMBER, "4");
        AbstractSyntaxTree AST = createASTWithEmptyMain(new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<"), num, num));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Outside of a function body, can only declare variables", error.getMessage());
    }

    /**
     * Source:
     *  return 0;
     *  fn main() {}
     */
    @Test
    void test_return_outsideOfFunction() {
        AbstractSyntaxTree AST = createASTWithEmptyMain(new AbstractSyntaxTree("CONTROL-FLOW", new StaticToken(TokenType.KW_RET), new VariableToken(TokenType.NUMBER, "0")));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(AST));
        assertEquals("Cannot return outside of a function", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "2.0"})
    void test_evaluateType_nonEqualityComparison_twoSameTypes(String num) {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, num));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<="), List.of(
            numNode,
            numNode
        ));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.BOOLEAN));
    }

    @ParameterizedTest
    @CsvSource({"2, 2.0", "2.0, 2"})
    void test_evaluateType_nonEqualityComparison_mixedTypes(String value1, String value2) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "<="), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, value1)),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, value2))
        ));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.BOOLEAN));
    }

    /**
     * Source Code:
     *  2 > "2";
     */
    @Test
    void test_evaluateType_nonEqualityComparison_wrongTypes() {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, ">"), List.of(
            numNode,
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"2\""))
        ));

        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Cannot compare INT with STRING using >", error.getMessage());
    }

    private static Stream<Arguments> bitwiseProvider() {
        AbstractSyntaxTree num = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree bool = new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE));
        return Stream.of(
            Arguments.of(num, num),
            Arguments.of(bool, bool),
            Arguments.of(num, bool),
            Arguments.of(bool, num)
        );
    }
    @ParameterizedTest
    @MethodSource("bitwiseProvider")
    void test_evaluateType_bitwise_simple(AbstractSyntaxTree left, AbstractSyntaxTree right) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "^"), List.of(left, right));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.INT));
    }

    /**
     * Source Code:
     *  2 ^ (2 < 2)
     */
    @Test
    void test_evaluateType_bitwise_complex() {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "^"), List.of(
            numNode,
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, ">"), List.of(
                numNode,
                numNode
            ))
        ));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(NodeType.INT));
    }

    /**
     * Source code:
     *  false & "false"
     */
    @Test
    void test_evaluateType_bitwise_invalid() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "&"), List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FALSE)),
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"false\""))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Binary expression (&) with BOOLEAN and STRING is not valid", error.getMessage());
    }

    private static Stream<Arguments> multiplicationOperatorProvider() {
        AbstractSyntaxTree intNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree doubleNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2.78"));
        AbstractSyntaxTree stringNode = new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"a\""));
        return Stream.of(
            Arguments.of(intNode, intNode, NodeType.INT),
            Arguments.of(doubleNode, doubleNode, NodeType.DOUBLE),
            Arguments.of(intNode, doubleNode, NodeType.DOUBLE),
            Arguments.of(doubleNode, intNode, NodeType.DOUBLE),
            Arguments.of(stringNode, intNode, NodeType.STRING),
            Arguments.of(intNode, stringNode, NodeType.STRING)
        );
    }
    @ParameterizedTest
    @MethodSource("multiplicationOperatorProvider")
    void test_evaluateType_multiplicationOperator_simple(AbstractSyntaxTree left, AbstractSyntaxTree right, NodeType expectedType) {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(left, right));
        assertTrue(semanticAnalyzer.evaluateType(AST).isType(expectedType));
    }

    /**
     * Source code:
     *  3.14 * "a"
     */
    @Test
    void test_evaluateType_multiplicationOperator_wrongMix() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3.14")),
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"a\""))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Cannot multiply DOUBLE with STRING", error.getMessage());
    }

    /**
     * Source code:
     *  null > 2 * 2
     */
    @Test
    void test_evaluateType_multiplicationOperator_completelyWrongTypes() {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree(new VariableToken(TokenType.OP, "*"), List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, ">"), List.of(
                numNode,
                numNode
            ))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.evaluateType(AST));
        assertEquals("Cannot multiply NULL with BOOLEAN", error.getMessage());
    }

    @Test
    void test_estimateArrayTypes_emptyArray() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT");
        assertTrue(semanticAnalyzer.estimateArrayTypes(AST).isType(NodeType.ARRAY));
    }

    @Test
    void test_estimateArrayTypes_simpleIntArray() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.INT), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleArrayContainsNull() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.INT), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleArrayContainsExpression() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL)),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "%"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2.78")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            )),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "3.14"))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.DOUBLE), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_nestedEmptyArrays() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT")
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.ARRAY), semanticAnalyzer.estimateArrayTypes(AST));
    }

    /**
     * Source Code:
     *  { { {"Typescript"}, {"C", "C++"} }, { {"PostgreSQL", "MongoDB"} } }
     */
    @Test
    void test_estimateArrayTypes_nestedArrays() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"Typescript\""))
                )),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"C\"")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"C++\""))
                ))
            )),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"PostgreSQL\"")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"MongoDB\""))
                ))
            ))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.ARRAY, NodeType.ARRAY, NodeType.STRING), semanticAnalyzer.estimateArrayTypes(AST));
    }

    /**
     * Source Code:
     *  { {}, { {true, true}, {} }, { {}, {true} } }
     */
    @Test
    void test_estimateArrayTypes_mixedDepths() {
        AbstractSyntaxTree trueNode = new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE));
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree("ARRAY-LIT"),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT", List.of(
                    trueNode,
                    trueNode
                )),
                new AbstractSyntaxTree("ARRAY-LIT")
            )),
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                new AbstractSyntaxTree("ARRAY-LIT"),
                new AbstractSyntaxTree("ARRAY-LIT", List.of(trueNode))
            ))
        ));
        assertEquals(new EntityType(NodeType.ARRAY, NodeType.ARRAY, NodeType.ARRAY, NodeType.BOOLEAN), semanticAnalyzer.estimateArrayTypes(AST));
    }

    @Test
    void test_estimateArrayTypes_simpleArrayError() {
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1")),
            new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"2\"")),
            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.estimateArrayTypes(AST));
        assertEquals(error.getMessage(), "Cannot mix INT elements with STRING elements in array literal");
    }

    /**
     * Source Code:
     *  {1, {1, 1}, 1}
     */
    @Test
    void test_estimateArrayTypes_improperNestingError() {
        AbstractSyntaxTree numNode = new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"));
        AbstractSyntaxTree AST = new AbstractSyntaxTree("ARRAY-LIT", List.of(
            numNode,
            new AbstractSyntaxTree("ARRAY-LIT", List.of(
                numNode,
                numNode
            )),
            numNode
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.estimateArrayTypes(AST));
        assertEquals(error.getMessage(), "Cannot mix non-array elements with nested array elements in array literal");
    }

    /**
     * handleArrayDeclaration -> case 2
     * Source code:
     *  Array<int> a;
     */
    @Test
    void test_handleArrayDeclaration_mutable_missingSizeAndValue() {
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Non-constant array 'a' missing size", error.getMessage());
    }

    /**
     * handleArrayDeclaration -> case 3
     * Source code:
     *  Array<int> a = {};
     */
    @Test
    void test_handleArrayDeclaration_mutable_missingSize() {
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a")),
                new AbstractSyntaxTree("ARRAY-LIT")
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Non-constant array 'a' missing size", error.getMessage());
    }

    /**
     * handleArrayDeclaration -> case 3
     * Source code:
     *  const Array<int> a;
     */
    @Test
    void test_handleArrayDeclaration_constant_missingSizeAndValue() {
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Constant array 'a' must be set to a value", error.getMessage());
    }

    /**
     * handleArrayDeclaration -> case 3
     * Source code:
     *  Array<int> a[3];
     */
    @Test
    void test_handleArrayDeclaration_mutable_missingValue() {
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "3"))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * handleArrayDeclaration -> case 4
     * Source code:
     *  Array<int> a[3] = {};
     */
    @Test
    void test_handleArrayDeclaration_mutable_full() {
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "3")),
                new AbstractSyntaxTree("ARRAY-LIT")
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * handleArrayDeclaration -> case 4
     * Source code:
     *  const Array<int> a[3];
     */
    @Test
    void test_handleArrayDeclaration_constant_missingValue() {
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "3"))
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Constant array 'a' must be set to a value", error.getMessage());
    }

    /**
     * handleArrayDeclaration -> case 3
     * Source code:
     *  const Array<int> a = {};
     */
    @Test
    void test_handleArrayDeclaration_constant_missingSizeWithValue() {
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a")),
                new AbstractSyntaxTree("ARRAY-LIT")
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * handleArrayDeclaration -> case 4
     * Source code:
     *  const Array<int> a[3] = {};
     */
    @Test
    void test_handleArrayDeclaration_constant_full() {
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "3")),
                new AbstractSyntaxTree("ARRAY-LIT")
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *  const Array<double> f = {9.8, 3.14};
     *  f[0];
     */
    @Test
    void test_arrayIndex_number() {
        Token name = new VariableToken(TokenType.ID, "f");
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_DOUBLE)),
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "9.8"), new VariableToken(TokenType.NUMBER, "3.14"))
            )),
            new AbstractSyntaxTree(name, List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "0"))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     * int i = 0;
     *  const Array<double> f = {9.8, 3.14};
     *  f[i];
     */
    @Test
    void test_arrayIndex_variable() {
        Token name = new VariableToken(TokenType.ID, "i");
        Token arrayName = new VariableToken(TokenType.ID, "f");
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("VAR-DECL", new StaticToken(TokenType.KW_INT), name, new VariableToken(TokenType.NUMBER, "0")),
            new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_DOUBLE)),
                new AbstractSyntaxTree(arrayName),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "9.8"), new VariableToken(TokenType.NUMBER, "3.14"))
            )),
            new AbstractSyntaxTree(arrayName, List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", name)
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *  const Array<double> f = {9.8, 3.14};
     *  f["0"];
     */
    @Test
    void test_arrayIndex_nonNumber() {
        Token name = new VariableToken(TokenType.ID, "f");
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_DOUBLE)),
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "9.8"), new VariableToken(TokenType.NUMBER, "3.14"))
            )),
            new AbstractSyntaxTree(name, List.of(
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.STRING, "\"0\""))
            ))
        );
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Array can only be indexed using int values, not STRING values", error.getMessage());
    }

    /**
     * Source code:
     *  const Array<double> f = {9.8, 3.14};
     *  double g = f[0];
     */
    @Test
    void test_arrayIndex_returnsSubType() {
        Token name = new VariableToken(TokenType.ID, "f");
        AbstractSyntaxTree source = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_CONST)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_DOUBLE)),
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "9.8"), new VariableToken(TokenType.NUMBER, "3.14"))
            )),
            new AbstractSyntaxTree("VAR-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_DOUBLE)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "g")),
                new AbstractSyntaxTree(name, List.of(
                    new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "0"))
                ))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *  { 2 + 2; }
     */
    @Test
    void test_functionReturns_voidNoExplicitReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        assertFalse(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    /**
     * Source code
     *  {
     *      2 + 2;
     *      return;
     *  }
     */
    @Test
    void test_functionReturns_voidExplicitReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            )),
            new AbstractSyntaxTree("CONTROL-FLOW", new StaticToken(TokenType.KW_RET))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        assertTrue(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    /**
     * Source code:
     *  { return null; }
     */
    @Test
    void test_functionReturns_voidExplicitNullReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_NULL))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Cannot return value from void function", error.getMessage());
    }

    /**
     * Source Code:
     *  { return 2 + 2; }
     */
    @Test
    void test_functionReturns_voidReturnTypeError() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
                ))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Cannot return value from void function", error.getMessage());
    }

    /**
     * Source code
     *  {
     *     return;
     *     2 + 2;
     *  }
     */
    @Test
    void test_functionReturns_returnWrongPlace() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET))
            )),
            new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.NONE);
        UnreachableCodeError error = assertThrows(UnreachableCodeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Unreachable statement following return", error.getMessage());
    }

    /**
     * Source code
     *  { return 2 + 2; }
     */
    @Test
    void test_functionReturns_nonVoidProperReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "+"), List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2")),
                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "2"))
                ))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        assertTrue(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    /**
     * Source code:
     *  { return "+"; }
     */
    @Test
    void test_functionReturns_nonVoidReturnTypeError() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                new AbstractSyntaxTree(new VariableToken(TokenType.STRING, "\"+\""))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Expected INT to be returned but got STRING", error.getMessage());
    }

    /**
     * Source code:
     *  { return; }
     */
    @Test
    void test_functionReturns_nonVoidMissingReturnValue() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", List.of(
            new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET))
            ))
        ));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
        assertEquals("Expected return type INT but didn't return a value", error.getMessage());
    }

    /**
     * Source code:
     *  { true; }
     */
    @Test
    void test_functionReturns_nonVoidMissingReturn() {
        AbstractSyntaxTree functionBody = new AbstractSyntaxTree("BLOCK-BODY", new StaticToken(TokenType.KW_TRUE));

        EntityType expectedReturnType = new EntityType(NodeType.INT);
        assertFalse(semanticAnalyzer.functionReturns(functionBody, expectedReturnType));
    }

    /**
     * Source code:
     *  {
     *      if (true)
     *          return true;
     *      else
     *          return false;
     *  }
     */
    @Test
    void test_conditionalBlockReturns_simple_true() {
        AbstractSyntaxTree conditionalBlock = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_IF), List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE))
                    ))
                ))
            )),
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_ELSE), List.of(
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_FALSE))
                    ))
                ))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.BOOLEAN);
        assertTrue(semanticAnalyzer.conditionalBlockReturns(conditionalBlock, expectedReturnType));
    }

    /**
     * Source code:
     *  {
     *      if (true)
     *          return true;
     *  }
     */
    @Test
    void test_conditionalBlockReturns_simple_false() {
        AbstractSyntaxTree conditionalBlock = new AbstractSyntaxTree("COND", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_IF), List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_TRUE))
                    ))
                ))
            ))
        ));
        EntityType expectedReturnType = new EntityType(NodeType.BOOLEAN);
        assertFalse(semanticAnalyzer.conditionalBlockReturns(conditionalBlock, expectedReturnType));
    }

    /**
     * Source code:
     *  generic var;
     */
    @Test
    void test_generic_var_outside_prototype() {
        AbstractSyntaxTree declaration = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree("VAR-DECL", new StaticToken(TokenType.KW_GEN), new VariableToken(TokenType.ID, "var"))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(declaration));
        assertEquals("Cannot use generic variable outside of prototype definition", error.getMessage());
    }

    /**
     * Source Code:
     *  Array<int> nums[2] = {2, 4}; 
     *  indexOf(nums, 2);
     */
    @Test
    void test_builtInPrototype_return_fn_call() {
        AbstractSyntaxTree fnCall = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "nums")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.NUMBER, "4"))
            )),
            new AbstractSyntaxTree("FUNC-CALL", List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "indexOf")),
                new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "nums"), new VariableToken(TokenType.NUMBER, "2"))
            ))
        );
        System.out.println(fnCall);
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(fnCall));
    }

    /**
     * Source Code:
     *  Array<int> nums[2] = {2, 4}; 
     *  int num = indexOf(nums, 2);
     */
    @Test
    void test_generic_return_prototype_call_assignment() {
        AbstractSyntaxTree fnCall = createASTOfMainBody(new AbstractSyntaxTree("ARRAY-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "nums")),
                new AbstractSyntaxTree("ARRAY-INDEX", new VariableToken(TokenType.NUMBER, "2")),
                new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "2"), new VariableToken(TokenType.NUMBER, "4"))
            )),
            new AbstractSyntaxTree("VAR-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "num")),
                new AbstractSyntaxTree("FUNC-CALL", List.of(
                    new AbstractSyntaxTree(new VariableToken(TokenType.ID, "indexOf")),
                    new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "nums"), new VariableToken(TokenType.NUMBER, "2"))
                ))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(fnCall));
    }

    /**
     * Source code:
     *   test();
     */
    @Test
    void test_nonDeclaredFunction_call_throwsError() {
        AbstractSyntaxTree fnCall = createASTOfMainBody(new AbstractSyntaxTree("FUNC-CALL", new VariableToken(TokenType.ID, "test")));
        ReferenceError error = assertThrows(ReferenceError.class, () -> semanticAnalyzer.analyze(fnCall));
        assertEquals("Could not find function definition for test([])", error.getMessage());
    }

    /**
     * Source code:
     *   fn test() {}
     *   test();
     */
    @Test
    void test_declareAndCallFunction() {
        Token name = new VariableToken(TokenType.ID, "test");
        AbstractSyntaxTree source = createASTOfFunctionDefinitionAndMainBody(new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), name),
            new AbstractSyntaxTree("FUNC-CALL", name)
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *  fn test(generic var) {}
     */
    @Test
    void test_functionHasGenericParam() {
        Token name = new VariableToken(TokenType.ID, "test");
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_GEN)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "var"))
                    ))
                ))
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Generic parameter found in function definition; generics can only be used in prototype", error.getMessage());
    }

    /**
     * Source code:
     *  fn test(): generic {
     *      return null;
     *  }
     */
    @Test
    void test_functionHasGenericReturn() {
        Token name = new VariableToken(TokenType.ID, "test");
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_GEN)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", new StaticToken(TokenType.KW_RET), new StaticToken(TokenType.KW_NULL))
                ))
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Generic return found in function definition; generics can only be used in prototype", error.getMessage());
    }

    /**
     * Source code:
     *  fn test() {
     *      generic var;
     *  }
     */
    @Test
    void test_functionBodyContainsGeneric() {
        Token name = new VariableToken(TokenType.ID, "test");
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_FN), List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("VAR-DECL", new StaticToken(TokenType.KW_GEN), new VariableToken(TokenType.ID, "var"))
                ))
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Cannot use generic variable outside of prototype definition", error.getMessage());
    }

    /**
     * Source code:
     *  prototype test() {}
     */
    @Test
    void test_prototypeDefinition_withoutParams() {
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), new VariableToken(TokenType.ID, "test"))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Prototype definition must contain at least one generic parameter", error.getMessage());
    }

    /**
     * Source code:
     *  prototype test(int i) {}
     */
    @Test
    void test_prototypeDefinition_withNonGenericParams() {
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), new VariableToken(TokenType.ID, "test")),
            new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                new AbstractSyntaxTree("FUNC-PARAM", new StaticToken(TokenType.KW_INT), new VariableToken(TokenType.ID, "i"))
            ))
        ));
        IllegalStatementError error = assertThrows(IllegalStatementError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Prototype definition must contain at least one generic parameter", error.getMessage());
    }

    /**
     * Source code:
     *  prototype test(generic g) {}
     */
    @Test
    void test_prototypeDefinition_withGenericParam() {
        AbstractSyntaxTree source = createASTOfFunctionDefinition(new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "test")),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", new StaticToken(TokenType.KW_GEN), new VariableToken(TokenType.ID, "g"))
                ))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *  prototype test(Array<generic> a) {}
     */
    @Test
    void test_prototypeDefinition_withGenericArrayParam() {
        AbstractSyntaxTree source = createASTOfFunctionDefinition(new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "test")),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_GEN)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
                    ))
                ))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *  prototype test(Array<generic> a, generic g) {}
     */
    @Test
    void test_prototypeDefinition_withMultipleGenericParams() {
        AbstractSyntaxTree source = createASTOfFunctionDefinition(new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), List.of(
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "test")),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_GEN)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
                    )),
                    new AbstractSyntaxTree("FUNC-PARAM", new StaticToken(TokenType.KW_GEN), new VariableToken(TokenType.ID, "g"))
                ))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *   prototype isEmpty(Array<generic> a): boolean {
     *       return length(a) == 0;
     *   }
     *   isEmpty({});
     */
    @Test
    void test_defineAndCall_prototype_withConcreteReturn() {
        Token name = new VariableToken(TokenType.ID, "isEmpty");
        AbstractSyntaxTree source = createASTOfFunctionDefinitionAndMainBody(new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_GEN)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
                    ))
                )),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_BOOL)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.OP, "=="), List.of(
                            new AbstractSyntaxTree("FUNC-CALL", List.of(
                                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length")),
                                new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "a"))
                            )),
                            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "0"))
                        ))
                    ))
                ))
            )),
            new AbstractSyntaxTree("FUNC-CALL", List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(new AbstractSyntaxTree("ARRAY-LIT")))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *   prototype test(Array<generic> a): generic {
     *       return length(a) - 1;
     *   }
     *   test({5});
     */
    @Test
    void test_defineAndCall_prototype_withGenericReturn_returnsWrongType() {
        Token name = new VariableToken(TokenType.ID, "isEmpty");
        AbstractSyntaxTree source = new AbstractSyntaxTree("PROGRAM", List.of(
            new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_GEN)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
                    ))
                )),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_GEN)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.OP, "-"), List.of(
                            new AbstractSyntaxTree("FUNC-CALL", List.of(
                                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length")),
                                new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "a"))
                            )),
                            new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
                        ))
                    ))
                ))
            )),
            new AbstractSyntaxTree("FUNC-CALL", List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "5"))))
            ))
        ));
        TypeError error = assertThrows(TypeError.class, () -> semanticAnalyzer.analyze(source));
        assertEquals("Expected GENERIC to be returned but got INT", error.getMessage());
    }

    /**
     * Source code:
     *   prototype test(Array<generic> a): generic {
     *       return a[length(a) - 1];
     *   }
     *   test({5});
     */
    @Test
    void test_defineAndCall_prototype_withGenericReturn() {
        Token name = new VariableToken(TokenType.ID, "isEmpty");
        AbstractSyntaxTree source = createASTOfFunctionDefinitionAndMainBody(new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_GEN)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
                    ))
                )),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_GEN)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"), List.of(
                            new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "-"), List.of(
                                    new AbstractSyntaxTree("FUNC-CALL", List.of(
                                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length")),
                                        new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "a"))
                                    )),
                                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
                                ))
                            ))
                        ))
                    ))
                ))
            )),
            new AbstractSyntaxTree("FUNC-CALL", List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "5"))))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }

    /**
     * Source code:
     *   prototype test(Array<generic> a): generic {
     *       return a[length(a) - 1];
     *   }
     *   int val = test({5});
     */
    @Test
    void test_defineAndCall_prototype_translation() {
        Token name = new VariableToken(TokenType.ID, "isEmpty");
        AbstractSyntaxTree source = createASTOfFunctionDefinitionAndMainBody(new AbstractSyntaxTree(new StaticToken(TokenType.KW_PROTO), List.of(
                new AbstractSyntaxTree(name),
                new AbstractSyntaxTree("FUNC-PARAMS", List.of(
                    new AbstractSyntaxTree("FUNC-PARAM", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_ARR), new StaticToken(TokenType.KW_GEN)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"))
                    ))
                )),
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_GEN)),
                new AbstractSyntaxTree("BLOCK-BODY", List.of(
                    new AbstractSyntaxTree("CONTROL-FLOW", List.of(
                        new AbstractSyntaxTree(new StaticToken(TokenType.KW_RET)),
                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "a"), List.of(
                            new AbstractSyntaxTree("ARRAY-INDEX", List.of(
                                new AbstractSyntaxTree(new VariableToken(TokenType.OP, "-"), List.of(
                                    new AbstractSyntaxTree("FUNC-CALL", List.of(
                                        new AbstractSyntaxTree(new VariableToken(TokenType.ID, "length")),
                                        new AbstractSyntaxTree("FUNC-PARAMS", new VariableToken(TokenType.ID, "a"))
                                    )),
                                    new AbstractSyntaxTree(new VariableToken(TokenType.NUMBER, "1"))
                                ))
                            ))
                        ))
                    ))
                ))
            )),
            new AbstractSyntaxTree("VAR-DECL", List.of(
                new AbstractSyntaxTree(new StaticToken(TokenType.KW_INT)),
                new AbstractSyntaxTree(new VariableToken(TokenType.ID, "val")),
                new AbstractSyntaxTree("FUNC-CALL", List.of(
                    new AbstractSyntaxTree(name),
                    new AbstractSyntaxTree("FUNC-PARAMS", List.of(new AbstractSyntaxTree("ARRAY-LIT", new VariableToken(TokenType.NUMBER, "5"))))
                ))
            ))
        );
        assertDoesNotThrow(() -> semanticAnalyzer.analyze(source));
    }
}
