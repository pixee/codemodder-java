package com.acme.testcode.jexlinjection;

import io.github.pixee.security.UnwantedTypes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlSandbox;

public final class JEXLInjectionSimpleWeaved {

  public void method(Socket socket) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      String input = "" + reader.read();
      JexlSandbox sandbox = new JexlSandbox(true);
      for (String cls : UnwantedTypes.all()) {
        sandbox.block(cls);
      }
      JexlEngine jexl = new JexlBuilder().sandbox(sandbox).create();
      JexlExpression expression = jexl.createExpression(input);
      JexlContext context = new MapContext();
      expression.evaluate(context);
    }
  }
}
