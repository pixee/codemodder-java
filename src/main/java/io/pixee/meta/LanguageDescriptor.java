package io.pixee.meta;


import java.util.ArrayList;
import java.util.List;

public class LanguageDescriptor {

    private String name;
    private List<ConceptDescriptor> concepts = new ArrayList<ConceptDescriptor>();

    public LanguageDescriptor(String name) {
        this.name = name;
    }

    public void addConcept(ConceptDescriptor cd) {
        this.concepts.add(cd);
        cd.setLanguage(this);
    }

    public ConceptDescriptor newConcept(String name) {
        ConceptDescriptor cd = new ConceptDescriptor(name);
        cd.setLanguage(this);
        this.concepts.add(cd);
        return cd;
    }




}
