/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.xsd;


/**
 * FloatConverter provides
 *
 * @author tiwe
 * @version $Id$
 */
public class FloatConverter extends AbstractConverter<Float> {

    @Override
    public Float fromString(String str) {
        return Float.valueOf(str);
    }

}
