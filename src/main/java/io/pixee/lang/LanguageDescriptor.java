package io.pixee.lang;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * represents a language by containing all the concepts in that language
 */
public class LanguageDescriptor {

    private final String name;
    private final Map<String, ConceptDescriptor> concepts = new HashMap<>();

    public LanguageDescriptor(String name) {
        this.name = name;
    }

    public void addConcept(ConceptDescriptor cd) {
        this.concepts.put(cd.name, cd);
        cd.setLanguage(this);
    }

    public ConceptDescriptor newConcept(String name) {
        ConceptDescriptor cd = new ConceptDescriptor(name);
        cd.setLanguage(this);
        this.concepts.put(cd.name, cd);
        return cd;
    }

    public ConceptDescriptor conceptByName(String name) {
        return concepts.get(name);
    }

    public String name() {
        return this.name;
    }

    public String plantUMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        for (ConceptDescriptor cd: concepts.values() ) {
            sb.append("class "+cd.name+" {\n");
            for (PropertyDescriptor attr: cd.primitiveProperties()) {
                sb.append("  " + attr.name + " : " + attr.type.toString() + "\n");
            }
            sb.append("}\n");
        }
        for (ConceptDescriptor cd: concepts.values() ) {
            for (ConceptDescriptor base: cd.baseConcepts()) {
                sb.append(base.name + " <|-- " + cd.name + "\n");
            }
            for (PropertyDescriptor rel: cd.nodeProperties()) {
                String card = rel.isList ? "\"*\"" : "";
                sb.append(cd.name + " *-> " + card + " " + ((ConceptType) rel.type).concept().name+ " : " + rel.name + "\n");
            }
        }
        sb.append("@enduml\n");
        return sb.toString();
    }

}
