grammar DebugTest;

start: expr EOF ;

expr: 
    expr '+' expr
    | '(' expr ')'
    | ID
    ;

ID: [a-zA-Z]+ ;
WS: [ \t\r\n]+ -> skip ;