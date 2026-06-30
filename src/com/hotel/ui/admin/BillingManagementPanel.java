package com.hotel.ui.admin;

import com.hotel.model.Billing;
import com.hotel.service.BillingService;
import com.hotel.ui.common.PlaceholderTextField;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for managing billing records: add, edit, soft-delete (archive),
 * view archived bills (with restore option), and refresh. The table shows
 * full joined details (guest name, room number) for every bill in the database.
 */
public class BillingManagementPanel extends JPanel {

    private final BillingService billingService = new BillingService();

    private ReadOnlyTableModel tableModel;
    private JTable table;
    private boolean viewingArchived = false;
    private JButton toggleArchiveButton;
    private JLabel titleLabel;
    private PlaceholderTextField searchField;

    private List<Billing> allBillings = new ArrayList<>();

    private static final String[] COLUMNS = {
            "Bill ID", "Reservation ID", "Guest", "Room", "Room Charges", "Additional",
            "Discount", "Tax", "Total", "Payment Status", "Payment Method"
    };

    public BillingManagementPanel() {
        initComponents();
        loadActiveBillings();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshCurrentView();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        titleLabel = UIUtils.createSectionTitle("Billing Management");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        JButton addButton = UIUtils.createStyledButton("Add Bill", UIUtils.SUCCESS_COLOR);
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
        searchField = new PlaceholderTextField("Search by Bill ID, Guest Name, or Reservation ID...");
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
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadActiveBillings() {
        UIUtils.runSafely(this, () -> {
            allBillings = billingService.getAllActiveBillings();
            applyFilterAndSearch();
        });
    }

    private void loadArchivedBillings() {
        UIUtils.runSafely(this, () -> {
            allBillings = billingService.getAllArchivedBillings();
            applyFilterAndSearch();
        });
    }

    private void applyFilterAndSearch() {
        String query = searchField.getText().trim().toLowerCase();
        List<Billing> filtered = new ArrayList<>();

        for (Billing b : allBillings) {
            boolean keep = true;
            if (!query.isEmpty()) {
                String billIdStr = String.valueOf(b.getBillId());
                String resIdStr = String.valueOf(b.getReservationId());
                String guestName = b.getGuestName() == null ? "" : b.getGuestName().toLowerCase();

                if (!billIdStr.contains(query)
                        && !resIdStr.contains(query)
                        && !guestName.contains(query)) {
                    keep = false;
                }
            }
            if (keep) {
                filtered.add(b);
            }
        }

        // Sort by Bill ID Ascending
        filtered.sort(java.util.Comparator.comparingInt(Billing::getBillId));

        populateTable(filtered);
    }

    private void populateTable(List<Billing> billings) {
        tableModel.setRowCount(0);
        for (Billing b : billings) {
            tableModel.addRow(new Object[]{
                    b.getBillId(),
                    b.getReservationId(),
                    b.getGuestName(),
                    b.getRoomNumber(),
                    String.format("₱%,.2f", b.getRoomCharges()),
                    String.format("₱%,.2f", b.getAdditionalCharges()),
                    String.format("₱%,.2f", b.getDiscount()),
                    String.format("₱%,.2f", b.getTax()),
                    String.format("₱%,.2f", b.getTotalAmount()),
                    b.getPaymentStatus(),
                    b.getPaymentMethod()
            });
        }
        UIUtils.formatTableColumns(table);
    }

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        if (viewingArchived) {
            titleLabel.setText("Billing Management — Archived Bills");
            toggleArchiveButton.setText("View Active");
            loadArchivedBillings();
        } else {
            titleLabel.setText("Billing Management");
            toggleArchiveButton.setText("View Archived");
            loadActiveBillings();
        }
    }

    public void refreshCurrentView() {
        if (viewingArchived) {
            loadArchivedBillings();
        } else {
            loadActiveBillings();
        }
    }

    private void handleAdd() {
        BillingFormDialog dialog = new BillingFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadActiveBillings();
        }
    }

    private void handleEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a bill to edit.");
            return;
        }
        if (viewingArchived) {
            UIUtils.showInfo(this, "Archived bills cannot be edited. Restore the bill first.");
            return;
        }
        int billId = (int) tableModel.getValueAt(row, 0);
        UIUtils.runSafely(this, () -> {
            Billing billing = billingService.getBillingOrThrow(billId);
            BillingFormDialog dialog = new BillingFormDialog(SwingUtilities.getWindowAncestor(this), billing);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadActiveBillings();
            }
        });
    }

    private void handleDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a bill to delete.");
            return;
        }
        int billId = (int) tableModel.getValueAt(row, 0);

        if (viewingArchived) {
            boolean confirmed = UIUtils.confirm(this,
                    "Restore bill #" + billId + " back to active bills?", "Confirm Restore");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    billingService.restoreBilling(billId);
                    UIUtils.showSuccess(this, "Bill restored successfully.");
                    loadArchivedBillings();
                });
            }
        } else {
            boolean confirmed = UIUtils.confirm(this,
                    "Move bill #" + billId + " to archive? This is a soft delete; the record is not permanently lost.",
                    "Confirm Delete");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    billingService.softDeleteBilling(billId);
                    UIUtils.showSuccess(this, "Bill archived successfully.");
                    loadActiveBillings();
                });
            }
        }
    }

    public void showBillForReservation(int reservationId) {
        loadActiveBillings();

        boolean found = false;
        for (int i = 0; i < table.getRowCount(); i++) {
            Object val = tableModel.getValueAt(i, 1);
            if (val instanceof Integer && (Integer) val == reservationId) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                found = true;
                break;
            }
        }

        if (!found) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "No billing record found for Reservation #" + reservationId + ".\nWould you like to create a new bill now?",
                    "Create Bill", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                Billing temp = new Billing();
                temp.setReservationId(reservationId);
                BillingFormDialog dialog = new BillingFormDialog(SwingUtilities.getWindowAncestor(this), temp);
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    loadActiveBillings();
                    for (int i = 0; i < table.getRowCount(); i++) {
                        Object val = tableModel.getValueAt(i, 1);
                        if (val instanceof Integer && (Integer) val == reservationId) {
                            table.setRowSelectionInterval(i, i);
                            table.scrollRectToVisible(table.getCellRect(i, 0, true));
                            break;
                        }
                    }
                }
            }
        }
    }
}
