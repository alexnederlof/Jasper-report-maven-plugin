[![Version](https://img.shields.io/maven-central/v/com.pro-crafting.tools/jasperreports-plugin?logo=apache-maven&style=for-the-badge)](https://search.maven.org/artifact/com.pro-crafting.tools/jasperreports-pluginm)
[![GitHub Actions Status](<https://img.shields.io/github/actions/workflow/status/pro-crafting/Jasper-report-maven-plugin/build.yml?branch=main&logo=GitHub&style=for-the-badge>)](https://github.com/pro-crafting/Jasper-report-maven-plugin/actions/workflows/build.yml)

JasperReports-plugin
=============

This maven plugin compiles Jasper files to the target directory. 

Migration to version 3
----------

[alexnederlof](https://github.com/alexnederlof) is the original author of this plugin, but has not used it in years. The plugin got adopted by me ([Postremus](https://github.com/Postremus)) and migrated to the pro-crafting organization.
Main goal of the adoption is to always provide a version for the latest JasperReports release.

For this reason, when migrating from jasperreports-plugin 2.8 to 3.0, you will need to keep these things in mind:

1. First, you will need to change the groupdId in the plugin definition, as outlined in section [Usage](#usage)
2. Second, the plugin now gets compiled using jdk 17. Compatibility with java 1.8 is ensured, since the CI build now forces an `--release 8` flag. If any problems arise though, please let me know by opening an issue - we can always adjust that.

Motivation
----------
The original jasperreports-maven-plugin from org.codehaus.mojo was a bit slow. This plugin is 10x faster. I tested it with 52 reports which took 48 seconds with the original plugin and only 4.7 seconds with this plugin.

Usage
-----
You can use the plugin by adding it to the plug-in section in your pom:

```xml
<build>
	<plugins>
		<plugin>
			<groupId>com.pro-crafting.tools</groupId>
			<artifactId>jasperreports-plugin</artifactId>
			<version>3.5.1</version>
			<executions>
				<execution>
					<phase>process-sources</phase>
	   				<goals>
	      					<goal>jasper</goal>
	   				</goals>
	   			</execution>
			</executions>
			<configuration>
				<!-- These are the default configurations: -->
				<compiler>net.sf.jasperreports.engine.design.JRJdtCompiler</compiler>
				<sourceDirectory>src/main/jasperreports</sourceDirectory>
				<outputDirectory>${project.build.directory}/jasper</outputDirectory>
				<outputFileExt>.jasper</outputFileExt>
				<xmlValidation>true</xmlValidation>
				<verbose>false</verbose>
				<numberOfThreads>4</numberOfThreads>
				<failOnMissingSourceDirectory>true</failOnMissingSourceDirectory>
				<sourceScanner>org.codehaus.plexus.compiler.util.scan.StaleSourceScanner</sourceScanner>
			</configuration>
		</plugin>
	</plugins>
</build>
```

You might also need to add the following repositories section to your pom in order to correctly resolve all dependencies:

```xml
<repositories>
    <repository>
        <id>jaspersoft-third-party</id>
        <url>https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/</url>
    </repository>
</repositories>
```

If you want to pass any Jasper options to the compiler you can do so by adding them to the configuration like so:

```xml
<plugin>
	...
	<configuration>
		...
		<additionalProperties>
			<net.sf.jasperreports.awt.ignore.missing.font>true</net.sf.jasperreports.awt.ignore.missing.font>
			<net.sf.jasperreports.default.pdf.font.name>Courier</net.sf.jasperreports.default.pdf.font.name>
			<net.sf.jasperreports.default.pdf.encoding>UTF-8</net.sf.jasperreports.default.pdf.encoding>
			<net.sf.jasperreports.default.pdf.embedded>true</net.sf.jasperreports.default.pdf.embedded>
           </additionalProperties>
	</configuration>
</plugin>
```

You can also add extra elements to the classpath using

```xml
<plugin>
	...
	<configuration>
		...
		<classpathElements>
			<element>your.classpath.element</element>
        </classpathElements>
	</configuration>
</plugin>
```

You can also use this alternative approach for JARs:

```xml
<plugin>
	...
	<configuration>
		...
		<additionalClasspath>/web/lib/ServiceBeans.jar;/web/lib/WebForms.jar</additionalClasspath>
	</configuration>
</plugin>
```

JasperReports Compatibility
-----

The following table shows which version of JasperReports is used for compilation for each plugin version

| Plugin Version | JasperReports Version |
|----------------|-----------------------|
| 3.5.1          | 6.20.1                |
| 3.5.0          | 6.20.0                |
| 3.4.1          | 6.19.1                |
| 3.4.0          | 6.19.0                |
| 3.3.1          | 6.18.1                |
| 3.3.0          | 6.18.0                |
| 3.2.0          | 6.17.0                |
| 3.1.0          | 6.16.0                |
| 3.0.0          | 6.15.0                |
