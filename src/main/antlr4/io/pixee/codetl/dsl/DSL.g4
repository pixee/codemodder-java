grammar DSL;

/*
 * Tokens (terminal)
 */
GIVEN: 'given';
METHOD_CALL: 'method_call';
WHERE: 'where';
NAME: 'name';
TYPE: 'type';
RETURN: 'return';
TRANSFORM: 'transform';

EQ: '=';
ASSIGN: ':=';

LPAREN : '(';
RPAREN : ')';

Identifier
   : VALID_ID_START VALID_ID_CHAR*
   ;

fragment VALID_ID_START
   : ('a' .. 'z') | ('A' .. 'Z') | '_'
   ;

fragment VALID_ID_CHAR
   : VALID_ID_START | ('0' .. '9')
   ;

variable:
    Identifier
   ;

typeName:
    Identifier ( '.' Identifier)*
   ;

methodName
	:	Identifier | '<init>'
	;

WHITESPACE: [ \r\n\t]+ -> skip;

start:
    condition
    transformation
    ;

condition:
    GIVEN METHOD_CALL var=variable WHERE
      'name' EQ mName=methodName
      'type' EQ type=typeName
    ;

transformation:
    TRANSFORM
      'name' EQ mName=methodName
      'type' EQ type=typeName
   ;