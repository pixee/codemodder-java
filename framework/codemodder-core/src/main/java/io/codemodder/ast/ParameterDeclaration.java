package io.codemodder.ast;

import com.github.javaparser.ast.body.Parameter;

public class ParameterDeclaration implements LocalDeclaration {

  private Parameter parameter;
  private LocalScope scope;

  public ParameterDeclaration(Parameter parameter) {
    this.parameter = parameter;
    scope = null;
  }

  @Override
  public LocalScope getScope() {
    if (scope == null) {
      scope = LocalScope.fromParameter(parameter);
    }
    return scope;
  }

  @Override
  public String getName() {
    return parameter.getNameAsString();
  }

  @Override
  public Parameter getDeclaration() {
    return parameter;
  }

  @Override
  public String toString() {
    return parameter.toString();
  }
}
