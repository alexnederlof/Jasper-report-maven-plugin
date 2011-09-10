package de.enovationbtc.jasperreport;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.util.JRProperties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * @goal jasper
 * @phase process-resources
 */
public class JasperReporter extends AbstractMojo {

   static final String ERROR_JRE_COMPILE_ERROR = "Some Jasper reports could not be compiled. See log above for details.";

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
    * The number of threads the reporting will use. Default is 4 which is good
    * for a lot of reports on a hard drive (in stead of SSD). If you only have a few, 
    * or if you have SSD, it might be faster to set it to 2.
    * 
    * @parameter default-value=4
    */
   private int numberOfThreads;

   private Log log;

   @Override
   public void execute() throws MojoExecutionException {
      log = getLog();

      if (outputDirectory.exists() && outputDirectory.listFiles().length > 0) {
         log.info("It seems the Jasper reports are already compiled. If you want to re-compile, run maven "
               + "with the 'clean' goal.");
         return;
      }

      logConfiguration(log);

      checkOutDirWritable(outputDirectory);

      List<File> files = getFiles(sourceDirectory);

      if (files.isEmpty()) {
         log.info("No files found to compile.");
         return;
      }

      log.info("Found " + files.size() + " files");

      JRProperties.setProperty(JRProperties.COMPILER_XML_VALIDATION, xmlValidation);

      List<CompileTask> tasks = generateTasks(files);
      executeTasks(tasks);
   }

   private void logConfiguration(Log log) {
      log.info("Generating Jasper reports");
      log.info("Outputdir=" + outputDirectory.getAbsolutePath());
      log.info("Sourcedir=" + sourceDirectory.getAbsolutePath());
      log.info("Output ext=" + outputFileExt);
      log.info("Source ext=" + sourceFileExt);
      log.info("XML Validation=" + xmlValidation);
      log.info("Numer of threads:" + numberOfThreads);
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
            throw new MojoExecutionException("The output dir was not writable. "
                  + "Try running maven with the 'clean' goal.");
         }
      } else if (!outputDirectory.mkdirs()) {
         throw new MojoExecutionException(this, "Output folder could not be created",
               "Outputfolder " + outputDirectory.getAbsolutePath() + " is not a folder");
      } else if (!outputDirectory.canWrite()) {
         throw new MojoExecutionException(this, "Could not write to output folder",
               "Could not write to output folder: " + outputDirectory.getAbsolutePath());
      } else {
         getLog().info("Output dir check OK");
      }
   }

   private List<File> getFiles(File directory) {
      if (directory == null || !directory.exists() || !directory.isDirectory()) {
         return Collections.emptyList();
      }
      return Arrays.asList(directory.listFiles());
   }

   private List<CompileTask> generateTasks(List<File> files) {
      List<CompileTask> tasks = new ArrayList<CompileTask>(files.size());
      for (File file : files) {
         tasks.add(new CompileTask(file, outputDirectory, sourceFileExt, outputFileExt, log));
      }
      return tasks;
   }

   private void executeTasks(List<CompileTask> tasks) throws MojoExecutionException {
      try {
         long t1 = System.currentTimeMillis();
         List<Future<String>> output = Executors.newFixedThreadPool(numberOfThreads).invokeAll(tasks);
         long time = (System.currentTimeMillis() - t1) / 1000;
         getLog().info("Generated " + output.size() + " reports in " + time + " seconds");
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

   private void checkForExceptions(List<Future<String>> output) throws InterruptedException, ExecutionException {
      for (Future<String> future : output) {
         future.get();         
      }
   }

}
