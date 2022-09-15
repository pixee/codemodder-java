package io.pixee.codetl.helloWorld;

import io.pixee.ast.CodeUnit;

public class HelloWorldPlayground {

    public static void main(String[] args) {
        CodeUnit program = ExampleProgramFactory.makeProgram();
        System.err.println(program.toString());
    }

}
