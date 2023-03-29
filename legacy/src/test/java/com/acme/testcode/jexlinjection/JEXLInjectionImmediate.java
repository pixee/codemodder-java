package com.acme.testcode.jexlinjection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;

public final class JEXLInjectionImmediate {

  public void method(Socket socket) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      String input = "" + reader.read();
      JexlContext context = new MapContext();
      new JexlBuilder().create().createExpression(input).evaluate(context);
    }
  }
}
