package smartparking;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public class LoginFrame extends JFrame {
    private static final String LOGIN_CARD = "login";
    private static final String REGISTER_CARD = "register";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(LoginFrame.class);

    private final ParkingManager manager;
    private final CardLayout formLayout = new CardLayout();
    private final JPanel formCards = new JPanel(formLayout);
    private final JTextField loginIdentifier = new JTextField();
    private final JPasswordField loginPassword = new JPasswordField();
    private final JCheckBox rememberMe = new JCheckBox("Remember me");
    private final JButton loginButton = primaryButton("Sign in");

    public LoginFrame(ParkingManager manager) {
        this.manager = manager;
        setTitle("Smart Parking | Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 650));
        setSize(1120, 740);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Theme.BACKGROUND);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.46;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        root.add(new BrandPanel(), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.54;
        JPanel formSide = new JPanel(new BorderLayout());
        formSide.setBackground(Theme.BACKGROUND);
        formSide.setBorder(BorderFactory.createEmptyBorder(38, 48, 34, 48));
        formCards.setOpaque(false);
        formCards.add(createLoginCard(), LOGIN_CARD);
        formCards.add(createRegistrationCard(), REGISTER_CARD);
        formSide.add(formCards, BorderLayout.CENTER);
        root.add(formSide, constraints);
        setContentPane(root);

        String remembered = PREFERENCES.get("rememberedUsername", "");
        loginIdentifier.setText(remembered);
        rememberMe.setSelected(!remembered.isBlank());
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel createLoginCard() {
        JPanel card = formCard();
        JLabel eyebrow = eyebrow("SMART PARKING MANAGEMENT");
        JLabel title = heading("Welcome back");
        JLabel description = description("Sign in to manage your parking operations.");
        card.add(eyebrow);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(description);
        card.add(Box.createVerticalStrut(34));
        card.add(fieldGroup("Username or email", loginIdentifier, "Enter your username or email"));
        card.add(Box.createVerticalStrut(18));
        card.add(passwordGroup("Password", loginPassword, "Enter your password"));
        card.add(Box.createVerticalStrut(12));

        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        rememberMe.setOpaque(false);
        rememberMe.setForeground(Theme.MUTED);
        rememberMe.setFont(bodyFont());
        options.add(rememberMe, BorderLayout.WEST);
        options.add(linkButton("Forgot password?", event -> showForgotPasswordInfo()), BorderLayout.EAST);
        card.add(options);
        card.add(Box.createVerticalStrut(24));

        loginButton.addActionListener(event -> signIn());
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(loginButton);
        card.add(Box.createVerticalStrut(24));
        card.add(separator());
        card.add(Box.createVerticalStrut(20));
        JPanel createAccount = new JPanel(new BorderLayout(5, 0));
        createAccount.setOpaque(false);
        JLabel prompt = description("Don't have an account?");
        createAccount.add(prompt, BorderLayout.CENTER);
        createAccount.add(linkButton("Create account", event -> showCard(REGISTER_CARD)), BorderLayout.EAST);
        createAccount.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(createAccount);
        return card;
    }

    private JPanel createRegistrationCard() {
        JPanel card = formCard();
        card.add(eyebrow("NEW TO SMART PARKING"));
        card.add(Box.createVerticalStrut(8));
        card.add(heading("Create your account"));
        card.add(Box.createVerticalStrut(5));
        card.add(description("Set up your administrator profile to get started."));
        card.add(Box.createVerticalStrut(20));

        JTextField fullName = new JTextField();
        JTextField email = new JTextField();
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JPasswordField confirmation = new JPasswordField();
        card.add(fieldGroup("Full name", fullName, "Your full name"));
        card.add(Box.createVerticalStrut(10));
        card.add(fieldGroup("Email address", email, "name@example.com"));
        card.add(Box.createVerticalStrut(10));
        card.add(fieldGroup("Username", username, "Choose a username"));
        card.add(Box.createVerticalStrut(10));
        card.add(passwordGroup("Password", password, "At least 8 characters"));
        card.add(Box.createVerticalStrut(10));
        card.add(passwordGroup("Confirm password", confirmation, "Enter your password again"));
        card.add(Box.createVerticalStrut(18));

        JButton register = primaryButton("Create account");
        register.addActionListener(event -> register(fullName, email, username, password, confirmation, register));
        register.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(register);
        card.add(Box.createVerticalStrut(15));
        JPanel loginLink = new JPanel(new BorderLayout(4, 0));
        loginLink.setOpaque(false);
        loginLink.add(description("Already have an account?"), BorderLayout.CENTER);
        loginLink.add(linkButton("Sign in", event -> showCard(LOGIN_CARD)), BorderLayout.EAST);
        loginLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(loginLink);
        return card;
    }

    private JPanel formCard() {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        return card;
    }

    private JPanel fieldGroup(String label, JTextField field, String placeholder) {
        JLabel caption = new JLabel(label);
        caption.setForeground(Theme.TEXT);
        caption.setFont(bodyFont().deriveFont(Font.BOLD));
        field.setFont(bodyFont());
        field.setPreferredSize(new Dimension(320, 46));
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.CYAN);
        field.setBackground(Theme.INPUT);
        field.setToolTipText(placeholder);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        JPanel group = new JPanel(new BorderLayout(0, 8));
        group.setOpaque(false);
        group.add(caption, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        return group;
    }

    private JPanel passwordGroup(String label, JPasswordField field, String placeholder) {
        JPanel group = fieldGroup(label, field, placeholder);
        char echoChar = field.getEchoChar();
        JButton toggle = linkButton("Show", event -> {
            boolean visible = "Show".equals(event.getActionCommand());
            field.setEchoChar(visible ? (char) 0 : echoChar);
            ((JButton) event.getSource()).setText(visible ? "Hide" : "Show");
            ((JButton) event.getSource()).setActionCommand(visible ? "Hide" : "Show");
        });
        JPanel inputAndToggle = new JPanel(new BorderLayout(8, 0));
        inputAndToggle.setOpaque(false);
        inputAndToggle.add(field, BorderLayout.CENTER);
        inputAndToggle.add(toggle, BorderLayout.EAST);
        group.remove(field);
        group.add(inputAndToggle, BorderLayout.CENTER);
        return group;
    }

    private static JPanel separator() {
        JPanel line = new JPanel();
        line.setBackground(Theme.BORDER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(1, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    private static JLabel eyebrow(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.CYAN);
        label.setFont(bodyFont().deriveFont(Font.BOLD, 13f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 30));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel description(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.MUTED);
        label.setFont(bodyFont());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static Font bodyFont() {
        return new Font("Segoe UI", Font.PLAIN, 15);
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(Theme.BACKGROUND);
        button.setBackground(Theme.CYAN);
        button.setFont(bodyFont().deriveFont(Font.BOLD, 16f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CYAN),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        button.setPreferredSize(new Dimension(300, 48));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return button;
    }

    private static JButton linkButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setForeground(Theme.CYAN);
        button.setBackground(Theme.BACKGROUND);
        button.setFont(bodyFont());
        button.setBorder(BorderFactory.createEmptyBorder(5, 4, 5, 4));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setActionCommand(text);
        button.addActionListener(action);
        return button;
    }

    private void showCard(String card) {
        formLayout.show(formCards, card);
        getRootPane().setDefaultButton(LOGIN_CARD.equals(card) ? loginButton : null);
    }

    private void signIn() {
        String identifier = loginIdentifier.getText().trim();
        char[] secret = loginPassword.getPassword();
        if (identifier.isBlank() || secret.length == 0) {
            Arrays.fill(secret, '\0');
            showError("Enter your username or email and password.");
            return;
        }
        loginButton.setEnabled(false);
        loginButton.setText("Signing in…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return manager.authenticate(identifier, secret) ? manager.getAccountName(identifier) : null;
            }

            @Override
            protected void done() {
                Arrays.fill(secret, '\0');
                loginButton.setEnabled(true);
                loginButton.setText("Sign in");
                try {
                    String accountName = get();
                    if (accountName == null) {
                        showError("The username/email or password is incorrect.");
                        return;
                    }
                    if (rememberMe.isSelected()) {
                        PREFERENCES.put("rememberedUsername", identifier);
                    } else {
                        PREFERENCES.remove("rememberedUsername");
                    }
                    dispose();
                    new DashboardFrame(manager, accountName).setVisible(true);
                } catch (Exception ex) {
                    showError("Could not sign in: " + errorMessage(ex));
                }
            }
        }.execute();
    }

    private void register(JTextField fullName, JTextField email, JTextField username,
                          JPasswordField password, JPasswordField confirmation, JButton button) {
        char[] secret = password.getPassword();
        char[] confirmed = confirmation.getPassword();
        if (!Arrays.equals(secret, confirmed)) {
            Arrays.fill(secret, '\0');
            Arrays.fill(confirmed, '\0');
            showError("The passwords do not match.");
            return;
        }

        String nameValue = fullName.getText();
        String emailValue = email.getText();
        String usernameValue = username.getText();
        button.setEnabled(false);
        button.setText("Creating account…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                manager.registerAccount(nameValue, emailValue, usernameValue, secret);
                return null;
            }

            @Override
            protected void done() {
                Arrays.fill(secret, '\0');
                Arrays.fill(confirmed, '\0');
                button.setEnabled(true);
                button.setText("Create account");
                try {
                    get();
                    loginIdentifier.setText(usernameValue.trim());
                    loginPassword.setText("");
                    fullName.setText("");
                    email.setText("");
                    username.setText("");
                    password.setText("");
                    confirmation.setText("");
                    showCard(LOGIN_CARD);
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Your account was created. Sign in to continue.",
                            "Account created", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showError(errorMessage(ex));
                }
            }
        }.execute();
    }

    private void showForgotPasswordInfo() {
        JOptionPane.showMessageDialog(this,
                "Password reset is not configured because this app has no email delivery service. "
                        + "Contact your Smart Parking administrator to reset your account.",
                "Password reset", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Smart Parking", JOptionPane.ERROR_MESSAGE);
    }

    private static String errorMessage(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private static final class BrandPanel extends JPanel {
        private BrandPanel() {
            setLayout(new BorderLayout());
            setBackground(Theme.SIDEBAR);
            setBorder(BorderFactory.createEmptyBorder(52, 48, 44, 38));

            JPanel copy = new JPanel();
            copy.setOpaque(false);
            copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
            JLabel logo = new JLabel("P  SMART PARKING");
            logo.setForeground(Theme.CYAN);
            logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
            JLabel title = new JLabel("<html>Parking,<br>made effortless.</html>");
            title.setForeground(Theme.TEXT);
            title.setFont(new Font("Segoe UI", Font.BOLD, 34));
            JLabel tagline = new JLabel("<html>Smarter spaces. Smoother arrivals.<br>One clear view of every space.</html>");
            tagline.setForeground(Theme.MUTED);
            tagline.setFont(bodyFont().deriveFont(17f));
            copy.add(logo);
            copy.add(Box.createVerticalStrut(54));
            copy.add(title);
            copy.add(Box.createVerticalStrut(18));
            copy.add(tagline);
            copy.add(Box.createVerticalStrut(24));
            JLabel welcome = new JLabel("<html><font color='#00D9FF'>WELCOME</font><br>"
                    + "<font color='#B8C0CC'>Your parking operations, organized in one place.</font></html>");
            welcome.setFont(bodyFont());
            copy.add(welcome);
            add(copy, BorderLayout.NORTH);
            add(new ParkingIllustration(), BorderLayout.CENTER);
        }
    }

    private static final class ParkingIllustration extends JPanel {
        private ParkingIllustration() {
            setOpaque(false);
            setPreferredSize(new Dimension(380, 240));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(0, 217, 255, 18));
            g.fillOval(width / 6, height / 10, width * 2 / 3, height * 4 / 5);
            int lotX = width / 8;
            int lotY = height / 4;
            int lotW = width * 3 / 4;
            int lotH = height / 2;
            g.setColor(new Color(35, 40, 48));
            g.fillRoundRect(lotX, lotY, lotW, lotH, 22, 22);
            g.setColor(new Color(0, 217, 255, 120));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(lotX, lotY, lotW, lotH, 22, 22);
            int slotWidth = lotW / 6;
            for (int i = 0; i < 5; i++) {
                int x = lotX + 14 + i * slotWidth;
                g.setColor(i == 2 ? new Color(0, 217, 255, 230) : new Color(190, 200, 214, 125));
                g.drawRoundRect(x, lotY + 13, slotWidth - 10, lotH - 26, 10, 10);
            }
            g.setColor(Theme.CYAN);
            g.setFont(new Font("Segoe UI", Font.BOLD, 32));
            g.drawString("P", lotX + lotW / 2 - 10, lotY + lotH / 2 + 11);
            g.setColor(new Color(184, 192, 204, 180));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(lotX + lotW / 2, lotY + lotH + 10, lotX + lotW / 2, height - 12);
            g.dispose();
        }
    }
}
