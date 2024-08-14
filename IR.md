# Intermediate Representation (IR)

### Goals
    - Go from AST to IR to make backend of compiler easier to work with
    - Ease translation from source code to stack operations used in VM bytecode
    - Allow for optimizations
    - Relatively easy to generate from AST and SymbolTable information

---
 
Source code:

```rust
    fn plus_one(int num): int {
        return num + 1;
    }
```

AST:

```
                      fn
    /               /       \           \
plus_one       params       int         body
                |                        |
               param                    return
               /   \                      |
             int   num                    +
                                        /   \
                                      num    1
``` 

ByteCode:

```
    plus_one:
        LOAD 0
        LOAD_CONST 1
        ADD
        RET
```




