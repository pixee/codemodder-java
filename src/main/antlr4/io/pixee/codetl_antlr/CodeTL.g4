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

type_identifier : DOTTED_IDENTIFIER;
ast_node_child : IDENTIFIER '=' (type_identifier | ast_node | ast_node_list);
ast_node_list: '[' (ast_node ','?)* ']';

RULE_SECTION_START : 'rule';
RULE_ID : [a-z]+ ':' ('java' | 'python') '/' [a-z-]+;
MATCH_SECTION_START : 'match' ;
REPLACE_SECTION_START : 'replace' ;
VARIABLE_IDENTIFIER : '$' IDENTIFIER ;
IDENTIFIER : [a-zA-Z]+ ;
DOTTED_IDENTIFIER : [a-zA-Z\\.]+ ;

WS : [ \t\r\n]+ -> skip ;