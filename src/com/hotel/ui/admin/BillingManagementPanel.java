package com.hotel.ui.admin;

import com.hotel.model.Billing;
import com.hotel.service.BillingService;
import com.hotel.ui.common.PlaceholderTextField;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for reviewing, managing, and printing billing records.
 * Generating bills is automated and does not allow manual creation from here.
 */
public class BillingManagementPanel extends JPanel {

    private final BillingService billingService = new BillingService();

    private ReadOnlyTableModel tableModel;
    private JTable table;
    private boolean viewingArchived = false;
    private JButton viewDetailsButton;
    private JButton editButton;
    private JButton printButton;
    private JButton deleteButton;
    private JButton deletePermanentlyButton;
    private JButton toggleArchiveButton;
    private JLabel titleLabel;
    private PlaceholderTextField searchField;

    private List<Billing> allBillings = new ArrayList<>();

    private static final String[] COLUMNS = {
            "Bill ID", "Reservation ID", "Guest Name", "Room Number", "Room Type", 
            "Room Charges", "Additional Charges", "Discount", "Tax", "Total Amount", 
            "Bill Status", "Billing Date"
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
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        titleLabel = UIUtils.createSectionTitle("Billing Management");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        viewDetailsButton = UIUtils.createStyledButton("View Details", UIUtils.PRIMARY_COLOR);
        viewDetailsButton.addActionListener(e -> handleViewDetails());

        editButton = UIUtils.createStyledButton("Edit Charges", UIUtils.ACCENT_COLOR);
        editButton.addActionListener(e -> handleEdit());

        printButton = UIUtils.createStyledButton("Print Invoice", new Color(41, 128, 185));
        printButton.addActionListener(e -> handlePrintInvoice());

        deleteButton = UIUtils.createStyledButton("Archive", UIUtils.DANGER_COLOR);
        deleteButton.addActionListener(e -> handleDelete());

        deletePermanentlyButton = UIUtils.createStyledButton("Delete Permanently", UIUtils.DANGER_COLOR);
        deletePermanentlyButton.setVisible(false);
        deletePermanentlyButton.addActionListener(e -> handleDeletePermanently());

        toggleArchiveButton = UIUtils.createStyledButton("View Archived", UIUtils.PRIMARY_COLOR);
        toggleArchiveButton.addActionListener(e -> toggleArchivedView());

        buttonsRow.add(viewDetailsButton);
        buttonsRow.add(editButton);
        buttonsRow.add(printButton);
        buttonsRow.add(deleteButton);
        buttonsRow.add(deletePermanentlyButton);
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
                    String text = "No billing records available. (Bills are generated automatically when stays check out.)";
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(text)) / 2;
                    int y = getHeight() / 2;
                    g2.drawString(text, x, y);
                    g2.dispose();
                }
            }
        };
        UIUtils.styleTable(table);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(230, 233, 237), 1, true));
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
                    b.getRoomType(),
                    String.format("₱%,.2f", b.getRoomCharges()),
                    String.format("₱%,.2f", b.getAdditionalCharges()),
                    String.format("₱%,.2f", b.getDiscount()),
                    String.format("₱%,.2f", b.getTax()),
                    String.format("₱%,.2f", b.getTotalAmount()),
                    b.getBillStatus().getDisplayName(),
                    b.getBillingDate() != null ? b.getBillingDate().toString() : "N/A"
            });
        }
        UIUtils.formatTableColumns(table);
    }

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        if (viewingArchived) {
            titleLabel.setText("Billing Management — Archived Bills");
            toggleArchiveButton.setText("View Active");
            viewDetailsButton.setVisible(false);
            editButton.setVisible(false);
            printButton.setVisible(false);
            deleteButton.setText("Restore");
            deleteButton.setBackground(UIUtils.SUCCESS_COLOR);
            deletePermanentlyButton.setVisible(true);
            loadArchivedBillings();
        } else {
            titleLabel.setText("Billing Management");
            toggleArchiveButton.setText("View Archived");
            viewDetailsButton.setVisible(true);
            editButton.setVisible(true);
            printButton.setVisible(true);
            deleteButton.setText("Archive");
            deleteButton.setBackground(UIUtils.DANGER_COLOR);
            deletePermanentlyButton.setVisible(false);
            loadActiveBillings();
        }
    }

    public void refreshCurrentView() {
        billingService.generateMissingBills();
        if (viewingArchived) {
            loadArchivedBillings();
        } else {
            loadActiveBillings();
        }
    }

    private void handleViewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a bill to view.");
            return;
        }
        int billId = (int) tableModel.getValueAt(row, 0);
        UIUtils.runSafely(this, () -> {
            Billing billing = billingService.getBillingOrThrow(billId);
            showDetailsModal(billing);
        });
    }

    private void showDetailsModal(Billing b) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Billing Details - Bill #" + b.getBillId(), true);
        dialog.setSize(480, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(0, 2, 8, 12));
        mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        mainPanel.setBackground(Color.WHITE);

        mainPanel.add(new JLabel("Bill ID:"));
        mainPanel.add(new JLabel(String.valueOf(b.getBillId())));

        mainPanel.add(new JLabel("Reservation ID:"));
        mainPanel.add(new JLabel(String.valueOf(b.getReservationId())));

        mainPanel.add(new JLabel("Guest Name:"));
        mainPanel.add(new JLabel(b.getGuestName()));

        mainPanel.add(new JLabel("Room Number:"));
        mainPanel.add(new JLabel(b.getRoomNumber()));

        mainPanel.add(new JLabel("Room Type:"));
        mainPanel.add(new JLabel(String.valueOf(b.getRoomType())));

        mainPanel.add(new JLabel("Check-in Date:"));
        mainPanel.add(new JLabel(b.getCheckInDate() != null ? b.getCheckInDate().toString() : "N/A"));

        mainPanel.add(new JLabel("Check-out Date:"));
        mainPanel.add(new JLabel(b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : "N/A"));

        mainPanel.add(new JLabel("Number of Nights:"));
        mainPanel.add(new JLabel(String.valueOf(b.getNumberOfNights())));

        mainPanel.add(new JLabel("Room Charges:"));
        mainPanel.add(new JLabel(String.format("₱%,.2f", b.getRoomCharges())));

        mainPanel.add(new JLabel("Additional Charges:"));
        mainPanel.add(new JLabel(String.format("₱%,.2f", b.getAdditionalCharges())));

        mainPanel.add(new JLabel("Discount:"));
        mainPanel.add(new JLabel(String.format("₱%,.2f", b.getDiscount())));

        mainPanel.add(new JLabel("Tax:"));
        mainPanel.add(new JLabel(String.format("₱%,.2f", b.getTax())));

        JLabel totalLbl = new JLabel("Total Amount:");
        totalLbl.setFont(UIUtils.FONT_BOLD);
        mainPanel.add(totalLbl);

        JLabel totalVal = new JLabel(String.format("₱%,.2f", b.getTotalAmount()));
        totalVal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalVal.setForeground(UIUtils.ACCENT_COLOR);
        mainPanel.add(totalVal);

        mainPanel.add(new JLabel("Bill Status:"));
        mainPanel.add(new JLabel(b.getBillStatus().getDisplayName()));

        mainPanel.add(new JLabel("Billing Date:"));
        mainPanel.add(new JLabel(b.getBillingDate() != null ? b.getBillingDate().toString() : "N/A"));

        dialog.add(mainPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Color.WHITE);
        JButton close = UIUtils.createStyledButton("Close", UIUtils.PRIMARY_COLOR);
        close.addActionListener(e -> dialog.dispose());
        btnPanel.add(close);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
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

    private void handlePrintInvoice() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtils.showInfo(this, "Please select a bill to print.");
            return;
        }
        int billId = (int) tableModel.getValueAt(row, 0);
        UIUtils.runSafely(this, () -> {
            Billing b = billingService.getBillingOrThrow(billId);
            showPrintDialog(b);
        });
    }

    private void showPrintDialog(Billing b) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Invoice - Bill #" + b.getBillId(), true);
        dialog.setSize(520, 640);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea printArea = new JTextArea();
        printArea.setEditable(false);
        printArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        printArea.setMargin(new Insets(16, 20, 16, 20));

        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("               GITAWAY HOTEL INVOICE              \n");
        sb.append("==================================================\n");
        sb.append(String.format("Invoice No   : %-20s\n", "INV-" + b.getBillId()));
        sb.append(String.format("Billing Date : %-20s\n", b.getBillingDate()));
        sb.append(String.format("Guest Name   : %-20s\n", b.getGuestName()));
        sb.append(String.format("Res ID       : %-20s\n", b.getReservationId()));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Room Number  : %-20s\n", b.getRoomNumber()));
        sb.append(String.format("Room Type    : %-20s\n", b.getRoomType()));
        sb.append(String.format("Check-in     : %-20s\n", b.getCheckInDate()));
        sb.append(String.format("Check-out    : %-20s\n", b.getCheckOutDate()));
        sb.append(String.format("Nights       : %-20d\n", b.getNumberOfNights()));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Room Charges        : %18s\n", String.format("₱%,.2f", b.getRoomCharges())));
        sb.append(String.format("Additional Charges  : %18s\n", String.format("₱%,.2f", b.getAdditionalCharges())));
        sb.append(String.format("Tax                 : %18s\n", String.format("₱%,.2f", b.getTax())));
        sb.append(String.format("Discount            : %18s\n", String.format("-₱%,.2f", b.getDiscount())));
        sb.append("==================================================\n");
        sb.append(String.format("TOTAL AMOUNT DUE    : %18s\n", String.format("₱%,.2f", b.getTotalAmount())));
        sb.append("==================================================\n");
        sb.append(String.format("Bill Status         : %-20s\n", b.getBillStatus().getDisplayName()));
        sb.append("--------------------------------------------------\n");
        sb.append("            Thank you for staying with us!        \n");
        sb.append("==================================================\n");

        printArea.setText(sb.toString());

        JScrollPane scroll = new JScrollPane(printArea);
        scroll.setBorder(null);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBackground(Color.WHITE);
        JButton close = UIUtils.createStyledButton("Close", UIUtils.PRIMARY_COLOR);
        close.addActionListener(e -> dialog.dispose());
        
        JButton printBtn = UIUtils.createStyledButton("Print Statement", UIUtils.SUCCESS_COLOR);
        printBtn.addActionListener(e -> {
            try {
                printArea.print();
            } catch (Exception ex) {
                UIUtils.showError(dialog, "Printing failed: " + ex.getMessage());
            }
        });

        panel.add(printBtn);
        panel.add(close);
        dialog.add(panel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void handleDelete() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            UIUtils.showInfo(this, "Please select at least one bill.");
            return;
        }
        List<Integer> billIds = new ArrayList<>();
        for (int row : selectedRows) {
            billIds.add((int) tableModel.getValueAt(row, 0));
        }

        if (viewingArchived) {
            boolean confirmed = UIUtils.confirm(this,
                    "Restore selected bill(s) back to active bills?", "Confirm Restore");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    for (int billId : billIds) {
                        billingService.restoreBilling(billId);
                    }
                    UIUtils.showSuccess(this, "Selected bill(s) restored successfully.");
                    loadArchivedBillings();
                });
            }
        } else {
            boolean confirmed = UIUtils.confirm(this,
                    "Move selected bill(s) to archive? This is a soft delete; the record is not permanently lost.",
                    "Confirm Archive");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    for (int billId : billIds) {
                        billingService.softDeleteBilling(billId);
                    }
                    UIUtils.showSuccess(this, "Selected bill(s) archived successfully.");
                    loadActiveBillings();
                });
            }
        }
    }

    private void handleDeletePermanently() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            UIUtils.showInfo(this, "Please select at least one bill to delete permanently.");
            return;
        }
        List<Integer> billIds = new ArrayList<>();
        for (int row : selectedRows) {
            billIds.add((int) tableModel.getValueAt(row, 0));
        }

        boolean confirmed = UIUtils.confirmPermanentDelete(this);
        if (confirmed) {
            UIUtils.runSafely(this, () -> {
                for (int billId : billIds) {
                    billingService.deleteBillingPermanently(billId);
                }
                UIUtils.showSuccess(this, "Selected bill(s) permanently deleted successfully.");
                loadArchivedBillings();
            });
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
            UIUtils.showInfo(this, "No billing record found for Reservation #" + reservationId + ".\nBilling records are generated automatically on CHECKED_OUT.");
        }
    }
}
