package io.pixee.languages.java;

import io.pixee.lang.ConceptDescriptor;
import io.pixee.lang.LanguageDescriptor;
import io.pixee.lang.PrimitiveType;
import io.pixee.lang.PropertyDescriptor;

/**
 * Defines the structure of the Hello World language
 */
public class JavaLanguage {

    public final LanguageDescriptor LANG;

    public final ConceptDescriptor METHOD;
    public final ConceptDescriptor STATEMENT_LIST;
    public final ConceptDescriptor STATEMENT;
    public final ConceptDescriptor EXPRESSION;
    public final ConceptDescriptor INTEGER_LIT;
    public final ConceptDescriptor INTEGER_TYPE;
    public final ConceptDescriptor TYPE;
    public final ConceptDescriptor LOCAL_VAR_DECL_STATEMENT;

    private JavaLanguage() {
        LANG = new LanguageDescriptor("Java");

        TYPE = LANG.newConcept("Type").markAbstract();
        INTEGER_TYPE = LANG.newConcept("IntType").base(TYPE);
        EXPRESSION = LANG.newConcept("Expression").markAbstract();
        INTEGER_LIT = LANG.newConcept("IntLit").base(EXPRESSION).add("value", PrimitiveType.INTEGER);

        METHOD = LANG.newConcept("Method");

        STATEMENT_LIST = LANG.newConcept("StatementList");
        STATEMENT = LANG.newConcept("Statement");
        LOCAL_VAR_DECL_STATEMENT = LANG.newConcept("LocalVarDecl").base(STATEMENT);

        METHOD.add("body", STATEMENT_LIST);
        STATEMENT_LIST.add("statements", STATEMENT, true);
        LOCAL_VAR_DECL_STATEMENT
                .add("name", PrimitiveType.STRING)
                .add("type", TYPE)
                .add("init", EXPRESSION);

    }

    public static final JavaLanguage INSTANCE = new JavaLanguage();

}
