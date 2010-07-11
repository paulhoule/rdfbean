package com.mysema.rdfbean.rdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.sql.DataSource;

import net.jcip.annotations.Immutable;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.mysema.commons.lang.Assert;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLQueryImpl;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.rdfbean.model.*;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.owl.OWL;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * RDBRepository provides
 *
 * @author tiwe
 * @version $Id$
 */
@Immutable
public class RDBRepository implements Repository{
    
    private final IdFactory idFactory = new MD5IdFactory();
    
    private final BidiMap<NODE,Long> nodeCache = new DualHashBidiMap<NODE,Long>();
    
    private final BidiMap<Locale,Integer> langCache = new DualHashBidiMap<Locale,Integer>();
    
    private final DataSource dataSource;
    
    private final SQLTemplates templates;
    
    private final IdSequence idSequence;
    
    public RDBRepository(
            DataSource dataSource, 
            SQLTemplates templates, 
            IdSequence idSequence) {
        this.dataSource = Assert.notNull(dataSource,"dataSource");
        this.templates = Assert.notNull(templates,"templates");
        this.idSequence = Assert.notNull(idSequence, "idSequence");
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public void execute(Operation operation) {
        RDFConnection connection = openConnection();
        try{
            try{
                RDFBeanTransaction tx = connection.beginTransaction(false, 0, 
                        Connection.TRANSACTION_READ_COMMITTED);
                try{
                    operation.execute(connection);    
                    tx.commit();
                }catch(IOException io){
                    tx.rollback();
                }                
            }finally{
                connection.close();
            }    
        }catch(IOException io){
            throw new RepositoryException(io);
        }
    }

    @Override
    public void export(Format format, OutputStream os) {
        // TODO Auto-generated method stub        
    }

    @Override
    public void initialize() {        
        try {
            initSchema();
            initTables();
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @SuppressWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    private void initSchema() throws IOException, SQLException {
        Connection conn = dataSource.getConnection();
        SQLQuery query = new SQLQueryImpl(conn, templates).from(QLanguage.language);        
        try{
            query.count();
        } catch (Exception e) {
            // TODO make the schema source customizable
            InputStream is = getClass().getResourceAsStream("/h2.sql");
            String schema = IOUtils.toString(is, "UTF-8");
            for (String clause : StringUtils.split(schema, ";")){
                Statement stmt2 = conn.createStatement();
                try{
                    stmt2.execute(clause);    
                }finally{
                    stmt2.close();    
                }
            }
        }
    }
    
    private void initTables() throws IOException {
        RDBConnection conn = openConnection();
        try{
            Set<NODE> nodes = new HashSet<NODE>();
            // common resources
            nodes.addAll(RDF.ALL);
            nodes.addAll(RDFS.ALL);
            nodes.addAll(XSD.ALL);
            nodes.addAll(OWL.ALL);   
            // commons literals
            nodes.add(new LIT(""));
            nodes.add(new LIT("true",XSD.booleanType));
            nodes.add(new LIT("false",XSD.booleanType));
            for (int i = -128; i < 128; i++){
                String str = String.valueOf(i);
                nodes.add(new LIT(str, XSD.byteType));
                nodes.add(new LIT(str, XSD.intType));
                nodes.add(new LIT(str, XSD.longType));
                nodes.add(new LIT(str+".0", XSD.doubleType));
                nodes.add(new LIT(str+".0", XSD.floatType));
            }
            // TODO ontology types and properties
            
            for (NODE node : nodes){
                Long nodeId = conn.addNode(node);
                nodeCache.put(node, nodeId);
            }    
            
            // init languages
            List<Locale> locales = new ArrayList<Locale>(Arrays.asList(Locale.getAvailableLocales()));
            locales.add(new Locale("fi"));
            locales.add(new Locale("sv"));
            for (Locale locale : locales){
                Integer langId = conn.addLang(locale);
                langCache.put(locale, langId);
            }
            
        }finally{
            conn.close();
        }                
    }

    @Override
    public RDBConnection openConnection() {
        try {
            Connection connection = dataSource.getConnection();
            RDBContext context = new RDBContext(idFactory, nodeCache, langCache, idSequence, connection, templates); 
            return new RDBConnection(context);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }        
    }

}