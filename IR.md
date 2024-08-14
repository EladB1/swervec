# Intermediate Representation (IR)

### Goals
    - Go from AST to IR to make backend of compiler easier to work with
    - Ease translation from source code to stack operations used in VM bytecode
    - Allow for optimizations
    - Relatively easy to generate from AST and SymbolTable information

---
 
### Existing representations

The source code is parsed into an Abstract Syntax Tree (AST) then to one or more Intermediate Representations (IRs) then eventually to the compiler target, which is VM bytecode

The current representations, without any IR, are written in the sub-sections below

**Source code**:

```rust
    const int max = 9;
    const int min = -1 * (max + 1);

    fn plus_one(int num): int {
        return num + 1;
    }

    fn main() {
        for (int i = min; i <= max; plus_one(i)) {
            println(i);
        }
    }
```

**AST**:

```
            PROGRAM
        /           |               \                   \ 
   VAR-DECL         VAR-DECL        <plus_one function> <main function>
/     |   |   \   /    |   |   \
const int max  9 const int min  *
                              /   \
                             -1   +
                                 /  \
                                max  1
```
*plus_one function*: 

```
                      fn
    /              /       \           \
plus_one       params       int         body
                |                        |
               param                    return
               /   \                      |
             int   num                    +
                                        /   \
                                      num    1
```

*main function*:

```
                fn
            /       \
          main      body
                      |
                      for
        /     |    \          \
      =       <       call      body
    /  \    /  \      /    \       \
   i   min i  max plus_one params   call
                             |     /    \
                             i  println  i
```

> AST has been broken up so that it's easier to read

**ByteCode**:

```
    plus_one:
        LOAD 0
        LOAD_CONST 1
        ADD
        RET
    
    _entry:
        LOAD_CONST 10
        GSTORE
        LOAD_CONST 1
        GLOAD_0
        ADD
        LOAD_CONST -1
        MUL
        GSTORE
        GLOAD 1
        STORE
        JMP .loop
        .loop:
            GLOAD 0
            LOAD 0
            LE
            JMPF .end
            LOAD 0
            CALL plus_one 1
            DUP
            STORE 0
            CALL println 1
            JMP .loop
        .end:
            LOAD_CONST 0
            RET
        HALT
```

---

## Possible IRs

> This section is being used to organize thoughts and will be cleaned up once a concrete approach is decided on




