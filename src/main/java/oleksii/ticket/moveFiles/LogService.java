package oleksii.ticket.moveFiles;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * simple log file creator
 */
@Component
public class LogService {
  public LogService() {
  }
  @Value("${output.logFile}")
  String logFilePath;
  FileHandler fh = null;
  Logger logger = Logger.getLogger("LogService");
  /**
   * Method takes string message that we want to log into a file, creates file and appends message
   * @param message
   */
  public void writeToLog(String message) {
    try {
      fh = new FileHandler(logFilePath, true);
      logger.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);
      logger.log(Level.FINE, "MyLogs");
      logger.info(message);
    } catch (SecurityException | IOException e) {
      e.printStackTrace();
    } finally {
      if(fh != null) { fh.close(); };
    }
  }

  public FileHandler getFileHandler() {
    return fh;
  }
}
