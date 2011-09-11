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
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * Test the report generation.
 */
public class JasperReportTest extends AbstractMojoTestCase {

   private static final String TARGET_EXAMPLE_FOLDER = "target/test-classes/exampleFolders";
   private static final String TARGET_EXAMPLE_OUT_FOLDER = "target/unitTestReports";
   private File examplesFolder;
   private File sourceFolder;
   private File destinationFolder;
   private File exampleOutputDir;

   @Override
   protected void setUp() throws Exception {
      super.setUp();
      examplesFolder = new File(getBasedir(), TARGET_EXAMPLE_FOLDER);
      exampleOutputDir = new File(getBasedir(), TARGET_EXAMPLE_OUT_FOLDER);
      assertTrue("The folder to copy the examples from doesn't exist", examplesFolder.exists());      
   }

   @Override
   protected void tearDown() throws Exception {
      super.tearDown();      
      for (File f : examplesFolder.listFiles()) {
         if (f.isDirectory() && f.getName().endsWith("_out")) {
//            FileUtils.deleteDirectory(f);
         }
      }
   }

   /**
    * Test the normal generation of Jasper reports. The files are retrieved from
    * the official jasper examples folder. No errors or warnings should occur.
    * 
    * @throws Exception When an unexpexted error occures.
    */
   public void testValidReportGeneration() throws Exception {
      String pluginPom = getBasedir() + "/src/test/resources/testSampleReportsPom.xml";

      setupSourceAndDestinationFolder("/sampleReports", "/sampleReports_out");

      getAndExecuteMojo(pluginPom);

      assertEquals("Files from sourcefolder do not correspond to files in the destinationFolder",
            sourceFolder.listFiles().length, destinationFolder.listFiles().length);
      assertAllFilesAreCompiled(sourceFolder, destinationFolder);

   }

   public void getAndExecuteMojo(String pluginPom) throws Exception, MojoExecutionException {
      JasperReporter mojo = (JasperReporter) lookupMojo("jasper", pluginPom);
      assertNotNull(mojo);
      mojo.execute();
   }

   public void setupSourceAndDestinationFolder(String sourceFolderName, String destinationFolderName) {
      sourceFolder = new File(getBasedir(), TARGET_EXAMPLE_FOLDER + sourceFolderName);
      destinationFolder = new File(getBasedir(), TARGET_EXAMPLE_OUT_FOLDER + destinationFolderName);
      if (destinationFolder.exists()) {
         destinationFolder.delete();
      }
      assertTrue("Source folder doesn't exist: " + sourceFolder.getAbsolutePath(), sourceFolder.exists());
      assertFalse("Destination folder shouldn't exist", destinationFolder.exists());
   }

   private void assertAllFilesAreCompiled(File sourceFolder, File destinationFolder) {
      assertTrue("Source folder is not a directory", sourceFolder.isDirectory());
      assertTrue("Destination is not a directory", destinationFolder.isDirectory());
      Set<String> filenames = new HashSet<String>();
      for (File file : sourceFolder.listFiles()) {
         if (file.isFile()) {
            filenames.add(getNameWithoutSuffix(file, ".jrxml"));
         }
      }
      for (File file : destinationFolder.listFiles()) {
         if (file.isFile()) {
            filenames.remove(getNameWithoutSuffix(file, ".jasper"));
         }
      }
      assertTrue("Files from sourcefolder do not correspond to files in the destinationFolder", filenames.isEmpty());
   }

   private String getNameWithoutSuffix(File file, String suffix) {
      return file.getName().substring(0, file.getName().indexOf(suffix));
   }

   /**
    * Test that an invalid Jasper file should stop the build completely by
    * throwing an {@link MojoExecutionException}.
    * 
    * @throws Exception When an unexpected error occurs.
    */
   public void testInvalidFilesStopBuild() throws Exception {
      setupSourceAndDestinationFolder("/brokenReports", "/brokenReports_out");
      try {
         getAndExecuteMojo(getBasedir() + "/src/test/resources/testBrokenReportsPom.xml");
         fail("An exception should have been thrown");
      } catch (MojoExecutionException e) {
         assertEquals(JasperReporter.ERROR_JRE_COMPILE_ERROR, e.getMessage());
      }
   }

   /**
    * Test that all files with an invalid suffix are not compiled.
    * 
    * @throws Exception When an unexpected error occurs.
    */
   public void testWrongSuffixDoesntCompile() throws Exception {
      setupSourceAndDestinationFolder("/wrongExtensions", "/wrongExtensions_out");
      getAndExecuteMojo(getBasedir() + "/src/test/resources/testWrongExtensionsPom.xml");
      assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
   }

   /**
    * Test that an empty folder doesn't create errors but just does nothing.
    * 
    * @throws Exception When an unexpected error occurs.
    */
   public void testEmptyDoesNothing() throws Exception {
      createTheEmptyFolderIfItDoesntExist();
      setupSourceAndDestinationFolder("/emptyFolder", "/emptyFolder_out");
      getAndExecuteMojo(getBasedir() + "/src/test/resources/testEmptyFolderPom.xml");
      assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
   }

   /**
    * The empty folder we test on is not transported by Git. We therefor have to
    * create it manually to do the test.
    */
   private void createTheEmptyFolderIfItDoesntExist() {
      sourceFolder = new File(getBasedir(), TARGET_EXAMPLE_FOLDER + "/emptyFolder");
      if (!sourceFolder.exists()) {
         sourceFolder.mkdir();
      }
   }

   /**
    * Test that the folder structure of the output is the same as the folder structure of the input.
    * @throws Exception
    */
   public void testFolderStructure() throws Exception {
      setupSourceAndDestinationFolder("/folderStructure", "/folderStructure_out");
      getAndExecuteMojo(getBasedir() + "/src/test/resources/testFolderStructurePom.xml");
      Set<String> filenames = detectFolderStructure(destinationFolder);
      String relativePath = destinationFolder.getAbsolutePath() + '/';
      String fileMissing = "A file in the folderstructure is missing";
      assertTrue(fileMissing, filenames.remove(relativePath + "LandscapeReport.jasper"));
      assertTrue(fileMissing, filenames.remove(relativePath + "level.1/level.2.1/LateOrdersReport.jasper"));
      assertTrue(fileMissing, filenames.remove(relativePath + "level.1/level.2.2/MasterReport.jasper"));
      assertTrue(fileMissing, filenames.remove(relativePath + "level.1/level.2.2/Level.3/LineChartReport.jasper"));
      assertTrue("There were more files found then expected", filenames.isEmpty());
   }

   private Set<String> detectFolderStructure(File folderToSearch) {
      Set<String> set = new HashSet<String>();
      for (File f : folderToSearch.listFiles()) {
         if (f.isDirectory()) {
            set.addAll(detectFolderStructure(f));
         } else {
            set.add(f.getAbsolutePath());
         }
      }
      return set;
   }

   

}
