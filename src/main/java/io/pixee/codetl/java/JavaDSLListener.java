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
    @Override
    public void enterRule_id(DSLParser.Rule_idContext ctx) {
        System.out.println("enterRule_id::" + ctx.id.getText());
    }
    
    @Override
    public void exitRule_id(DSLParser.Rule_idContext ctx) {
    	System.out.println("exitRule_id::" + ctx.id.getText());
    }
    
    @Override
    public void enterMatch(DSLParser.MatchContext ctx) {
    	System.out.println("enterMatch::" + ctx.getText());
    }
	
	@Override
	public void exitMatch(DSLParser.MatchContext ctx) {
		System.out.println("exitMatch::" + ctx.getText());
	}
	
	@Override public void enterReplace(DSLParser.ReplaceContext ctx) {
		var text = ctx.getText().split("with")[1].strip();
		LOG.info("enterReplace::{}", text);
	}

    private static final Logger LOG = LogManager.getLogger(JavaDSLListener.class);
}