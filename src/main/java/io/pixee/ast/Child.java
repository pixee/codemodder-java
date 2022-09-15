package io.pixee.ast;

import io.pixee.meta.LanguageDescriptor;
import io.pixee.meta.PropertyDescriptor;

public class Child {

    private PropertyDescriptor role;
    private String tempRoleName;
    public final Data data;

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

}
