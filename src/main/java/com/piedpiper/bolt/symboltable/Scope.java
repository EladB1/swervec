package com.piedpiper.bolt.symboltable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Scope {
    private String name = "local";
    private Boolean isClosed = false;

    public Scope(String name) {
        this.name = name;
    }

    public void leaveScope() {
        isClosed = true;
    }


    /*
    private void test() {
        int j = 5;
        int x;
        for (int i = 0; i < 5; i++) {
            x = i * j;
        }

    }

    scope 0 (built in):
    scope 1 (global):
    scope 2 (class): test
    scope 3 (method): j x
    scope 4 (for): i

    after for loop runs scope4 is closed

    */
}
