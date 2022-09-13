package io.pixee.codetl.java;

import io.pixee.dsl.java.DSLParser;
import io.pixee.dsl.java.DSLBaseListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.HashMap;
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

   		this.result.setRule(rule);
   		
   		LOG.info("Rule is extracted: {}", rule);
    }
	
	@Override
	public void enterMatch(DSLParser.MatchContext ctx) {
		String matchText = ctx.getText().split("match")[1].strip();
		
		var matchReplaceCtx = ctx.match_replace();
        String nodeType = matchReplaceCtx.id.getText();
        String variableName = "";
        Map<String, String> attributes = getAttributes(matchReplaceCtx);
		
		this.result.setMatch(nodeType, variableName, attributes);
		
		LOG.info("Match is extracted:{}", matchText);
	}
	
	@Override public void enterReplace(DSLParser.ReplaceContext ctx) {
		String matchText = ctx.getText().split("with")[1].strip();

		var matchReplaceCtx = ctx.match_replace();
        String nodeType = matchReplaceCtx.id.getText();
        String variableName = "";
        Map<String, String> attributes = getAttributes(matchReplaceCtx);

		this.result.setReplace(nodeType, variableName, attributes);
		
		LOG.info("Replace is extracted: {}", matchText);
	}
	
	@NotNull
    private Map<String, String> getAttributes(DSLParser.Match_replaceContext matchReplaceCtx) {
        List<String> names = matchReplaceCtx
                .name
                .stream()
                .map(Token::getText)
                .collect(Collectors.toList());

        List<String> values = matchReplaceCtx
                .value
                .stream()
                .map(RuleContext::getText)
                .collect(Collectors.toList());

        return IntStream
                .range(0, names.size())
                .boxed()
                .collect(Collectors.toMap(names::get, values::get));
    }
	
	private static final Logger LOG = LogManager.getLogger(JavaDSLListener.class);
}
