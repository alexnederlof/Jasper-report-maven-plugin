package com.alexnederlof.jasperreport;

import java.util.Map;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.design.JRCompiler;
import net.sf.jasperreports.engine.design.JRJdtCompiler;
import net.sf.jasperreports.engine.xml.JRReportSaxParserFactory;

public class Jasper460Configurator implements JasperConfigurator {

	@Override
	public void configure(JasperReporter reporter) {
		
		DefaultJasperReportsContext jrContext = DefaultJasperReportsContext.getInstance();

		boolean xmlValidation = reporter.getXmlValidation();
		String compiler = reporter.getCompiler();
		Map<String, String> additionalProperties = reporter.getAdditionalProperties();

		jrContext.setProperty(JRReportSaxParserFactory.COMPILER_XML_VALIDATION, String.valueOf(xmlValidation));
		jrContext.setProperty(JRCompiler.COMPILER_PREFIX, compiler == null ? JRJdtCompiler.class.getName() : compiler);
		jrContext.setProperty(JRCompiler.COMPILER_KEEP_JAVA_FILE, Boolean.FALSE.toString());

		if (additionalProperties != null) {
			JRPropertiesUtil propertiesUtil = JRPropertiesUtil.getInstance(jrContext);
			for (Map.Entry<String, String> additionalProperty : additionalProperties.entrySet()) {
				propertiesUtil.setProperty(additionalProperty.getKey(), additionalProperty.getValue());
			}
		}
	}

	@Override
	public void revert() {
		// No revert needed
	}
}