package io.codemodder;

import io.codemodder.codetf.CodeTFAiMetadata;

/** Marks a class so that it can provide {@link CodeTFAiMetadata} to its consumers. */
public interface AIMetadataProvider {

  /**
   * @return the {@link CodeTFAiMetadata} describing how a class used an AI service.
   */
  CodeTFAiMetadata codeTFAiMetadata();
}
