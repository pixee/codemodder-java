package io.pixee.ast;

import io.pixee.meta.ConceptDescriptor;
import io.pixee.meta.LanguageDescriptor;
import io.pixee.meta.PropertyDescriptor;

import java.util.ArrayList;
import java.util.List;

public class Node extends Data {

    public final ConceptDescriptor concept;
    private List<Child> children = new ArrayList<Child>();
    private Node parent;


    public Node(ConceptDescriptor concept) {
        this.concept = concept;
    }

    public Node add(PropertyDescriptor pd, Data data) {
        this.children.add(new Child(pd, data));
        return this;
    }

    public Node add(String propName, Data data) {
        this.children.add(new Child(propName, data));
        return this;
    }

    public List<Node> childNodes() {
        return children.stream().filter(c -> c.data instanceof Node).map(c -> ((Node) c.data)).toList();
    }

    public void resolve(LanguageDescriptor lang, Node parent) {
        this.parent = parent;
        this.children.stream().forEach(c -> c.resolve(this));
        this.childNodes().stream().forEach(c -> c.resolve(lang, this));
    }


    public String dump(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.concept.name + " {\n");
        for (Child c : children.stream().filter(c -> c.data instanceof Value).toList()) {
            sb.append(indent + "  " + c.role().name + ": ");
            sb.append(((Value) c.data).data + "\n");
        }
        for (Child c : children.stream().filter(c -> c.data instanceof Node).toList()) {
            sb.append(indent + "  " + c.role().name + ": ");
            sb.append(((Node) c.data).dump(indent+"  "));
        }
        sb.append(indent + "}\n");
        return sb.toString();
    }
}

