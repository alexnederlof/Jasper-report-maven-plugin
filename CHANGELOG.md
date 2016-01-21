# 2.1 [unreleased]

- Added `failOnMissingSourceDirectory` property, so that one can continue the build while sourceDirectory does not exist.

# 2.0

This release sets Jasper to version 6 and updates some other deps. Also uses the 
new OSS release system instead of the old OSS parent pom.

# 1.9

- Fixes a bug that required a dependency on Servlet. [#13](https://github.com/alexnederlof/Jasper-report-maven-plugin/issues/13)

# 1.8

- Fixes [#7](https://github.com/alexnederlof/Jasper-report-maven-plugin/issues/7) For each `.jrxml` file search the compiled .jasper if not found or older than source file recompile it.
- Fixes [#9](https://github.com/alexnederlof/Jasper-report-maven-plugin/issues/9) Project classpath added to ClassLoader.

Thanks to [@lucarota](https://github.com/lucarota)

# 1.7

 - Allows developers to set additional properties [#5](https://github.com/alexnederlof/Jasper-report-maven-plugin/pull/5). Thanks to [@Plozi](https://github.com/plozi)

# 1.6

- Prepared the project to be hosted on Maven central. No code changes.

# 1.5

- Allow setting of a custom compiler.

# 1.4

-	Updated the Jasper version
