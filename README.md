JasperReports-plugin
=============

This maven compiles Jasper files to the target directory. 

Motivation
----------
The original jasperreports-maven-plugin from org.codehaus.mojo was a bit slow. This plugin is 10x faster. I tested it with 52 reports which took 48 seconds with the original plugin and only 4.7 seconds with this plugin.

Usage
-----
This plugin is not (yet) in the central maven repo. To use it you can clone this repository. After that just run

	mvn clean install

You can then use the plugin using by adding this to the pom: (replace the version number!);

	<build>
		<plugins>
			<plugin>
				<groupId>com.alexnederlof</groupId>
				<artifactId>jasperreports-plugin</artifactId>
				<version>1.2-SNAPSHOT</version>
				<executions>
					<execution>
						<phase>process-sources</phase>
		   				<goals>
		      				<goal>jasper</goal>
		   				</goals>
		   			</execution>
				</executions>
				<configuration>
					<outputDirectory>${project.build.directory}/jasper</outputDirectory>					
				</configuration>
			</plugin>
		</plugins>
	</build>
