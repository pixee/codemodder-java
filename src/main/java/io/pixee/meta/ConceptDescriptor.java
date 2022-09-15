package io.pixee.meta;

import java.util.ArrayList;
import java.util.List;

public class ConceptDescriptor {

    private final String name;
    private List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();

    public ConceptDescriptor(String name ) {
        this.name = name;
    }

    public ConceptDescriptor add(PropertyDescriptor pd) {
        properties.add(pd);
        return this;
    }

    public ConceptType getType() {
        return new ConceptType(this);
    }
}
