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


1.
```
int plus_one(int num-0) {
    return 1 num-0 ADD
}

int _entry() {
    // global vars
    global const int max-0 = 9
    global const int min-0 = (1 max-0 ADD) -1 MUL
    // main
    int i-0 = min-0
    jump .loop
    .loop {
        jumpf (max-0 i-0 LE) .end
        println(i-0)
        i-0 = plus_one(i-0)
        jump .loop
    }
    .end {
        return 0
    }
    
}
```

2. SSA form

- example:

```
int plus_one(int num-0) {
    int num-1 = num-0 + 1
    return num-1
}

int _entry() {
    const int max-1 = 9
    const int min-1 = -1 * (max-1 + 1)
    int i-1 = min-1

    goto .loop

    .loop:
        boolean t1 = (i-1 <= max-1)
        if (!t1) goto .end

        call println(i-1)
        int i-2 = call plus_one(i-1)

        goto .loop

    .end:
        return 0
}
```

3. Three Address Code (TAC) form

- example:

```
int plus_one(int num-0) {
    t0 = num-0 + 1
    return t0
}

int _entry() {
    global const int max-0 = 9
    t0 = max-0 + 1
    global const int min-0 = -1 * t0

    int i-0 = min-0
    goto .loop

    .loop:
        t1 = (i-0 <= max-0)
        if (!t1) goto .end
        call println(i-0)
        t2 = call plus_one(i-0)
        i-0 = t2
        goto .loop

    .end:
        return 0
}
```

## Initial approach

Going to try to use SSA form to represent programs

Example:

```rust
const double PI = 3.14;
int globVal = 3;

fn doMath(int num): int {
    int result = num + 1;
    result *= globVal;
    if (result % 2 == 0)
        result++;
    return result;
}

fn main() {
    float y = PI * PI;
    y *= doMath(toInt(PI));
    println(y);
    y /= 2;
    println(y);
}
```

SSA IR (represented as ProgramIR containing functionIRs containing BlockIRs)

```json
ProgramIR: {
    Functions: [
        {name: 'doMath', blocks: [
            {'default': [
                {operation: 'ADD', destination: 'result-0', operands: ['num', '1']},
                {operation: 'MUL', destination: 'result-1', operands: ['result-0', 'globVal']},
                {operation: 'REM', destination: 't0', operands: ['result-1', '2']},
                {operation: 'EQ', destination: 't1', operands: ['t0, 0']},
                {operation: 'GOTO', destination: '.if-0', operands: ['t1', 'true']},
                {operation: 'GOTO', destination: '.end-if-0', operands: []}
            ]},
            {'.if-0': [
                {operation: 'ADD', destination: 'result-2', operands: ['result-1', '1']},
                {operation: 'GOTO', destination: '.end-if-0', operands: []}
            ]},
            {'.end-if-0': [
                {operation: 'PHI', destination: 'result-3', operation: ['result-1', 'result-2']},
                {operation: 'RETURN', destination: null, operation: ['result-3']}
            ]}
        ]},
        {name: '_entry', blocks: [
            {'globals': [
                {operation: 'STORE', destination: 'PI', operands: ['3.14']},
                {operation: 'STORE', destination: 'globVar', operands: ['3']}
            ]},
            {'default': [
                {operation: 'MUL', destination: 'y-0', operation: ['PI', 'PI']},
                {operation: 'CALL', destination: 't2', operation: ['toInt', 'PI']},
                {operation: 'CALL', destination: 't3', operation: ['doMath', 't2']},
                {operation: 'MUL', destination: 'y-1', operation: ['y-0', 't3']},
                {operation: 'CALL', destination: null, operation: ['println', 'y-1']},
                {operation: 'DIV', destination: 'y-2', operation: ['y-1', '2']},
                {operation: 'CALL', destination: null, operation: ['println', 'y-2']},
                {operation: 'RETURN', destination: null, operation: ['0']}
            ]}
        ]}
    ]
}
```

Target bytecode

```
doMath:
    LOAD_CONST 1
    LOAD 0
    ADD
    STORE
    GLOAD 1
    LOAD 1
    MUL
    STORE 1
    LOAD_CONST 0
    LOAD_CONST 2
    LOAD 1
    REM
    EQ
    JMPT .if-0
    JMP .end
    .if-0:
        LOAD_CONST 1
        LOAD 1
        ADD
        STORE 1
        JMP .end-if-0
    .end-if-0:
        LOAD 1
        RET

_entry:
    LOAD_CONST 3.14
    GLOAD
    LOAD_CONST 3
    GLOAD
    GLOAD 0
    GLOAD 0
    MUL
    STORE
    GLOAD 0
    CALL _toInt_d 1
    CALL doMath
    LOAD 0
    MUL
    STORE 0
    LOAD 0
    CALL println 1
    LOAD_CONST 2
    LOAD 0
    DIV
    STORE 0
    LOAD 0
    CALL println 1
    LOAD_CONST 0
    RET
    HALT
```