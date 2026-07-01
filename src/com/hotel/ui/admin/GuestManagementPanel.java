package com.hotel.ui.admin;

import com.hotel.model.Guest;
import com.hotel.service.GuestService;
import com.hotel.ui.common.PlaceholderTextField;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for managing hotel guests: add, edit, soft-delete (archive),
 * view archived guests (with restore option), and refresh.
 */
public class GuestManagementPanel extends JPanel {

    private final GuestService guestService = new GuestService();
    private final com.hotel.dao.UserDao userDao = new com.hotel.dao.impl.UserDaoImpl();
    private final java.util.Map<Integer, String> userIdToUsernameMap = new java.util.HashMap<>();

    private ReadOnlyTableModel tableModel;
    private JTable table;
    private boolean viewingArchived = false;
    private JButton toggleArchiveButton;
    private JLabel titleLabel;
    private PlaceholderTextField searchField;

    private List<Guest> allGuests = new ArrayList<>();

    private static final String[] COLUMNS = {
            "ID", "First Name", "Last Name", "Email", "Phone"
    };

    public GuestManagementPanel() {
        initComponents();
        loadActiveGuests();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        titleLabel = UIUtils.createSectionTitle("Guest Management");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        JButton addButton = UIUtils.createStyledButton("Add Guest", UIUtils.SUCCESS_COLOR);
        addButton.addActionListener(e -> handleAdd());

        JButton editButton = UIUtils.createStyledButton("Edit", UIUtils.ACCENT_COLOR);
        editButton.addActionListener(e -> handleEdit());

        JButton deleteButton = UIUtils.createStyledButton("Delete", UIUtils.DANGER_COLOR);
        deleteButton.addActionListener(e -> handleDelete());

        toggleArchiveButton = UIUtils.createStyledButton("View Archived", UIUtils.PRIMARY_COLOR);
        toggleArchiveButton.addActionListener(e -> toggleArchivedView());

        buttonsRow.add(addButton);
        buttonsRow.add(editButton);
        buttonsRow.add(deleteButton);
        buttonsRow.add(toggleArchiveButton);
        headerRow.add(buttonsRow, BorderLayout.EAST);

        // Search Bar Panel
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UIUtils.FONT_BOLD);
        searchField = new PlaceholderTextField("Search by Guest ID, Name, Username, or Contact Number...");
        searchField.setPreferredSize(new Dimension(0, 36));
        searchField.setFont(UIUtils.FONT_REGULAR);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilterAndSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilterAndSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilterAndSearch(); }
        });
        searchPanel.add(searchLbl, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);
        northContainer.add(headerRow);
        northContainer.add(searchPanel);

        add(northContainer, BorderLayout.NORTH);

        tableModel = new ReadOnlyTableModel(COLUMNS, 0);
        table = new JTable(tableModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.GRAY);
                    g2.setFont(UIUtils.FONT_HEADER);
                    String text = "No matching records found.";
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(text)) / 2;
                    int y = getHeight() / 2;
                    g2.drawString(text, x, y);
                    g2.dispose();
                }
            }
        };
        UIUtils.styleTable(table);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(230, 233, 237), 1, true));
        add(scrollPane, BorderLayout.CENTER);
    }

    private String getUsername(Integer userId) {
        if (userId == null) {
            return "N/A";
        }
        if (userIdToUsernameMap.containsKey(userId)) {
            return userIdToUsernameMap.get(userId);
        }
        String username = userDao.findById(userId)
                .map(com.hotel.model.User::getUsername)
                .orElse("N/A");
        userIdToUsernameMap.put(userId, username);
        return username;
    }

    private void loadActiveGuests() {
        UIUtils.runSafely(this, () -> {
            allGuests = guestService.getAllActiveGuests();
            applyFilterAndSearch();
        });
    }

    private void loadArchivedGuests() {
        UIUtils.runSafely(this, () -> {
            allGuests = guestService.getAllArchivedGuests();
            applyFilterAndSearch();
        });
    }

    private void applyFilterAndSearch() {
        String query = searchField.getText().trim().toLowerCase();
        List<Guest> filtered = new ArrayList<>();

        for (Guest g : allGuests) {
            boolean keep = true;
            if (!query.isEmpty()) {
                String fullName = g.getFullName().toLowerCase();
                String email = g.getEmail() == null ? "" : g.getEmail().toLowerCase();
                String phone = g.getPhone() == null ? "" : g.getPhone().toLowerCase();
                String idNum = g.getIdNumber() == null ? "" : g.getIdNumber().toLowerCase();
                String guestIdStr = String.valueOf(g.getGuestId());
                String username = getUsername(g.getUserId()).toLowerCase();

                if (!guestIdStr.contains(query)
                        && !fullName.contains(query)
                        && !email.contains(query)
                        && !phone.contains(query)
                        && !idNum.contains(query)
                        && !username.contains(query)) {
                    keep = false;
                }
            }
            if (keep) {
                filtered.add(g);
            }
        }

        // Default sort by Guest ID Ascending
        filtered.sort(java.util.Comparator.comparingInt(Guest::getGuestId));

        populateTable(filtered);
    }

    private void populateTable(List<Guest> guests) {
        tableModel.setRowCount(0);
        for (Guest g : guests) {
            tableModel.addRow(new Object[]{
                    g.getGuestId(),
                    g.getFirstName(),
                    g.getLastName(),
                    g.getEmail(),
                    g.getPhone()
            });
        }
        UIUtils.formatTableColumns(table);
    }

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        if (viewingArchived) {
            titleLabel.setText("Guest Management — Archived Guests");
            toggleArchiveButton.setText("View Active");
            loadArchivedGuests();
        } else {
            titleLabel.setText("Guest Management");
            toggleArchiveButton.setText("View Archived");
            loadActiveGuests();
        }
    }

    public void refreshCurrentView() {
        if (viewingArchived) {
            loadArchivedGuests();
        } else {
            loadActiveGuests();
        }
    }

    private void handleAdd() {
        GuestFormDialog dialog = new GuestFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadActiveGuests();
        }
    }

    private void handleEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a guest to edit.");
            return;
        }
        if (viewingArchived) {
            UIUtils.showInfo(this, "Archived guests cannot be edited. Restore the guest first.");
            return;
        }
        int guestId = (int) tableModel.getValueAt(row, 0);
        UIUtils.runSafely(this, () -> {
            Guest guest = guestService.getGuestOrThrow(guestId);
            GuestFormDialog dialog = new GuestFormDialog(SwingUtilities.getWindowAncestor(this), guest);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadActiveGuests();
            }
        });
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a guest to delete.");
            return;
        }
        int guestId = (int) tableModel.getValueAt(row, 0);
        String guestName = tableModel.getValueAt(row, 1) + " " + tableModel.getValueAt(row, 2);

        if (viewingArchived) {
            boolean confirmed = UIUtils.confirm(this,
                    "Restore guest " + guestName + " back to active guests?", "Confirm Restore");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    guestService.restoreGuest(guestId);
                    UIUtils.showSuccess(this, "Guest restored successfully.");
                    loadArchivedGuests();
                });
            }
        } else {
            boolean confirmed = UIUtils.confirm(this,
                    "Move guest " + guestName + " to archive? This is a soft delete; the record is not permanently lost.",
                    "Confirm Delete");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    guestService.softDeleteGuest(guestId);
                    UIUtils.showSuccess(this, "Guest archived successfully.");
                    loadActiveGuests();
                });
            }
        }
    }
}
