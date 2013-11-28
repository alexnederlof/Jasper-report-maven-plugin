package com.alexnederlof.jasperreport;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.design.JRCompiler;
import net.sf.jasperreports.engine.design.JRJdtCompiler;
import net.sf.jasperreports.engine.xml.JRReportSaxParserFactory;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This plugin compiles jasper source files to the target folder. While doing
 * so, it keeps the folder structure in tact.
 *
 * @goal jasper
 * @phase process-resources
 */
public class JasperReporter extends AbstractMojo {

    static final String ERROR_JRE_COMPILE_ERROR = "Some Jasper reports could not be compiled. See log above for details.";

    /**
     * This is the java compiler used
     *
     * @parameter default-value="net.sf.jasperreports.engine.design.JRJdtCompiler"
     * @required
     */
    private String compiler;

    /**
     * This is where the .jasper files are written.
     *
     * @parameter expression="${project.build.outputDirectory}/jasper"
     */
    private File outputDirectory;

    /**
     * This is where the xml report design files should be.
     *
     * @parameter default-value="src/main/jasperreports"
     */
    private File sourceDirectory;

    /**
     * The extension of the source files to look for. Finds files with a .jrxml
     * extension by default.
     *
     * @parameter default-value=".jrxml"
     */
    private String sourceFileExt;

    /**
     * The extension of the compiled report files. Creates files with a .jasper
     * extension by default.
     *
     * @parameter default-value=".jasper"
     */
    private String outputFileExt;

    /**
     * Check the source files before compiling. Default value is true.
     *
     * @parameter default-value="true"
     */
    private boolean xmlValidation;

    /**
     * If verbose is on the plug-in will report which reports it is compiling and
     * which files are being skipped.
     *
     * @parameter default-value="false"
     */
    private boolean verbose;

    /**
     * The number of threads the reporting will use. Default is 4 which is good
     * for a lot of reports on a hard drive (in stead of SSD). If you only have a
     * few, or if you have SSD, it might be faster to set it to 2.
     *
     * @parameter default-value=4
     */
    private int numberOfThreads;

    /*
    *
    * @parameter
    *
    * */
    private Map<String, String> additionalProperties;

    private Log log;

    public JasperReporter() {
    }

    @Override
    public void execute() throws MojoExecutionException {
        log = getLog();

        if (outputDirectory.exists() && outputDirectory.listFiles().length > 0) {
            log.info("It seems the Jasper reports are already compiled. If you want to re-compile, run maven "
                    + "with the 'clean' goal.");
            return;
        }
        if (1 == 1) {
            logConfiguration(log);
        }
        checkOutDirWritable(outputDirectory);

        configureJasper();

        List<CompileTask> tasks = generateTasks(sourceDirectory, outputDirectory);

        if (tasks.isEmpty()) {
            log.info("Nothing to compile");
            return;
        }
        executeTasks(tasks);
    }

    private void logConfiguration(Log log) {
        log.info("Generating Jasper reports");
        log.info("Outputdir=" + outputDirectory.getAbsolutePath());
        log.info("Sourcedir=" + sourceDirectory.getAbsolutePath());
        log.info("Output ext=" + outputFileExt);
        log.info("Source ext=" + sourceFileExt);
        log.info("Addition properties=" + additionalProperties);
        log.info("XML Validation=" + xmlValidation);
        log.info("JasperReports Compiler=" + compiler);
        log.info("Number of threads:" + numberOfThreads);
    }

    /**
     * Check if the output directory exist and is writable. If not, try to create
     * an output dir and see if that is writable.
     *
     * @param outputDirectory
     * @throws MojoExecutionException
     */
    private void checkOutDirWritable(File outputDirectory) throws MojoExecutionException {
        if (outputDirectory.exists()) {
            if (outputDirectory.canWrite()) {
                return;
            } else {
                throw new MojoExecutionException("The output dir exists but was not writable. "
                        + "Try running maven with the 'clean' goal.");
            }
        }
        checkIfOutpuCanBeCreated();
        checkIfOutputDirIsWritable();
        if (verbose) {
            getLog().info("Output dir check OK");
        }
    }

    private void configureJasper() {
        DefaultJasperReportsContext jasperReportsContext = DefaultJasperReportsContext.getInstance();
        jasperReportsContext.setProperty(JRReportSaxParserFactory.COMPILER_XML_VALIDATION, String.valueOf(xmlValidation));
        jasperReportsContext.setProperty(JRCompiler.COMPILER_PREFIX, compiler == null ?
                JRJdtCompiler.class.getName() : compiler);
        jasperReportsContext.setProperty(JRCompiler.COMPILER_KEEP_JAVA_FILE, Boolean.FALSE.toString());

        if (additionalProperties != null) {
            configureAdditionalProperties(JRPropertiesUtil.getInstance(jasperReportsContext));
        }
    }

    private void configureAdditionalProperties(JRPropertiesUtil propertiesUtil) {
        for (Map.Entry<String, String> additionalProperty : additionalProperties.entrySet()) {
            propertiesUtil.setProperty(additionalProperty.getKey(), additionalProperty.getValue());
        }
    }

    private void checkIfOutpuCanBeCreated() throws MojoExecutionException {
        if (!outputDirectory.mkdirs()) {
            throw new MojoExecutionException(this, "Output folder could not be created",
                    "Outputfolder " + outputDirectory.getAbsolutePath() + " is not a folder");
        }
    }

    private void checkIfOutputDirIsWritable() throws MojoExecutionException {
        if (!outputDirectory.canWrite()) {
            throw new MojoExecutionException(this, "Could not write to output folder",
                    "Could not write to output folder: " + outputDirectory.getAbsolutePath());
        }
    }

    private List<CompileTask> generateTasks(File sourceDirectory, File destinationDirectory) {
        Validate.isTrue(sourceDirectory.isDirectory(), sourceDirectory.getName() + " is not a directory");
        List<CompileTask> tasks = new LinkedList<CompileTask>();
        for (File f : sourceDirectory.listFiles()) {
            generateTasks(destinationDirectory, tasks, f);
        }
        return tasks;
    }

    private void generateTasks(File destinationDirectory, List<CompileTask> tasks, File f) {
        if (f.isDirectory()) {
            tasks.addAll(generateTasks(f, createNewDest(f, destinationDirectory)));
        } else { // It is a file
            if (f.getName().endsWith(sourceFileExt)) {
                tasks.add(new CompileTask(f, destinationDirectory, sourceFileExt, outputFileExt, log, verbose));
            } else if (verbose) {
                log.info("Skipped " + f.getName() + " because it doesnt have the extension " + sourceFileExt);
            }
        }
    }

    private File createNewDest(File origin, File destinationDirectory) {
        File newDest = new File(destinationDirectory, origin.getName());
        newDest.mkdir();
        return newDest;
    }

    private void executeTasks(List<CompileTask> tasks) throws MojoExecutionException {
        try {
            long t1 = System.currentTimeMillis();
            List<Future<Void>> output = Executors.newFixedThreadPool(numberOfThreads).invokeAll(tasks);
            long time = (System.currentTimeMillis() - t1);
            getLog().info("Generated " + output.size() + " jasper reports in " + (time / 1000.0) + " seconds");
            checkForExceptions(output);
        } catch (InterruptedException e) {
            log.error("Failed to compile Japser reports: Interrupted!", e);
            throw new MojoExecutionException("Error while compiling Jasper reports", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof JRException) {
                throw new MojoExecutionException(ERROR_JRE_COMPILE_ERROR, e);
            } else {
                throw new MojoExecutionException("Error while compiling Jasper reports", e);
            }
        }
    }

    private void checkForExceptions(List<Future<Void>> output) throws InterruptedException, ExecutionException {
        for (Future<Void> future : output) {
            future.get();
        }
    }

}
