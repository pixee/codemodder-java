package io.pixee.codetl.java;

public final class ASTNodeTypes {

    private ASTNodeTypes() {
        // restrict instantiation
    }

    public static final String Statement = "Statement";
    public static final String ConstructorCall = "ConstructorCall";
    public static final String StaticMethodCall = "StaticMethodCall";
    public static final String InstanceMethodCall = "InstanceMethodCall";
}
