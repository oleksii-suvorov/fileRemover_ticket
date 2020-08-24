package oleksii.ticket.moveFiles;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * CopyAndRemoveService - main business logic, which contains methods:
 * moveFilesAndRemoveDirs that calls removeDirectories method which lastly calls removeEmptyDirs method
 */
@Component
public class CopyAndRemoveService {
  @Autowired
  LogService log;

  @Value("${input.source}")
  private String sourcePath;

  @Value("${input.target}")
  private String targetPath;

  private File source;
  private File target;

  @Value("${input.extension}")
  private String extension;
  @Value("${input.pattern}")
  private String pattern;
  private final String FS = System.getProperty("file.separator");
  /**
   * creates string pattern which means:
   * any quantity of any symbols + our pattern to search in file text + any quantity of any symbols
   */
  private String regex = String.format(".{0,}(%s).{0,}", pattern);
  private Set<String> directoriesToRemove = new HashSet<>();

  public CopyAndRemoveService() {}
  /**
   * @param data an array of data from command line arguments
   *  source - folder where we are going to search and move our files
   *  target - folder where to copy found files
   *  extension - type of files (for instance "zip" or "txt")
   *  pattern - text we need to match in names of files
   */
  public CopyAndRemoveService(String[] data) {
    this.source = new File(setSeparator(data[0]));
    this.target = new File(setSeparator(data[1]));
    this.extension = data[2].trim().toLowerCase();
    this.pattern = data[3].trim();
  }
  /**
   *
   * @param path string with a path to check file separator in concrete operation system
   * @return string with formatted path which fits OS
   */
  private String setSeparator(String path) {
    return FS.equals("\\") ? path.replaceAll("/", FS + FS)
          : path.replaceAll("\\\\", FS);
  }
  /**
   * Method moves all the files of a specific extension to another specified directory.
   * These files should contain specified text in it's titles
   *
   * Also method controls if in the target directory are already exist files with the same name that in source
   * directory. In this case method will remove equal files from source directory.
   *
   * Method opens another one thread to create log file
   */
  public void moveFilesAndRemoveDirs() throws IOException {
    sourcePath = setSeparator(sourcePath);
    targetPath = setSeparator(targetPath);
    this.source = new File(sourcePath);
    this.target = new File(targetPath);
    List<String> filesMoved = new ArrayList();
    String[] targetDirFiles = target.list((dir, name) -> new File(dir, name).isFile());
    Set<String> targetDirFilesSet = new HashSet<>(Arrays.asList(targetDirFiles));
    Iterator<File> files = FileUtils.iterateFiles(source, new String[]{extension.toLowerCase()}, true);
    while(files.hasNext()) {
      File currentFile = files.next();
      String currentDirectory = currentFile.getParent();
      String fileName = currentFile.getName();
      if (Pattern.compile(this.pattern, Pattern.CASE_INSENSITIVE).matcher(fileName).find() &&
          !currentDirectory.equals(sourcePath)) {
        try {
          if(filesMoved.contains(fileName)) {
            FileUtils.forceDelete(currentFile);
            continue;
          }
          else if(targetDirFiles.length != 0 && targetDirFilesSet.contains(fileName)) {
            log.writeToLog("File " + currentFile + " already exists in target directory. Removing...");
            FileUtils.forceDelete(currentFile);
            continue;
          }
          FileUtils.moveToDirectory(currentFile, target, true);
          filesMoved.add(fileName);
        } catch (IOException e) {
          log.writeToLog(e.getMessage());
        }
        directoriesToRemove.add(currentDirectory);
      }
    }

    removeDirectories();
    ExecutorService es = Executors.newSingleThreadExecutor();
    es.submit(() -> {
      StringBuilder sb = new StringBuilder().append("Total files moved to ")
            .append(target.toString()).append(": ").append(filesMoved.size());

      log.writeToLog(sb.toString());
      for(String file: filesMoved) {
        log.writeToLog(file);
      }
    });
    es.shutdown();
    try {
      es.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Method witch removes all subdirectories of relocated files except source directory
   * If in directory is still any file, it won't be removed
   * For a security reason this method is private and can be called only after moveFiles method was executed
   */
  private void removeDirectories() throws IOException {
    for (String dir : this.directoriesToRemove) {
      if (dir.equals(source.toString()) || Files.list(Paths.get(dir)).findAny().isPresent()) {
        continue;
      }
      File directory = new File(dir);
        try {
          FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
          System.out.println(e.getMessage());
          log.writeToLog(e.getMessage());
        }
        log.writeToLog(directory.toString() + " was successfully removed.");
    }
    removeEmptyDirs();
  }

  /**
   * Method takes all directories that are left after removeDirectories execution method and removes only empty of them
   */
  private void removeEmptyDirs() {
    String[] dirs = this.source.list((dir, name) -> new File(dir, name).isDirectory());
    if (dirs.length != 0) {
      for (String dir : dirs) {
        String currentDirectoryPath = source.getAbsolutePath() + FS + dir;
        try {
          if(!Files.list(Paths.get(currentDirectoryPath)).findAny().isPresent()) {
            FileUtils.deleteDirectory(new File(currentDirectoryPath));
            log.writeToLog("Found and removed empty directory " + currentDirectoryPath);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }


  /**
   * after bean destroying this method closes file handler
   */
  @PreDestroy
  public void closeFileHandler() {
    System.out.println("Closing file handler...");
    log.getFileHandler().close();
  }

}
