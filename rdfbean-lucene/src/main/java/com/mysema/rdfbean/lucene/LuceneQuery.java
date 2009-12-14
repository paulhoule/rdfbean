/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.compass.core.CompassHit;
import org.compass.core.CompassHits;
import org.compass.core.CompassSession;
import org.compass.core.Resource;

import com.mysema.commons.lang.Assert;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.object.Session;
import com.mysema.util.EmptyCloseableIterator;

/**
 * LuceneQuery provides
 *
 * @author tiwe
 * @version $Id$
 */
public class LuceneQuery {

    private final CompassSession compassSession;
    
    private final LuceneConfiguration conf;
    
    private String query;
    
    private final Session session;
    
    public LuceneQuery(LuceneConfiguration conf, Session session, CompassSession compassSession) {
        this.conf = Assert.notNull(conf);
        this.session = Assert.notNull(session);
        this.compassSession = Assert.notNull(compassSession);
    }

    private <T> T get(Resource resource, Class<T> clazz){
        // TODO : notify listeners of loaded resource
        ID id = (ID) conf.getConverter().fromString(resource.getId());
        return session.get(clazz, id);
    }

    public <T> CloseableIterator<T> iterate(final Class<T> clazz){
        final CompassHits hits = compassSession.find(query);
        if (hits.length() > 0){
            final Iterator<CompassHit> iterator = hits.iterator();
            return new CloseableIterator<T>(){
                public void close() throws IOException {
                    hits.close();                
                }
                public boolean hasNext() {
                    return iterator.hasNext();
                }
                public T next() {
                    return get(iterator.next().resource(), clazz);
                }
                public void remove() {
                    throw new UnsupportedOperationException();                
                }            
            };    
        }else{
            hits.close();
            return new EmptyCloseableIterator<T>();
        }
        
    }
    
    public <T> List<T> list(Class<T> clazz) {
        CompassHits hits = compassSession.find(query);
        if (hits.length() > 0){
            List<T> results = new ArrayList<T>(hits.length());
            for (int i = 0; i < hits.length(); i++){
                results.add(get(hits.resource(i), clazz));
            }
            hits.close();
            return results;
        }else{
            hits.close();
            return Collections.emptyList();
        }        
    }
    
    public LuceneQuery query(String query) {
        this.query = Assert.notNull(query);
        return this;        
    }

}