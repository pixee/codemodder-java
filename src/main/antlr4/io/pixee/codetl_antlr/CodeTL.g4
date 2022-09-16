grammar CodeTL;

codeTlRule:
  rule_statement
  match_statement
  (replace_statement)+
  ;

rule_statement : RULE_SECTION_START RULE_ID ;
match_statement : MATCH_SECTION_START ast_node;
replace_statement : REPLACE_SECTION_START VARIABLE_IDENTIFIER ast_node;

ast_node :
 IDENTIFIER
 VARIABLE_IDENTIFIER?
 '{'
    ast_node_child*
 '}'
 ;



ast_node_child  : IDENTIFIER '=' (child_value | ast_node | ast_node_list);
child_value     : type_identifier | numeric_value;
ast_node_list   : '[' (ast_node ','?)* ']';
type_identifier : DOTTED_IDENTIFIER;
numeric_value   : NUMBER;

RULE_SECTION_START : 'rule';
// should these be hardcoded? Doesn't this depend on the language config?
RULE_ID : [a-z]+ ':' ('java' | 'python' | 'helloworld') '/' [a-z-]+;
MATCH_SECTION_START : 'match' ;
REPLACE_SECTION_START : 'replace' ;
VARIABLE_IDENTIFIER : '$' IDENTIFIER ;
IDENTIFIER : [a-zA-Z]+ ;
DOTTED_IDENTIFIER : [a-zA-Z\\.]+ ;
NUMBER              :  ('0'..'9')+ ('.' ('0'..'9')+)? ;

WS : [ \t\r\n]+ -> skip ;