package com.mysema.rdfbean.xsd;

import java.math.BigInteger;

public class BigIntegerConverterTest extends AbstractConverterTest<BigInteger>{

    @Override
    Converter<BigInteger> createConverter() {
	return new BigIntegerConverter();
    }

    @Override
    BigInteger createValue() {
	return BigInteger.ONE;
    }

    
}
