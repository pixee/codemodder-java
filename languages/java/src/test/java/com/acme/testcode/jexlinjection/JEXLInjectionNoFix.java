package com.acme.testcode.jexlinjection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

public final class JEXLInjectionNoFix {

  public void method(Socket socket, JexlEngine jexl) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      String input = reader.readLine();
      JexlExpression expression = jexl.createExpression(input);
      JexlContext context = new MapContext();
      expression.evaluate(context);
    }
  }
}
