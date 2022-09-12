package io.pixee.codetl.java;

import io.pixee.dsl.java.DSLParser;
import io.pixee.dsl.java.DSLBaseListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class JavaDSLListener extends DSLBaseListener {
	public JavaDSLResult result;
	
	public JavaDSLListener() {
		this.result = new JavaDSLResult();
	}
    
    @Override
    public void exitRule_id(DSLParser.Rule_idContext ctx) {
        String rule = ctx.getText().split("/")[1].strip();
   		LOG.info("Rule is extracted: {}", rule);

   		this.result.setRule(rule);
    }
	
	@Override
	public void exitMatch(DSLParser.MatchContext ctx) {
		String matchText = ctx.getText();
		LOG.info("Match is extracted:{}", matchText);

		String[] definitions = matchText.split("{")[0].strip().split(" ");
		String[] contents = matchText.split("{")[1].split("}")[0].split("\n");
		String nodeType;
		String variableName = "";
		HashMap<String, String> properties = new HashMap<String, String>();

		if (definitions.length > 2) {
			nodeType = definitions[0];
			variableName = definitions[1];
		} else {
			nodeType = definitions[0];
		}
		
		for (String temp: contents) {
			properties.put(temp.split("=")[0].strip(), temp.split("=")[1].strip());
		}

		this.result.setMatch(nodeType, variableName, properties);
	}
	
	@Override public void enterReplace(DSLParser.ReplaceContext ctx) {
		String matchText = ctx.getText().split("with")[1].strip();
		LOG.info("Replace is extracted: {}", matchText);

		String[] definitions = matchText.split("{")[0].strip().split(" ");
		String[] contents = matchText.split("{")[1].split("}")[0].split("\n");
		String nodeType;
		String variableName = "";
		HashMap<String, String> properties = new HashMap<String, String>();

		if (definitions.length > 2) {
			nodeType = definitions[0];
			variableName = definitions[1];
		} else {
			nodeType = definitions[0];
		}
		
		for (String temp: contents) {
			properties.put(temp.split("=")[0].strip(), temp.split("=")[1].strip());
		}

		this.result.setReplace(nodeType, variableName, properties);
	}
	
	private static final Logger LOG = LogManager.getLogger(JavaDSLListener.class);
}