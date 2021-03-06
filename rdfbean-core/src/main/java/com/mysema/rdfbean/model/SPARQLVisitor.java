package com.mysema.rdfbean.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.JoinExpression;
import com.mysema.query.QueryFlag;
import com.mysema.query.QueryFlag.Position;
import com.mysema.query.QueryMetadata;
import com.mysema.query.QueryModifiers;
import com.mysema.query.support.SerializerBase;
import com.mysema.query.types.Constant;
import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.Expression;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Operator;
import com.mysema.query.types.Ops;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.TemplateExpression;
import com.mysema.rdfbean.xsd.ConverterRegistryImpl;

/**
 * @author tiwe
 * 
 */
public class SPARQLVisitor extends SerializerBase<SPARQLVisitor> implements RDFVisitor<Void, Void> {

    private static final Set<Operator<?>> REGEX_OPS = ImmutableSet.<Operator<?>> of(
            Ops.MATCHES, Ops.MATCHES_IC, Ops.STARTS_WITH, Ops.STARTS_WITH_IC,
            Ops.ENDS_WITH, Ops.ENDS_WITH_IC, Ops.STRING_CONTAINS, Ops.STRING_CONTAINS_IC,
            Ops.STRING_IS_EMPTY);

    @Nullable
    private PatternBlock lastPattern;

    private final String prefix;

    private final Stack<Operator<?>> operators = new Stack<Operator<?>>();

    private boolean inlineAll = false;

    private boolean inlineResources = false;

    private boolean likeAsMatches = false;
    
    private boolean inToOr = false;

    @Nullable
    private QueryMetadata metadata;

    public SPARQLVisitor() {
        this(SPARQLTemplates.DEFAULT, "");
    }

    public SPARQLVisitor(SPARQLTemplates templates, String prefix) {
        super(templates);
        setNormalize(false);
        this.prefix = prefix;
    }

    // @Override
    protected void appendAsString(Expression<?> expr) {
        Object constant;
        if (expr instanceof Constant<?>) {
            constant = ((Constant<?>) expr).getConstant();
        } else if (expr instanceof ParamExpression<?> && metadata != null) {
            if (metadata.getParams().containsKey(expr)) {
                constant = metadata.getParams().get(expr);
            } else {
                constant = ((ParamExpression<?>) expr).getName();
            }

        } else {
            constant = expr.toString();
        }
        if (constant instanceof NODE) {
            append(((NODE) constant).getValue());
        } else {
            append(constant.toString());
        }
    }

    @Nullable
    public Void visit(QueryMetadata md, QueryLanguage<?, ?> queryType) {
        metadata = md;
        QueryModifiers mod = md.getModifiers();
        append(prefix);
        Set<QueryFlag> flags = metadata.getFlags();

        // start
        serialize(Position.START, flags);

        // select
        if (queryType == QueryLanguage.TUPLE) {
            append("SELECT ");
            if (md.isDistinct()) {
                append("DISTINCT ");
            }
            if (!md.getProjection().isEmpty()) {
                for (Expression<?> expr : md.getProjection()) {
                    if (expr instanceof TemplateExpression<?> || expr instanceof com.mysema.query.types.Operation<?>) {
                        append("(").handle(expr).append(")");
                    } else {
                        handle(expr);
                    }
                    append(" ");
                }
            } else {
                append("*");
            }
            append("\n");

            // ask
        } else if (queryType == QueryLanguage.BOOLEAN) {
            append("ASK ");

            // construct
        } else if (queryType == QueryLanguage.GRAPH) {
            if (md.getProjection().size() == 1 && md.getProjection().get(0) instanceof GroupBlock) {
                append("CONSTRUCT ").handle("", md.getProjection()).append("\n");
            } else {
                append("CONSTRUCT { ").handle("", md.getProjection()).append("}\n");
            }
        }
        lastPattern = null;

        // from
        for (JoinExpression je : md.getJoins()) {
            UID uid = (UID) ((Constant<?>) je.getTarget()).getConstant();
            append("FROM <").append(uid.getId()).append(">\n");
        }

        // where
        if (md.getWhere() != null) {
            if (queryType != QueryLanguage.BOOLEAN) {
                append("WHERE \n  ");
            }
            if (md.getWhere() instanceof GroupBlock) {
                handle(md.getWhere());
            } else {
                append("{ ").handle(md.getWhere()).append("}");
            }
            append("\n");
        }

        // order
        if (!md.getOrderBy().isEmpty()) {
            append("ORDER BY ");
            boolean first = true;
            for (OrderSpecifier<?> order : md.getOrderBy()) {
                if (!first) {
                    append(" ");
                }
                if (order.isAscending()) {
                    handle(order.getTarget());
                } else {
                    append("DESC(").handle(order.getTarget()).append(")");
                }
                first = false;
            }
            append("\n");
        }

        // group by
        if (!md.getGroupBy().isEmpty()) {
            append("GROUP BY ").handle(" ", md.getGroupBy()).append("\n");
        }

        // having
        if (md.getHaving() != null) {
            append("HAVING (").handle(md.getHaving()).append(")\n");
        }

        // limit
        if (mod.getLimit() != null) {
            append("LIMIT ").append(mod.getLimit().toString()).append("\n");
        }

        // offset
        if (mod.getOffset() != null) {
            append("OFFSET ").append(mod.getOffset().toString()).append("\n");
        }

        metadata = null;
        return null;

    }

    @SuppressWarnings("unchecked")
    @Override
    public Void visit(SubQueryExpression<?> expr, Void context) {
        for (Map.Entry<ParamExpression<?>, Object> entry : metadata.getParams().entrySet()) {
            expr.getMetadata().setParam((ParamExpression) entry.getKey(), entry.getValue());
        }

        if (!operators.isEmpty() && operators.peek() == Ops.EXISTS) {
            handle(expr.getMetadata().getWhere());
        } else {
            visit(expr.getMetadata(), QueryLanguage.TUPLE);
        }

        return null;
    }

    @Nullable
    public Void visit(UnionBlock expr, @Nullable Void context) {
        lastPattern = null;
        boolean first = true;
        for (Block block : expr.getBlocks()) {
            if (!first) {
                append("UNION ");
            }
            if (block instanceof PatternBlock) {
                append("{ ").handle(block).append("} ");
            } else {
                handle(block);
            }
            lastPattern = null;
            first = false;
        }
        return null;
    }

    @Nullable
    public Void visit(GroupBlock expr, @Nullable Void context) {
        lastPattern = null;
        append("{ ");
        visitBlocks(expr.getBlocks());
        visitFilter(expr.getFilters());
        append("} ");
        lastPattern = null;
        return null;
    }

    @Nullable
    public Void visit(GraphBlock expr, @Nullable Void context) {
        lastPattern = null;
        append("GRAPH ").handle(expr.getContext()).append(" { ");
        visitBlocks(expr.getBlocks());
        visitFilter(expr.getFilters());
        append("} ");
        lastPattern = null;
        return null;
    }

    @Nullable
    public Void visit(OptionalBlock expr, @Nullable Void context) {
        lastPattern = null;
        append("OPTIONAL { ");
        visitBlocks(expr.getBlocks());
        visitFilter(expr.getFilters());
        append("} ");
        lastPattern = null;
        return null;
    }

    private void visitBlocks(List<Block> blocks) {
        for (Block block : blocks) {
            if (lastPattern != null && !(block instanceof PatternBlock)) {
                append(".\n  ");
                lastPattern = null;
            }
            handle(block);
        }
    }

    private void visitFilter(@Nullable Predicate filter) {
        if (filter != null) {
            if (lastPattern != null) {
                append(". ");
            }
            lastPattern = null;
            append("FILTER(").handle(filter).append(") ");
        }
        lastPattern = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitConstant(Object constant) {
        // convert literal values to LIT objects
        if (constant instanceof String) {
            constant = new LIT(constant.toString());
        } else if (ConverterRegistryImpl.DEFAULT.supports(constant.getClass())) {
            UID datatype = ConverterRegistryImpl.DEFAULT.getDatatype(constant.getClass());
            String value = ConverterRegistryImpl.DEFAULT.toString(constant);
            constant = new ConstantImpl<LIT>(LIT.class, new LIT(value, datatype));
        }

        if (constant instanceof QueryMetadata) {
            QueryMetadata md = (QueryMetadata) constant;
            handle(md.getWhere());

        } else if (constant instanceof Block) {
            handle((Expression<?>) constant);

        } else if (inlineAll && NODE.class.isInstance(constant)) {
            NODE node = (NODE) constant;
            if (node.isBNode()) {
                append("_:" + node.getValue());
            } else if (node.isURI()) {
                append("<" + node.getValue() + ">");
            } else {
                append(node.toString());
            }

        } else if (inlineResources && UID.class.isInstance(constant)) {
            UID node = (UID) constant;
            append("<" + node.getValue() + ">");

        } else if (Collection.class.isAssignableFrom(constant.getClass())) {
            boolean first = true;
            append("(");
            for (Object o : (Collection) constant) {
                if (!first) {
                    append(", ");
                }
                visitConstant(o);
                first = false;
            }
            append(")");

        } else if (!getConstantToLabel().containsKey(constant)) {
            String constLabel = "_c" + (getConstantToLabel().size() + 1);
            getConstantToLabel().put(constant, constLabel);
            append("?" + constLabel);

        } else {
            append("?" + getConstantToLabel().get(constant));
        }
    }

    @Nullable
    public Void visit(PatternBlock expr, @Nullable Void context) {
        // resource = true;
        if (lastPattern == null || !lastPattern.getSubject().equals(expr.getSubject())) {
            if (lastPattern != null) {
                append(".\n  ");
            }
            handle(expr.getSubject()).append(" ");
            handle(expr.getPredicate()).append(" ");

        } else if (!lastPattern.getPredicate().equals(expr.getPredicate())) {
            append("; ");
            handle(expr.getPredicate()).append(" ");

        } else {
            append(", ");
        }

        handle(expr.getObject()).append(" ");
        lastPattern = expr;
        // resource = false;
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void visitOperation(Class<?> type, Operator<?> operator, List<? extends Expression<?>> args) {
        if (operator == Ops.IN && inToOr) { 
            BooleanBuilder builder = new BooleanBuilder();
            for (Object arg : ((Constant<Collection>)args.get(1)).getConstant()) {
                builder.or(ExpressionUtils.eq(args.get(0), new ConstantImpl(arg)));
            }
            builder.getValue().accept(this, null);
            return;
            
        } else if (operator == Ops.LIKE && likeAsMatches && args.get(1) instanceof Constant) {
            operator = Ops.MATCHES;
            String value = ((Constant<LIT>) args.get(1)).getConstant().getValue().replace("%", ".*").replace("_", ".");
            args = Arrays.asList(args.get(0), new ConstantImpl<LIT>(LIT.class, new LIT(value)));
        } else if (REGEX_OPS.contains(operator) && args.get(1) instanceof Constant
                && ((Constant) args.get(1)).getConstant() instanceof LIT) {
            args = Arrays.<Expression<?>> asList(args.get(0),
                    new ConstantImpl(((Constant<LIT>) args.get(1)).getConstant().getValue()));
        }
        operators.push(operator);
        try {
            if (operator == Ops.NUMCAST) {
                UID datatype = (UID) ((Constant<?>) args.get(1)).getConstant();
                append("xsd:" + datatype.ln() + "(");
                handle(args.get(0));
                append(")");
            } else {
                super.visitOperation(type, operator, args);
            }
        } finally {
            operators.pop();
        }
    }

    @Override
    @Nullable
    public Void visit(ParamExpression<?> param, @Nullable Void context) {
        getConstantToLabel().put(param, param.getName());
        append("?" + param.getName());
        return null;
    }

    public void addBindings(SPARQLQuery query, QueryMetadata md) {
        for (Map.Entry<Object, String> entry : getConstantToLabel().entrySet()) {
            if (entry.getKey() instanceof ParamExpression<?>) {
                if (md.getParams().containsKey(entry.getKey())) {
                    query.setBinding(entry.getValue(), (NODE) md.getParams().get(entry.getKey()));
                }
            } else {
                query.setBinding(entry.getValue(), (NODE) entry.getKey());
            }
        }
    }

    public void setInlineResources(boolean b) {
        inlineResources = b;
    }

    public void setLikeAsMatches(boolean likeAsMatches) {
        this.likeAsMatches = likeAsMatches;
    }

    public void setInlineAll(boolean inlineAll) {
        this.inlineAll = inlineAll;
    }

    public void setInToOr(boolean inToOr) {
        this.inToOr = inToOr;
    }
    
}
