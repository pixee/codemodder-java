package io.pixee.ast;

import io.pixee.lang.ConceptDescriptor;
import io.pixee.lang.LanguageDescriptor;
import io.pixee.lang.PropertyDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a node in the AST. Can contain children and points
 * to the concept from the language definition of which it represents
 * an instance.
 */
public class Node extends Data {

    public final ConceptDescriptor concept;
    private final List<Child> children = new ArrayList<Child>();
    private Node parent;


    public Node(ConceptDescriptor concept) {
        this.concept = concept;
    }

    public Node add(PropertyDescriptor pd, Data data) {
        this.children.add(new Child(pd, data));
        return this;
    }

    public Node add(Child c) {
        this.children.add(c);
        return this;
    }

    public Node add(String propName, Data data) {
        this.children.add(new Child(propName, data));
        return this;
    }

    public List<Node> childNodes() {
        return children.stream().filter(c -> c.data() instanceof Node).map(c -> ((Node) c.data())).toList();
    }

    public void resolve(LanguageDescriptor lang, Node parent) {
        this.parent = parent;
        this.children.stream().forEach(c -> c.resolve(this));
        this.childNodes().stream().forEach(c -> c.resolve(lang, this));
    }

    public Collection<Child> children() {
        return this.children;
    }

    public Collection<Child> childrenFor(PropertyDescriptor property) {
        return this.children.stream().filter(it -> it.role() == property).toList();
    }

    public boolean hasChildrenFor(PropertyDescriptor property) {
        return !childrenFor(property).isEmpty();
    }

    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.concept.name + " {\n");
        for (Child c : children.stream().filter(c -> c.data() instanceof Value).toList()) {
            sb.append(indent + "  " + c.role().name + ": ");
            sb.append(((Value) c.data()).data + "\n");
        }
        for (Child c : children.stream().filter(c -> c.data() instanceof Node).toList()) {
            sb.append(indent + "  " + c.role().name + ": ");
            sb.append(((Node) c.data()).dump(indent+"  "));
        }
        sb.append(indent + "}\n");
        return sb.toString();
    }

    public Node parent() {
        return parent;
    }

    public void replaceChild(Node matched, Node replacement) {
        for (Child c: children) {
            if (c.data() instanceof Node && c.data() == matched) {
                c.setData(replacement);
            }
        }
        replacement.resolve(this.concept.language(),this);
    }

    @Override
    public String toString() {
        return "node:" + concept.name;
    }
}

