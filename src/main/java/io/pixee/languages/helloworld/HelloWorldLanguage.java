package io.pixee.languages.helloworld;

import io.pixee.lang.*;

public class HelloWorldLanguage {

    public final LanguageDescriptor LANG;
    public final ConceptDescriptor NUM_LIT;
    public final ConceptDescriptor VAR;
    public final ConceptDescriptor PROGRAM;

    private HelloWorldLanguage() {
        LANG = new LanguageDescriptor("HelloWorld");
        NUM_LIT = LANG.newConcept("NumLit").add(new PropertyDescriptor("value", PrimitiveType.STRING));
        VAR = LANG.newConcept("Var").add(new PropertyDescriptor("initial", NUM_LIT.getType())).add(new PropertyDescriptor("name", PrimitiveType.STRING));
        PROGRAM = LANG.newConcept("Program").add(new PropertyDescriptor("variables", VAR.getType(),true));
    }

    public static final HelloWorldLanguage INSTANCE = new HelloWorldLanguage();

}
