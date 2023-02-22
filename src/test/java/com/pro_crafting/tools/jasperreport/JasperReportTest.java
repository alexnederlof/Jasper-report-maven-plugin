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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.util.NullOutputStream;
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		examplesFolder = new File(getBasedir(), TARGET_EXAMPLE_FOLDER);
		assertTrue("The folder to copy the examples from doesn't exist", examplesFolder.exists());
	}


	/**
	 * Test the normal generation of Jasper reports. The files are retrieved from the official
	 * jasper examples folder. No errors or warnings should occur.
	 *
	 * @throws Exception
	 *             When an unexpexted error occures.
	 */
	public void testValidReportGeneration() throws Exception {
		String pluginPom = getBasedir() + "/src/test/resources/testSampleReportsPom.xml";

		setupSourceAndDestinationFolder("/sampleReports", "/sampleReports_out");

		getAndExecuteMojo(pluginPom);

		assertEquals("Files from sourcefolder do not correspond to files in the destinationFolder",
				sourceFolder.listFiles().length, destinationFolder.listFiles().length);
		assertAllFilesAreCompiled(sourceFolder, destinationFolder);

	}

	/**
	 * Test the normal generation of Jasper reports with additional properties. The files are
	 * retrieved from the official jasper examples folder. No errors or warnings should occur.
	 *
	 * @throws Exception
	 *             When an unexpexted error occures.
	 */
	public void testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationExpectNoErrorOnCompilation()
			throws Exception {
		String pluginPom = getBasedir() + "/src/test/resources/testSampleReportsWithAdditionalPropertiesPom.xml";
		setupSourceAndDestinationFolder("/sampleReports", "/sampleReports_out");

		getAndExecuteMojo(pluginPom);
		String defaultPdfFontName = DefaultJasperReportsContext.getInstance()
			.getProperty("net.sf.jasperreports.default.pdf.font.name");
		String pdfEmbeddedValue = DefaultJasperReportsContext.getInstance()
			.getProperty("net.sf.jasperreports.default.pdf.embedded");

		assertEquals("Files from sourcefolder do not correspond to files in the destinationFolder",
				sourceFolder.listFiles().length, destinationFolder.listFiles().length);
		assertAllFilesAreCompiled(sourceFolder, destinationFolder);
		assertTrue(defaultPdfFontName != null);
		assertTrue("default pdf font name has not been set properly", defaultPdfFontName.compareTo("Courier") == 0);
		assertTrue("net.sf.jasperreports.default.pdf.embedded has not been set properly",
				pdfEmbeddedValue.compareTo("true") == 0);
	}

	public void testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationAndExportToPdfExpectNoErrors()
			throws Exception {
		// compile reports with modified default font property first
		testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationExpectNoErrorOnCompilation();

		// now based on the templates, create PDF's
		assertTrue("Destination is not a directory", destinationFolder.isDirectory());

		List<File> testFiles = Arrays.asList(destinationFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.toString()
					.contains("PlainTextReportWithDefaultFontReport");
			}
		}));

		if (testFiles.size() != 1) {
			fail("Expected exactly one testfile to be found in directory");
		}

		createPdf("PlainTextReportWithDefaultFontReport.jasper");

	}

	private void createPdf(String filename) {
		File file = new File(destinationFolder.getPath() + "/" + filename);
		try {
			JasperPrint print = JasperFillManager.fillReport(new FileInputStream(file), new HashMap<String, Object>());
			JasperExportManager.exportReportToPdfStream(print, new NullOutputStream());
		}
		catch (IOException e) {
			fail("Unable to create exportfile: Errormessage:" + e.getMessage());
		}
		catch (JRException e) {
			fail("Unable to create pdf: Errormessage:" + e.getMessage());
		}
		catch (IllegalArgumentException e) {
			fail("Unable to create pdf: IllegalArgumentException:" + e.getMessage());
		}

	}

	private void getAndExecuteMojo(String pluginPom) throws Exception, MojoExecutionException {
		JasperReporter mojo = (JasperReporter) lookupMojo("jasper", pluginPom);
		assertNotNull(mojo);
		mojo.execute();
	}

	/**
	 * Create the source and destination folder. If the destination folder already exsist is shall
	 * be deleted. Otherwise the tests can't run properly.
	 */
	private void setupSourceAndDestinationFolder(String sourceFolderName, String destinationFolderName)
			throws IOException {
		sourceFolder = new File(getBasedir(), TARGET_EXAMPLE_FOLDER + sourceFolderName);
		destinationFolder = new File(getBasedir(), TARGET_EXAMPLE_OUT_FOLDER + destinationFolderName);
		if (destinationFolder.exists()) {
			FileUtils.deleteDirectory(destinationFolder);
		}
		assertTrue("Source folder doesn't exist: " + sourceFolder.getAbsolutePath(), sourceFolder.exists());
		assertFalse("Destination folder shouldn't exist", destinationFolder.exists());
	}

	/**
	 * For this method to work all files need to be in one folder. The could be enhanced later to
	 * also search all subfolders.
	 */
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
		return file.getName()
			.substring(0, file.getName()
				.indexOf(suffix));
	}

    /**
     * Test that an invalid Jasper file should stop the build completely by throwing an
     * {@link MojoExecutionException}.
     *
     * @throws Exception
     *             When an unexpected error occurs.
     */
    public void testInvalidFilesStopBuild() throws Exception {
        setupSourceAndDestinationFolder("/brokenReports", "/brokenReports_out");
        try {
            getAndExecuteMojo(getBasedir() + "/src/test/resources/testBrokenReportsPom.xml");
            fail("An exception should have been thrown");
        }
        catch (MojoExecutionException e) {
            assertEquals(JasperReporter.ERROR_JRE_COMPILE_ERROR, e.getMessage());
        }
    }

	/**
	 * Test that skipping the plugin does not compile any Jasper file.
	 *
     * @throws Exception
     *             When an unexpected error occurs.
	 */
	public void testSkipDoesntCompile() throws Exception {
		setupSourceAndDestinationFolder("/sampleReports", "/skipReports_out");
		getAndExecuteMojo(getBasedir() + "/src/test/resources/testSkipSampleReportsPom.xml");
        assertFalse("Output folder should not exist", destinationFolder.exists());
	}

	/**
	 * Test that all files with an invalid suffix are not compiled.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	public void testWrongSuffixDoesntCompile() throws Exception {
		setupSourceAndDestinationFolder("/wrongExtensions", "/wrongExtensions_out");
		getAndExecuteMojo(getBasedir() + "/src/test/resources/testWrongExtensionsPom.xml");
		assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
	}

	/**
	 * Test that an empty folder doesn't create errors but just does nothing.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	public void testEmptyDoesNothing() throws Exception {
		createTheEmptyFolderIfItDoesntExist();
		setupSourceAndDestinationFolder("/emptyFolder", "/emptyFolder_out");
		getAndExecuteMojo(getBasedir() + "/src/test/resources/testEmptyFolderPom.xml");
		assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
	}

	/**
	 * Test that a non-existent sourceDirectory fails the build.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	public void testNonExistentFolderStopBuild() throws Exception {
		try {
			getAndExecuteMojo(getBasedir() + "/src/test/resources/testNonExistentFolderPom.xml");
			fail("An exception should have been thrown");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("nonExistentFolder"));
		}
	}

	/**
	 * Test that a non-existent sourceDirectory just be skipped if failOnMissingSourceDirectory=true.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	public void testNonExistentFolderAllowed() throws Exception {
		setupSourceAndDestinationFolder("/emptyFolder", "/emptyFolder_out");
		getAndExecuteMojo(getBasedir() + "/src/test/resources/testNonExistentFolderAllowedPom.xml");
		assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
	}

	/**
	 * The empty folder we test on is not transported by Git. We therefor have to create it manually
	 * to do the test.
	 */
	private void createTheEmptyFolderIfItDoesntExist() {
		sourceFolder = new File(getBasedir(), TARGET_EXAMPLE_FOLDER + "/emptyFolder");
		if (!sourceFolder.exists()) {
			sourceFolder.mkdir();
		}
	}

	/**
	 * Test that the folder structure of the output is the same as the folder structure of the
	 * input.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	public void testFolderStructure() throws Exception {
		setupSourceAndDestinationFolder("/folderStructure", "/folderStructure_out");
		getAndExecuteMojo(getBasedir() + "/src/test/resources/testFolderStructurePom.xml");
		Set<String> filenames = detectFolderStructure(destinationFolder);
		String relativePath = destinationFolder.getAbsolutePath() + '/';
		String fileMissing = "A file in the folderstructure is missing";
		assertTrue(fileMissing, filenames.remove(new File(relativePath + "LandscapeReport.jasper").getAbsolutePath()));
		assertTrue(
				fileMissing,
				filenames.remove(new File(relativePath + "level.1/level.2.1/LateOrdersReport.jasper").getAbsolutePath()));
		assertTrue(fileMissing,
				filenames.remove(new File(relativePath + "level.1/level.2.2/MasterReport.jasper").getAbsolutePath()));
		assertTrue(
				fileMissing,
				filenames.remove(new File(relativePath + "level.1/level.2.2/Level.3/LineChartReport.jasper").getAbsolutePath()));
		assertTrue("There were more files found then expected", filenames.isEmpty());
	}

	private Set<String> detectFolderStructure(File folderToSearch) {
		Set<String> set = new HashSet<String>();
		for (File f : folderToSearch.listFiles()) {
			if (f.isDirectory()) {
				set.addAll(detectFolderStructure(f));
			}
			else {
				set.add(f.getAbsolutePath());
			}
		}
		return set;
	}

}
