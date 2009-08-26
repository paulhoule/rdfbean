/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.sesame;


import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.openrdf.rio.RDFFormat;
import org.openrdf.store.StoreException;

import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SessionUtil;

/**
 * @author sasa
 *
 */
public class SessionTestBase {
    
    protected static final Locale FI = new Locale("fi");

    protected static final Locale EN = new Locale("en");
    
    protected static final List<Locale> locales = Arrays.asList(FI, EN);
    
    protected static final MemoryRepository repository;
    
    static{
        repository = new MemoryRepository();
        repository.setSources(
                new RDFSource("classpath:/test.ttl", RDFFormat.TURTLE, TEST.NS),
                new RDFSource("classpath:/foaf.rdf", RDFFormat.RDFXML, FOAF.NS)
        );
        repository.initialize();
    }

//    @BeforeClass
//    public static void setup() throws StoreException, RDFParseException, IOException, ClassNotFoundException {
//        if (repository == null) {
//            repository = new MemoryRepository();
//            repository.setSources(
//                    new RDFSource("classpath:/test.ttl", RDFFormat.TURTLE, TEST.NS),
//                    new RDFSource("classpath:/foaf.rdf", RDFFormat.RDFXML, FOAF.NS)
//            );
//            repository.initialize();
//        }
//    }

    protected static Session createSession(Package... packages) throws StoreException, ClassNotFoundException {
        return SessionUtil.openSession(repository, locales, packages);
    }

    protected static Session createSession(Locale locale, Package... packages) throws StoreException, ClassNotFoundException {
        return SessionUtil.openSession(repository, locale, packages);
    }

    protected static Session createSession(Class<?>... classes) throws StoreException {
        return SessionUtil.openSession(repository, locales, classes);
    }

    protected static Session createSession(Locale locale, Class<?>... classes) throws StoreException {
        return SessionUtil.openSession(repository, locale, classes);
    }

}
