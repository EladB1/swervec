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

Control Flow Graph (CFG) in SSA form

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

SSA IR (represented as CFG)

```json
{
    "ProgramIR": {
        "Functions": [
            {"name": "doMath", "blocks": [
                {
                    "label": "default",
                    "instructions": [
                        {"operation": "PARAM", "destination": "num-1", "operands": ["num"]},
                        {"operation": "ADD", "destination": "t0", "operands": ["num-1", "1"]},
                        {"operation": "MUL", "destination": "t1", "operands": ["t0", "globVal-1"]}
                    ],
                    "parents": [],
                    "children": [".if-0", ".join-0"]
                },
                {
                    "label": ".if-0",
                    "instructions": [
                        {"operation": "REM", "destination": "t2", "operands": ["t1", "2"]},
                        {"operation": "EQ", "destination": "t3", "operands": ["t2, 0"]}
                    ],
                    "parents": ["default"],
                    "children": [".then-0"]
                },
                {
                    "label": ".then-0",
                    "instructions": [
                        {"operation": "ADD", "destination": "t4", "operands": ["t1", "1"]}
                    ],
                    "parents": [".if-0", "default"],
                    "children": [".join-0"]
                },
                {
                    "label": ".join-0",
                    "instructions": [
                        {"operation": "PHI", "destination": "t5", "operands": ["t1", "t4"]}
                    ],
                    "parents": [".if-0", ".then-0"],
                    "children": [".return-0"]
                },
                {
                    "label": "return-0",
                    "instructions": [
                        {"operation": "RETURN", "destination": null, "operands": ["t5"]}
                    ],
                    "parents": [".join-0"],
                    "children": []
                }
            ]},
            {"name": "_entry", "blocks": [
                {
                    "label": "globals",
                    "instructions": [
                        {"operation": "STORE", "destination": "PI", "operands": ["3.14"]},
                        {"operation": "STORE", "destination": "globVar-1", "operands": ["3"]}
                    ],
                    "parents": [],
                    "children": ["default"]
                },
                {
                    "label": "default",
                    "instructions": [
                        {"operation": "MUL", "destination": "t0", "operands": ["PI", "PI"]},
                        {"operation": "CALL", "destination": "t1", "operands": ["toInt", "PI"]},
                        {"operation": "CALL", "destination": "t2", "operands": ["doMath", "t1"]},
                        {"operation": "MUL", "destination": "t3", "operands": ["t0", "t2"]},
                        {"operation": "CALL", "destination": null, "operands": ["println", "t3"]},
                        {"operation": "DIV", "destination": "t4", "operands": ["t3", "2"]},
                        {"operation": "CALL", "destination": null, "operands": ["println", "t4"]}
                    ],
                    "parents": ["globals"],
                    "children": ["default"]
                },
                {
                    "label": "ret-0",
                    "instructions": [
                        {"operation": "RETURN", "destination": null, "operands": ["0"]}
                    ],
                    "parents": ["default"],
                    "children": []
                }
            ]}
        ]
    }
}
```

## Source code to IR examples

> default values of uninitialized variables are:</br>
> &nbsp;int = 0</br>
> &nbsp;double = 0</br>
> &nbsp;boolean = false</br>
> &nbsp;string = empty string</br>
> &nbsp;array = null</br>

**Source**: `int y;`</br> 
**IR**: `{"operation": null, "destination": "t0", "operands": ["0"]}`

**Source**: `const int y = 5;`</br>
**IR**: `{"operation": null, "destination": "y-0", "operands": ["5"]}`

**Source**: `2+2`</br>
**IR**: `{"operation: "ADD", "destination": "t0", "operands": ["2", "2"]}`

**Source**: `println("Hello, world!");`</br>
**IR**: `{"operation": "CALL", "destination": null, "operands": ["println", "\"Hello, world!\""]}`

**Source**:

```rust
    int x = -1;
    int y;
    if (x < 0)
        y = -1;
    else
        y = 1;
    int z = y * 2;
```

**IR**:
```
default:
    {"operation": null, "destination": "t0", "operands": ["-1"]},
    {"operation": null, "destination": "t1", "operands": ["0"]},
    {"operation", "goto", "destination": ".if-0", "operands": []}
.if-0:
    {"operation": "LT", "destination": "t2", "operands": ["t0", "0"]},
    {"operation": "goto", "destination": ".then-0": "operands": ["t2"]},
    {"operation": "goto", "destination": ".then-1": "operands": []} 
.then-0:
    {"operation": null, "destination": "t3", "operands": ["-1"]},
    {"operation": "goto", "destination": ".join-0": "operands": []}
.then-1:
    {"operation": null, "destination": "t4", "operands": ["1"]},
    {"operation": "goto", "destination": ".join-0": "operands": []}
.join-0:
    {"operation": "phi", "destination": "t5", "operands": ["t1", "t3", "t4"]},
    {"operation": "MUL, "destination": "t6", "operands": ["t5", "2"]}
```

**Source**:

```rust
    int sum = 0;
    for (int i = 1; i <= 10; i++) {
        sum += i;
    }
    println(sum);
```

**IR**:
```
default:
    {"operation": null, "destination": "t0", "operands": ["0"]},
    {"operation": null, "destination": "t1", "operands" ["1"]},
    {"operation": "goto", "destination": ".loop-cond-0", "operands": []}
.loop-cond-0:
    {"operation": "phi", "destination": "t2", "operands": ["t1", "t6"]}
    {"operation": "LE", "destination": "t3", "operands": ["t2", "10"]}
    {"operation": "goto", "destination": ".loop-body-0", "operands": ["t3"]}
    {"operation": "goto", "destination": ".join-0", "operands": []}
.loop-body-0:
    {"operation": "phi": "destination": "t4", "operands": ["t0", t5"]}
    {"operation": "ADD", "destination": "t5", "operands": ["t4", "t2"]}
    {"operation": "goto", "destination": ".loop-update-0", "operands": []}
.loop-update-0:
    {"operation": "ADD", "destination": "t6", "operands": ["t2", "1"]},
    {"operation": "goto", "destination": ".loop-cond-0", "operands": []}
.join-0:
    {"operation": "phi", "destination": "t7", "operands": ["t0", "t5"]}
    {"operation": "CALL", "destination": null, "operands": ["println", "t7"]}
```