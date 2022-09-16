package io.pixee.lang;

import java.util.ArrayList;
import java.util.List;

public class ConceptDescriptor {

    public final String name;
    private final List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
    private LanguageDescriptor language;

    public ConceptDescriptor(String name ) {
        this.name = name;
    }

    public ConceptDescriptor add(PropertyDescriptor pd) {
        properties.add(pd);
        return this;
    }

    void setLanguage(LanguageDescriptor ld) {
        this.language = ld;
    }

    public LanguageDescriptor language() {
        return language;
    }

    public ConceptType getType() {
        return new ConceptType(this);
    }

    public PropertyDescriptor prop(String name) {
        return properties.stream().filter(prop -> prop.name.equals(name)).findFirst().orElseThrow();
    }

    public Iterable<PropertyDescriptor> properties() {
        return this.properties;
    }
}
