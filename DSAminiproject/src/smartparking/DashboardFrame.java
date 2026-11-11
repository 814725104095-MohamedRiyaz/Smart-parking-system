package smartparking;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Component;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class DashboardFrame extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ParkingManager manager;
    private final String adminName;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final JPanel slotGrid = new JPanel(new GridLayout(3, 5, 10, 10));
    private JButton backButton;
    private final JLabel[] dashboardValues = new JLabel[4];
    private final JLabel[] reportValues = new JLabel[5];
    private JProgressBar occupancyBar;
    private JTable slotsTable;
    private JTable queueTable;
    private JTable historyTable;
    private JTextField historySearch;

    public DashboardFrame(ParkingManager manager, String adminName) {
        this.manager = manager;
        this.adminName = adminName;
        setTitle("Smart Parking Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1200, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BACKGROUND);
        setLayout(new BorderLayout());
        add(createSidebar(), BorderLayout.WEST);
        add(createMainPanel(), BorderLayout.CENTER);
        showPage("Dashboard");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(244, 0));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 16, 22, 16));
        sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("<html><b>SMART<br>PARKING</b></html>", SwingConstants.LEFT);
        brand.setForeground(Theme.CYAN);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 10, 24, 0));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);
        addNavigation(sidebar, "Dashboard", "Dashboard");
        addNavigation(sidebar, "Parking Slots", "Parking Slots");
        addNavigation(sidebar, "Vehicle Entry", "Vehicle Entry");
        addNavigation(sidebar, "Vehicle Exit", "Vehicle Exit");
        addNavigation(sidebar, "Waiting Queue", "Waiting Queue");
        addNavigation(sidebar, "Parking History", "Parking History");
        addNavigation(sidebar, "Reports", "Reports");
        sidebar.add(javax.swing.Box.createVerticalGlue());
        JLabel signedIn = new JLabel("<html><font color='#aab4cb'>Signed in as</font><br>"
                + "<b>" + escapeHtml(adminName) + "</b></html>");
        signedIn.setForeground(Theme.TEXT);
        signedIn.setBorder(BorderFactory.createEmptyBorder(18, 10, 0, 0));
        signedIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(signedIn);
        JButton logout = navButton("Logout");
        logout.setForeground(new Color(255, 150, 160));
        logout.setBackground(Theme.SIDEBAR);
        logout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(11, 12, 11, 8)));
        logout.addActionListener(event -> {
            dispose();
            new LoginFrame(manager).setVisible(true);
        });
        sidebar.add(logout);
        return sidebar;
    }

    private JPanel createMainPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BACKGROUND);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(22, 28, 16, 28));
        pageTitle.setForeground(Theme.TEXT);
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 30));
        JLabel subheading = new JLabel("Parking operations overview");
        subheading.setForeground(Theme.MUTED);
        header.add(pageTitle, BorderLayout.WEST);
        JPanel headerActions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 0));
        headerActions.setOpaque(false);
        headerActions.add(subheading);
        backButton = button("Back to Dashboard", () -> showPage("Dashboard"));
        backButton.setVisible(false);
        headerActions.add(backButton);
        header.add(headerActions, BorderLayout.EAST);
        main.add(header, BorderLayout.NORTH);

        content.setBackground(Theme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(0, 28, 26, 28));
        content.add(createDashboardPage(), "Dashboard");
        content.add(createSlotsPage(), "Parking Slots");
        content.add(createEntryPage(), "Vehicle Entry");
        content.add(createExitPage(), "Vehicle Exit");
        content.add(createQueuePage(), "Waiting Queue");
        content.add(createHistoryPage(), "Parking History");
        content.add(createReportsPage(), "Reports");
        main.add(content, BorderLayout.CENTER);
        return main;
    }

    private JPanel createDashboardPage() {
        JPanel page = pagePanel();
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 14));
        cards.setOpaque(false);
        String[] titles = {"Total slots", "Available", "Occupied", "Waiting"};
        Color[] colors = {Theme.CYAN, Theme.GREEN, Theme.RED, Theme.PURPLE};
        for (int i = 0; i < titles.length; i++) {
            JPanel card = cardPanel();
            card.setLayout(new BorderLayout(0, 12));
            JLabel title = mutedLabel(titles[i]);
            title.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            JLabel value = new JLabel("—");
            value.setForeground(colors[i]);
            value.setFont(new Font("Segoe UI", Font.BOLD, 36));
            dashboardValues[i] = value;
            card.add(title, BorderLayout.NORTH);
            card.add(value, BorderLayout.CENTER);
            cards.add(card);
        }
        page.add(cards, BorderLayout.NORTH);
        JPanel welcome = cardPanel();
        welcome.setLayout(new GridLayout(0, 1, 0, 10));
        JLabel heading = new JLabel("Welcome to your parking control center");
        heading.setForeground(Theme.TEXT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        welcome.add(heading);
        welcome.add(mutedLabel("Use the navigation to manage parking, process vehicle entry and exit, "
                + "and review operational reports."));
        JPanel actions = new JPanel(new GridLayout(1, 3, 12, 12));
        actions.setOpaque(false);
        actions.add(actionButton("Register vehicle", "Vehicle Entry"));
        actions.add(actionButton("Process vehicle exit", "Vehicle Exit"));
        actions.add(actionButton("View parking slots", "Parking Slots"));
        welcome.add(actions);
        page.add(welcome, BorderLayout.CENTER);

        JPanel slotSection = new JPanel(new BorderLayout(0, 12));
        slotSection.setOpaque(false);
        JLabel slotHeading = new JLabel("LIVE PARKING SLOTS");
        slotHeading.setForeground(Theme.TEXT);
        slotHeading.setFont(new Font("Segoe UI", Font.BOLD, 17));
        slotSection.add(slotHeading, BorderLayout.NORTH);
        slotGrid.setOpaque(false);
        slotSection.add(slotGrid, BorderLayout.CENTER);
        page.add(slotSection, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createSlotsPage() {
        JPanel page = pagePanel();
        slotsTable = table(new String[]{"Slot", "Status", "Vehicle"});
        page.add(tablePanel(slotsTable), BorderLayout.CENTER);
        page.add(buttonRow(button("Refresh", this::refreshSlots)), BorderLayout.SOUTH);
        return page;
    }

    private JPanel createEntryPage() {
        JPanel page = pagePanel();
        JPanel form = formCard("Register vehicle entry");
        JTextField number = inputField();
        JTextField owner = inputField();
        JComboBox<String> type = new JComboBox<>(new String[]{"CAR", "BIKE", "OTHER"});
        styleInput(type);
        addField(form, "Vehicle number", number);
        addField(form, "Owner name", owner);
        addField(form, "Vehicle type", type);
        JButton submit = button("Register vehicle", () -> {
            Vehicle vehicle = new Vehicle(number.getText().trim().toUpperCase(Locale.ROOT),
                    owner.getText().trim(), (String) type.getSelectedItem());
            runTask(() -> manager.enterVehicle(vehicle), result -> {
                if (result.queued) {
                    showInfo(vehicle.getNumber() + " was added to the FIFO waiting queue.");
                } else {
                    showInfo(vehicle.getNumber() + " assigned to slot " + result.slot + ".");
                }
                number.setText("");
                owner.setText("");
                refreshAll();
            });
        });
        form.add(buttonRow(submit));
        page.add(form, BorderLayout.NORTH);
        return page;
    }

    private JPanel createExitPage() {
        JPanel page = pagePanel();
        JPanel form = formCard("Process vehicle exit");
        JTextField number = inputField();
        addField(form, "Vehicle number", number);
        JLabel note = mutedLabel("Fee is rounded up hourly with a one-hour minimum.");
        form.add(note);
        JLabel vehicleInfo = mutedLabel("Search a vehicle number to confirm its active parking details.");
        form.add(vehicleInfo);
        form.add(buttonRow(button("Search vehicle", () -> {
            runTask(() -> manager.findActiveVehicle(number.getText()), vehicle -> {
                if (vehicle == null) {
                    vehicleInfo.setText("No active parking record found.");
                } else {
                    vehicleInfo.setText(vehicle.getNumber() + "  |  " + vehicle.getOwner()
                            + "  |  " + vehicle.getType());
                }
            });
        })));
        JButton submit = button("Calculate fee and release slot", () -> {
            String vehicleNumber = number.getText().trim().toUpperCase(Locale.ROOT);
            runTask(() -> manager.exitVehicle(vehicleNumber), result -> {
                ParkingRecord record = manager.getMostRecentExit();
                String message = String.format("%s exited from slot %s.%nParking fee: %.2f",
                        record.getVehicleNumber(), record.getSlotNumber(), record.getFee());
                if (result.assignedVehicle != null) {
                    message += "\nWaiting vehicle " + result.assignedVehicle + " was assigned the released slot.";
                }
                showInfo(message);
                number.setText("");
                vehicleInfo.setText("Search a vehicle number to confirm its active parking details.");
                refreshAll();
            });
        });
        form.add(buttonRow(submit));
        page.add(form, BorderLayout.NORTH);
        return page;
    }

    private JPanel createQueuePage() {
        JPanel page = pagePanel();
        queueTable = table(new String[]{"Position", "Vehicle", "Owner", "Type", "Queued at"});
        page.add(tablePanel(queueTable), BorderLayout.CENTER);
        page.add(buttonRow(button("Refresh queue", this::refreshQueue)), BorderLayout.SOUTH);
        return page;
    }

    private JPanel createHistoryPage() {
        JPanel page = pagePanel();
        historySearch = inputField();
        historySearch.setToolTipText("Vehicle number or owner name");
        JButton search = button("Search", this::refreshHistory);
        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setOpaque(false);
        controls.add(historySearch, BorderLayout.CENTER);
        controls.add(search, BorderLayout.EAST);
        historyTable = table(new String[]{"Vehicle", "Owner", "Type", "Slot", "Entry", "Exit", "Fee"});
        page.add(controls, BorderLayout.NORTH);
        page.add(tablePanel(historyTable), BorderLayout.CENTER);
        return page;
    }

    private JPanel createReportsPage() {
        JPanel page = pagePanel();
        JPanel cards = new JPanel(new GridLayout(2, 3, 14, 14));
        cards.setOpaque(false);
        String[] titles = {"Vehicles entered today", "Revenue today", "Available slots",
                "Occupied slots", "Occupancy"};
        for (int i = 0; i < titles.length; i++) {
            JPanel card = cardPanel();
            card.setLayout(new BorderLayout(0, 12));
            card.add(mutedLabel(titles[i]), BorderLayout.NORTH);
            JLabel value = new JLabel("—");
            value.setForeground(i == 1 ? Theme.GREEN : Theme.CYAN);
            value.setFont(new Font("Segoe UI", Font.BOLD, 27));
            reportValues[i] = value;
            card.add(value, BorderLayout.CENTER);
            if (i == 4) {
                occupancyBar = new JProgressBar(0, 100);
                occupancyBar.setForeground(Theme.CYAN);
                occupancyBar.setBackground(new Color(38, 40, 44));
                occupancyBar.setBorderPainted(false);
                occupancyBar.setStringPainted(true);
                occupancyBar.setString("Occupied");
                occupancyBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
                occupancyBar.setPreferredSize(new Dimension(180, 26));
                card.add(occupancyBar, BorderLayout.SOUTH);
            }
            cards.add(card);
        }
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        cards.add(empty);
        page.add(cards, BorderLayout.NORTH);
        page.add(buttonRow(button("Refresh report", this::refreshReports)), BorderLayout.SOUTH);
        return page;
    }

    private void addNavigation(JPanel sidebar, String label, String key) {
        JButton button = navButton(label);
        button.addActionListener(event -> showPage(key));
        navigationButtons.put(key, button);
        sidebar.add(button);
    }

    private JButton navButton(String text) {
        JButton button = new SidebarButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setForeground(Theme.MUTED);
        button.setBackground(Theme.SIDEBAR);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SIDEBAR),
                BorderFactory.createEmptyBorder(11, 12, 11, 8)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        button.setPreferredSize(new Dimension(210, 52));
        return button;
    }

    private JButton actionButton(String text, String page) {
        JButton button = button(text, () -> showPage(page));
        button.setPreferredSize(new Dimension(150, 44));
        return button;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new RoundedActionButton(text);
        button.setBackground(Theme.PURPLE);
        button.setForeground(Theme.BACKGROUND);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.addActionListener(event -> action.run());
        return button;
    }

    private JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);
        return panel;
    }

    private JPanel cardPanel() {
        JPanel panel = new RoundedCard();
        panel.setBackground(Theme.PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        return panel;
    }

    private JPanel formCard(String title) {
        JPanel panel = cardPanel();
        panel.setLayout(new GridLayout(0, 1, 0, 8));
        JLabel heading = new JLabel(title);
        heading.setForeground(Theme.TEXT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(heading);
        return panel;
    }

    private void addField(JPanel form, String label, Component field) {
        JLabel caption = mutedLabel(label);
        form.add(caption);
        styleInput(field);
        form.add(field);
    }

    private void styleInput(Component component) {
        if (component instanceof JTextField field) {
            field.setBackground(Theme.INPUT);
            field.setForeground(Theme.TEXT);
            field.setCaretColor(Theme.CYAN);
            field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        } else if (component instanceof JComboBox<?> combo) {
            combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton arrow = new JButton("▼");
                    arrow.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    arrow.setForeground(Theme.CYAN);
                    arrow.setBackground(Theme.INPUT);
                    arrow.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    arrow.setFocusPainted(false);
                    return arrow;
                }
            });
            combo.setBackground(Theme.INPUT);
            combo.setForeground(Theme.TEXT);
            combo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            combo.setOpaque(true);
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setBackground(isSelected ? new Color(0, 91, 112) : Theme.INPUT);
                    setForeground(Theme.TEXT);
                    setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                    return this;
                }
            });
        }
    }

    private JTextField inputField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(300, 38));
        return field;
    }

    private JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        return label;
    }

    private JTable table(String[] columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setBackground(Theme.PANEL);
        table.setForeground(Theme.TEXT);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(new Color(0, 91, 112));
        table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setUI(new javax.swing.plaf.basic.BasicTableHeaderUI());
        table.getTableHeader().setBackground(new Color(31, 33, 37));
        table.getTableHeader().setForeground(Theme.TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setOpaque(true);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setOpaque(true);
        headerRenderer.setBackground(new Color(31, 33, 37));
        headerRenderer.setForeground(Theme.TEXT);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        return table;
    }

    private JScrollPane tablePanel(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(Theme.PANEL);
        scroll.setOpaque(true);
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.getViewport().setOpaque(true);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        styleScrollBar(scroll.getVerticalScrollBar());
        styleScrollBar(scroll.getHorizontalScrollBar());
        JPanel corner = new JPanel();
        corner.setBackground(Theme.PANEL);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        return scroll;
    }

    private void styleScrollBar(javax.swing.JScrollBar scrollBar) {
        scrollBar.setBackground(Theme.PANEL);
        scrollBar.setForeground(new Color(72, 78, 88));
        scrollBar.setOpaque(true);
        scrollBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(72, 78, 88);
                trackColor = Theme.PANEL;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return scrollbarButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return scrollbarButton();
            }

            private JButton scrollbarButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                button.setVisible(false);
                return button;
            }
        });
    }

    private JPanel buttonRow(Component... components) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 3));
        for (Component component : components) panel.add(component);
        return panel;
    }

    private void showPage(String key) {
        pageTitle.setText(key);
        backButton.setVisible(!"Dashboard".equals(key));
        for (Map.Entry<String, JButton> entry : navigationButtons.entrySet()) {
            boolean active = entry.getKey().equals(key);
            JButton nav = entry.getValue();
            nav.setForeground(active ? Theme.CYAN : Theme.MUTED);
            nav.setBackground(active ? new Color(0, 55, 70) : Theme.SIDEBAR);
            nav.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(active ? Theme.CYAN : Theme.SIDEBAR),
                    BorderFactory.createEmptyBorder(11, 12, 11, 8)));
            if (nav instanceof SidebarButton sidebarButton) {
                sidebarButton.setActive(active);
            }
        }
        cardLayout.show(content, key);
        switch (key) {
            case "Dashboard" -> {
                refreshDashboard();
                refreshSlots();
            }
            case "Parking Slots" -> refreshSlots();
            case "Waiting Queue" -> refreshQueue();
            case "Parking History" -> refreshHistory();
            case "Reports" -> refreshReports();
            default -> { }
        }
    }

    private void refreshAll() {
        refreshDashboard();
        refreshSlots();
        refreshQueue();
        refreshReports();
        if (historyTable != null) refreshHistory();
    }

    private void refreshDashboard() {
        runTask(manager::getDashboardStats, stats -> {
            dashboardValues[0].setText(String.valueOf(stats.total));
            dashboardValues[1].setText(String.valueOf(stats.available));
            dashboardValues[2].setText(String.valueOf(stats.occupied));
            dashboardValues[3].setText(String.valueOf(stats.waiting));
        });
    }

    private void refreshSlots() {
        runTask(manager::getSlots, rows -> {
            setRows(slotsTable, rows.toArray(new Object[0][]));
            refreshSlotGrid(rows);
        });
    }

    private void refreshSlotGrid(java.util.List<String[]> rows) {
        slotGrid.removeAll();
        for (String[] row : rows) {
            boolean available = "AVAILABLE".equalsIgnoreCase(row[1]);
            JPanel tile = new JPanel(new BorderLayout(4, 4));
            tile.setBackground(Theme.PANEL);
            tile.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(available ? new Color(35, 112, 73) : new Color(125, 53, 58)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            JLabel slot = new JLabel(row[0]);
            slot.setForeground(Theme.TEXT);
            slot.setFont(new Font("Segoe UI", Font.BOLD, 14));
            JLabel status = new JLabel(available ? "AVAILABLE" : "OCCUPIED");
            status.setForeground(available ? Theme.GREEN : Theme.RED);
            status.setFont(new Font("Segoe UI", Font.BOLD, 11));
            tile.add(slot, BorderLayout.NORTH);
            tile.add(status, BorderLayout.CENTER);
            slotGrid.add(tile);
        }
        slotGrid.revalidate();
        slotGrid.repaint();
    }

    private void refreshQueue() {
        runTask(manager::getWaitingVehicles, rows -> setRows(queueTable, rows.toArray(new Object[0][])));
    }

    private void refreshHistory() {
        String search = historySearch == null ? "" : historySearch.getText();
        runTask(() -> manager.getHistory(search), rows -> {
            Object[][] data = new Object[rows.size()][7];
            for (int i = 0; i < rows.size(); i++) {
                ParkingRecord row = rows.get(i);
                data[i] = new Object[]{row.getVehicleNumber(), row.getOwner(), row.getType(), row.getSlotNumber(),
                        DATE_FORMAT.format(row.getEntryTime()),
                        row.getExitTime() == null ? "In parking" : DATE_FORMAT.format(row.getExitTime()),
                        String.format("%.2f", row.getFee())};
            }
            setRows(historyTable, data);
        });
    }

    private void refreshReports() {
        runTask(manager::getReportStats, stats -> {
            reportValues[0].setText(String.valueOf(stats.today));
            reportValues[1].setText(String.format("%.2f", stats.revenue));
            reportValues[2].setText(String.valueOf(stats.available));
            reportValues[3].setText(String.valueOf(stats.occupied));
            double occupancy = stats.total == 0 ? 0 : (100.0 * stats.occupied / stats.total);
            reportValues[4].setText(String.format("%.1f%%", occupancy));
            occupancyBar.setValue((int) Math.round(occupancy));
            occupancyBar.setString(String.format("%.1f%% occupied", occupancy));
        });
    }

    private void setRows(JTable table, Object[][] rows) {
        if (table == null) return;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Object[] row : rows) model.addRow(row);
    }

    private <T> void runTask(Callable<T> work, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception { return work.call(); }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(DashboardFrame.this,
                            cause.getMessage() == null ? cause.toString() : cause.getMessage(),
                            "Operation failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Smart Parking", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class RoundedCard extends JPanel {
        private RoundedCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g.setColor(new Color(0, 0, 0, 110));
            g.fillRoundRect(2, 4, width - 5, height - 5, 18, 18);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, width - 7, height - 7, 18, 18);
            g.setColor(Theme.BORDER);
            g.drawRoundRect(0, 0, width - 8, height - 8, 18, 18);
            g.dispose();
        }
    }

    private static final class RoundedActionButton extends JButton {
        private RoundedActionButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int inset = 2;
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRoundRect(inset, inset + 2, getWidth() - 2 * inset, getHeight() - 2 * inset, 14, 14);
            g.setColor(getModel().isRollover() ? Theme.CYAN : getBackground());
            g.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class SidebarButton extends JButton {
        private boolean active;

        private SidebarButton(String text) {
            super(text);
        }

        private void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(active ? new Color(0, 55, 70) : Theme.SIDEBAR);
            g.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 12, 12);
            if (active) {
                g.setColor(Theme.CYAN);
                g.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 12, 12);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
