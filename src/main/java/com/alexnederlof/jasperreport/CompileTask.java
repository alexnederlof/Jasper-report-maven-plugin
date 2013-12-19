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
import java.util.concurrent.Callable;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import org.apache.maven.plugin.logging.Log;

/**
 * A task that compiles a Jasper sourcefile.
 */
public class CompileTask implements Callable<Void> {

	private final File source;
	private final File destFolder;
	private final String sourceExtension;
	private final String outExtension;
	private final Log log;
	private final boolean verbose;

	/**
	 * @param source
	 *            The source file.
	 * @param destFolder
	 *            The destination folder.
	 * @param sourceExtension
	 *            The source extension.
	 * @param outExtension
	 *            The output extension.
	 * @param log
	 *            The logger.
	 * @param verbose
	 *            If the output should be verbose.
	 */
	public CompileTask(File source, File destFolder, String sourceExtension, String outExtension, Log log,
			boolean verbose) {
		this.source = source;
		this.destFolder = destFolder;
		this.sourceExtension = sourceExtension;
		this.outExtension = outExtension;
		this.log = log;
		this.verbose = verbose;
	}

	/**
	 * Compile the source file. If the source file doesn't have the right extension, it is skipped.
	 * 
	 * @return Debug output of the compile action.
	 * @throws Exception
	 *             when anything goes wrong while compiling.
	 */
	@Override
	public Void call() throws Exception {
		File out = null;
		try {
			if (source.getAbsolutePath()
				.endsWith(sourceExtension)) {
				String newFilename = getNewFilename();
				out = new File(destFolder, newFilename);
				JasperCompileManager.compileReportToStream(new FileInputStream(source), new FileOutputStream(out));
				if (verbose) {
					log.info("Compiling " + newFilename);
				}
			}
		}
		catch (Exception e) {
			cleanUpAndThrowError(out, e);
		}
		return null;
	}

	private void cleanUpAndThrowError(File out, Exception e) throws JRException {
		log.error("Could not compile " + source.getName(), e);
		if (out != null && out.exists()) {
			out.delete();
		}
		throw new JRException("Could not compile " + source.getName(), e);
	}

	private String getNewFilename() {
		return source.getName()
			.substring(0, source.getName()
				.lastIndexOf(sourceExtension)) + outExtension;
	}

}
