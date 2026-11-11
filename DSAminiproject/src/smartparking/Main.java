package smartparking;

import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                configureLookAndFeel();
                ParkingManager manager = new ParkingManager();
                showStartup(manager);
            } catch (Exception ex) {
                showStartupError(ex);
            }
        });
    }

    private static void configureLookAndFeel() {
        Font body = new Font("Segoe UI", Font.PLAIN, 15);
        UIManager.put("Label.font", body);
        UIManager.put("Label.foreground", Theme.TEXT);
        UIManager.put("Panel.background", Theme.BACKGROUND);
        UIManager.put("Button.font", body);
        UIManager.put("Button.background", Theme.PURPLE);
        UIManager.put("Button.foreground", Theme.BACKGROUND);
        UIManager.put("TextField.font", body);
        UIManager.put("PasswordField.font", body);
        UIManager.put("ComboBox.background", Theme.INPUT);
        UIManager.put("ComboBox.foreground", Theme.TEXT);
        UIManager.put("ComboBox.selectionBackground", new java.awt.Color(0, 91, 112));
        UIManager.put("ComboBox.selectionForeground", Theme.TEXT);
        UIManager.put("List.background", Theme.PANEL);
        UIManager.put("List.foreground", Theme.TEXT);
        UIManager.put("List.selectionBackground", new java.awt.Color(0, 91, 112));
        UIManager.put("List.selectionForeground", Theme.TEXT);
        UIManager.put("ScrollPane.background", Theme.PANEL);
        UIManager.put("Viewport.background", Theme.PANEL);
        UIManager.put("ScrollBar.background", Theme.PANEL);
        UIManager.put("ScrollBar.track", Theme.BACKGROUND);
        UIManager.put("ScrollBar.thumb", new java.awt.Color(72, 78, 88));
        UIManager.put("OptionPane.background", Theme.PANEL);
        UIManager.put("OptionPane.messageForeground", Theme.TEXT);
        UIManager.put("OptionPane.buttonFont", body);
        UIManager.put("OptionPane.messageFont", body);
    }

    private static void showStartup(ParkingManager manager) {
        JFrame startup = new JFrame("Smart Parking");
        startup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startup.setSize(new Dimension(360, 140));
        startup.setLocationRelativeTo(null);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(28, 28, 28, 28));
        JLabel title = new JLabel("Smart Parking");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel status = new JLabel("Connecting to the parking database…");
        status.setForeground(Theme.MUTED);
        content.add(title, BorderLayout.NORTH);
        content.add(status, BorderLayout.CENTER);
        startup.setContentPane(content);
        startup.setVisible(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                manager.bootstrapAdmin();
                return null;
            }

            @Override
            protected void done() {
                startup.dispose();
                try {
                    get();
                    new LoginFrame(manager).setVisible(true);
                } catch (Exception ex) {
                    showStartupError(ex.getCause() == null ? ex : ex.getCause());
                }
            }
        }.execute();
    }

    private static void showStartupError(Throwable error) {
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        JOptionPane.showMessageDialog(null,
                "Could not start Smart Parking.\n" + message
                        + "\nCheck MySQL settings and run database/smart_parking.sql.",
                "Startup error", JOptionPane.ERROR_MESSAGE);
    }
}
