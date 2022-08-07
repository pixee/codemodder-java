package io.pixee.codetl.java;

import io.pixee.codefixer.java.VisitorFactory;

public interface DSL {
  VisitorFactory parse(String input);
}
