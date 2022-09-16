package io.pixee.ast;

import io.pixee.lang.PropertyDescriptor;

/**
 * Represents information about a child of a node, including the
 * property/role and the child itself; points to a property from
 * the language definition
 */
public class Child {

    private PropertyDescriptor role;
    private String tempRoleName;
    private Data data;

    public Child(PropertyDescriptor role, Data data) {
        this.role = role;
        this.data = data;
    }

    public Child copy() {
        return new Child(role, data.copy());
    }

    public PropertyDescriptor role() {
        return this.role;
    }

    public Child(String role, Data node) {
        this.tempRoleName = role;
        this.data = node;
    }

    void resolve(Node parent) {
        if (role == null) {
            role = parent.concept.prop(tempRoleName);
        }
    }

    public Data data() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "child:" + role.name+"=" + this.data;
    }
}
