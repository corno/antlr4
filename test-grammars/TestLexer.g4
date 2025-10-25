lexer grammar TestLexer;

// Keywords
IF: 'if' ;
WHILE: 'while' ;
FOR: 'for' ;

// Identifiers
fragment LETTER: [a-zA-Z] ;
fragment DIGIT: [0-9] ;
ID: LETTER (LETTER | DIGIT)* ;

// Numbers
INT: DIGIT+ ;
FLOAT: DIGIT+ '.' DIGIT+ ;

// Whitespace
WS: [ \t\r\n]+ -> skip ;

// Comments 
COMMENT: '/*' .*? '*/' -> skip ;
LINE_COMMENT: '//' ~[\r\n]* -> skip ;