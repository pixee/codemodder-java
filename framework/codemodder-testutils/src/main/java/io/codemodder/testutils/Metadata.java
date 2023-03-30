package io.codemodder.testutils;

import io.codemodder.Changer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Metadata {

  Class<? extends Changer> codemodType();

  String testResourceDir();

  String[] dependencies();
}
