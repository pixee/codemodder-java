package io.pixee.codetl.helloWorld;

import io.pixee.meta.*;

public class HelloWorldLanguage {

    public static final LanguageDescriptor DEF;

    static {
        DEF = new LanguageDescriptor("HelloWorld");
        ConceptDescriptor numlit = DEF.newConcept("NumLit").add(new PropertyDescriptor("value", PrimitiveType.STRING));
        ConceptDescriptor var = DEF.newConcept("Var").add(new PropertyDescriptor("initial", numlit.getType()));
        DEF.newConcept("Program").add(new PropertyDescriptor("variables", var.getType(),true));
    }

}
