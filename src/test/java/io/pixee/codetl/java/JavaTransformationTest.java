package io.pixee.codetl.java;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.codetl.CodeTLRuleDefinition;
import io.pixee.codetl.base.HelloWordEnd2EndTrafoTest;
import io.pixee.lang.PrimitiveType;
import io.pixee.languages.helloworld.HelloWorldLanguage;
import io.pixee.languages.java.JavaLanguage;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

final class JavaTransformationTest extends HelloWordEnd2EndTrafoTest {

    @Test
    void it_transforms_simple() {
        System.err.println(JavaLanguage.INSTANCE.LANG.plantUMLString());


        String ruleCode = """
                rule pixee:java/stuff
                match
                    LocalVarDecl {}
                replace $n
                    LocalVarDecl {
                        name = "hello"
                        type = IntType {}
                        init = IntLit {value = 10}
                    }
                """;

        CodeTLRuleDefinition r = parseCodeTL(ruleCode);
        System.err.println(r.getNodeToMatch().dump(""));
        System.err.println(r.getReplacementNode().dump(""));

    }

}
