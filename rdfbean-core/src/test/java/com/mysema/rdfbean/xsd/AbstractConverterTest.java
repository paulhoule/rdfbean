package com.mysema.rdfbean.xsd;

import static org.junit.Assert.*;

import org.junit.Test;

public abstract class AbstractConverterTest<T> {
    
    abstract T createValue();
    
    abstract Converter<T> createConverter();
    
    @Test
    public void test() {
	Converter<T> converter = createConverter();
	T value = createValue();
	String str = converter.toString(value);
	assertEquals(value, converter.fromString(str));
    }

}
