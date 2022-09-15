package io.pixee.codetl.helloWorld;

import io.pixee.meta.*;

public class HelloWorldLanguage {

    public final LanguageDescriptor DESCRIPTOR;
    public final ConceptDescriptor NUM_LIT;
    public final ConceptDescriptor VAR;
    public final ConceptDescriptor PROGRAM;

    public HelloWorldLanguage() {
        DESCRIPTOR = new LanguageDescriptor("HelloWorld");
        NUM_LIT = DESCRIPTOR.newConcept("NumLit").add(new PropertyDescriptor("value", PrimitiveType.STRING));
        VAR = DESCRIPTOR.newConcept("Var").add(new PropertyDescriptor("initial", NUM_LIT.getType())).add(new PropertyDescriptor("name", PrimitiveType.STRING));
        PROGRAM = DESCRIPTOR.newConcept("Program").add(new PropertyDescriptor("variables", VAR.getType(),true));
    }

}
