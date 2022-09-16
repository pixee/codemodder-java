package io.pixee.lang;

/**
 * Collection of primitive types for properties
 */
public class PrimitiveType extends Type {

    private final String name;

    public PrimitiveType(String name) {
        this.name = name;
    }

    public static PrimitiveType INTEGER = new PrimitiveType("integer");
    public static PrimitiveType STRING = new PrimitiveType("string");

    @Override
    public String toString() {
        return name;
    }
}
