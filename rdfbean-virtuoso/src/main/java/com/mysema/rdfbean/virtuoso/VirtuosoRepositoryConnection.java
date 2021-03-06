package com.mysema.rdfbean.virtuoso;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.mysema.commons.l10n.support.LocaleUtil;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.query.QueryMetadata;
import com.mysema.query.types.ParamExpression;
import com.mysema.rdfbean.model.BID;
import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.IdSequence;
import com.mysema.rdfbean.model.InferenceOptions;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.QueryLanguage;
import com.mysema.rdfbean.model.QueryOptions;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFBeanTransaction;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.SPARQLQuery;
import com.mysema.rdfbean.model.SPARQLUpdate;
import com.mysema.rdfbean.model.SPARQLVisitor;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.UpdateLanguage;
import com.mysema.rdfbean.model.io.SPARQLUpdateWriter;
import com.mysema.rdfbean.model.io.TurtleStringWriter;
import com.mysema.rdfbean.object.SQLConnectionCallback;
import com.mysema.rdfbean.owl.OWL;

/**
 * @author tiwe
 * 
 */
public class VirtuosoRepositoryConnection implements RDFConnection {

    private static final Map<QueryLanguage<?, ?>, SPARQLQuery.ResultType> resultTypes = new HashMap<QueryLanguage<?, ?>, SPARQLQuery.ResultType>();

    static {
        resultTypes.put(QueryLanguage.BOOLEAN, SPARQLQuery.ResultType.BOOLEAN);
        resultTypes.put(QueryLanguage.GRAPH, SPARQLQuery.ResultType.TRIPLES);
        resultTypes.put(QueryLanguage.TUPLE, SPARQLQuery.ResultType.TUPLES);
    }

    private static final Logger logger = LoggerFactory.getLogger(VirtuosoRepositoryConnection.class);

    private static final int BATCH_SIZE = 5000;

    private static final String DEFAULT_OUTPUT = "sparql\n ";

    private static final String JAVA_OUTPUT = "sparql define output:format '_JAVA_'\n ";

    private static final String INTERNAL_PREFIX = "http://www.openlinksw.com/";

    private static final String SPARQL_CREATE_GRAPH = "sparql create silent graph iri(??)";

    private static final String SPARQL_SELECT_KNOWN_GRAPHS = "DB.DBA.SPARQL_SELECT_KNOWN_GRAPHS()";

    private static final String SPARQL_DROP_GRAPH = "sparql drop silent graph iri(??)";

    private static final String SPARQL_DELETE = "sparql define output:format '_JAVA_' " +
            "delete from graph iri(??) {`iri(??)` `iri(??)` " +
            "`bif:__rdf_long_from_batch_params(??,??,??)`}";

    public static void bindBlankNode(PreparedStatement ps, int col, BID n) throws SQLException {
        ps.setString(col, "_:" + n.getValue());
    }

    public static void bindResource(PreparedStatement ps, int col, ID n) throws SQLException {
        if (n.isURI()) {
            bindURI(ps, col, n.asURI());
        } else if (n.isBNode()) {
            bindBlankNode(ps, col, n.asBNode());
        } else {
            throw new IllegalArgumentException(n.toString());
        }
    }

    public static void bindURI(PreparedStatement ps, int col, UID n) throws SQLException {
        ps.setString(col, n.getValue());
    }

    public static void bindValue(PreparedStatement ps, int col, NODE n) throws SQLException {
        if (n.isURI()) {
            ps.setInt(col, 1);
            ps.setString(col + 1, n.getValue());
            ps.setNull(col + 2, java.sql.Types.VARCHAR);

        } else if (n.isBNode()) {
            ps.setInt(col, 1);
            ps.setString(col + 1, "_:" + n.getValue());
            ps.setNull(col + 2, java.sql.Types.VARCHAR);

        } else if (n.isLiteral()) {
            LIT lit = n.asLiteral();
            if (lit.getLang() != null) {
                ps.setInt(col, 5);
                ps.setString(col + 1, lit.getValue());
                ps.setString(col + 2, LocaleUtil.toLang(lit.getLang()));
            } else {
                ps.setInt(col, 4);
                ps.setString(col + 1, lit.getValue());
                ps.setString(col + 2, lit.getDatatype().getId());
            }
        } else {
            throw new IllegalArgumentException(n.toString());
        }
    }

    private final IdSequence idSequence;

    private final Collection<UID> allowedGraphs;

    private final Connection connection;

    private final Converter converter;

    private final UID defaultGraph;

    private final int prefetchSize;

    protected VirtuosoRepositoryConnection(
            IdSequence idSequence,
            Converter converter,
            int prefetchSize,
            UID defGraph,
            Collection<UID> allowedGraphs,
            Connection connection) {
        this.idSequence = idSequence;
        this.converter = converter;
        this.connection = connection;
        this.prefetchSize = prefetchSize;
        this.defaultGraph = defGraph;
        this.allowedGraphs = allowedGraphs;
    }

    public void addBulk(Collection<STMT> addedStatements) throws SQLException, IOException {
        verifyNotReadOnly();

        Map<UID, TurtleStringWriter> writers = new HashMap<UID, TurtleStringWriter>();

        // write statements to writers
        for (STMT stmt : addedStatements) {
            assertAllowedGraph(stmt.getContext());
            UID context = stmt.getContext() != null ? stmt.getContext() : defaultGraph;
            TurtleStringWriter writer = writers.get(context);
            if (writer == null) {
                writer = new TurtleStringWriter(true);
                writers.put(context, writer);
                writer.begin();
            }
            writer.handle(stmt);
        }

        // create graphs
        PreparedStatement stmt = connection.prepareStatement(SPARQL_CREATE_GRAPH);
        try {
            for (UID graph : writers.keySet()) {
                stmt.setString(1, graph.getId());
                stmt.execute();
                stmt.clearParameters();
            }
        } finally {
            stmt.close();
        }

        // load data
        stmt = connection.prepareStatement("DB.DBA.TTLP(?,'',?,0)");
        try {
            for (Map.Entry<UID, TurtleStringWriter> entry : writers.entrySet()) {
                entry.getValue().end();
                stmt.setString(1, entry.getValue().toString());
                stmt.setString(2, entry.getKey().getId());
                stmt.execute();
                stmt.clearParameters();
            }
        } finally {
            stmt.close();
        }

    }

    public void removeBulk(Collection<STMT> deletedStatements) throws SQLException, IOException {
        verifyNotReadOnly();

        Map<UID, SPARQLUpdateWriter> writers = new HashMap<UID, SPARQLUpdateWriter>();

        // write statements to writers
        for (STMT stmt : deletedStatements) {
            assertAllowedGraph(stmt.getContext());
            UID context = stmt.getContext() != null ? stmt.getContext() : defaultGraph;
            SPARQLUpdateWriter writer = writers.get(context);
            if (writer == null) {
                writer = new SPARQLUpdateWriter(context, true);
                writers.put(context, writer);
                writer.begin();
            }
            writer.handle(stmt);
        }

        // load data
        Statement stmt = connection.createStatement();
        try {
            for (Map.Entry<UID, SPARQLUpdateWriter> entry : writers.entrySet()) {
                entry.getValue().end();
                stmt.execute("sparql " + entry.getValue().toString()); // NOSONAR
            }
        } finally {
            stmt.close();
        }

    }

    private void assertAllowedGraph(@Nullable UID context) {
        if (context != null && !isAllowedGraph(context)) {
            throw new IllegalStateException("Context not allowed for update " + context.getId());
        }
    }

    @Override
    public RDFBeanTransaction beginTransaction(boolean readOnly, int txTimeout, int isolationLevel) {
        return new VirtuosoTransaction(connection, readOnly, txTimeout, isolationLevel);
    }

    private void bindNodes(PreparedStatement ps, List<NODE> nodes) throws SQLException {
        int offset = 1;
        for (NODE node : nodes) {
            if (node.isResource()) {
                bindResource(ps, offset++, node.asResource());
            } else {
                bindValue(ps, offset, node);
                offset += 3;
            }
        }
    }

    @Override
    public void clear() {
        // ?!?
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public BID createBNode() {
        return new BID();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <D, Q> Q createUpdate(UpdateLanguage<D, Q> updateLanguage, final D definition) {
        if (updateLanguage == UpdateLanguage.SPARQL_UPDATE) {
            return (Q) new SPARQLUpdate() {
                @Override
                public void execute() {
                    executeSPARQLUpdate(DEFAULT_OUTPUT +
                            "DEFINE input:default-graph-uri <" + defaultGraph.getId() + ">\n" +
                            definition.toString());
                }

            };
        } else {
            throw new UnsupportedOperationException(updateLanguage.toString());
        }
    }

    private void executeSPARQLUpdate(String sparqlUpdate) {
        try {
            Statement stmt = connection.createStatement();
            try {
                stmt.execute(sparqlUpdate); // NOSONAR
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <D, Q> Q createQuery(QueryLanguage<D, Q> queryLanguage, D definition) {
        if (queryLanguage.equals(QueryLanguage.SPARQL)) {
            String query = definition.toString();
            SPARQLQuery.ResultType resultType = getResultType(query);
            return (Q) createSPARQLQuery(query, resultType);

        } else if (queryLanguage.equals(QueryLanguage.BOOLEAN) ||
                queryLanguage.equals(QueryLanguage.GRAPH) ||
                queryLanguage.equals(QueryLanguage.TUPLE)) {
            SPARQLVisitor visitor = new SPARQLVisitor(VirtuosoSPARQLTemplates.DEFAULT, "");
            visitor.setInlineResources(true); // TODO : remove this when full
                                              // prepared statement usage is
                                              // faster with Virtuoso
            QueryMetadata md = (QueryMetadata) definition;
            visitor.visit(md, queryLanguage);
            SPARQLQuery query = createSPARQLQuery(visitor.toString(), resultTypes.get(queryLanguage));
            if (logger.isInfoEnabled()) {
                logBindings(visitor.getConstantToLabel(), md);
            }
            visitor.addBindings(query, md);
            return (Q) query;

        } else {
            throw new IllegalArgumentException("Unsupported query language " + queryLanguage);
        }
    }

    private void logBindings(Map<Object, String> constantToLabel, QueryMetadata md) {
        for (Map.Entry<Object, String> entry : constantToLabel.entrySet()) {
            if (entry.getKey() instanceof ParamExpression<?>) {
                if (md.getParams().containsKey(entry.getKey())) {
                    logger.info(entry.getValue() + " = " + md.getParams().get(entry.getKey()));
                }
            } else {
                logger.info(entry.getValue() + " = " + entry.getKey());
            }
        }
    }

    private SPARQLQuery createSPARQLQuery(String query, SPARQLQuery.ResultType resultType) {
        if (logger.isInfoEnabled()) {
            logger.info(query);
        }
        if (resultType == SPARQLQuery.ResultType.BOOLEAN) {
            return new BooleanQueryImpl(connection, prefetchSize, JAVA_OUTPUT + query);
        } else if (resultType == SPARQLQuery.ResultType.TUPLES) {
            return new TupleQueryImpl(connection, converter, prefetchSize, DEFAULT_OUTPUT + query);
        } else if (resultType == SPARQLQuery.ResultType.TRIPLES) {
            return new GraphQueryImpl(connection, converter, prefetchSize, JAVA_OUTPUT + query);
        } else {
            throw new IllegalArgumentException("No result type for " + query);
        }
    }

    @Override
    public boolean exists(ID subject, UID predicate, NODE object, UID context, boolean includeInferred) {
        STMTIterator stmts = findStatements(subject, predicate, object, context, includeInferred, true);
        try {
            return stmts.hasNext();
        } finally {
            stmts.close();
        }
    }

    @Override
    public CloseableIterator<STMT> findStatements(
            @Nullable ID subject,
            @Nullable UID predicate,
            @Nullable NODE object,
            @Nullable UID context, boolean includeInferred) {
        return findStatements(subject, predicate, object, context, includeInferred, false);
    }

    private STMTIterator findStatements(
            @Nullable ID subject,
            @Nullable UID predicate,
            @Nullable NODE object,
            @Nullable UID context, boolean includeInferred, boolean hasOnly) {

        List<NODE> nodes = new ArrayList<NODE>(8);
        String s = "?s", p = "?p", o = "?o";

        // if (context != null){
        // nodes.add(context);
        // }
        if (subject != null) {
            nodes.add(subject);
            s = "`iri(??)`";
        }
        if (predicate != null) {
            nodes.add(predicate);
            p = "`iri(??)`";
        }
        if (object != null) {
            nodes.add(object);
            if (object.isResource()) {
                o = "`iri(??)`";
            } else {
                o = "`bif:__rdf_long_from_batch_params(??,??,??)`";
            }
        }

        // query construction
        StringBuffer query = new StringBuffer("sparql select * ");
        if (context != null) {
            query.append("from named <" + context.getId() + "> ");
            // query.append("from named iri(??) ");
        }
        query.append("where { graph ?g { " + s + " " + p + " " + o + " } }");
        if (hasOnly) {
            query.append(" limit 1");
        }

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(query.toString()); // NOSONAR
            bindNodes(ps, nodes);
            ps.setFetchSize(prefetchSize);
            rs = ps.executeQuery();
            return new STMTIterator(converter, ps, rs, subject, predicate, object, defaultGraph);
        } catch (SQLException e) {
            AbstractQueryImpl.close(ps, rs); // NOSONAR
            throw new RepositoryException("Query execution failed : " + query.toString(), e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public long getNextLocalId() {
        return idSequence.getNextId();
    }

    private SPARQLQuery.ResultType getResultType(String definition) {
        String normalized = definition.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
        if (normalized.startsWith("select ") || normalized.contains(" select")) {
            return SPARQLQuery.ResultType.TUPLES;
        } else if (normalized.startsWith("ask ") || normalized.contains(" ask ")) {
            return SPARQLQuery.ResultType.BOOLEAN;
        } else if (normalized.startsWith("construct ") || normalized.contains(" construct ")) {
            return SPARQLQuery.ResultType.TRIPLES;
        } else if (normalized.startsWith("describe ") || normalized.contains(" describe ")) {
            return SPARQLQuery.ResultType.TRIPLES;
        } else {
            throw new IllegalArgumentException("Illegal query " + definition);
        }
    }

    boolean isAllowedGraph(UID context) {
        return !context.getId().startsWith(INTERNAL_PREFIX)
                && !context.getId().equals("http://localhost:8890/DAV")
                && !context.getId().equals(RDF.NS)
                && !context.getId().equals(RDFS.NS)
                && !context.getId().equals(OWL.NS)
                && (context.equals(defaultGraph) || allowedGraphs.isEmpty() || allowedGraphs.contains(context));
    }

    public boolean isReadOnly() {
        try {
            return connection.isReadOnly();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    public void load(Format format, InputStream is, @Nullable UID context, boolean replace) throws SQLException, IOException {
        if (context != null && replace) {
            remove(null, null, null, context);
        }
        PreparedStatement stmt = null;
        try {
            if (format == Format.N3 || format == Format.TURTLE || format == Format.NTRIPLES) {
                byte[] bytes = ByteStreams.toByteArray(is);
                String content = new String(bytes, format == Format.NTRIPLES ? Charsets.US_ASCII : Charsets.UTF_8);
                stmt = connection.prepareStatement("DB.DBA.TTLP(?,'',?,0)");
                stmt.setString(1, content);
                stmt.setString(2, context != null ? context.getId() : defaultGraph.getId());
                stmt.execute();
            } else if (format == Format.RDFXML) {
                loadRdfXml(is, context);
            } else {
                throw new IllegalArgumentException("Unsupported format " + format);
            }
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private void loadRdfXml(InputStream is, @Nullable UID context) {
        try {
            UID currentContext = context != null ? context : defaultGraph;
            RDFParser rioParser = Rio.createParser(RDFFormat.RDFXML);
            rioParser.setRDFHandler(new RDFStreamingHandler(this, currentContext));
            // parses and adds triples
            rioParser.parse(is, currentContext.getId());
        } catch (RDFParseException e) {
            throw new RepositoryException(e);
        } catch (RDFHandlerException e) {
            throw new RepositoryException(e);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }
    

    private void remove(Collection<STMT> removedStatements) throws SQLException {
        verifyNotReadOnly();

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(VirtuosoRepositoryConnection.SPARQL_DELETE);
            int count = 0;

            for (STMT stmt : removedStatements) {
                assertAllowedGraph(stmt.getContext());
                ps.setString(1, stmt.getContext() != null ? stmt.getContext().getId() : defaultGraph.getId());
                bindResource(ps, 2, stmt.getSubject());
                bindURI(ps, 3, stmt.getPredicate());
                bindValue(ps, 4, stmt.getObject());
                ps.addBatch();
                count++;

                if (count > BATCH_SIZE) {
                    ps.executeBatch();
                    ps.clearBatch();
                    count = 0;
                }
            }

            if (count > 0) {
                ps.executeBatch();
                ps.clearBatch();
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    @Override
    public void remove(ID subject, UID predicate, NODE object, UID context) {
        try {
            removeMatch(subject, predicate, object, context);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    private void removeMatch(@Nullable ID subject, @Nullable UID predicate, @Nullable NODE object,
            @Nullable UID context) throws SQLException {
        assertAllowedGraph(context);
        PreparedStatement ps = null;
        try {
            // context given
            if (subject == null && predicate == null && object == null && context != null) {
                ps = connection.prepareStatement(SPARQL_DROP_GRAPH);
                ps.setString(1, context.getId());
                ps.execute();
                if (logger.isInfoEnabled()) {
                    logger.info("Dropped " + context.getId());
                }

                // all given
            } else if (subject != null && predicate != null && object != null && context != null) {
                ps = connection.prepareStatement(VirtuosoRepositoryConnection.SPARQL_DELETE);
                ps.setString(1, context.getId());
                bindResource(ps, 2, subject);
                bindURI(ps, 3, predicate);
                bindValue(ps, 4, object);
                ps.execute();

            } else if (subject == null && predicate == null && object == null && context == null && allowedGraphs.isEmpty()) {
                ps = connection.prepareStatement("RDF_GLOBAL_RESET()");
                ps.execute();

                // no context
            } else if (context == null) {
                Set<UID> graphs = new HashSet<UID>();
                graphs.add(defaultGraph);

                // collect graphs
                ps = connection.prepareStatement(SPARQL_SELECT_KNOWN_GRAPHS);
                ps.setFetchSize(25);
                ResultSet rs = null;
                try {
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        UID graph = new UID(rs.getString(1));
                        if (isAllowedGraph(graph)) {
                            graphs.add(graph);
                        }
                    }
                } finally {
                    AbstractQueryImpl.close(ps, rs);
                    ps = null;
                }

                for (UID graph : graphs) {
                    removeMatch(subject, predicate, object, graph);
                }

            } else {
                String s = "?s", p = "?p", o = "?o", c = "iri(??)";
                List<NODE> nodes = new ArrayList<NODE>(8);
                nodes.add(context);
                if (subject != null) {
                    nodes.add(subject);
                    s = "`iri(??)`";
                }
                if (predicate != null) {
                    nodes.add(predicate);
                    p = "`iri(??)`";
                }
                if (object != null) {
                    nodes.add(object);
                    if (object.isResource()) {
                        o = "`iri(??)`";
                    } else {
                        o = "`bif:__rdf_long_from_batch_params(??,??,??)`";
                    }
                }

                nodes.add(context);
                if (subject != null) {
                    nodes.add(subject);
                }
                if (predicate != null) {
                    nodes.add(predicate);
                }
                if (object != null) {
                    nodes.add(object);
                }

                String delete = String.format("sparql delete from %1$s { %2$s %3$s %4$s } " +
                        "where { graph `%1$s` { %2$s %3$s %4$s } }", c, s, p, o);

                ps = connection.prepareStatement(delete); // NOSONAR
                bindNodes(ps, nodes);
                ps.execute();
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    @Override
    public void update(Collection<STMT> removedStatements, Collection<STMT> addedStatements) {
        if (removedStatements != null && !removedStatements.isEmpty()) {
            try {
                remove(removedStatements);
            } catch (SQLException e) {
                throw new RepositoryException(e);
            }
        }
        if (addedStatements != null && !addedStatements.isEmpty()) {
            try {
                addBulk(addedStatements);
            } catch (SQLException e) {
                throw new RepositoryException(e);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }
    }

    public <T> T doInConnection(SQLConnectionCallback<T> callback) {
        return callback.doInConnection(connection);
    }

    protected void verifyNotReadOnly() {
        if (isReadOnly()) {
            throw new RepositoryException("Connection is in read-only mode");
        }
    }

    @Override
    public QueryOptions getQueryOptions() {
        return QueryOptions.COUNT_VIA_AGGREGATION;
    }

    @Override
    public InferenceOptions getInferenceOptions() {
        return InferenceOptions.DEFAULT;
    }
}
