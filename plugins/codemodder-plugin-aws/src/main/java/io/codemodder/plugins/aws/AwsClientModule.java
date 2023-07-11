package io.codemodder.plugins.aws;

import com.google.inject.AbstractModule;
import software.amazon.awssdk.services.translate.TranslateClient;

/** Provides AWS clients. */
public final class AwsClientModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(TranslateClient.class).toProvider(() -> TranslateClient.builder().build());
  }
}
