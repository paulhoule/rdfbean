/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.sesame.query;

import static com.mysema.query.types.operation.Ops.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openrdf.query.algebra.*;
import org.openrdf.query.algebra.Compare.CompareOp;
import org.openrdf.query.algebra.MathExpr.MathOp;

import com.mysema.query.types.operation.Operator;
import com.mysema.rdfbean.query.QDSL;

/**
 * Operations provides Operator -> ValueExpr mappings for Sesame query creation
 *
 * @author tiwe
 * @version $Id$
 */
public class Operations {
    
    private final Map<Operator<?>,Transformer> opToTransformer = new HashMap<Operator<?>,Transformer>();
    
    public Operations(Functions functions){        
        opToTransformer.put(AND, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args){
                if (args.get(0) == null){
                    return args.get(1);
                }else if (args.get(1) == null){
                    return args.get(0);
                }else{
                    return new And(args.get(0), args.get(1));    
                }                
            }            
        });        
        opToTransformer.put(OR, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args){
                return new Or(args.get(0), args.get(1));
            }            
        });
        opToTransformer.put(NOT, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args){
                return new Not(args.get(0));
            }            
        });        
        
        Map<Operator<?>, CompareOp> compareOps = new HashMap<Operator<?>, CompareOp>();
        compareOps.put(EQ_OBJECT, CompareOp.EQ); // TODO : datatype inference 
        compareOps.put(EQ_PRIMITIVE, CompareOp.EQ); // TODO : datatype inference
        compareOps.put(NE_OBJECT,  CompareOp.NE); // TODO : datatype inference
        compareOps.put(NE_PRIMITIVE, CompareOp.NE); // TODO : datatype inference
        
        compareOps.put(LT, CompareOp.LT);
        compareOps.put(BEFORE, CompareOp.LT);
        compareOps.put(LOE, CompareOp.LE);
        compareOps.put(BOE, CompareOp.LE);
        compareOps.put(GT, CompareOp.GT);
        compareOps.put(AFTER, CompareOp.GT);
        compareOps.put(GOE, CompareOp.GE);
        compareOps.put(AOE, CompareOp.GE);
        for (final Map.Entry<Operator<?>, CompareOp> entry : compareOps.entrySet()){
            opToTransformer.put(entry.getKey(), new Transformer(){
                @Override
                public ValueExpr transform(List<ValueExpr> args) {
                    return new Compare(args.get(0), args.get(1), entry.getValue());
                }                
            });
        }      
        
        opToTransformer.put(BETWEEN, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                return new And(
                    new Compare(args.get(0), args.get(1),CompareOp.GE),    
                    new Compare(args.get(0), args.get(2),CompareOp.LE));
            }            
        });

        opToTransformer.put(STARTS_WITH, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                ValueExpr first = new Str(args.get(0));
                Var arg2 = ((Var)args.get(1));
                if (arg2.getValue() != null){
                    return new Regex(first, ((Var)args.get(1)).getValue().stringValue()+"*",true);
                }else{
                    return new FunctionCall(QDSL.startsWith.getId(), args);
                }
            }            
        });
        opToTransformer.put(ENDS_WITH, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                ValueExpr first = new Str(args.get(0));
                Var arg2 = ((Var)args.get(1));
                if (arg2.getValue() != null){
                    return new Regex(first, "*"+((Var)args.get(1)).getValue().stringValue(),true); 
                }else{
                    return new FunctionCall(QDSL.endsWith.getId(), args);
                }                
            }            
        });
        opToTransformer.put(STRING_CONTAINS, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                ValueExpr first = new Str(args.get(0));
                Var arg2 = ((Var)args.get(1));
                if (arg2.getValue() != null){
                    return new Regex(first, "*"+((Var)args.get(1)).getValue().stringValue()+"*",true);    
                }else{
                    return new FunctionCall(QDSL.stringContains.getId(), args);
                }
                
            }            
        });
        opToTransformer.put(STRING_IS_EMPTY, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                ValueExpr first = new Str(args.get(0));
                return new Regex(first, "", false);  // TODO : optimize          
            }            
        });

        opToTransformer.put(STARTS_WITH_IC, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                ValueExpr first = new Str(args.get(0));
                Var arg2 = ((Var)args.get(1));
                if (arg2.getValue() != null){
                    return new Regex(first, ((Var)args.get(1)).getValue().stringValue()+"*",false);    
                }else{
                    return new FunctionCall(QDSL.startsWithIc.getId(), args);
                }
                
            }            
        });
        opToTransformer.put(ENDS_WITH_IC, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                ValueExpr first = new Str(args.get(0));
                Var arg2 = ((Var)args.get(1));
                if (arg2.getValue() != null){
                    return new Regex(first, "*"+((Var)args.get(1)).getValue().stringValue(),false);
                }else{
                    return new FunctionCall(QDSL.endsWithIc.getId(), args);
                }
            }            
        });
        opToTransformer.put(MATCHES, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                ValueExpr first = new Str(args.get(0));
                ValueExpr second = new Str(args.get(1));
                return new Regex(first, second, null);
            }            
        }); 
        
        final Iterator<MathOp> mathOps = Arrays.asList(
                MathOp.PLUS, 
                MathOp.MINUS, 
                MathOp.MULTIPLY, 
                MathOp.DIVIDE).iterator();
        for (Operator<?> op : Arrays.<Operator<?>>asList(ADD, SUB, MULT, DIV)){
            opToTransformer.put(op, new Transformer(){
                @Override
                public ValueExpr transform(List<ValueExpr> args) {
                    return new MathExpr(args.get(0), args.get(1), mathOps.next());
                }
                
            });
        }
        
        opToTransformer.put(STRING_CAST, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
               return new Str(args.get(0));
            }            
        });
        opToTransformer.put(IS_NULL, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                return new Not(new Bound((Var) args.get(0)));
            }            
        });
        opToTransformer.put(IS_NOT_NULL, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                return new Bound((Var) args.get(0));
            }            
        });
        opToTransformer.put(NUMCAST, new Transformer(){
            @Override
            public ValueExpr transform(List<ValueExpr> args) {
                return new FunctionCall( ((Var)args.get(1)).getValue().stringValue(), args.get(0));
            }            
        });
        
        functions.addTransformers(opToTransformer);
        
    }
    
    public Transformer getTransformer(Operator<?> op){
        return opToTransformer.get(op);
    }
}