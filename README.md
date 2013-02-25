JasperReports-plugin
=============

This maven compiles Jasper files to the target directory. 

Motivation
----------
The original jasperreports-maven-plugin from org.codehaus.mojo was a bit slow. This plugin is 10x faster. I tested it with 52 reports which took 48 seconds with the original plugin and only 4.7 seconds with this plugin.

Usage
-----
You can use the plugin by adding it to the plug-in section in your pom;

	<build>
		<plugins>
			<plugin>
				<groupId>com.alexnederlof</groupId>
				<artifactId>jasperreports-plugin</artifactId>
				<version>1.6</version>
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
