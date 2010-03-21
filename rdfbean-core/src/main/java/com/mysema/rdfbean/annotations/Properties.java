/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mapping of multiple predicates to this property 
 *
 * <p>Note! This annotation is highly experimental. Should not be used in production.</p>
 *
 * @author tiwe
 * @author mala
 * @version $Id$
 *
 */
@Documented
@Target( { METHOD, FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Properties {
    /**
     * include otherwise mapped properties or not
     */
    boolean includeMapped() default false;
    
    /**
     * Include also compatible types.
     * 
     * For instance a map of doubles might also include floats, or supertype as Number 
     * could contain all kind of number types
     * 
     */
    boolean includeCompatible() default false;
    
    /**
     * True if invalid values should be ignored.
     */
    boolean ignoreInvalid() default false;
}
