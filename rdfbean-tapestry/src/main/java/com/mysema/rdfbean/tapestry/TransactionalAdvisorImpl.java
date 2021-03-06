/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.rdfbean.tapestry;

import java.lang.reflect.Method;

import org.apache.tapestry5.ioc.MethodAdvice;
import org.apache.tapestry5.ioc.MethodAdviceReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.NotTransactional;
import org.springframework.transaction.annotation.Transactional;

import com.mysema.rdfbean.model.RDFBeanTransaction;
import com.mysema.rdfbean.object.FlushMode;
import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SessionFactory;
import com.mysema.rdfbean.object.SimpleSessionContext;

/**
 * @author tiwe
 */
public class TransactionalAdvisorImpl implements TransactionalAdvisor {

    static final Logger logger = LoggerFactory.getLogger(TransactionalAdvisorImpl.class);

    private final MethodAdvice advice;

    private final SimpleSessionContext sessionContext;

    public TransactionalAdvisorImpl(SessionFactory sessionFactory) {
        this.sessionContext = new SimpleSessionContext(sessionFactory);
        this.advice = new TransactionalMethodAdvice(this, sessionContext);
        sessionFactory.setSessionContext(sessionContext);
    }

    @SuppressWarnings("unchecked")
    public void addTransactionCommitAdvice(MethodAdviceReceiver receiver) {
        if (receiver.getInterface().getAnnotation(Transactional.class) != null) {
            Transactional annotation = (Transactional) receiver.getInterface().getAnnotation(Transactional.class);
            if (isIntercepted(annotation)) {
                for (Method m : receiver.getInterface().getMethods()) {
                    if (m.getAnnotation(NotTransactional.class) == null) {
                        receiver.adviseMethod(m, advice);
                    }
                }
            }

        } else {
            for (Method m : receiver.getInterface().getMethods()) {
                if (m.getAnnotation(Transactional.class) != null) {
                    Transactional annotation = m.getAnnotation(Transactional.class);
                    if (isIntercepted(annotation)) {
                        receiver.adviseMethod(m, advice);
                    }
                }
            }
        }

    }

    private boolean isIntercepted(Transactional annotation) {
        switch (annotation.propagation()) {
        case NOT_SUPPORTED:
        case NEVER:
        case SUPPORTS:
            return false;
        default:
            return true;
        }

    }

    public RDFBeanTransaction doBegin(Session session) {
        RDFBeanTransaction txn = session.beginTransaction(
                false, // not readonly
                -1, // default timeout
                -1); // default isolation

        session.setFlushMode(FlushMode.COMMIT);
        return txn;
    }

    public void doCommit(Session session, RDFBeanTransaction txn) {
        RuntimeException commitException = null;
        try {
            session.flush();
            txn.commit();

        } catch (RuntimeException re) {
            doRollback(txn);
            commitException = re;
        }

        if (commitException != null) {
            throw commitException;
        }
    }

    public void doRollback(RDFBeanTransaction txn) {
        txn.rollback();
    }
}
