grammar EBNFTest;

// Test EBNF operators
list: ID (',' ID)* ;
optional_item: ID? ;
one_or_more: ID+ ;

// Lexer rules
ID: [a-zA-Z]+ ;
WS: [ \t\r\n]+ -> skip ;