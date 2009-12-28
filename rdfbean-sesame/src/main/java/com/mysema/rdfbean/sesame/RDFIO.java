/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.sesame;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.rdfbean.model.io.RDFSource;

/**
 * @author sasa
 *
 */
public class RDFIO {
    
    private static final Logger logger = LoggerFactory.getLogger(RDFIO.class);
    
    private RDFSource[] sources;
    
    private File target;

    private RDFFormat targetFormat;

    public void setSources(RDFSource... sources) {
        this.sources = sources;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public void setTargetFormat(RDFFormat targetFormat) {
        this.targetFormat = targetFormat;
    }
    
    public void export(RepositoryConnection conn, RDFFormat format, OutputStream out) throws StoreException, RDFHandlerException {
        RDFWriter writer = Rio.createWriter(targetFormat, out);
        conn.export(writer);
    }

    public boolean run() throws StoreException, RDFParseException, IOException, RDFHandlerException {
        Assert.notNull(sources);

        MemoryStore store = new MemoryStore();
        Repository repository = new SailRepository(store);
        repository.initialize();
        RepositoryConnection conn = repository.getConnection();
        for (RDFSource source : sources) {
            try {
                conn.add(source.openStream(), source.getContext(), FormatHelper.getFormat(source.getFormat()));
                System.out.println(source.getFormat() + " syntax validated OK.");
            } catch (RDFParseException e) {
                System.err.println(source.getResource() + " failed " + source.getFormat() +
                        " validation: " + e.getMessage());
                return false;
            }
        }
        if (target != null) {
            if (targetFormat != null) {
                targetFormat = RDFFormat.forFileName(target.getName());
            }
            target = target.getAbsoluteFile();
            File dir = target.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()){
                    logger.error(dir.getPath() + " was not created successfully");
                }
            } else {
            }
            System.out.println("EXPORTING as " + targetFormat + " into " + target);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
            try {
                export(conn, targetFormat, out);
            } finally {
                out.flush();
                out.close();
            }
        }
        return true;
    }
    
}
