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
 - `fn`
 - `prototype`
 - `generic`
 - `int`
 - `double`
 - `boolean`
 - `string`
 - `Array`
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
  - double
  - string
  - boolean

Array types are dependent on the depth (level of nesting) and the type of data stored in the array.
For example, `Array<Array<int>>` is used to store an array of arrays of ints.

Typecasting is only allowed via builtin functions
  - `toString()`
  - `toInt()`
  - `toDouble()`

#### Operations on types

Binary operators
---

- Addition
  - int, int => int
  - double, double => double
  - int, double => double
  - double, int => double
  - string, string => string
  - Array, Array => Array (must have matching types)
- Multiplication
  - int, int => int
  - double, double => double
  - int, double => double
  - double, int => double
  - string, int => string
  - int, string => string
- Subtraction / Division / Modulus / Exponent
  - int, int => int
  - double, double => double
  - int, double => double
  - double, int => double
- Bitwise (`^` XOR, `&` AND)
  - int, int => int
  - boolean, boolean => int
  - int, boolean => int
  - boolean, int => int
- Comparison (`<`, `<=`, `>`, `>=`)
  - int, int => boolean
  - double, double => boolean
  - int, double => boolean
  - double, int => boolean
- Equality (`==`, `!=`)
  - two operands with same type => boolean
  - anything, null => boolean
  - null, anything => boolean
  - int, double => boolean
  - double, int => boolean
- Logical operators (`&&`, `||`)
  - boolean, boolean => boolean

Unary operators
---
  - `!`
    - boolean => boolean
  - `++`, `--` (either side)
    - int => int
    - double => double
  - `-` (minus)
    - int => int
    - double => double

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
 - `const double PI = 3.14;`

A valid variable name can contain uppercase letters, lowercase letters, numbers, and underscores, but must start with an uppercase or lowercase letter.

Once a variable is declared, you can reassign it (unless it's a `const` variable). Re-declaring, by specifying a type before it, is an error unless it's part of a loop condition.

#### Arrays
Arrays can be declared using the `Array` keyword and must specify their types by wrapping the type in `<` and `>`. There are a few different ways to declare arrays that change their behavior.

1. immutable arrays
 - The variable the array is set to cannot be reassigned and the array is immutable.
 - Since the array cannot change, the size is optional for these
 - Examples:
   - `const Array<int> fib = {1, 1, 2, 3, 5, 8};`
   - `const Array<string> items[2] = {"hammer", "nail"};`
2. mutable arrays
  - These are just arrays declared without `const` and are therefore the default
  - The variable the array is set to can be reassigned and the array is mutable
  - Examples:
    - `Array<int> scores[1] = {98};`

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

      fn toString(double value): string {
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

### Program Structure

All programs must have an entry point into the execution of the program.
That's why the compiler requires you to define a main function.

The main function can take one of a few forms:
1. no params, no return
2. no params, return int
3. `int` param and `Array<string>` param, no return
4. `int` param and `Array<string>` param, int return

The return from the main function is treated as the program's return code. If your main doesn't return, it will automatically have a return code of 0 (successful execution).

The parameters specified in main function forms 3 and 4 are used to deal with command line arguments.

Typically, they'll look like this:

```
fn main(int argc, Array<string> argv) {}
```

`argv` is an array of the command line arguments (separated by space) and `argc` is the length of `argv`.

> You don't have to use the parameter names, but those names are common practice in C, which is an inspiration for this language.

The only code allowed outside a function are variable declarations (including arrays).
Any variables declared outside a function will be global; while any variables inside a function are local to the function.

### Built-ins

There are built-in variables, functions, and prototypes in order to make development easier. These names are reserved so re-defining them is an error.

#### Variables

| name      | type            | description                                          |
|-----------|-----------------|------------------------------------------------------|
| INT_MIN   | int             | minimum value of an integer                          |
| INT_MAX   | int             | maximum value of an integer                          |
| DOUBLE_MIN | double           | minimum value of a double                             |
| DOUBLE_MAX | double           | maximum value of a double                             |

#### Functions

| name         | param types             | return type     | description                                                     |
|--------------|-------------------------|-----------------|-----------------------------------------------------------------|
| printerr     | string                  | none            | print to stderr                                                 |
| printerr     | string, int             | none            | print to stderr and terminate the program with an exit code     |
| length       | string                  | int             | get length of string                                            |
| max          | int, int                | int             | compare two values and return the greater one                   |
| max          | double, double          | double          | compare two values and return the greater one                   |
| max          | int, double             | double          | compare two values and return the greater one                   |
| max          | double, int             | double          | compare two values and return the greater one                   |
| min          | int, int                | int             | compare two values and return the lesser one                    |
| min          | double, double          | double          | compare two values and return the lesser one                    |
| min          | int, double             | double          | compare two values and return the lesser one                    |
| min          | double, int             | double          | compare two values and return the lesser one                    |
| replace      | string, string, string  | string          | Replace the first matching substring with a different substring |
| replaceAll   | string, string, string  | string          | Replace all matching substrings with a different substring      |
| split        | string                  | Array\<string\> | Split string character by character                             |
| split        | string, string          | Array\<string\> | Split string by delimiter                                       |
| slice        | string, int             | string          | Create string from the index to the end of the string           |
| slice        | string, int, int        | string          | Create string from start to end index of string                 |
| contains     | string, string          | boolean         | check if string contains substring                              |
| toInt        | double                  | int             | return int from double (rounds down)                            |
| toInt        | string                  | int             | return int from string                                          |
| toDouble     | int                     | double          | return double version of int                                    |
| toDouble     | string                  | double          | return double from string                                       |
| at           | string, int             | string          | index string                                                    |
| join         | Array\<string\>, string | string          | Combine strings into one string with delimiter                  |
| reverse      | string                  | string          | reverse string                                                  |
| startsWith   | string, string          | boolean         | check if string starts with substring                           |
| endsWith     | string, string          | boolean         | check if string ends with substring                             |
| sleep        | double                  | none            | pause execution for specified amount of time (seconds)          |
| sleep        | int                     | none            | pause execution for specified amount of time (seconds)          |
| exit         | none                    | none            | stop the program with exit code 0                               |
| exit         | int                     | none            | stop the program with supplied exit code                        |
| removeAll    | string, string          | string          | Remove every instance of substring                              |
| fileExists   | string                  | boolean         | check if the file exists                                        |
| readFile     | string                  | Array\<string\> | Get contents of file line by line                               |
| writeFile    | string, string          | none            | Write to the file                                               |
| appendToFile | string, string          | none            | Add to the end of the file                                      |
| renameFile   | string, string          | none            | Change the name of the file                                     |
| deleteFile   | string                  | none            | Delete the file                                                 |
| getEnv       | string                  | string          | Get a value from a specified environment variable               |
| setEnv       | string, string          | none            | Set the value of an environment variable                        |


#### Prototypes

| name        | param types                    | return type      | description                                               |
|-------------|--------------------------------|------------------|-----------------------------------------------------------|
| print       | generic                        | none             | print to the screen                                       |
| println     | generic                        | none             | print to the screen with new line character               |
| getType     | generic                        | string           | Get the type of the variable or constant                  |
| length      | Array\<generic\>               | int              | Return length of array                                    |
| capacity    | Array\<generic\>               | int              | Get the total size of the array                           |
| toString    | Array\<generic\>               | string           | create string from Array                                  |
| slice       | Array\<generic\>, int          | Array\<generic\> | Create an array from the index to the end of the array    |
| slice       | Array\<generic\>, int, int     | Array\<generic\> | Create array from start to end index of array             |
| contains    | Array\<generic\>, generic      | boolean          | Check if the array contains an element                    |
| append      | Array\<generic\>, generic      | none             | Add an element to the end of an array                     |
| prepend     | Array\<generic\>, generic      | none             | Add an element to the beginning of an array               |
| insert      | Array\<generic\>, generic, int | none             | Add element at index                                      |
| removeIndex | Array\<generic\>, int          | none             | Remove element at index from array                        |
| remove      | Array\<generic\>, generic      | none             | Remove first matching element from array                  |
| removeAll   | Array\<generic\>, generic      | none             | Remove all matching elements from array                   |
| indexOf     | Array\<generic\>, generic      | int              | Get the index of an array element; return -1 if not found |
| toString    | generic                        | string           | return string version of parameter                        |
| reverse     | Array\<generic\>               | none             | In place reversing of array                               |
| sort        | Array\<generic\>               | none             | In place sort an array                                    |
