package io.pixee.codetl.helloWorld;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.codetl.base.HelloWordEnd2EndTrafoTest;
import io.pixee.lang.PrimitiveType;
import io.pixee.languages.helloworld.HelloWorldLanguage;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

final class HelloWorldTransformationTest extends HelloWordEnd2EndTrafoTest {

    @Test
    void it_transforms_simple() {
        String helloWorldCode = """
                var x = 10
                var y = 20
                """;

        String ruleCode = """
                rule pixee:helloworld/stuff
                match
                    Variable {}
                replace $n
                    Variable {
                        name = "a"
                        initial = NumLit {
                            value = 10
                        }
                    }
                """;

        String res = transformCodeWithRuleToString(helloWorldCode, ruleCode);

        assertThat(res, is("""
                Program {
                  variables: Variable {
                    name: a
                    initial: NumLit {
                      value: 10
                    }
                  }
                  variables: Variable {
                    name: a
                    initial: NumLit {
                      value: 10
                    }
                  }
                }
                """));
    }


    @Test
    void it_transforms_simple2() {
        String helloWorldCode = """
                var x = 10
                var y = 20
                """;

        String ruleCode = """
                rule pixee:helloworld/stuff
                match
                    Variable {
                        name = "x"
                        initial = NumLit {
                            value = 10
                        }
                    }                
                replace $n
                    Variable {
                        name = "a"
                        initial = NumLit {
                            value = 30
                        }
                    }
                """;

        String res = transformCodeWithRuleToString(helloWorldCode, ruleCode);

        assertThat(res, is("""
                Program {
                  variables: Variable {
                    name: a
                    initial: NumLit {
                      value: 30
                    }
                  }
                  variables: Variable {
                    name: y
                    initial: NumLit {
                      value: 20
                    }
                  }
                }
                """));
    }




    @Test
    void it_makes_programs() {
        CodeUnit codeUnit = makeProgram();
        assertThat(codeUnit, is(not(nullValue())));
    }

    static CodeUnit makeProgram() {
        HelloWorldLanguage hw = HelloWorldLanguage.INSTANCE;
        CodeUnit code = new CodeUnit(hw.LANG, new Node(hw.PROGRAM));
        Node varX = new Node(hw.VAR)
                .add("initial", new Node(hw.NUM_LIT)
                        .add("value", new Value(PrimitiveType.STRING, "10")));
        varX.add("name", new Value(PrimitiveType.STRING, "x"));
        Node varY = new Node(hw.VAR)
                .add("initial", new Node(hw.NUM_LIT)
                        .add("value", new Value(PrimitiveType.STRING, "20")));
        varY.add("name", new Value(PrimitiveType.STRING, "y"));

        code.root.add("variables", varX);
        code.root.add("variables", varY);
        return code.resolve();
    }
}
