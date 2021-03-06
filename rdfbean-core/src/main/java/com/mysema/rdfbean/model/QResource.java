package com.mysema.rdfbean.model;

/**
 * @author tiwe
 * 
 * @param <T>
 */
public class QResource<T extends ID> extends QNODE<T> {

    private static final long serialVersionUID = -5812253741051256616L;

    public QResource(Class<T> type, String variable) {
        super(type, variable);
    }

    public PatternBlock a(Object type) {
        return Blocks.pattern(this, RDF.type, type);
    }

    public PatternBlock a(Object type, Object context) {
        return Blocks.pattern(this, RDF.type, type, context);
    }

    public PatternBlock has(Object predicate, Object object) {
        return Blocks.pattern(this, predicate, object);
    }

    public PatternBlock has(Object predicate, Object object, Object context) {
        return Blocks.pattern(this, predicate, object, context);
    }

}
