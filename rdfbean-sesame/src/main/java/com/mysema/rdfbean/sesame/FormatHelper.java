package com.mysema.rdfbean.sesame;

import org.openrdf.rio.RDFFormat;

import com.mysema.rdfbean.model.io.Format;

/**
 * FormatHelper provides
 *
 * @author tiwe
 * @version $Id$
 */
final class FormatHelper {
    
    private FormatHelper(){}
    
    public static RDFFormat getFormat(Format format){
        switch(format){
        case N3: return RDFFormat.N3;
        case NTRIPLES: return RDFFormat.NTRIPLES;
        case RDFA: return RDFFormat.RDFA;
        case RDFXML: return RDFFormat.RDFXML;
        case TRIG: return RDFFormat.TRIG;
        case TURTLE: return RDFFormat.TURTLE;
        }
        throw new IllegalArgumentException("Unsupported format : " + format);
    }

}
