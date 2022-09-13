package io.pixee.codetl.java;

import java.util.Map;

public final class JavaDSLResult {
    public String rule;
    public ASTNode match;
    public ASTNode replace;
    
    public JavaDSLResult() {
    }
    
    public void setRule(String rule) {
    	this.rule = rule;
    }
    
    public void setMatch(String nodeType, String variableName, Map<String, String> attributes) {
    	this.match = new ASTNode(nodeType, variableName, attributes);
    }
    
    public void setReplace(String nodeType, String variableName, Map<String, String> attributes) {
    	this.replace = new ASTNode(nodeType, variableName, attributes);
    }
}
