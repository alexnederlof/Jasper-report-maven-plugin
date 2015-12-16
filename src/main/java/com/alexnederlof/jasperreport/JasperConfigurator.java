package com.alexnederlof.jasperreport;

public interface JasperConfigurator {

	void configure(JasperReporter reporter);
	
	void revert();
	
}
