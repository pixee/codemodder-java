package io.pixee.codetl;

import io.pixee.ast.ErrorNode;
import io.pixee.ast.Node;
import io.pixee.lang.ConceptDescriptor;
import io.pixee.lang.LanguageDescriptor;
import io.pixee.tools.ASTStructureChecker;
import io.pixee.tools.Checker;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

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
        return new Node(concept);
    }

    @Override
    public Iterable<Checker> checkers() {
        return List.of(new ASTStructureChecker(language));
    }


}
