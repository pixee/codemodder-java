package io.pixee.meta;

import java.util.ArrayList;
import java.util.List;

public class ConceptDescriptor {

    public final String name;
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

    public PropertyDescriptor prop(String name) {
        return properties.stream().filter(prop -> prop.name.equals(name)).findFirst().orElseThrow();
    }
}
