JasperReports-plugin
=============

This maven compiles Jasper files to the target directory. 

# 👋 This plugin is up for adoption 
I've not used this plugin myself in years, so I'd like to hand this project over
to people who actually use it. If you are interested, please open an issue to
reach me. - Alex

Motivation
----------
The original jasperreports-maven-plugin from org.codehaus.mojo was a bit slow. This plugin is 10x faster. I tested it with 52 reports which took 48 seconds with the original plugin and only 4.7 seconds with this plugin.

Usage
-----
You can use the plugin by adding it to the plug-in section in your pom;

```xml
<build>
	<plugins>
		<plugin>
			<groupId>com.alexnederlof</groupId>
			<artifactId>jasperreports-plugin</artifactId>
			<version>2.8</version>
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

