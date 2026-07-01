package com.hotel.ui.admin;

import com.hotel.exception.HotelException;
import com.hotel.model.Billing;
import com.hotel.model.BillStatus;
import com.hotel.model.Reservation;
import com.hotel.service.BillingService;
import com.hotel.service.ReservationService;
import com.hotel.ui.common.UIUtils;
import com.hotel.util.Validator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Modal dialog used for "Edit Bill" in Billing management.
 * Room charges are derived automatically from the selected reservation's
 * total amount and are not directly editable; only additional charges,
 * discount, tax, and status can be adjusted by the admin. Recalculates total immediately.
 */
public class BillingFormDialog extends JDialog {

    private final BillingService billingService = new BillingService();
    private final ReservationService reservationService = new ReservationService();

    private final Billing existingBilling;
    private boolean saved = false;

    private JComboBox<Reservation> reservationCombo;
    private JLabel roomChargesValueLabel;
    private JTextField additionalChargesField;
    private JTextField discountField;
    private JTextField taxField;
    private JLabel totalAmountValueLabel;
    private JComboBox<BillStatus> billStatusCombo;

    public BillingFormDialog(Window owner, Billing existingBilling) {
        super(owner, "Edit Bill", ModalityType.APPLICATION_MODAL);
        this.existingBilling = existingBilling;
        initComponents();
        if (existingBilling != null) {
            populateFields(existingBilling);
        }
        setupRecalculationListeners();
        recalculateTotalPreview();
    }

    private void initComponents() {
        setSize(440, 540);
        setLocationRelativeTo(getOwner());
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        int row = 0;

        gbc.gridy = row++;
        form.add(formLabel("Reservation"), gbc);
        reservationCombo = new JComboBox<>();
        reservationCombo.setPreferredSize(new Dimension(0, 36));
        reservationCombo.setFont(UIUtils.FONT_REGULAR);
        reservationCombo.setEnabled(false); // Bill reservation cannot be changed once created
        gbc.gridy = row++;
        form.add(reservationCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Room Charges (auto-calculated)"), gbc);
        roomChargesValueLabel = new JLabel("₱0.00");
        roomChargesValueLabel.setFont(UIUtils.FONT_BOLD);
        gbc.gridy = row++;
        form.add(roomChargesValueLabel, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Additional Charges (₱)"), gbc);
        additionalChargesField = new JTextField("0");
        additionalChargesField.setPreferredSize(new Dimension(0, 36));
        additionalChargesField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(additionalChargesField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Discount (₱)"), gbc);
        discountField = new JTextField("0");
        discountField.setPreferredSize(new Dimension(0, 36));
        discountField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(discountField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Tax (₱)"), gbc);
        taxField = new JTextField("0");
        taxField.setPreferredSize(new Dimension(0, 36));
        taxField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(taxField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Bill Status"), gbc);
        billStatusCombo = new JComboBox<>(BillStatus.values());
        billStatusCombo.setPreferredSize(new Dimension(0, 36));
        billStatusCombo.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(billStatusCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Total Amount Preview"), gbc);
        totalAmountValueLabel = new JLabel("₱0.00");
        totalAmountValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalAmountValueLabel.setForeground(UIUtils.ACCENT_COLOR);
        gbc.gridy = row++;
        form.add(totalAmountValueLabel, gbc);

        add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JButton saveButton = UIUtils.createStyledButton("Save", UIUtils.SUCCESS_COLOR);
        saveButton.addActionListener(e -> handleSave());
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtils.FONT_BOLD);
        return label;
    }

    private void populateFields(Billing billing) {
        reservationCombo.removeAllItems();
        // Create a dummy representation for the combo to show the linked reservation info
        Reservation dummyRes = new Reservation();
        dummyRes.setReservationId(billing.getReservationId());
        dummyRes.setGuestId(0);
        // Note: reservationCombo displays the string representation of Reservation.
        // Let's configure reservationCombo to show the correct ID since reservation is read-only here.
        reservationCombo.addItem(dummyRes);
        reservationCombo.setSelectedIndex(0);

        roomChargesValueLabel.setText(String.format("₱%,.2f", billing.getRoomCharges()));
        additionalChargesField.setText(billing.getAdditionalCharges().toPlainString());
        discountField.setText(billing.getDiscount().toPlainString());
        taxField.setText(billing.getTax().toPlainString());
        billStatusCombo.setSelectedItem(billing.getBillStatus());
    }

    private void setupRecalculationListeners() {
        DocumentListener recalculateListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalculateTotalPreview(); }
            public void removeUpdate(DocumentEvent e) { recalculateTotalPreview(); }
            public void changedUpdate(DocumentEvent e) { recalculateTotalPreview(); }
        };
        additionalChargesField.getDocument().addDocumentListener(recalculateListener);
        discountField.getDocument().addDocumentListener(recalculateListener);
        taxField.getDocument().addDocumentListener(recalculateListener);
    }

    private void recalculateTotalPreview() {
        try {
            BigDecimal roomCharges = existingBilling != null ? existingBilling.getRoomCharges() : BigDecimal.ZERO;
            BigDecimal additionalCharges = parseBigDecimalSafely(additionalChargesField.getText());
            BigDecimal discount = parseBigDecimalSafely(discountField.getText());
            BigDecimal tax = parseBigDecimalSafely(taxField.getText());

            BigDecimal total = roomCharges.add(additionalCharges).add(tax).subtract(discount);
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO;
            }
            totalAmountValueLabel.setText(String.format("₱%,.2f", total));
        } catch (Exception e) {
            totalAmountValueLabel.setText("₱---");
        }
    }

    private BigDecimal parseBigDecimalSafely(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal bd = new BigDecimal(text.trim());
            return bd.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : bd;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void handleSave() {
        try {
            BigDecimal additionalCharges = Validator.parseBigDecimal(additionalChargesField.getText(), "Additional charges");
            BigDecimal discount = Validator.parseBigDecimal(discountField.getText(), "Discount");
            BigDecimal tax = Validator.parseBigDecimal(taxField.getText(), "Tax");
            BillStatus billStatus = (BillStatus) billStatusCombo.getSelectedItem();

            Validator.requireNonNegative(additionalCharges, "Additional charges");
            Validator.requireNonNegative(discount, "Discount");
            Validator.requireNonNegative(tax, "Tax");
            Validator.requireNonNull(billStatus, "Bill Status");

            if (existingBilling != null && existingBilling.getBillId() > 0) {
                billingService.updateBilling(existingBilling.getBillId(), additionalCharges, discount, tax, billStatus);
                UIUtils.showSuccess(this, "Bill updated successfully.");
                saved = true;
            }
            dispose();
        } catch (HotelException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
