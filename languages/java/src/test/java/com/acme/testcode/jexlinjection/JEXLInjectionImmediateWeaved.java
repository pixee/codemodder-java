package com.acme.testcode.jexlinjection;

import io.openpixee.security.UnwantedTypes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlSandbox;

public final class JEXLInjectionImmediateWeaved {

  public void method(Socket socket) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      String input = "" + reader.read();
      JexlContext context = new MapContext();
      JexlSandbox sandbox = new JexlSandbox(true);
      for (String cls : UnwantedTypes.all()) {
        sandbox.block(cls);
      }
      new JexlBuilder().sandbox(sandbox).create().createExpression(input).evaluate(context);
    }
  }
}
