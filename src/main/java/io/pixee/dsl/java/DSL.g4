grammar DSL;

/*
 * Tokens (terminal)
 */
MATCH: 'match';
REPLACE: 'replace';
WITH: 'with';

COLON: ':';
EQ: '=';

INSERT: 'insert';
INTO: 'into';
DATA: 'data';
FLOW: 'flow';
BEFORE: 'before';
AFTER: 'after';

AND: 'and';
OR: 'or';
WHERE: 'where';

CURLY_BRACKET_OPEN: '{';
CURLY_BRACKET_CLOSE: '}';

InsertIntoDataFlow:
    INSERT INTO DATA FLOW ( BEFORE | AFTER )
   ;

Identifier
   : VALID_ID_START VALID_ID_CHAR*
   ;

RuleIdentifier
   : VALID_ID_START VALID_RULE_ID_CHAR*
   ;

fragment VALID_ID_START:
   ('a' .. 'z') | ('A' .. 'Z') | '_'
   ;

fragment VALID_ID_CHAR:
   VALID_ID_START | ('0' .. '9')
   ;

fragment VALID_RULE_ID_CHAR:
   VALID_ID_START | ('0' .. '9') | '-'
   ;

Variable:
   '$'Identifier
   ;

RuleId:
   RuleIdentifier ':' RuleIdentifier '/' RuleIdentifier
   ;

WHITESPACE: [ \r\n\t]+ -> skip;

/*
 * Rules
 */
type:
    Identifier ('.' Identifier)*
    ;

rule_id:
   'rule' id=RuleId
   ;

match_replace:
    id=Identifier CURLY_BRACKET_OPEN
        name+=Identifier EQ value+=type
        ( name+=Identifier EQ value+=type )*
    CURLY_BRACKET_CLOSE
    ;

match:
    MATCH
    match_replace
    ;

replace:
    REPLACE (var=Variable WITH)?
    match_replace
    ;

clause:
    var+=Identifier '.' method+=Identifier '(' target += Identifier ')'
    ;

where:
    WHERE
    clause ( (AND | OR) clause )*
    ;

start:
    rule_id
    match
    (where)?
    replace
    ;