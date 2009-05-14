/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.model;

import java.io.Serializable;
import java.util.Arrays;

import com.mysema.query.annotations.Entity;

/**
 * @author sasa
 *
 */
@Entity
public abstract class NODE implements Serializable {

    private static final long serialVersionUID = -6921484648846884179L;

    // This is closed api
    NODE() {}
    
    public abstract String getValue();
    
    static boolean nullSafeEquals(Object n1, Object n2) {
        if (n1 == null) {
            if (n2 == null) {
                return true;
            } else {
                return false;
            }
        } else if (n2 == null) {
            return false;
        } else {
            return n1.equals(n2);
        }
    }
    
    public abstract NodeType getNodeType();

    static int hashCode(Object... objects) {
        // XXX implement hashCode algorithm
        return Arrays.asList(objects).hashCode();
    }

    public final boolean isResource() {
        return this instanceof ID;
    }

    public boolean isLiteral() {
        return this instanceof LIT;
    }
}