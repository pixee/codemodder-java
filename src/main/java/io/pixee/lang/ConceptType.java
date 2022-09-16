package io.pixee.lang;

/**
 * represents a concept as a type so it can be used as the
 * type of a property
 */
public class ConceptType extends Type {

    private final ConceptDescriptor concept;

    public ConceptType(ConceptDescriptor cd) {
        concept = cd;
    }

    @Override
    public String toString() {
        return "concept:" + concept.name;
    }
}
