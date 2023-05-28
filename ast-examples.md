# AST Examples

## Arithmetic

**Code**: 

`2 + 2 + 2`

**AST**:

```
        +
       / \
      2   +
         / \
        2   2
```

**Code**:

`1 * 2 + 3`

**AST**:

```
        +
     /    \
    *      3
   / \
  1   2
```

**Code**:

`4 * (2 + 3) - 1`

**AST**:

```
        -
     /    \
    *      1
   / \
  4   +
     /  \
    2    3
```

**Code**:

`2 + 2 < 3 || 3 - 2 == 1`

```
          ||
       /       \
      <         ==
    /  \       /  \
   +     3     -    1
 /  \   / \
2   2   3   2
```

**Code**:

`2 ** 3 ** 4 ** 5`

**AST**:

```
        **
      /    \
    2       **
          /    \
         3      **
               /  \
              4    5
```

## Variable Declarations

**Code**:

`string str`

**AST**:

```
     var-decl
    /        \
  string     str
```

**Code**:

`string str = someValue`

**AST**:

```
           var-decl
         /      |    \
       string   str   someValue
```

**Code**:

`const float x = y ** 2`

**AST**:

```
         var-decl
      /    |    \   \
    const  float  x  **
                     / \
                    y   2
```

**Code**:

`const Array<int> arr[3] = {1, 2, 3}`

**AST**:

```
                      array-decl

                /     |   \   \      \
               const Array arr index  array-lit
                      |           \    /   |   \
                     int           3   1    2    3
```

**Code**:

`const mut Array<int> arr[3] = {1, 2, 3}`

**AST**:

```
                      array-decl

                /     |      |    \   \      \
               const  mut   Array  arr index  array-lit
                             |           |    /   |   \
                            int          3   1    2    3
```

**Code**:

`mut Array<Array<string>> arr[4][5] = {}`

**AST**:

```
                      array-decl

                   /      |    \   \      \
                 mut   Array  arr index  array-lit
                         |          |   
                       Array        4
                         |          |
                       string       5   
```


## Function declarations

**Code**:

```
fn test(int i, int j): int {
    return i * j
}
```

**AST**:

```
                        fn
            /     /          \      \
           test  params     int     body
                /     |               |
              param   param         return
              |  |    \   |           |
             int i    int  j          *
                                     / \
                                    i   j
```

## Function Calls

**Code**:

`prompt()`

**AST**:

```
        call
         |
        prompt
```

**Code**:

`prompt(user_input)`
 
**AST**:

```
          call
        /      \
     prompt    params
                  |
                user_input
```

**Code**:

`prompt(user_input, context)`

**AST**:

```
            call
        /         \
     prompt       params
                /       \
        user_input      context
```

## Conditional Statements

**Code**:

`if (x < 5) { return false } else if (x % 2 == 0) { return true }`

**AST**:

```
                  cond
             /              \    
           if               else if
         /   \              /     \
       <      body         ==     body
     /   \      |         /  \      |
    x     5     return    %    0    return
                  |      / \         |
                 false  x  2       true
```

**Code**:

`while (true) { doSomething() }`

**AST**:

```
       while
      /     \
    true    body
               \
              call
                \
                doSomething
```

**Code**:

`for (i = 0; i < len; i++) { arr[i]++ }`

**AST**:

```
               for
        /     |    \         \
      =       <     unary     body
    /  \    /  \      /  \      |
   i    0   i   len  i    ++  unary
                             |   \ 
                            arr  ++
                            |
                        index
                         |
                         i
```                              

**Code**:

`for (float flt : floats) {print(-1 * flt)}`

**AST**:

```
                 for
            /    |      \
    var-decl     floats   body
    /     |                |
   float  flt             call
                          /   \
                     print   params
                              |
                              *
                            /   \
                        unary     flt
                        /  \
                       -    1
```

## Misc.

**Code**:

`x === y ? 0 : 1`

**AST**:

```
       ternary
    /     |     \
  ===     0      1
  / \
 x   y
```


**Code**:

`arr[3][0][i+1]`

**AST**:

```
           arr
            |
          index
         /     \
        3      index
              /     \
             0      index
                      |
                      +
                     / \
                    i   1
```
