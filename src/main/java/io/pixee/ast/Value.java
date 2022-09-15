package io.pixee.ast;

import io.pixee.meta.PrimitiveType;

public class Value extends Data {

    public final PrimitiveType type;
    public final Object data;

    public Value(PrimitiveType type, Object data) {
        this.type = type;
        this.data = data;
    }

}
