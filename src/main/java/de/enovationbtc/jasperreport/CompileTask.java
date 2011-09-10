package de.enovationbtc.jasperreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import org.apache.maven.plugin.logging.Log;

/**
 * A task that compiles a Jasper sourcefile.
 * 
 */
public class CompileTask implements Callable<String> {

   private final File source;
   private final File destFolder;
   private final String sourceExtension;
   private final String outExtension;
   private final Log log;

   public CompileTask(File source, File destFolder, String sourceExtension, String outExtension, Log log) {
      super();
      this.source = source;
      this.destFolder = destFolder;
      this.sourceExtension = sourceExtension;
      this.outExtension = outExtension;
      this.log = log;
   }

   /**
    * Compile the file source file.
    * @return Debug output of the compile action.
    * @throws Exception when anything goes wrong while compiling.
    */
   public String call() throws Exception {
      try {
         if (!source.getAbsolutePath().endsWith(sourceExtension)) {
            String txt = "Skipped " + source.getName() + " because it doesn't have the extention: " + sourceExtension;
            log.info(txt);
            return txt;
         } else {
            String newFilename = getNewFilename();
            File out = new File(destFolder, newFilename);
            JasperCompileManager.compileReportToStream(new FileInputStream(source), new FileOutputStream(out));
            log.info("Compiling " + newFilename);
            return "compiled " + newFilename;
         }
      } catch (JRException e) {
         log.error("Could not compile " + source.getName(), e);
         throw e;
      }
   }

   private String getNewFilename() {
      return source.getName().substring(0, source.getName().lastIndexOf(sourceExtension)) + outExtension;
   }

}
