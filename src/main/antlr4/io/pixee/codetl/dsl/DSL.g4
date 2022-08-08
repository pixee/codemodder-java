grammar DSL;

/*
 * Tokens (terminal)
 */
MATCH: 'MATCH';
CONS_CALL: 'ConsCall';
REPLACE: 'REPLACE';
WITH: 'WITH';

COLON: ':';

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
    '$'Identifier
   ;

StringLiteral
	:	'"' StringCharacters? '"'
	;

fragment
StringCharacters
	:	StringCharacter+
	;

fragment
StringCharacter
	:	~["\\\r\n]
	;

WHITESPACE: [ \r\n\t]+ -> skip;

start:
    condition
    transformation
    ;

condition:
    MATCH
       CONS_CALL var=variable
        'target' COLON target=StringLiteral
    ;

transformation:
    REPLACE var=variable WITH
      CONS_CALL
       'target' COLON target=StringLiteral
   ;