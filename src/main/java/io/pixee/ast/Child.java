package io.pixee.ast;

import io.pixee.lang.PropertyDescriptor;

public class Child {

    private PropertyDescriptor role;
    private String tempRoleName;
    private Data data;

    public Child(PropertyDescriptor role, Data data) {
        this.role = role;
        this.data = data;
    }

    public PropertyDescriptor role() {
        return this.role;
    }

    public Child(String role, Data node) {
        this.tempRoleName = role;
        this.data = node;
    }

    void resolve(Node parent) {
        role = parent.concept.prop(tempRoleName);
    }

    public Data data() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
