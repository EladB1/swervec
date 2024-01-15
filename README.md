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
 - Manual testing: `./gradlew run --args=<path/to/file>`

#### Testing Objectives
1. Provide proof of correctness for implementation of compiler and language ideas
2. Handle regressions breaking existing code
3. Document compiler and language features/structure through usage
4. Catch bugs as early as possible

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

The order of function declarations matter so if you have a function calling another one, the caller must be defined after the called function.

### Prototypes and generics

There are certain times when the developers will want to pass in Arrays as parameters to functions but don't necessarily care what's in the arrays.

For example, when you want to get the length of an array, it doesn't matter whether the array is of type `Array<string>` or `Array<Array<int>>`.

In order to add flexibility to the type system without completely giving up type safety, two new keywords were introduced, `prototype` and `generic`.

#### Generics

The `generic` keyword is a type label that can be placed in front of a variable during declaration that states that we'll figure out the type later on.

You cannot perform any operations on generic variables that aren't compatible with every type.

Example:
```
  generic var1;
  generic var2;
  // ...assign them values at some point
  var1 + var2;
```

The above example will produce an error because you aren't allowed to do addition with generics.

Generic arrays are declared by using `Array<generic>` which let you treat the variable as an array without caring about what subtype it contains.

When you index a generic array, it will return a generic type. You can't index `Array<generic>` twice, but you can index `Array<Array<generic>>` twice.

#### Prototypes

A prototype is a function definition that contains one or more generic parameters. It can return nothing, a concrete (non-generic) type, or a generic type.

The use of the `generic` keyword outside a prototype definition is not allowed. You cannot initialize a generic to any literal type except for `null`.

When a prototype is called with concrete types, the semantic analyzer will re-analyze it with the concrete parameter types in place of the generic ones to make sure it's type safe and to place any local variables that were generic as concrete types in the symbol table.
Generic returns are analyzed to approximate their return types and transformed into functions.

#### Translation to functions (AKA Monomorphization)

On a function call:

1. Check the function part of the symbol table for a matching definition; if found, return that
2. Check prototypes part of symbol table for a matching definition; if not found, throw error
3. If prototype returns nothing or concrete type, run process of type checking local variables and de-genericizing them in the symbol table, return the return type
4. If prototype returns generic type (`generic` or array of generics), do the same analysis of step 3 but also derive a concrete return type
5. Return the first non-null return type derived from step 4; if there are more than one non-null types, throw an error

Since generics can call other generics, this process is inherently recursive.

Prototypes can also be directly or indirectly (they call functions that call them) recursive, we need to store a queue of the prototypes being translated and if we detect a duplicate translation, we return null for that call.

> Prototypes can slow compilation due to this process and are still less type safe than normal functions
> so use them sparingly and only when the types truly don't matter

### Built-ins

There are built-in variables, functions, and prototypes in order to make development easier. These names are reserved so re-defining them is an error.

#### Variables

| name      | type          | description                                          |
|-----------|---------------|------------------------------------------------------|
| INT_MIN   | int           | minimum value of an integer                          |
| INT_MAX   | int           | maximum value of an integer                          |
| FLOAT_MIN | float         | minimum value of a float                             |
| FLOAT_MAX | float         | maximum value of a float                             |
| argv      | Array<string> | array of strings representing command line arguments |
| argc      | int           | length of `argv`                                     |

#### Functions

| name         | param types           | return type   | description                                           |
|--------------|-----------------------|---------------|-------------------------------------------------------|
| length       | string                | int           | get length of string                                  |
| toString     | int                   | string        | return string version of int                          |
| toString     | float                 | string        | return string version of float                        |
| toString     | boolean               | string        | return string version of boolean                      |
| toInt        | float                 | int           | return int from float (rounds down)                   |
| toInt        | string                | int           | return int from string                                |
| toFloat      | int                   | float         | return float version of int                           |
| toFloat      | string                | float         | return float from string                              |
| max          | int, int              | int           | compare two values and return the greater one         |
| max          | float, float          | float         | compare two values and return the greater one         |
| max          | int, float            | float         | compare two values and return the greater one         |
| max          | float, int            | float         | compare two values and return the greater one         |
| min          | int, int              | int           | compare two values and return the lesser one          |
| min          | float, float          | float         | compare two values and return the lesser one          |
| min          | int, float            | float         | compare two values and return the lesser one          |
| min          | float, int            | float         | compare two values and return the lesser one          |
| contains     | string, string        | boolean       | check if string contains substring                    |
| startsWith   | string, string        | boolean       | check if string starts with substring                 |
| endsWith     | string, string        | boolean       | check if string ends with substring                   |
| exit         | none                  | none          | stop the program with exit code 0                     |
| exit         | int                   | none          | stop the program with supplied exit code              |
| fileExists   | string                | boolean       | check if the file exists                              |
| readFile     | string                | Array<string> | Get contents of file line by line                     |
| writeFile    | string, string        | none          | Write to the file                                     |
| appendToFile | string, string        | none          | Add to the end of the file                            |
| sleep        | float                 | none          | pause execution for specified amount of time          |
| slice        | string, int           | string        | Create string from the index to the end of the string |
| slice        | string, int, int      | string        | Create string from start to end index of string       |
| remove       | string, string        | string        | Remove first instance of substring                    |
| remove       | string, int           | string        | Remove character at index                             |
| removeAll    | string, string        | string        | Remove every instance of substring                    |
| search       | string, int           | int           | Find index of start of substring                      |
| reverse      | string                | string        | reverse string                                        |
| split        | string                | Array<string> | Split string character by character                   |
| split        | string, string        | Array<string> | Split string by delimeter                             |
| join         | Array<string>, string | string        | Combine strings into one string with delimeter        |
| at           | string, int           | string        | index string                                          |
| print        | string                | none          | print to the screen                                   |
| print        | int                   | none          | print to the screen                                   |
| print        | float                 | none          | print to the screen                                   |
| print        | boolean               | none          | print to the screen                                   |

#### Prototypes

| name     | param types             | return type | description                                               |
|----------|-------------------------|-------------|-----------------------------------------------------------|
| length   | Array<generic>          | int         | Return length of array                                    |
| toString | Array<generic>          | string      | create string from Array                                  |
| contains | Array<generic>, generic | boolean     | Check if the array contains an element                    |
| remove   | Array<generic>, int     | none        | Remove element at index from array                        |
| pop      | Array<generic>          | generic     | remove and return first element from array                |
| append   | Array<generic>, generic | none        | Add an elment to the end of an array                      |
| prepend  | Array<generic>, generic | none        | Add an elment to the beginning of an array                |
| sort     | Array<generic>          | none        | In place sort an array                                    |
| indexOf  | Array<generic>, generic | int         | Get the index of an array element; return -1 if not found |
| reverse  | Array<generic>          | none        | In place reversing of array                               |
| print    | Array<generic>          | none        | print array to screen                                     |
