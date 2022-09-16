package io.pixee.ast;

import io.pixee.lang.PrimitiveType;

public class Value extends Data {

    public final PrimitiveType type;
    public final Object data;

    public Value(PrimitiveType type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public String toString() {
        return "value(" + type + "):" + data;
    }
}
