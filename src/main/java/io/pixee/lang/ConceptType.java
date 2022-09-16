package io.pixee.lang;

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
