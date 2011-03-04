package com.mysema.rdfbean.model;


/**
 * @author tiwe
 *
 */
public class QID extends QNODE<ID>{

    private static final long serialVersionUID = -2696989113637909131L;

    public QID(String variable) {
        super(ID.class, variable);
    }

    public PatternBlock a(Object type){
        return Blocks.pattern(this, RDF.type, type);
    }

    public PatternBlock has(Object predicate, Object object){
        return Blocks.pattern(this, predicate, object);
    }



}
