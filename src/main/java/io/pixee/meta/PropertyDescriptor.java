package io.pixee.meta;

public class PropertyDescriptor {

    public final String name;
    public final boolean isList;
    public final Type type;

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
