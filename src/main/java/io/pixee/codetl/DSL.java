package io.pixee.codetl;

import io.pixee.codefixer.java.VisitorFactory;

public interface DSL {
	VisitorFactory parse(String input);
}
