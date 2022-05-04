package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.Optional;
import java.util.function.Predicate;

final class SSLProtocols {

  private SSLProtocols() {}

  static final String safeTlsVersion = "TLSv1.2";

  static Predicate<StringLiteralExpr> isUnsafeStringLiteral =
      new Predicate<StringLiteralExpr>() {
        @Override
        public boolean test(final StringLiteralExpr literal) {
          return !safeTlsVersion.equals(literal.asString());
        }
      };

  static Predicate<NameExpr> isUnsafeStringVariable =
      new Predicate<NameExpr>() {
        @Override
        public boolean test(final NameExpr variableName) {
          ClassOrInterfaceDeclaration holdingType = ASTs.findTypeFrom(variableName);
          if (holdingType != null) {
            Optional<FieldDeclaration> field =
                holdingType.getFieldByName(variableName.getNameAsString());
            if (field.isPresent()) {
              FieldDeclaration fieldDeclaration = field.get();
              NodeList<VariableDeclarator> variables = fieldDeclaration.getVariables();
              // if it's more than 1, i'm not sure what to do...
              if (variables.size() == 1) {
                VariableDeclarator variableDeclarator = variables.get(0);
                final Type stringType1 = new ClassOrInterfaceType("java.lang.String");
                final Type stringType2 = new ClassOrInterfaceType("String");

                Type variableType = variableDeclarator.getType();
                if (stringType1.equals(variableType) || stringType2.equals(variableType)) {
                  if (variableName.toString().equals(variableDeclarator.getName().toString())) {
                    if (variableDeclarator.getInitializer().isPresent()) {
                      Expression variableExpression = variableDeclarator.getInitializer().get();
                      return variableExpression.isStringLiteralExpr()
                          && isUnsafeStringLiteral.test(variableExpression.asStringLiteralExpr());
                    }
                  }
                }
              }
            }
          }
          return false;
        }
      };

  static Predicate<ArrayCreationExpr> isUnsafeConstantStringArray =
      new Predicate<ArrayCreationExpr>() {
        @Override
        public boolean test(final ArrayCreationExpr arrayCreationExpr) {
          if (arrayCreationExpr.getInitializer().isPresent()) {
            NodeList<Expression> values = arrayCreationExpr.getInitializer().get().getValues();
            if (values.size() == 0) {
              // don't know what to do here -- probably unsafe?
              return true;
            } else if (values.size() > 1) {
              // should only be one -- the safe one
              return true;
            }
            // ok, we've confirmed it's one, let's make sure it's the safe one
            Expression protocol = values.get(0);
            if (protocol.isStringLiteralExpr()) {
              return isUnsafeStringLiteral.test(protocol.asStringLiteralExpr());
            } else if (protocol.isNameExpr()) {
              return isUnsafeStringVariable.test(protocol.asNameExpr());
            }
          }
          return true;
        }
      };

  static Predicate<MethodCallExpr> hasUnsafeArrayArgumentVariable =
      new Predicate<MethodCallExpr>() {
        @Override
        public boolean test(final MethodCallExpr methodCallExpr) {
          NameExpr variableName = methodCallExpr.getArgument(0).asNameExpr();
          ClassOrInterfaceDeclaration holdingType = ASTs.findTypeFrom(variableName);
          if (holdingType != null) {
            Optional<FieldDeclaration> field =
                holdingType.getFieldByName(variableName.getNameAsString());
            if (field.isPresent()) {
              FieldDeclaration fieldDeclaration = field.get();
              NodeList<VariableDeclarator> variables = fieldDeclaration.getVariables();
              // if it's more than 1, i'm not sure what to do...
              if (variables.size() == 1) {
                VariableDeclarator variableDeclarator = variables.get(0);
                final Type stringType1 =
                    new ArrayType(new ClassOrInterfaceType("java.lang.String"));
                final Type stringType2 = new ArrayType(new ClassOrInterfaceType("String"));

                Type variableType = variableDeclarator.getType();
                if (stringType1.equals(variableType) || stringType2.equals(variableType)) {
                  if (variableName.toString().equals(variableDeclarator.getName().toString())) {
                    if (variableDeclarator.getInitializer().isPresent()) {
                      Expression variableExpression = variableDeclarator.getInitializer().get();
                      return variableExpression.isArrayCreationExpr()
                          && isUnsafeConstantStringArray.test(
                              variableExpression.asArrayCreationExpr());
                    }
                  }
                }
              }
            }
          }
          return false;
        }
      };

  static Predicate<MethodCallExpr> hasUnsafeArrayArgument =
      new Predicate<MethodCallExpr>() {
        @Override
        public boolean test(final MethodCallExpr methodCallExpr) {
          ArrayCreationExpr argument = methodCallExpr.getArgument(0).asArrayCreationExpr();
          return isUnsafeConstantStringArray.test(argument);
        }
      };
}
