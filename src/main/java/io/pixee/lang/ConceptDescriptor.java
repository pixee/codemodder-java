package io.pixee.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * reperents a kind of AST node, a building block of a language. Has a name
 * plus typed properties (plus additional info in the future).
 */
public class ConceptDescriptor {

    public final String name;
    private final List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
    private LanguageDescriptor language;
    private List<ConceptType> baseConcepts = new ArrayList<>();
    private boolean isAbstract = false;

    public ConceptDescriptor(String name) {
        this.name = name;
    }

    public ConceptDescriptor markAbstract() {
        this.isAbstract = true;
        return this;
    }


    public ConceptDescriptor add(PropertyDescriptor pd) {
        properties.add(pd);
        return this;
    }

    public ConceptDescriptor add(String name, ConceptDescriptor cd) {
        properties.add(new PropertyDescriptor(name, cd));
        return this;
    }

    public ConceptDescriptor add(String name, Type type) {
        properties.add(new PropertyDescriptor(name, type));
        return this;
    }

    public ConceptDescriptor add(String name, ConceptDescriptor cd, boolean isList) {
        properties.add(new PropertyDescriptor(name, cd.getType(), isList));
        return this;
    }

    public ConceptDescriptor base(ConceptDescriptor base) {
        baseConcepts.add(new ConceptType(base));
        return this;
    }

    public Collection<ConceptDescriptor> baseConcepts() {
        return baseConcepts.stream().map(ConceptType::concept).toList();
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
        return properties.stream().filter(prop -> prop.name.equals(name)).findFirst().orElse(null);
    }

    public Collection<PropertyDescriptor> properties() {
        return this.properties;
    }

    public Collection<PropertyDescriptor> primitiveProperties() {
        return this.properties.stream().filter(it -> it.type instanceof PrimitiveType).toList();
    }

    public Collection<PropertyDescriptor> nodeProperties() {
        return this.properties.stream().filter(it -> it.type instanceof ConceptType).toList();
    }
}
