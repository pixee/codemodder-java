package io.pixee.lang;

public class PrimitiveType extends Type {

    private String name;

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
