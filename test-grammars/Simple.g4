grammar Simple;

// Parser rules
expr: expr '+' term | term ;
term: '(' expr ')' | NUMBER ;

// Lexer rules  
NUMBER: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;