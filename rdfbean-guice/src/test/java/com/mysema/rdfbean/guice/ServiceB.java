package com.mysema.rdfbean.guice;

/**
 * ServiceB provides
 *
 * @author tiwe
 * @version $Id$
 */
public interface ServiceB {

    @Transactional
    void txMethod();

    @Transactional(type=TransactionType.READ_ONLY)
    void txReadonly();
    
    void nonTxMethod();
}
