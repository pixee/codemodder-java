package io.pixee.codetl.java;

import java.util.Map;

public final class ASTNode {
    public String nodeType; // ASTNodeType
    public String type;
    public String variableName;
    public Map<String, String> attributes;

    public ASTNode(String nodeType, String variableName, Map<String, String> attributes) {
        this.nodeType = nodeType;
        this.variableName = variableName;
        this.attributes = attributes;
    }
}
