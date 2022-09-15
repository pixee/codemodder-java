package io.pixee.meta;

public class PropertyDescriptor {

    private final String name;
    private final boolean isList;
    private Type type;

    public PropertyDescriptor(String name, Type type) {
        this.name = name;
        this.type = type;
        this.isList = false;
    }

    public PropertyDescriptor(String name, Type type, boolean isList) {
        this.name = name;
        this.type = type;
        this.isList = isList;
    }

}
