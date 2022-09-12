package io.pixee.codetl.java;

import java.util.HashMap;

public final class ASTNode {
    public String nodeType; // ASTNodeType
    public String type;
    public String variableName;
    public String target;
    public String name;

    public ASTNode(String nodeType, String variableName, HashMap<String, String> properties) {
        this.nodeType = nodeType;
        this.variableName = variableName;

//        for (HashMap.Entry<String, String> set: properties.entrySet()) {
//        	switch(set.getKey()) {
//        		case "target":
//        			this.target = set.getValue();
//        			break;
//        		case "name":
//        			this.name = set.getValue();
//        			break;
//        		default:
//        			break;
//        	}
//        }
    }
}