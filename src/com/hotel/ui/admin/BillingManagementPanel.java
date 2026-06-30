package com.hotel.ui.admin;

import com.hotel.model.Billing;
import com.hotel.service.BillingService;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;

import javax.swing.*;
import java.awt.*;
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

    private static final String[] COLUMNS = {
            "Bill ID", "Reservation ID", "Guest", "Room", "Room Charges", "Additional",
            "Discount", "Tax", "Total", "Payment Status", "Payment Method"
    };

    public BillingManagementPanel() {
        initComponents();
        loadActiveBillings();
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

        add(headerRow, BorderLayout.NORTH);

        tableModel = new ReadOnlyTableModel(COLUMNS, 0);
        table = new JTable(tableModel);
        UIUtils.styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadActiveBillings() {
        UIUtils.runSafely(this, () -> {
            List<Billing> billings = billingService.getAllActiveBillings();
            populateTable(billings);
        });
    }

    private void loadArchivedBillings() {
        UIUtils.runSafely(this, () -> {
            List<Billing> billings = billingService.getAllArchivedBillings();
            populateTable(billings);
        });
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
}
