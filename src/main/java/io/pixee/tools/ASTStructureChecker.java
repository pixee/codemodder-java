package io.pixee.tools;

import io.pixee.ast.Child;
import io.pixee.ast.ErrorChild;
import io.pixee.ast.ErrorNode;
import io.pixee.ast.Node;
import io.pixee.lang.LanguageDescriptor;
import io.pixee.lang.PropertyDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Checks an AST against its language definition
 */
public class ASTStructureChecker implements Checker {

    private LanguageDescriptor language;

    public ASTStructureChecker(LanguageDescriptor lang) {
        this.language = lang;
    }

    @Override
    public Collection<Message> execute(Node n, boolean isUsedAsPattern) {
        List<Message> messages = new ArrayList<>();
        checkNode(n, messages, isUsedAsPattern);
        return messages;
    }

    public Collection<Message> executeAndPrint(Node n, boolean isUsedAsPattern) {
        Collection<Message> messages = execute(n, isUsedAsPattern);
        System.err.println("Messages for "+n);
        for (Message m:messages) {
            System.err.println("- "+m);
        }
        return messages;
    }

    private void checkNode(Node n, List<Message> messages, boolean isUsedAsPattern) {
        if (n instanceof ErrorNode) messages.add(new NodeRelatedMessage(n, ((ErrorNode) n).problem()));
        for (Child c: n.children()) {
            if (c instanceof ErrorChild) messages.add(new ChildRelatedMessage(c, ((ErrorChild) c).problem()));
        }
        if (!isUsedAsPattern) {
            for (PropertyDescriptor property: n.concept.properties()) {
                if (!n.hasChildrenFor(property)) messages.add(new NodeRelatedMessage(n, "child missing for property "+property.name));
            }
        }
        for (Node childNode: n.childNodes()) {
            checkNode(childNode, messages, isUsedAsPattern);
        }
    }
}
