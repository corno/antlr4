// Test grammar to demonstrate the JSON export
grammar TestCalc;

// Parser rules
expr 
    : expr '+' term      # AddExpr
    | expr '-' term      # SubExpr  
    | term               # TermExpr
    ;

term
    : term '*' factor    # MulTerm
    | term '/' factor    # DivTerm
    | factor             # FactorTerm
    ;

factor
    : '(' expr ')'       # ParenFactor
    | NUMBER             # NumberFactor
    | ID                 # IdFactor
    ;

// Lexer rules
NUMBER : [0-9]+ ('.' [0-9]+)?;
ID     : [a-zA-Z][a-zA-Z0-9]*;
WS     : [ \t\r\n]+ -> skip;

// String literals: '+', '-', '*', '/', '(', ')'