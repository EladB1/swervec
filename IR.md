# Intermediate Representation (IR)

### Goals
    - Go from AST to IR to make backend of compiler easier to work with
    - Ease translation from source code to stack operations used in VM bytecode
    - Allow for optimizations
    - Relatively easy to generate from AST and SymbolTable information

---
 
### Existing representations

The source code is parsed into an Abstract Syntax Tree (AST) then to one or more Intermediate Representations (IRs) then eventually to the compiler target, which is VM bytecode

The current representations, without any IR, are written in the subsections below

**Source code**:

```rust
    const int max = 9;
    const int min = -1 * (max + 1);

    fn plus_one(int num): int {
        return num + 1;
    }

    fn main() {
        for (int i = min; i <= max; i = plus_one(i)) {
            println(i);
        }
    }
```

**AST**:

```
            PROGRAM
        /           |               \                   \ 
   VAR-DECL         VAR-DECL         VAR-ASSIGN    <main function>
/     |   |   \   /    |   |   \          |
const int max  9 const int min  *  <plus_one function>
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
        /     /        \     \
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
        LOAD_CONST 9
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

requirements/conventions:

 - functions retain their return types
 - variables retain information like type and const
 - global variables get an explicit `global` added to them
 - global variable declarations and the main body are both added into the entry point function `_entry`
 - `return 0` is automatically added to `_entry` if `main()` does not have a return
 - loops are "unrolled" to be in the simplest, most straightforward form
 - Remove as much *syntactic sugar* as possible without losing critical information about the program
 - overloaded functions in the source/AST have their names changed to more easily differentiate them
 - uninitialized variables are set to some default value in IR (i.e. `null` or 0)

1. TAC + SSA

Source: 
```rust
    const int max = 9;
    const int min = -1 * (max + 1);

    fn plus_one(int num): int {
        return num + 1;
    }

    fn main() {
        for (int i = min; i <= max; i = plus_one(i)) {
            println(i);
        }
    }
```

IR:

```   
   _entry:
      global max-0 = 9
      t0 = max-0 + 1
      global min-0 = -1 * t0
      t1 = call main
      return t1
  
   main:
      i-0 = min-0
      JMP .start_loop_0
      .start_loop_0:
         t3 = i-0 <= max-0
         JMPF t3 .end_loop_0
         param i-0
         call println 1
         param i-0
         i-1 = call plus_one 1
         i-0 = phi(i-0, i-1)
      .end_loop_0:
         return 0
         
   plus_one:
      param num-0
      t2 = num-0 + 1
      return t2
```