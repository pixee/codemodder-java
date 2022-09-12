package io.pixee.codetl.java;

import java.util.HashMap;

public final class JavaDSLResult {
    public String rule;
    public ASTNode match;
    public ASTNode replace;
    
    public JavaDSLResult() {
    }
    
    public void setRule(String rule) {
    	this.rule = rule;
    }
    
    public void setMatch(String nodeType, String variableName, HashMap<String, String> properties) {
    	this.match = new ASTNode(nodeType, variableName, properties);
    }
    
    public void setReplace(String nodeType, String variableName, HashMap<String, String> properties) {
    	this.replace = new ASTNode(nodeType, variableName, properties);
    }
}