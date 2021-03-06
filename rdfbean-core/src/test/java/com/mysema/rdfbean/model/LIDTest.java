/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.rdfbean.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LIDTest {

    private LID lid = new LID("1");

    @Test
    public void HashCode() {
        LID lid2 = new LID("1");
        assertEquals(lid, lid2);
    }

    @Test
    public void EqualsObject() {
        LID lid2 = new LID("1");
        assertEquals(lid.hashCode(), lid2.hashCode());
    }

}
