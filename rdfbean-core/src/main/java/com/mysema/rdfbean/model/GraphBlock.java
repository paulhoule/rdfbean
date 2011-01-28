package com.mysema.rdfbean.model;

import java.util.List;

import javax.annotation.Nullable;

import com.mysema.query.types.Expression;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.Visitor;

public class GraphBlock implements Block{
    
    private static final long serialVersionUID = -4450740702187022383L;

    private final List<Block> blocks;
    
    @Nullable
    private final Predicate filters;
    
    private final Expression<UID> context;
    
    public GraphBlock(Expression<UID> context, List<Block> blocks, Predicate... filters) {
        this.blocks = blocks;
        this.context = context;
        this.filters = ExpressionUtils.allOf(filters);
    }
    
    @Override
    public Predicate not() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R, C> R accept(Visitor<R, C> v, C context) {
        return (R)((SPARQLVisitor)v).visit(this, null);        
    }

    @Override
    public Class<? extends Boolean> getType() {
        return Boolean.class;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    @Nullable
    public Predicate getFilters() {
        return filters;
    }

    public Expression<UID> getContext() {
        return context;
    }

}
