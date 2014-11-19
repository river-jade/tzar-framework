package au.edu.rmit.tzar.gui;

import javax.swing.*;

/**
 * Generic error dialog box.
 */
class ErrorDialog {
  private final JFrame frame;

  public ErrorDialog(JFrame frame) {
    this.frame = frame;
  }

  public void display(String message, Exception e) {
    if (!message.isEmpty())
      message = message + ": ";
    message = message + e.getMessage();
    JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
  }

  public void display(Exception e) {
    display("", e);
  }
}
