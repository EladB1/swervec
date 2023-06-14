package com.piedpiper.bolt;

import com.piedpiper.bolt.error.ReferenceError;
import com.piedpiper.bolt.error.SourceCodeError;
import com.piedpiper.bolt.error.SyntaxError;
import com.piedpiper.bolt.error.TypeError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class TestApp {
    private void assertError(String[] args, Class<? extends SourceCodeError> errorClass, String message, int lineNumber) {
        Throwable error = assertThrows(errorClass, () -> App.main(args));
        assertEquals("Line " + lineNumber + "\n\t" + message, error.getMessage());
    }

    @Test
    void testValidProgram() {
        String[] args = new String[]{"examples/valid.bolt"};
        assertDoesNotThrow(() -> App.main(args));
    }

    @Test
    void testInvalidProgram() {
        String[] args = new String[]{"examples/invalid.bolt"};
        assertError(args, SyntaxError.class, "Found invalid number '0..5'", 16);
    }

    @Test
    void test_loopScope() {
        String[] args = new String[]{"src/integration/resources/invalid/loop_scope.bolt"};
        assertError(args, ReferenceError.class, "Variable 'y' used before being defined in current scope", 10);
    }
}
