/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.rdfbean.domains;

import java.util.Date;

import com.mysema.rdfbean.annotations.ClassMapping;
import com.mysema.rdfbean.annotations.Id;
import com.mysema.rdfbean.annotations.Predicate;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.IDType;

public interface LiteralsDomain {

    @ClassMapping
    public static class Literals {

        @Id(IDType.RESOURCE)
        public ID id;

        @Predicate
        public int intValue;

        @Predicate
        public long longValue;

        @Predicate
        public double doubleValue;

        @Predicate
        public float floatValue;

        @Predicate
        public byte byteValue;

        @Predicate
        public short shortValue;

        @Predicate
        public Date dateValue;

        @Predicate
        public String stringValue;

        @Predicate
        public boolean booleanValue;

        public ID getId() {
            return id;
        }

        public int getIntValue() {
            return intValue;
        }

        public long getLongValue() {
            return longValue;
        }

        public double getDoubleValue() {
            return doubleValue;
        }

        public float getFloatValue() {
            return floatValue;
        }

        public byte getByteValue() {
            return byteValue;
        }

        public short getShortValue() {
            return shortValue;
        }

        public Date getDateValue() {
            return dateValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public boolean isBooleanValue() {
            return booleanValue;
        }

    }

}
