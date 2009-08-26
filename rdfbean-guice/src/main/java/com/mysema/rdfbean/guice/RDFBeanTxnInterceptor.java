package com.mysema.rdfbean.guice;

import java.lang.reflect.Method;

import net.jcip.annotations.ThreadSafe;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.mysema.rdfbean.object.*;


/**
 * RDFBeanTxnInterceptor provides
 *
 * @author tiwe
 * @version $Id$
 *
 */
@ThreadSafe
public class RDFBeanTxnInterceptor implements MethodInterceptor, SessionContext{ 
    
    private SimpleSessionContext sessionContext;

    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Session session = sessionContext.getOrCreateSession();

        // allow silent joining of enclosing transactional methods 
        // (NOTE: this ignores the current method's txn-al settings)
        if (session.getTransaction() != null && session.getTransaction().isActive()){
            return methodInvocation.proceed();
        }            

        // read out transaction settings
        Transactional transactional = readTransactionMetadata(methodInvocation);

        // read-only txn?
        FlushMode savedFlushMode = session.getFlushMode();
        if (TransactionType.READ_ONLY.equals(transactional.type())){
            session.setFlushMode(FlushMode.MANUAL);
        }            

        try {
            //no transaction already started, so start one and enforce its semantics
            RDFBeanTransaction txn = session.beginTransaction();
            Object result;

            try {
                result = methodInvocation.proceed();

            } catch(Exception e) {

                // commit transaction only if rollback didnt occur
                if (rollbackIfNecessary(transactional, e, txn))
                    txn.commit();

                // propagate whatever exception is thrown anyway
                throw e;
            }

            // everything was normal so commit the txn 
            // (do not move into try block as it interferes with the advised 
            // method's throwing semantics)
            Exception commitException = null;
            try {
                txn.commit();
            } catch(RuntimeException re) {
                txn.rollback();
                commitException = re;
            }

            // propagate anyway
            if (commitException != null){
                throw commitException;
            }
                

            // or return result
            return result;
        } finally {

            // if read-only txn, then restore flushmode, default is automatic flush
            // if (session.isOpen() && TransactionType.READ_ONLY.equals(transactional.type())){
            if (TransactionType.READ_ONLY.equals(transactional.type())){
                session.setFlushMode(savedFlushMode);
            }
            
            sessionContext.releaseSession();
                
        }
    }

    private Transactional readTransactionMetadata(MethodInvocation methodInvocation) {
        Transactional transactional;
        Method method = methodInvocation.getMethod();

        // try the class if there's nothing on the method 
        // (only go up one level in the hierarchy, to skip the proxy)
        Class<?> targetClass = methodInvocation.getThis().getClass().getSuperclass();

        if (method.isAnnotationPresent(Transactional.class)){
            transactional = method.getAnnotation(Transactional.class);
        }else if (targetClass.isAnnotationPresent(Transactional.class)){
            transactional = targetClass.getAnnotation(Transactional.class);
        }else{
            //if there is no transactional annotation of Warp's present, use the default
            transactional = Internal.class.getAnnotation(Transactional.class);   
        }   
        return transactional;
    }
    
    private boolean rollbackIfNecessary(Transactional transactional, Exception e, 
            RDFBeanTransaction txn) {
        boolean commit = true;
        
        //check rollback clauses
        for (Class<? extends Exception> rollBackOn : transactional.rollbackOn()) {
            //if one matched, try to perform a rollback
            if (rollBackOn.isInstance(e)) {
                commit = false;
                
                // check exceptOn clauses (supercedes rollback clause)
                for (Class<? extends Exception> exceptOn : transactional.exceptOn()) {
                    // An exception to the rollback clause was found, DONT rollback 
                    // (i.e. commit and throw anyway)
                    if (exceptOn.isInstance(e)) {
                        commit = true;
                        break;
                    }
                }
                //rollback only if nothing matched the exceptOn check
                if (!commit) {
                    txn.rollback();
                }
                //otherwise continue to commit
                break;
            }
        }

        return commit;
    }
    
    /**
     * Return the Session associated to the active transaction or <code>null</code> 
     * if no transaction is active
     */
    @Override
    public Session getCurrentSession() {        
        return sessionContext.getCurrentSession();
    }

    @Inject
    public void setSessionFactory(SessionFactory sessionFactory){
        this.sessionContext = new SimpleSessionContext(sessionFactory);
        ((SessionFactoryImpl)sessionFactory).setSessionContext(this);
    }

    @Transactional
    private static class Internal { }

}