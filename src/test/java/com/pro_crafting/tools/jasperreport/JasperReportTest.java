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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.util.NullOutputStream;


/**
 * Test the report generation.
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions("3.6.3")
public class JasperReportTest {

	@Rule
	public final TestResources resources = new TestResources();

	private final MavenRuntime mavenRuntime;

	public JasperReportTest(MavenRuntimeBuilder builder) throws Exception {
		this.mavenRuntime = builder.build();
	}


	/**
	 * Test the normal generation of Jasper reports. The files are retrieved from the official
	 * jasper examples folder. No errors or warnings should occur.
	 *
	 * @throws Exception
	 *             When an unexpexted error occures.
	 */
	@Test
	public void testValidReportGeneration() throws Exception {
		File basedir = this.resources.getBasedir("sampleReports");

		MavenExecutionResult result = getAndExecuteMojo(basedir);
		result.assertErrorFreeLog();

		File sourceFolder = new File(basedir, "src/main/jasperreports");
		File destinationFolder = new File(basedir, "target/classes/jasper");

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
	@Test
	public void testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationExpectNoErrorOnCompilation()
			throws Exception {
		File basedir = this.resources.getBasedir("sampleReportsWithAdditionalProperties");

		runSampleReportsWithAdditionalProperties(basedir);
	}


	private void runSampleReportsWithAdditionalProperties(File basedir) throws Exception, MojoExecutionException {
		MavenExecutionResult result = getAndExecuteMojo(basedir);
		result.assertErrorFreeLog();

		File sourceFolder = new File(basedir, "src/main/jasperreports");
		File destinationFolder = new File(basedir, "target/classes/jasper");

		assertEquals("Files from sourcefolder do not correspond to files in the destinationFolder",
				sourceFolder.listFiles().length, destinationFolder.listFiles().length);
		assertAllFilesAreCompiled(sourceFolder, destinationFolder);
		result.assertLogText("net.sf.jasperreports.default.pdf.font.name=Courier");
		result.assertLogText("net.sf.jasperreports.default.pdf.embedded=true");
	}

	@Test
	public void testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationAndExportToPdfExpectNoErrors()
			throws Exception {
		// compile reports with modified default font property first
		File basedir = this.resources.getBasedir("sampleReportsWithAdditionalProperties");

		runSampleReportsWithAdditionalProperties(basedir);

		// now based on the templates, create PDF's
		File destinationFolder = new File(basedir, "target/classes/jasper");
		assertTrue("Destination is not a directory", destinationFolder.isDirectory());

		List<File> testFiles = Arrays.asList(destinationFolder.listFiles(
				pathname -> pathname.toString().contains("PlainTextReportWithDefaultFontReport")));

		assertEquals("Expected exactly one testfile to be found in directory", 1, testFiles.size());

		createPdf(destinationFolder, "PlainTextReportWithDefaultFontReport.jasper");

	}

	private void createPdf(File destinationFolder, String filename) throws Exception {
		File file = new File(destinationFolder, filename);
		JasperPrint print;
		try (InputStream inputStream = new FileInputStream(file)) {
			print = JasperFillManager.fillReport(new FileInputStream(file), new HashMap<>());
		}
		JasperExportManager.exportReportToPdfStream(print, new NullOutputStream());
	}

	private MavenExecutionResult getAndExecuteMojo(File basedir) throws Exception, MojoExecutionException {
		MavenExecution execution = this.mavenRuntime.forProject(basedir);

		return execution.execute("process-sources");
	}

	/**
	 * For this method to work all files need to be in one folder. The could be enhanced later to
	 * also search all subfolders.
	 */
	private void assertAllFilesAreCompiled(File sourceFolder, File destinationFolder) {
		assertTrue("Source folder is not a directory", sourceFolder.isDirectory());
		assertTrue("Destination is not a directory", destinationFolder.isDirectory());
		Set<String> filenames = new HashSet<>();
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
	@Test
	public void testInvalidFilesStopBuild() throws Exception {

		File basedir = this.resources.getBasedir("brokenReports");

		MavenExecutionResult result = getAndExecuteMojo(basedir);

		result.assertLogText("BUILD FAILURE");
		result.assertLogText(JasperReporter.ERROR_JRE_COMPILE_ERROR);

		File destinationFolder = new File(basedir, "target/classes/jasper");
		assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
	}

	/**
	 * Test that skipping the plugin does not compile any Jasper file.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	@Test
	public void testSkipDoesntCompile() throws Exception {
		File basedir = this.resources.getBasedir("skipSampleReports");

		MavenExecutionResult result = getAndExecuteMojo(basedir);
		result.assertErrorFreeLog();

		File destinationFolder = new File(basedir, "target/classes/jasper");
		assertFalse("Output folder should not exist", destinationFolder.exists());
	}

	/**
	 * Test that all files with an invalid suffix are not compiled.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	@Test
	public void testWrongSuffixDoesntCompile() throws Exception {
		File basedir = this.resources.getBasedir("wrongExtensions");

		MavenExecutionResult result = getAndExecuteMojo(basedir);
		result.assertErrorFreeLog();

		File destinationFolder = new File(basedir, "target/classes/jasper");
		assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
	}

	/**
	 * Test that an empty folder doesn't create errors but just does nothing.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	@Test
	public void testEmptyDoesNothing() throws Exception {
		File basedir = this.resources.getBasedir("emptyFolder");

		MavenExecutionResult result = getAndExecuteMojo(basedir);
		result.assertErrorFreeLog();

		File destinationFolder = new File(basedir, "target/classes/jasper");
		assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
	}

	/**
	 * Test that a non-existent sourceDirectory fails the build.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	@Test
	public void testNonExistentFolderStopBuild() throws Exception {
		File basedir = this.resources.getBasedir("nonExistentFolder");

		MavenExecutionResult result = getAndExecuteMojo(basedir);

		result.assertLogText("BUILD FAILURE");
		result.assertLogText("nonExistentFolder");
	}

	/**
	 * Test that a non-existent sourceDirectory just be skipped if failOnMissingSourceDirectory=true.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	@Test
	public void testNonExistentFolderAllowed() throws Exception {
		File basedir = this.resources.getBasedir("nonExistentFolderAllowed");

		MavenExecutionResult result = getAndExecuteMojo(basedir);
		result.assertErrorFreeLog();

		File destinationFolder = new File(basedir, "target/classes/jasper");
		assertTrue("Output folder should be empty", destinationFolder.list().length == 0);
	}

	/**
	 * Test that the folder structure of the output is the same as the folder structure of the
	 * input.
	 *
	 * @throws Exception
	 *             When an unexpected error occurs.
	 */
	@Test
	public void testFolderStructure() throws Exception {
		File basedir = this.resources.getBasedir("folderStructure");

		MavenExecutionResult result = getAndExecuteMojo(basedir);
		result.assertErrorFreeLog();

		File destinationFolder = new File(basedir, "target/classes/jasper");
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
		Set<String> set = new HashSet<>();
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
