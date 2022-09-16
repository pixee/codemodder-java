package io.pixee.codetl;

import io.pixee.ast.*;
import io.pixee.codetl_antlr.CodeTLParser;
import io.pixee.lang.ConceptDescriptor;
import io.pixee.lang.LanguageDescriptor;
import io.pixee.lang.PrimitiveType;
import io.pixee.lang.PropertyDescriptor;
import io.pixee.tools.ASTStructureChecker;
import io.pixee.tools.Checker;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
 * A language provider that works for any language because it uses
 * reflective APIs to build the AST.
 */
public class GenericLanguageProvider implements SubjectLanguageProvider{

    private final LanguageDescriptor language;

    public GenericLanguageProvider(LanguageDescriptor lang) {
        this.language = lang;
    }

    @Override
    public Node parseMatchNode(ParseTree tree) {
        return parseNode(tree);
    }

    @Override
    public Node parseReplacementNode(ParseTree tree) {
        return parseNode(tree);
    }

    private Node parseNode(ParseTree tree) {
        String conceptName = tree.getChild(0).getText();
        ConceptDescriptor concept = language.conceptByName(conceptName);
        if (concept == null) return new ErrorNode("concept named "+conceptName+" not found in language "+ language.name());
        Node theNode = new Node(concept);
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof CodeTLParser.Ast_node_childContext) {
                parseAndAddChild(theNode, ((CodeTLParser.Ast_node_childContext) child));
            }    
        }
        return theNode;
    }

    private void parseAndAddChild(Node n, CodeTLParser.Ast_node_childContext tree) {
        String propName = tree.getChild(0).getText();
        PropertyDescriptor prop = n.concept.prop(propName);
        if (prop == null) {
            n.add(new ErrorChild(n.concept.name+" does not have a property " + propName));
        } else {
            Data data = null;
            ParseTree dataContext = tree.getChild(2);
            if (dataContext instanceof CodeTLParser.ExpressionContext) {
                ParseTree expr = dataContext.getChild(0);
                if (expr instanceof CodeTLParser.Number_literalContext numberLit) {
                    data = new Value(PrimitiveType.INTEGER, Integer.valueOf(numberLit.NUMBER().getText()));
                } else if (expr instanceof CodeTLParser.String_literalContext stringLit) {
                    data = new Value(PrimitiveType.STRING, stringLit.STRING().getText().replaceAll("\"",""));
                } else {
                    throw new RuntimeException("Not yet implemented: " + dataContext.getClass().getName());
                }
            } else if (dataContext instanceof CodeTLParser.Ast_nodeContext) {
                data = parseNode(dataContext);
            } else {
                throw new RuntimeException("Not yet implemented: " + dataContext.getClass().getName());
            }
            n.add(new Child(prop, data));
        }
    }

    @Override
    public Iterable<Checker> checkers() {
        return List.of(new ASTStructureChecker(language));
    }


}
