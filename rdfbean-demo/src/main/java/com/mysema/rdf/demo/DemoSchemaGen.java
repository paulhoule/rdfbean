/*
 * Copyright (c) 2009 Mysema Ltd & Ostajanmarkkinat.com
 * All rights reserved.
 * 
 */
package com.mysema.rdf.demo;

import java.io.IOException;

import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.store.StoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mysema.commons.lang.Assert;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.object.identity.IdentityService;
import com.mysema.rdfbean.sesame.SesameSchemaGen;

/**
 * BMSchemaGen provides
 *
 * @author tiwe
 * @version $Id$
 */
public class DemoSchemaGen {
    
    public static void main(String[] args) throws StoreException, RDFHandlerException, RDFParseException, IOException {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("classpath:/persistence.xml");
        DemoSchemaGen schemaGen = new DemoSchemaGen((Configuration) appContext.getBean("configuration"));
        schemaGen.generateBMSchema();
    }
    
    private Configuration configuration;
    
    public DemoSchemaGen(Configuration configuration) {
        this.configuration = Assert.notNull(configuration);
    }

    public void generateBMSchema() throws RDFHandlerException, RDFParseException, IOException, StoreException {
        new SesameSchemaGen()
        .setNamespace("demo", DEMO.NS)
        .setOntology(DEMO.CONTEXT)
        .addExportNamespace(DEMO.NS)
        .generateRDFXML(configuration, System.out);
    }

}