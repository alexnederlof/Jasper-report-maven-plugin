package com.alexnederlof.jasperreport;

import static org.apache.commons.lang.StringUtils.join;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.jasperreports.engine.util.JRProperties;

@SuppressWarnings("deprecation")
public class Jasper353Configurator implements JasperConfigurator {

	@Override
	public void configure(JasperReporter reporter) {

		JRProperties.backupProperties();
		
		String compiler = reporter.getCompiler();
		if (compiler != null) {
			JRProperties.setProperty(JRProperties.COMPILER_CLASS, compiler);
		}

		List<String> classpathElements = reporter.getClasspathElements();
		if (!classpathElements.isEmpty()) {
			JRProperties.setProperty(JRProperties.COMPILER_CLASSPATH, join(classpathElements, reporter.getPathSeparatorChar()));
		}

		JRProperties.setProperty(JRProperties.COMPILER_XML_VALIDATION, reporter.getXmlValidation());

		Map<String, String> additionalProperties = reporter.getAdditionalProperties();
		if (additionalProperties != null) {
			for (Entry<String, String> property : additionalProperties.entrySet()) {
				JRProperties.setProperty(property.getKey(), property.getValue());
			}
		}
	}

	@Override
	public void revert() {
		JRProperties.restoreProperties();
	}
	
}