# Formal Grammar

```
PROGRAM ::= ( STMNT / FUNC-DEF )+
STMNT ::= ( ( VAR-DECL / VAR-ASSIGN / EXPR / CONTROL-FLOW ) SC ) / COND / LOOP
EXPR ::= ( LOGICAL-OR / TERNARY )
ARITH-EXPR ::= TERM (ADD-OP ARITH-EXPR)*
TERM ::= EXPO (MULT-OP TERM)*
EXPO ::= FACTOR ( "**" EXPO )*
FACTOR ::= VALUE / "(" EXPR ")" / UNARY-OP
LOGICAL-OR ::=  ( LOGICAL-OR "||" )* LOGICAL-AND
LOGICAL-AND ::= ( LOGICAL-AND "&&" )* CMPR-EXPR
CMPR-EXPR ::= EXPR ( CMPR-OP EXPR )?
UNARY-OP ::= LEFT-UNARY-OP / ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) ( "++" / "--" )
LEFT-UNARY-OP ::= ( "++" / "--" / "-" ) ( ID / NUMBER / ARRAY-ACCESS / FUNC-CALL ) / "!" (BOOLEAN / ID / ARRAY-ACCESS / FUNC-CALL)
ADD-OP ::= "+" / "-" / "^" / "&"
MULT-OP ::= "*" / "/" / "%"
CMPR-OP ::= "<" / ">" / "<=" / ">=" / "!=" / "=="
TERNARY ::= LOGICAL-OR "?" EXPR ":" EXPR
VALUE ::=  FUNC-CALL / ARRAY-ACCESS / ID / STRING-LIT / NUMBER / BOOLEAN / ARRAY-LIT / null
FUNC-CALL ::= ID ( ( "(" (EXPR ("," EXPR)* )? ")" )
ARRAY-ACCESS ::= ID ARRAY-INDEX
ARRAY-INDEX ::= ( "[" EXPR "]" )+
ARRAY-LIT ::= "{" (EXPR ("," EXPR)* )? "}"
COND ::= IF ( ELSEIF )* ( ELSE )?
IF ::= "if" "(" EXPR ")" COND-BODY
ELSEIF ::= "else" "if" "(" EXPR ")" COND-BODY
ELSE ::= "else" COND-BODY
COND-BODY ::= ( ( "{" ( STMNT )* "}" ) / STMNT )
LOOP ::= ( WHILE-LOOP / FOR-LOOP ) "{" ( STMNT )* "}"
WHILE-LOOP ::= "while" "(" EXPR ")"
FOR-LOOP ::= "for" "(" ( ( ( VAR-DECL / VAR-ASSIGN ) ";" EXPR ";" EXPR ) / TYPE ID : EXPR ) ")"
FUNC-DEF ::= ( "fn" / "prototype" ) ID "(" (FUNC-PARAM ("," FUNC-PARAM)* )? ")" ( ":" TYPE )? "{" ( STMNT )* "}"
FUNC-PARAM ::= TYPE ID
ARRAY-TYPE ::= "Array" "<" TYPE ">"
IMMUTABLE-ARRAY-DECL ::= "const" ARRAY-TYPE ID ( ARRAY-INDEX )? "=" EXPR
ARRAY-DECL ::= ARRAY-TYPE ID ARRAY-INDEX ( "=" EXPR )? / IMMUTABLE-ARRAY-DECL
VAR-DECL ::= ("const")? TYPE ID ( "=" EXPR )? / ARRAY-DECL
VAR-ASSIGN ::= ( ID / ARRAY-ACCESS )  ( "+" / "-" / "*" / "/" )? = EXPR
TYPE ::= "int" / "string" / "double" / "boolean" / "generic" / ARRAY-TYPE
CONTROL-FLOW ::= "return" ( EXPR )? / "continue" / "break"
```