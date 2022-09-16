grammar CodeTL;

codeTlRule:
  rule_statement
  match_statement
  (replace_statement)+
  ;

rule_statement   : RULE_SECTION_START RULE_ID ;
match_statement  : MATCH_SECTION_START ast_node;
replace_statement: REPLACE_SECTION_START VARIABLE_IDENTIFIER ast_node;

ast_node         : IDENTIFIER VARIABLE_IDENTIFIER? '{'
                       ast_node_child*
                   '}';

ast_node_child  : IDENTIFIER '=' (expression | ast_node | ast_node_list)?;
ast_node_list   : '[' (ast_node ','?)* ']';

expression      : (number_literal | string_literal | type_identifier);
number_literal  : NUMBER;
string_literal  : STRING;
type_identifier : DOTTED_IDENTIFIER;


RULE_SECTION_START     : 'rule';
RULE_ID                : [a-z]+ ':' ('java' | 'python' | 'helloworld') '/' [a-z-]+; // should these be hardcoded? Doesn't this depend on the language config?
MATCH_SECTION_START    : 'match' ;
REPLACE_SECTION_START  : 'replace' ;
VARIABLE_IDENTIFIER    : '$' IDENTIFIER ;
IDENTIFIER             : [a-zA-Z]+ ;
DOTTED_IDENTIFIER      : [a-zA-Z\\.]+ ;
NUMBER                 :  ('0'..'9')+ ('.' ('0'..'9')+)? ;
STRING                 : '"'[a-zA-Z0-9]*'"';

WS : [ \t\r\n]+ -> skip ;