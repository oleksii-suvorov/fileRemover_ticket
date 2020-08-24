package oleksii.ticket.moveFiles;

import oleksii.ticket.moveFiles.config.SpringConfig;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.io.IOException;

/**
 * @author oleskiy.OS
 * args - can be inserted from command line or from inputData.properties file
 *
 * Application takes all files of specific extension from specified folder and moves it to another specified folder,
 * then it removes all sub-folders from which files have been taken.
 *
 * If in any folder remains files which were not deleated this directory wil not be removed.
 */
public class App {
  public static void main(String[] args) throws IOException {
//    CopyAndRemoveService copyAndRemove = new CopyAndRemoveService(args); //to insert args from cmd line
    ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
    CopyAndRemoveService copyAndRemove = context.getBean(CopyAndRemoveService.class);
    copyAndRemove.moveFilesAndRemoveDirs();
    context.close();
  }
}
