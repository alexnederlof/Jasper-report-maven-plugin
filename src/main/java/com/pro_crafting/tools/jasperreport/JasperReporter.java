package com.pro_crafting.tools.jasperreport;

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
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRReport;
import net.sf.jasperreports.engine.design.JRCompiler;
import net.sf.jasperreports.engine.design.JRJdtCompiler;
import net.sf.jasperreports.engine.xml.JRReportSaxParserFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

/**
 * This plugin compiles jasper source files to the target folder. While doing
 * so, it keeps the folder structure in tact.
 */
@Mojo(defaultPhase = LifecyclePhase.PROCESS_RESOURCES, name = "jasper", requiresDependencyResolution =
		ResolutionScope.COMPILE)
public class JasperReporter extends AbstractMojo {

	static final String ERROR_JRE_COMPILE_ERROR =
			"Some Jasper reports could not be compiled. See log above for details.";

	/**
	 * This is the java compiler used
	 */
	@Parameter(defaultValue = "net.sf.jasperreports.engine.design.JRJdtCompiler", required = true)
	private String compiler;

	/**
	 * This is where the .jasper files are written.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/jasper")
	private File outputDirectory;

	/**
	 * This is where the xml report design files should be.
	 */
	@Parameter(defaultValue = "src/main/jasperreports")
	private File sourceDirectory;

	/**
	 * The extension of the source files to look for. Finds files with a .jrxml
	 * extension by default.
	 */
	@Parameter(defaultValue = ".jrxml")
	private String sourceFileExt;

	/**
	 * The extension of the compiled report files. Creates files with a .jasper
	 * extension by default.
	 *
	 */
	@Parameter(defaultValue = ".jasper")
	private String outputFileExt;

	/**
	 * Check the source files before compiling. Default value is true.
	 *
	 */
	@Parameter(defaultValue = "true")
	private boolean xmlValidation;

	/**
     * Set this to "true" to bypass compiling reports. Default value is false.
     *
     */
    @Parameter( defaultValue = "false" )
    private boolean skip;

	/**
	 * If verbose is on the plug-in will report which reports it is compiling
	 * and which files are being skipped.
	 *
	 */
	@Parameter(defaultValue = "false")
	private boolean verbose;

	/**
	 * The number of threads the reporting will use. Default is 4 which is good
	 * for a lot of reports on a hard drive (in stead of SSD). If you only have
	 * a few, or if you have SSD, it might be faster to set it to 2.
	 *
	 */
	@Parameter(defaultValue = "4")
	private int numberOfThreads;

	@Parameter(property = "project.compileClasspathElements")
	private List<String> classpathElements;

	/**
	 * Use this parameter to add additional properties to the Jasper compiler.
	 * For example.
	 *
	 * <pre>
	 * {@code
	 * <configuration>
	 * 	...
	 * 		<additionalProperties>
	 * 			<net.sf.jasperreports.awt.ignore.missing.font>true
	 * 			</net.sf.jasperreports.awt.ignore.missing.font>
	 *          <net.sf.jasperreports.default.pdf.font.name>Courier</net.sf.jasperreports.default.pdf.font.name>
	 *          <net.sf.jasperreports.default.pdf.encoding>UTF-8</net.sf.jasperreports.default.pdf.encoding>
	 *          <net.sf.jasperreports.default.pdf.embedded>true</net.sf.jasperreports.default.pdf.embedded>
	 * </additionalProperties>
	 * </configuration>
	 * }
	 * </pre>
	 *
	 */
	@Parameter
	private Map<String, String> additionalProperties;

	/**
	 * If failOnMissingSourceDirectory is on the plug-in will fail the build if
	 * source directory does not exist. Default value is true.
	 *
	 */
	@Parameter(defaultValue = "true")
	private boolean failOnMissingSourceDirectory = true;

	/**
	 * This is the source inclusion scanner class used, a
	 * <code>org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner</code>
	 * implementation class.
	 *
	 */
	@Parameter(defaultValue = "org.codehaus.plexus.compiler.util.scan.StaleSourceScanner")
	private String sourceScanner = StaleSourceScanner.class.getName();

	/**
	 * Provides the option to add additional JARs to the Classpath for compiling. This is handy in case you have
	 * references to external Java-Beans in your JasperReports.
	 *
	 * <pre>
	 * {@code
	 * <configuration>
	 *  ...
	 *      <additionalClasspath>/web/lib/ServiceBeans.jar;/web/lib/WebForms.jar</additionalClasspath>
	 *  ...
	 * </configuration>
	 * }
	 * </pre>
	 *
	 */
	@Parameter
	private String additionalClasspath;

	private Log log;

	public JasperReporter() {
	}

	@Override
	public void execute() throws MojoExecutionException {
		log = getLog();

        if ( isSkip() )
        {
            log.info( "Compiling Jasper reports is skipped." );
            return;
        }

        if (verbose) {
			logConfiguration(log);
		}

		checkOutDirWritable(outputDirectory);

		SourceMapping mapping = new SuffixMapping(sourceFileExt, outputFileExt);
		Set<File> sources = jrxmlFilesToCompile(mapping);
		if (sources.isEmpty()) {
			log.info("Nothing to compile - all Jasper reports are up to date");
		}
		else {
			log.info("Compiling " + sources.size() + " Jasper reports design files.");

			List<CompileTask> tasks = generateTasks(sources, mapping);
			if (tasks.isEmpty()) {
				log.info("Nothing to compile");
				return;
			}

			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(getClassLoader(classLoader));
			try {
				configureJasper();
				executeTasks(tasks);
			}
			finally {
				if (classLoader != null) {
					Thread.currentThread().setContextClassLoader(classLoader);
				}
			}
		}
	}

	/**
	 * Determines source files to be compiled.
	 *
	 * @param mapping The source files
	 *
	 * @return set of jxml files to compile
	 *
	 * @throws MojoExecutionException When there's trouble with the input
	 */
	private Set<File> jrxmlFilesToCompile(SourceMapping mapping) throws MojoExecutionException {
		if (!sourceDirectory.isDirectory()) {
			String message = sourceDirectory.getName() + " is not a directory";
			if (failOnMissingSourceDirectory) {
				throw new IllegalArgumentException(message);
			}
			else {
				log.warn(message + ", skip JasperReports reports compiling.");
				return Collections.emptySet();
			}
		}

		try {
			SourceInclusionScanner scanner = createSourceInclusionScanner();
			scanner.addSourceMapping(mapping);
			return scanner.getIncludedSources(sourceDirectory, outputDirectory);
		}
		catch (InclusionScanException e) {
			throw new MojoExecutionException("Error scanning source root: \'" + sourceDirectory + "\'.", e);
		}
	}

	private void logConfiguration(Log log) {
		log.info("Generating Jasper reports");
		log.info("Output dir: " + outputDirectory.getAbsolutePath());
		log.info("Source dir: " + sourceDirectory.getAbsolutePath());
		log.info("Output ext: " + outputFileExt);
		log.info("Source ext: " + sourceFileExt);
		log.info("Addition properties: " + additionalProperties);
		log.info("XML Validation: " + xmlValidation);
		log.info("JasperReports Compiler: " + compiler);
		log.info("Number of threads: " + numberOfThreads);
		log.info("classpathElements: " + classpathElements);
		log.info("additionalClasspath: " + additionalClasspath);
		log.info("Source Scanner: " + sourceScanner);
	}

	/**
	 * Check if the output directory exist and is writable. If not, try to
	 * create an output dir and see if that is writable.
	 *
	 * @param outputDirectory The dir where the result will be placed
	 *
	 * @throws MojoExecutionException When the output directory is not writable
	 */
	private void checkOutDirWritable(File outputDirectory) throws MojoExecutionException {
		if (!outputDirectory.exists()) {
			checkIfOutputCanBeCreated();
			checkIfOutputDirIsWritable();
			if (verbose) {
				log.info("Output dir check OK");
			}
		}
		else if (!outputDirectory.canWrite()) {
			throw new MojoExecutionException(
					"The output dir exists but was not writable. "
							+ "Try running maven with the 'clean' goal.");
		}
	}

	private void configureJasper() {
		DefaultJasperReportsContext jrContext = DefaultJasperReportsContext.getInstance();

		jrContext.setProperty(JRReportSaxParserFactory.COMPILER_XML_VALIDATION, String.valueOf(xmlValidation));
		jrContext.setProperty(JRCompiler.COMPILER_PREFIX + JRReport.LANGUAGE_JAVA, compiler == null ? JRJdtCompiler.class.getName() : compiler);

		if (additionalProperties != null) {
			configureAdditionalProperties(JRPropertiesUtil.getInstance(jrContext));
		}
	}

	private ClassLoader getClassLoader(ClassLoader classLoader)
			throws MojoExecutionException {
		List<URL> classpath = new ArrayList<>();
		if (classpathElements != null) {
			for (String element : classpathElements) {
				try {
					File f = new File(element);
					classpath.add(f.toURI().toURL());
					log.debug("Added to classpath " + element);
				}
				catch (Exception e) {
					throw new MojoExecutionException(
							"Error setting classpath " + element + " " + e.getMessage());
				}
			}
		}

		if (additionalClasspath != null) {
			for (String element : additionalClasspath.split("[;]")) {
				try {
					File f = new File(element);
					classpath.add(f.toURI().toURL());
					log.debug("Added additionalClasspath to classpath " + element);
				}
				catch (Exception e) {
					throw new MojoExecutionException("Error setting classpath " + element + " " + e.getMessage());
				}
			}
		}

		URL[] urls = classpath.toArray(new URL[0]);
		return new URLClassLoader(urls, classLoader);
	}

	private void configureAdditionalProperties(JRPropertiesUtil propertiesUtil) {
		for (Map.Entry<String, String> additionalProperty : additionalProperties.entrySet()) {
			propertiesUtil.setProperty(additionalProperty.getKey(), additionalProperty.getValue());
		}
	}

	private void checkIfOutputCanBeCreated() throws MojoExecutionException {
		if (!outputDirectory.mkdirs()) {
			throw new MojoExecutionException(this, "Output folder could not be created", "Outputfolder "
					+ outputDirectory.getAbsolutePath() + " is not a folder");
		}
	}

	private void checkIfOutputDirIsWritable() throws MojoExecutionException {
		if (!outputDirectory.canWrite()) {
			throw new MojoExecutionException(this, "Could not write to output folder",
					"Could not write to output folder: " + outputDirectory.getAbsolutePath());
		}
	}

	private String getRelativePath(String root, File file) throws MojoExecutionException {
		try {
			return file.getCanonicalPath().substring(root.length() + 1);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not getCanonicalPath from file " + file, e);
		}
	}

	private List<CompileTask> generateTasks(Set<File> sources, SourceMapping mapping) throws MojoExecutionException {
		List<CompileTask> tasks = new LinkedList<>();
		try {
			String root = sourceDirectory.getCanonicalPath();

			for (File src : sources) {
				String srcName = getRelativePath(root, src);
				try {
					File destination = mapping.getTargetFiles(outputDirectory, srcName).iterator().next();
					createDestination(destination.getParentFile());
					tasks.add(new CompileTask(src, destination, log, verbose));
				}
				catch (InclusionScanException e) {
					throw new MojoExecutionException("Error compiling report design : " + src, e);
				}
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Could not getCanonicalPath from source directory " + sourceDirectory, e);
		}
		return tasks;
	}

	private void createDestination(File destinationDirectory) throws MojoExecutionException {
		if (!destinationDirectory.exists()) {
			if (destinationDirectory.mkdirs()) {
				log.debug("Created directory " + destinationDirectory.getName());
			}
			else {
				throw new MojoExecutionException("Could not create directory " + destinationDirectory.getName());
			}
		}
	}

	private void executeTasks(List<CompileTask> tasks) throws MojoExecutionException {
		ExecutorService threadPool = newThreadPool();
		try {
			long t1 = System.currentTimeMillis();
			List<Future<Void>> output =
					threadPool.invokeAll(tasks);
			long time = (System.currentTimeMillis() - t1);
			log.info("Generated " + output.size() + " jasper reports in " + (time / 1000.0) + " seconds");
			checkForExceptions(output);
		}
		catch (InterruptedException e) {
			log.error("Failed to compile Japser reports: Interrupted!", e);
			throw new MojoExecutionException("Error while compiling Jasper reports", e);
		}
		catch (ExecutionException e) {
			if (e.getCause() instanceof JRException) {
				throw new MojoExecutionException(ERROR_JRE_COMPILE_ERROR, e);
			}
			else {
				throw new MojoExecutionException("Error while compiling Jasper reports", e);
			}
		}
		finally {
			threadPool.shutdown();
		}
	}

	private ExecutorService newThreadPool() {
		return Executors.newFixedThreadPool(numberOfThreads, new JasperReporterThreadFactory());
	}

	private void checkForExceptions(List<Future<Void>> output) throws InterruptedException, ExecutionException {
		for (Future<Void> future : output) {
			future.get();
		}
	}

	private SourceInclusionScanner createSourceInclusionScanner() throws MojoExecutionException {
		if (sourceScanner.equals(StaleSourceScanner.class.getName())) {
			return new StaleSourceScanner();
		}
		else if (sourceScanner.equals(SimpleSourceInclusionScanner.class.getName())) {
			return new SimpleSourceInclusionScanner(Collections.singleton("**/*" + sourceFileExt),
					Collections.emptySet());
		}
		else {
			throw new MojoExecutionException("sourceScanner not supported: \'" + sourceScanner + "\'.");
		}
	}

    private boolean isSkip()
    {
        return skip;
    }

	/**
	 * Thread factory the compile threads. Sets the thread name and marks it as a daemon thread.
	 */
	private static final class JasperReporterThreadFactory implements ThreadFactory {

		private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "jasper-compiler-" + THREAD_COUNTER.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}

	}
}
