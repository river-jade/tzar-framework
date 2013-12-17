package au.edu.rmit.tzar.gui;

import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.api.CodeSource;
import au.edu.rmit.tzar.api.Constants;
import au.edu.rmit.tzar.api.ProjectSpec;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.commands.ExecLocalRuns;
import au.edu.rmit.tzar.commands.ScheduleRuns;
import au.edu.rmit.tzar.db.DaoFactory;
import au.edu.rmit.tzar.parser.YamlParser;
import au.edu.rmit.tzar.repository.CodeSourceFactory;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.io.Files;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.logging.*;

/**
 * Swing graphical user interface class for Tzar.
 */
public class TzarGui {
  private static Logger LOG = Logger.getLogger(TzarGui.class.getName());

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

  public TzarGui() {
    frame = new JFrame("Tzar computation framework");
    frame.setContentPane(mainPanel);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    errorDialog = new ErrorDialog(frame);
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
  }

  private void initialiseLoggingPane() throws TzarException {
    try {
      setupLogging();
    } catch (IOException e) {
      throw new TzarException(e);
    }

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
    Logger.getLogger("").addHandler(handler);
    handler.setFormatter(new SimpleFormatter());

  }

  private void execLocalRuns() {
    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws IOException, TzarException, InterruptedException {
        ProjectSpec projectSpec;
        String projectPath = pathToProject.getText();
        projectSpec = new YamlParser().projectSpecFromYaml(new File(projectPath, Constants.PROJECT_YAML));
        CodeSourceImpl.RepositoryTypeImpl repositoryType = CodeSourceImpl.RepositoryTypeImpl.valueOf(repoType
            .getSelectedItem().toString());
        CodeSourceImpl codeSource = new CodeSourceImpl(Utils.makeAbsoluteUri(projectPath), repositoryType,
            revisionNumber.getText());
        RunFactory runFactory = new RunFactory(codeSource, runsetName.getText(), "", projectSpec);
        RunnerFactory runnerFactory = new RunnerFactory();
        File tzarBaseDir = new File(baseDirectory.getText());
        File modelPath = new File(tzarBaseDir, Constants.DEFAULT_MODEL_CODE_DIR);
        ExecLocalRuns execLocalRuns = new ExecLocalRuns((Integer) numRuns.getValue(), runFactory, tzarBaseDir, modelPath,
            runnerFactory);
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
          if (cause instanceof Error)
            throw (Error) cause;
          else
            errorDialog.display((Exception) cause);
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
          codeSource = CodeSourceFactory.createCodeSource(revision, repositoryType,
              sourceUri, Files.createTempDir());
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
