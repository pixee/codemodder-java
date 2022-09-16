grammar helloWorldGrammar;

program: variable* EOF;

variable: VAR ID '=' numlit NEWLINE ;

numlit: NUMBER ;

fragment LOWERCASE  : [a-z] ;
fragment UPPERCASE  : [A-Z] ;
WS                  : (' ' | '\t') -> skip;
NEWLINE             : ('\r'? '\n' | '\r')+ ;
VAR                 : 'var';
ID                  : LOWERCASE+ ;
NUMBER              :  ('0'..'9')+ ('.' ('0'..'9')+)? ;