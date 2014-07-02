package au.edu.rmit.tzar.gui;

import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.*;
import au.edu.rmit.tzar.commands.ExecLocalRuns;
import au.edu.rmit.tzar.commands.ScheduleRuns;
import au.edu.rmit.tzar.db.DaoFactory;
import au.edu.rmit.tzar.repository.CodeSourceFactory;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import au.edu.rmit.tzar.runners.RunnerFactory;
import com.google.common.base.Optional;
import com.google.common.io.Files;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.logging.*;

/**
 * Swing graphical user interface class for Tzar.
 */
public class TzarGui {
  private static Logger LOG = Logger.getLogger(TzarGui.class.getName());
  public static String APP_NAME = "Tzar computation framework";

  private JTabbedPane tabbedPane;
  private JPanel mainPanel;

  // exec local runs
  private JComboBox repoType;
  private JSpinner numRuns;
  private JButton executeButton;
  private JButton fileChooserButton;
  private JTextField pathToProject;
  private JCheckBox headRevisionCheckBox;
  private JTextField runsetName;
  private JTextField revisionNumber;
  private JPanel execLocalRuns;

  // output log
  private JPanel outputLog;
  private JTextArea outputLogText;
  private JButton clearButton;

  private JTextField scheduleRunsPathToProject;
  private JButton scheduleRunsOpenFileBrowser;
  private JCheckBox scheduleRunsHeadRevision;
  private JTextField scheduleRunsClusterName;
  private JTextField scheduleRunsRunset;
  private JSpinner scheduleRunsNumRuns;
  private JButton scheduleRunsExecute;
  private JTextField scheduleRunsRevision;
  private JTextField baseDirectory;
  private JTextField dbConnectionString;
  private ErrorDialog errorDialog;
  private final JFrame frame;

  private JButton stopButton;
  private JButton javaExampleButton;
  private JButton jythonExampleButton;
  private JButton jythonCallingRButton;
  private JButton pythonExampleButton;
  private JButton rExampleButton;
  private StopRun stopRun;

  public TzarGui() {
    frame = new JFrame(APP_NAME);
    frame.setContentPane(mainPanel);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    errorDialog = new ErrorDialog(frame);
    stopRun = new StopRun();
  }

  /**
   * Initialise and display the Tzar graphical user interface.
   * @throws TzarException if an error occurs setting up the UI.
   */
  public void display() throws TzarException {
    frame.pack();
    frame.setVisible(true);

    initialiseLoggingPane();
    initialiseExecLocalPane();
    initialiseScheduleRunsPane();
    initialiseSettings();
    initialiseExamplePane();
  }

  private void initialiseLoggingPane() throws TzarException {
    try {
      setupLogging();
    } catch (IOException e) {
      throw new TzarException(e);
    }

    stopButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopRun.stop();
      }
    });
    clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        outputLogText.setText("");
      }
    });
  }

  private void initialiseScheduleRunsPane() {
    scheduleRunsOpenFileBrowser.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openFileChooser(scheduleRunsPathToProject);
      }
    });

    scheduleRunsHeadRevision.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        headRevisionChecked(scheduleRunsRevision, scheduleRunsHeadRevision);
      }
    });

    scheduleRunsClusterName.setText(Constants.DEFAULT_CLUSTER_NAME);
    scheduleRunsRunset.setText(Constants.DEFAULT_RUNSET);
    initialiseNumRuns(scheduleRunsNumRuns);
    scheduleRunsExecute.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        try {
          scheduleRuns(scheduleRunsRevision.getText(), scheduleRunsPathToProject.getText(),
              dbConnectionString.getText(), scheduleRunsRunset.getText(), scheduleRunsClusterName.getText(),
              (Integer) scheduleRunsNumRuns.getValue());
        } catch (TzarException e) {
          LOG.log(Level.SEVERE, "An error occurred scheduling runs.", e);
          errorDialog.display("An error occurred scheduling runs.", e);
        }
      }
    });
  }

  private void initialiseExecLocalPane() {
    for (CodeSource.RepositoryType type : CodeSourceImpl.RepositoryTypeImpl.values()) {
      repoType.addItem(type);
    }

    initialiseNumRuns(numRuns);

    fileChooserButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openFileChooser(TzarGui.this.pathToProject);
      }
    });

    executeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        tabbedPane.setSelectedComponent(outputLog);
        execLocalRuns();
      }
    });

    headRevisionCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        headRevisionChecked(revisionNumber, headRevisionCheckBox);
      }
    });

    runsetName.setText(Constants.DEFAULT_RUNSET);
  }

  private void initialiseSettings() {
    dbConnectionString.setText(System.getenv(Constants.DB_ENVIRONMENT_VARIABLE_NAME));
    baseDirectory.setText(Constants.DEFAULT_TZAR_BASE_DIR.getAbsolutePath());
  }

  private void initialiseExamplePane() {
    initialiseExampleButton(javaExampleButton, "java");
    initialiseExampleButton(jythonExampleButton, "jython");
    initialiseExampleButton(jythonCallingRButton, "jython-calling-R");
    initialiseExampleButton(pythonExampleButton, "python");
    initialiseExampleButton(rExampleButton, "R");
  }

  private void initialiseExampleButton(JButton exampleButton, final String exampleName) {
    exampleButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        populateRunPaneWithExample(exampleName);
      }
    });
  }

  private void populateRunPaneWithExample(String exampleName) {
    try {
      String version = getVersion();
      String url = String.format("http://tzar-framework.googlecode.com/svn/tags/v%s/example-projects/example-%s",
          version, exampleName);
      repoType.setSelectedItem(CodeSourceImpl.RepositoryTypeImpl.SVN);
      numRuns.setValue(1);
      pathToProject.setText(url);
      headRevisionCheckBox.setSelected(false); // clear the selection
      headRevisionCheckBox.doClick(); // then select it!
      runsetName.setText("");
      tabbedPane.setSelectedIndex(0);
    } catch (TzarException cause) {
      errorDialog.display(cause);
    }
  }

  private static String getVersion() throws TzarException {
    BufferedReader in = new BufferedReader(new InputStreamReader(TzarGui.class.getResourceAsStream(
        Constants.VERSION_PROPERTIES)));
    try {
      String line = in.readLine();
      return line.split(" ")[1];
    } catch (IOException e) {
      throw new TzarException(e);
    }
  }

  private void initialiseNumRuns(JSpinner numRuns) {
    numRuns.setValue(1);
    numRuns.setModel(new SpinnerNumberModel(1, 1, 9999, 1));
  }

  private void headRevisionChecked(JTextField textFieldToUpdate, JCheckBox checkBox) {
    if (checkBox.isSelected()) {
      textFieldToUpdate.setText(Constants.HEAD_REVISION);
      textFieldToUpdate.setEnabled(false);
    } else {
      textFieldToUpdate.setText("");
      textFieldToUpdate.setEnabled(true);
      textFieldToUpdate.requestFocusInWindow();
    }
  }

  private void openFileChooser(JTextField textFieldToUpdate) {
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (chooser.showDialog(mainPanel, "Choose") == JFileChooser.APPROVE_OPTION) {
      textFieldToUpdate.setText(chooser.getSelectedFile().getAbsolutePath());
    }
  }

  private void setupLogging() throws IOException {
    if (System.getProperty("java.util.logging.config.file") == null) {
      LogManager.getLogManager().readConfiguration(TzarGui.class.getResourceAsStream("/logging.properties"));
    }
    Handler handler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        if (isLoggable(record))
          synchronized (outputLogText) {
            outputLogText.append(getFormatter().format(record));
            outputLogText.setCaretPosition(outputLogText.getDocument().getLength());
          }
      }

      @Override
      public void flush() {

      }

      @Override
      public void close() throws SecurityException {

      }
    };
    handler.setLevel(Level.INFO);
    Logger.getLogger("").addHandler(handler);
    handler.setFormatter(new SimpleFormatter());

  }

  private void execLocalRuns() {
    stopRun.reset();
    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        CodeSourceImpl.RepositoryTypeImpl repositoryType = CodeSourceImpl.RepositoryTypeImpl.valueOf(repoType
            .getSelectedItem().toString());

        File tzarBasePath = new File(baseDirectory.getText());
        File tzarOutputPath = new File(tzarBasePath, Constants.LOCAL_OUTPUT_DATA_DIR);

        File modelPath = new File(tzarBasePath, Constants.DEFAULT_MODEL_CODE_DIR);
        String revision = revisionNumber.getText().trim();
        String projectPath = pathToProject.getText().trim();

        CodeSourceImpl codeSource = CodeSourceFactory.createCodeSource(revision, repositoryType,
            Utils.makeAbsoluteUri(projectPath), true /* force download of model code */);

        ProjectSpec projectSpec = codeSource.getProjectSpec(Files.createTempDir());
        String runsetName = TzarGui.this.runsetName.getText().trim();
        RunFactory runFactory = new RunFactory(codeSource, runsetName, "", projectSpec);
        RunnerFactory runnerFactory = new RunnerFactory();
        ExecLocalRuns execLocalRuns = new ExecLocalRuns((Integer) numRuns.getValue(), runFactory,
            tzarOutputPath, modelPath, runnerFactory, Optional.fromNullable(projectSpec.getMapReduce()), stopRun);
        execLocalRuns.execute();
        return null;
      }

      @Override
      protected void done() {
        try {
          get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof Error) {
            throw (Error) cause;
          } else {
            LOG.log(Level.SEVERE, "An error occurred executing local runs.", e);
            errorDialog.display((Exception) cause);
          }
        }
      }
    }.execute();
  }

  private void scheduleRuns(final String revision, final String projectPath, final String dbUrl, final String runset,
      final String clusterName, final int numRuns) throws TzarException {
    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws TzarException {
        DaoFactory daoFactory = new DaoFactory(dbUrl);

        CodeSourceImpl.RepositoryTypeImpl repositoryType = CodeSourceImpl.RepositoryTypeImpl.SVN;

        CodeSourceImpl codeSource;
        URI sourceUri;
        try {
          sourceUri = new URI(projectPath);
        } catch (URISyntaxException e) {
          throw new TzarException(String.format("Project not found at path: %s", projectPath), e);
        }
        try {
          codeSource = CodeSourceFactory.createCodeSource(revision, repositoryType, sourceUri,
              true /* force download of model code */);
        } catch (CodeSourceImpl.InvalidRevisionException e) {
          throw new TzarException(String.format("Invalid revision %s for repository type %s", revision,
              repositoryType));
        }
        ProjectSpec projectSpec;
        try {
          projectSpec = codeSource.getProjectSpec(Files.createTempDir());
        } catch (FileNotFoundException e) {
          errorDialog.display("Couldn't find project spec", e);
          return null;
        }

        RunFactory runFactory = new RunFactory(codeSource, runset, clusterName, projectSpec);
        new ScheduleRuns(daoFactory.createRunDao(), numRuns, runFactory).execute();
        return null;
      }

      @Override
      protected void done() {
        try {
          get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof Error)
            throw (Error) cause;
          else
            errorDialog.display((Exception) cause);
        }
      }
    }.execute();
  }

  public static void main(String[] args) throws TzarException {
    new TzarGui().display();
  }
}
