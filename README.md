# boltc: The Bolt compiler

> Since this project is a work in progress, a lot of the stuff in here is subject to change and can become outdated

## About this project
This is a simple compiler for a made up programming language, __Bolt__.

The compiler phases are planned to be:

    1. Lexical Analysis
    2. Parsing
    3. Semantic Analysis
    4. IR generation
    5. Optimization
    6. Code generation

Although the phases can, and most likely will, change as the project goes on.

The goal is to create a somewhat practical language that is statically typed and is influenced by syntactic features of other languages.

> Memory management is TBD

__Parsing Strategy__: Recursive Descent, LL(1)

The parser produces an AST. 

Examples of the AST structure can be found [here](ast-examples.md).

## Testing strategy

This project has both unit and integration tests.

Unit tests will be used to test simple cases, and integration tests will be used to handle more complex ones (such as testing variable scope)

There are no coverage requirements, but the objective is to cover enough edge cases that someone would be confident different parts of the compiler will work

#### Testing commands

 - To run unit tests only: `./gradlew test`
 - To run integration tests only: `./gradlew integration`
 - To run both: `./gradlew check`

## Language specification

Statements in the language must end with a semicolon

Statements are:
  - control flow
    - break
    - continue
    - return
  - variable declaration
  - variable assignment
  - arithmetic and logic
  - literal values
  - array indexes
  - etc.

### Reserved words:
 - `const`
 - `mut`
 - `prototype`
 - `generic`
 - `int`
 - `float`
 - `boolean`
 - `string`
 - `Array`
 - `fn`
 - `return`
 - `if`
 - `else`
 - `for`
 - `while`
 - `continue`
 - `break`
 - `true`
 - `false`
 - `null`

### Operators:
 - `=`
 - `==`
 - `!=`
 - `<`
 - `<=`
 - `>=`
 - `>`
 - `+`
 - `+=`
 - `-`
 - `-=`
 - `*`
 - `*=`
 - `**`
 - `/`
 - `/=`
 - `%`
 - `&&`
 - `||`
 - `++`
 - `--`

### Comments
Inline comments can be made with `//` which will cause the compiler to ignore the rest of the line (unless `//` is in a string)

Multiline comments can be made as starting with `/*` and ending with `*/`. Unterminated multiline comments are a syntax error.

### Type System
Bolt is intended to be a statically, strongly typed language.
Variable types and function return types must be explicitly declared. 

Basic types include
  - int
  - float
  - string
  - boolean

Array types are dependent on the depth (level of nesting) and the type of data stored in the array.
For example, `Array<Array<int>>` is used to store an array of arrays of ints.

Typecasting is only allowed via builtin functions
  - `toString()`
  - `toInt()`
  - `toFloat()`

#### Operations on types

Binary operators
---

- Addition
  - int, int => int
  - float, float => float
  - int, float => float
  - float, int => float
  - string, string => string
  - Array, Array => Array (must have matching types)
- Multiplication
  - int, int => int
  - float, float => float
  - int, float => float
  - float, int => float
  - string, int => string
  - int, string => string
- Subtraction / Division / Modulus / Exponent
  - int, int => int
  - float, float => float
  - int, float => float
  - float, int => float
- Bitwise (`^` XOR, `&` AND)
  - int, int => int
  - boolean, boolean => int
  - int, boolean => int
  - boolean, int => int
- Comparison (`<`, `<=`, `>`, `>=`)
  - int, int => boolean
  - float, float => boolean
  - int, float => boolean
  - float, int => boolean
- Equality (`==`, `!=`)
  - two operands with same type => boolean
  - anything, null => boolean
  - null, anything => boolean
  - int, float => boolean
  - float, int => boolean
- Logical operators (`&&`, `||`)
  - boolean, boolean => boolean

Unary operators
---
  - `!`
    - boolean => boolean
  - `++`, `--` (either side)
    - int => int
    - float => float
  - `-` (minus)
    - int => int
    - float => float

Other type requirements
---

- ternary: boolean `?` anything `:` anything
- conditional blocks

```c
    if (boolean) {
      // ...
    }
    else if (boolean) {
      // ...
    }
```

 - while loops: `while (boolean) {}`
 - for loops
   - `for (type element : container)`
     - container must be either a string or array
     - the type of element must be either a string if container is a string
     - or the matching type of the container elements
   - `for (int i = 0; i < value; i++)`
     - the first part of the loop must be a variable declaration or assignment
     - the second part must be a boolean
     - the third part must match the type of the first part
 - array index
   - The variable being indexed must be an array (containing any type)
   - the index value itself must be an int
   - cannot index greater than the array depth
 - void functions
   - can have implicit or explicit `return`
   - cannot return any value (including `null`) during explicit return
 - non-void functions
   - must have explicit `return` that returns a value matching the declared return type
   - all branches (conditionals, loops, etc.) must lead to return 

#### Generics

In order to make the type system slightly more flexible, the `generic` keyword was introduced.
Generic arrays are arrays made of up of generic values (i.e. `Array<generic>`).
Generic variables and arrays can only be used in a function definition.

A generic function AKA prototype is any function with one or more generic parameters (generic variable/array). 
A generic function gets its proper types on function call. 
Generic functions can return `generic` or `Array<generic>`, but you should consider 
what your function is returning.

> Generics can lead to unsafe and/or buggy code, so you should only use them when needed; the compiler will warn you about using generics

Generic arrays are the main motivation behind generic functions, they allow you to treat an array
the same no matter what kind of data it contains or what depth it's nested at.
This can be necessary for dealing with arrays.

### Variable Declarations
variables declarations must have a type and can optionally be `const`

Examples:
 - `int myVar = 10;`
 - `const float PI = 3.14;`

A valid variable name can contain uppercase letters, lowercase letters, numbers, and underscores, but must start with an uppercase or lowercase letter.

Once a variable is declared, you can reassign it (unless it's a `const` variable). Re-declaring, by specifying a type before it, is an error unless it's part of a loop condition.

#### Arrays
Arrays can be declared using the `Array` keyword and must specify their types by wrapping the type in `<` and `>`. There are a few different ways to declare arrays that change their behavior.

1. `const` arrays
 - The variable the array is set to cannot be reassigned and the array is immutable.
 - Since the array cannot change, the size is optional for these
 - Examples:
   - `const Array<int> fib = {1, 1, 2, 3, 5, 8};`
   - `const Array<string> items[2] = {"hammer", "nail"};`
2. `const mut` arrays
  - The variable the array is set to cannot be reassigned, but the array is immutable
  - Example: `const mut Array<float> constants[3] = {3.14, 9.8, 6.67 * 10 ** -11};`
3. regular arrays
  - These are just arrays declared without `const`
  - You can optionally declare them with the keyword `mut`, but that is unnecessary
  - The variable the array is set to can be reassigned and the array is mutable
  - Examples:
    - `Array<int> scores[1] = {98};`
    - `mut Array<string> values[4] = {"v1", "v2", "v3", "v4"};`

#### Strings
Regular strings, like in many other languages, must be wrapped in double quotes and can not contain any nested double quotes unless those are escaped

Examples:
 - `string animal = "elephant"; // valid string`
 - `string JSON = "{\"key\": \"value\"}"; // valid string`
 - `JSON = "{"key": "value"}"; // invalid string`

Another way to declare strings is to use multiline strings. The multiline strings in Bolt were inspired by the syntax of multiline comments, so they operate in a similar way. You can start multiline string with `/"` and terminate them with `"/`. Multiline strings allow you to nest quotes without escaping them, and they preserve any whitespace characters such as tabs and newlines. Unterminated multiline strings are a syntax error!

Examples:
- ```
    print(/"hi there"/); // valid string
  ```

- ```
    string rows = /"
        row1,
        row2,
        ...
        rowN
    "/; // valid string
  ```

- ```
    string response = /"{
        "key": "value"
    }"/; // valid string
  ```

- ```
    // invalid string
    string incomplete = /"
        hello
    // Reached EOF without terminating string
  ```

### Function declarations

Functions are declared with the `fn` keyword followed by the name, and then parameters enclosed in parenthesis. A function body starts with an opening curly brace and must be terminated by a closing curly brace. 

Functions with that are not void must specify the return type by adding `:` after the closing parenthesis of the parameters and then the type. Void functions can skip the `:` and type. 

All parameters must be valid variables and have a type to the left of them.

Examples:
- ```
    fn multiply(int x, int y) : int {
        return x * y;
    }
  ```

- ```
    fn voidFunction() {
        return;
    }
  ```

Function names can be reused only with different parameters.


  Valid Example:

  - ```
      fn toString(int value): string {
        // ...
      }

      fn toString(float value): string {
        // ...
      }
    ```

Invalid Example:

 - ```
      fn toString(int value): string {
        // ...
      }

      fn toString(int value): string {
        // ...
      }
    ```

The order of function declarations matter so if you have a function calling another one, the caller must be defined after the called function