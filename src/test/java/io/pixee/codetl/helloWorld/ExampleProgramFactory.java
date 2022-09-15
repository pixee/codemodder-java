package io.pixee.codetl.helloWorld;

import io.pixee.ast.Child;
import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.meta.LanguageDescriptor;
import io.pixee.meta.PrimitiveType;

public class ExampleProgramFactory {

    static CodeUnit makeProgram() {
        HelloWorldLanguage hw = new HelloWorldLanguage();
        CodeUnit code = new CodeUnit(hw.DESCRIPTOR, new Node(hw.PROGRAM));
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
