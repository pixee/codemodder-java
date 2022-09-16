package io.pixee.tools;

import io.pixee.ast.ErrorNode;
import io.pixee.ast.Node;
import io.pixee.lang.LanguageDescriptor;

import java.util.ArrayList;
import java.util.List;

public class ASTStructureChecker implements Checker {

    private LanguageDescriptor language;

    public ASTStructureChecker(LanguageDescriptor lang) {
        this.language = lang;
    }

    @Override
    public Iterable<Message> run(Node n) {
        List<Message> messages = new ArrayList<>();
        checkNode(n, messages);
        return messages;
    }

    private void checkNode(Node n, List<Message> messages) {
        if (n instanceof ErrorNode) messages.add(new NodeRelatedMessage(n, ((ErrorNode) n).problem()));
    }
}
