grammar DSL;

/*
 * Tokens (terminal)
 */
MATCH: 'MATCH';
CONS_CALL: 'ConsCall';
STATIC_CALL: 'StaticCall';
METHOD_CALL: 'MethodCall';
REPLACE: 'REPLACE';
WITH: 'WITH';

COLON: ':';

INSERT: 'INSERT';
INTO: 'INTO';
DATA: 'DATA';
FLOW: 'FLOW';
BEFORE: 'BEFORE';
AFTER: 'AFTER';

InsertIntoDataFlow:
    INSERT INTO DATA FLOW ( BEFORE | AFTER )
   ;

Identifier
   : VALID_ID_START VALID_ID_CHAR*
   ;

fragment VALID_ID_START
   : ('a' .. 'z') | ('A' .. 'Z') | '_'
   ;

fragment VALID_ID_CHAR
   : VALID_ID_START | ('0' .. '9')
   ;

Variable:
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

/*
 * Rules
 */

constructorCall:
    MATCH
       CONS_CALL var=Variable
        'target' COLON source=StringLiteral
    REPLACE var=Variable WITH
      CONS_CALL
       'target' COLON target=StringLiteral
    ;

replaceStaticCall:
    (
        ( REPLACE var=Variable WITH ) |
        InsertIntoDataFlow
    )
      STATIC_CALL
       'target' COLON target=StringLiteral args=StringLiteral
    ;

methodCall:
    MATCH
       METHOD_CALL var=Variable
        'target' COLON source=StringLiteral
    ( replaceStaticCall )
    ;

start:
    constructorCall |
    methodCall
    ;