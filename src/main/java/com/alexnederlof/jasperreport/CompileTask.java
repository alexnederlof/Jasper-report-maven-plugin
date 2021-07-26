package com.alexnederlof.jasperreport;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import org.apache.maven.plugin.logging.Log;

/**
 * A task that compiles a Jasper sourcefile.
 */
public class CompileTask implements Callable<Void> {

    private final JasperCompileManager compileManager;
    private final File source;
    private final File destination;
    private final Log log;
    private final boolean verbose;

    /**
     * @param source The source file.
     * @param destination The destination file.
     * @param log The logger.
     * @param verbose If the output should be verbose.
     */
    CompileTask(JasperCompileManager compileManager, File source, File destination, Log log, boolean verbose) {
        super();
        this.compileManager = compileManager;
        this.source = source;
        this.destination = destination;
        this.log = log;
        this.verbose = verbose;
    }

    /**
     * Compile the source file.
     *
     * @return Debug output of the compile action.
     * @throws Exception when anything goes wrong while compiling.
     */
    @Override
    public Void call() throws Exception {
        try (OutputStream out = new FileOutputStream(destination); InputStream in = new FileInputStream(source)) {
            compileManager.compileToStream(in, out);
            if (verbose) {
                log.info("Compiling " + source.getName());
            }
        } catch (Exception e) {
            cleanUpAndThrowError(destination, e);
        }
        return null;
    }

    private void cleanUpAndThrowError(File out, Exception e) throws JRException {
        log.error("Could not compile " + source.getName() + " because " + e.getMessage(), e);
        if (out != null && out.exists()) {
            out.delete();
        }
        throw new JRException("Could not compile " + source.getName(), e);
    }
}
