package io.codemodder;

import com.google.inject.AbstractModule;

/** Binds the XPathStreamProcessor for codemods. */
final class XPathStreamProcessorModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(XPathStreamProcessor.class).to(DefaultXPathStreamProcessor.class);
  }
}
