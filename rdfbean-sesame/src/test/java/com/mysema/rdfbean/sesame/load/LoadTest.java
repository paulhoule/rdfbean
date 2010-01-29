/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.sesame.load;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.annotations.ClassMapping;
import com.mysema.rdfbean.annotations.Id;
import com.mysema.rdfbean.annotations.Predicate;
import com.mysema.rdfbean.model.MiniRepository;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.object.DefaultConfiguration;
import com.mysema.rdfbean.object.FlushMode;
import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SessionFactoryImpl;
import com.mysema.rdfbean.sesame.MemoryRepository;
import com.mysema.rdfbean.sesame.NativeRepository;
import com.mysema.rdfbean.sesame.SessionTestBase;

/**
 * SavingTest provides
 *
 * @author tiwe
 * @version $Id$
 */
public class LoadTest extends SessionTestBase{
    
    @ClassMapping(ns=TEST.NS)
    public static class Revision {
        @Predicate
        long svnRevision;
        
        @Predicate
        long created;
                
        @Predicate
        Entity revisionOf;
                                        
    }
    
    @ClassMapping(ns=TEST.NS)
    public static class Entity {        
        @Id
        String id;

        @Predicate
        Document document;
        
        @Predicate
        String text;
                                
    }
    
    @ClassMapping(ns=TEST.NS)
    public static class Document {        
        @Id
        String id;
        
        @Predicate
        String text;
                   
    }
    
    @After
    public void tearDown() throws IOException{
        FileUtils.deleteDirectory(new File("target/native"));
    }
    
    @Test
    public void test() throws IOException{        
        loadTest(new MiniRepository());
        
        // Sesame repositories
        loadTest(new DirectMemoryRepository());
        loadTest(new InferencingMemoryRepository());
        loadTest(new MemoryRepository(null, true));        
        loadTest(new NativeRepository(new File("target/native"), false));
    }        
    
    private void loadTest(Repository repository) throws IOException{
        System.out.println("testing " + repository.getClass().getSimpleName());
        System.out.println();
        
        SessionFactoryImpl sessionFactory = new SessionFactoryImpl(Locale.ENGLISH);
        sessionFactory.setConfiguration(new DefaultConfiguration(Document.class, Entity.class, Revision.class));
        sessionFactory.setRepository(repository);
        sessionFactory.initialize();
        
        Session localSession = sessionFactory.openSession();
        
        try{
            loadTest(localSession, 10);
            loadTest(localSession, 50);
            loadTest(localSession, 100);
            loadTest(localSession, 500);
            loadTest(localSession, 1000);  
        }finally{
            localSession.close();
            sessionFactory.close();
        }            
    }
    
    private void loadTest(Session session, int size){
        session.setFlushMode(FlushMode.MANUAL);
        List<Object> objects = new ArrayList<Object>();
        for (int i = 0; i < size; i++){
            Document document = new Document();
            document.text = UUID.randomUUID().toString();
            objects.add(document);
            
            Entity entity = new Entity();
            entity.document = document;
            entity.text = UUID.randomUUID().toString();
            objects.add(entity);
            
            for (int created : Arrays.asList(1,2,3,4,5,6)){
                Revision rev = new Revision();
                rev.svnRevision = 1;
                rev.revisionOf = entity;
                rev.created = created;
                objects.add(rev);            
            }   
        }
        
        long t1 = System.currentTimeMillis();
        for (Object o : objects){
            session.save(o);
        }
        long t2 = System.currentTimeMillis();
        session.flush();
        long t3 = System.currentTimeMillis();
        System.out.println("  Save of " + objects.size() + " objects took " + (t2-t1)+"ms");
        System.out.println("  Flush took " + (t3-t2)+"ms");
        System.out.println();
    }

}