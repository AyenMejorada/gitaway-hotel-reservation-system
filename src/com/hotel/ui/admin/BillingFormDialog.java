package com.hotel.ui.admin;

import com.hotel.exception.HotelException;
import com.hotel.model.Billing;
import com.hotel.model.PaymentMethod;
import com.hotel.model.PaymentStatus;
import com.hotel.model.Reservation;
import com.hotel.service.BillingService;
import com.hotel.service.ReservationService;
import com.hotel.ui.common.UIUtils;
import com.hotel.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Modal dialog used for both "Add Bill" and "Edit Bill" in Billing management.
 * Room charges are derived automatically from the selected reservation's
 * total amount and are not directly editable; only additional charges,
 * discount, and tax can be adjusted by the admin.
 */
public class BillingFormDialog extends JDialog {

    private final BillingService billingService = new BillingService();
    private final ReservationService reservationService = new ReservationService();

    private final Billing existingBilling; // null when adding
    private boolean saved = false;

    private JComboBox<Reservation> reservationCombo;
    private JLabel roomChargesValueLabel;
    private JTextField additionalChargesField;
    private JTextField discountField;
    private JTextField taxField;
    private JComboBox<PaymentStatus> paymentStatusCombo;
    private JComboBox<PaymentMethod> paymentMethodCombo;

    public BillingFormDialog(Window owner, Billing existingBilling) {
        super(owner, existingBilling == null ? "Add Bill" : "Edit Bill", ModalityType.APPLICATION_MODAL);
        this.existingBilling = existingBilling;
        initComponents();
        loadComboData();
        if (existingBilling != null) {
            populateFields(existingBilling);
        }
    }

    private void initComponents() {
        setSize(440, 620);
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
        reservationCombo.addActionListener(e -> updateRoomChargesPreview());
        gbc.gridy = row++;
        form.add(reservationCombo, gbc);
        if (existingBilling != null) {
            reservationCombo.setEnabled(false); // bill is tied to one reservation once created
        }

        gbc.gridy = row++;
        form.add(formLabel("Room Charges (auto-calculated)"), gbc);
        roomChargesValueLabel = new JLabel("₱0.00");
        roomChargesValueLabel.setFont(UIUtils.FONT_BOLD);
        gbc.gridy = row++;
        form.add(roomChargesValueLabel, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Additional Charges (₱)"), gbc);
        additionalChargesField = new JTextField("0");
        gbc.gridy = row++;
        form.add(additionalChargesField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Discount (₱)"), gbc);
        discountField = new JTextField("0");
        gbc.gridy = row++;
        form.add(discountField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Tax (₱)"), gbc);
        taxField = new JTextField("0");
        gbc.gridy = row++;
        form.add(taxField, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Payment Status"), gbc);
        paymentStatusCombo = new JComboBox<>(PaymentStatus.values());
        gbc.gridy = row++;
        form.add(paymentStatusCombo, gbc);

        gbc.gridy = row++;
        form.add(formLabel("Payment Method"), gbc);
        paymentMethodCombo = new JComboBox<>(PaymentMethod.values());
        gbc.gridy = row++;
        form.add(paymentMethodCombo, gbc);

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

    private void loadComboData() {
        try {
            List<Reservation> reservations = reservationService.getAllActiveReservations();
            for (Reservation r : reservations) {
                reservationCombo.addItem(r);
            }
            updateRoomChargesPreview();
        } catch (HotelException ex) {
            UIUtils.showError(this, "Failed to load reservations: " + ex.getMessage());
        }
    }

    private void updateRoomChargesPreview() {
        Reservation selected = (Reservation) reservationCombo.getSelectedItem();
        if (selected != null) {
            roomChargesValueLabel.setText(String.format("₱%,.2f", selected.getTotalAmount()));
        } else {
            roomChargesValueLabel.setText("₱0.00");
        }
    }

    private void populateFields(Billing billing) {
        for (int i = 0; i < reservationCombo.getItemCount(); i++) {
            Reservation r = reservationCombo.getItemAt(i);
            if (r.getReservationId() == billing.getReservationId()) {
                reservationCombo.setSelectedIndex(i);
                break;
            }
        }
        roomChargesValueLabel.setText(String.format("₱%,.2f", billing.getRoomCharges()));
        additionalChargesField.setText(billing.getAdditionalCharges().toPlainString());
        discountField.setText(billing.getDiscount().toPlainString());
        taxField.setText(billing.getTax().toPlainString());
        paymentStatusCombo.setSelectedItem(billing.getPaymentStatus());
        paymentMethodCombo.setSelectedItem(billing.getPaymentMethod());
    }

    private void handleSave() {
        try {
            Reservation selectedReservation = (Reservation) reservationCombo.getSelectedItem();
            Validator.requireNonNull(selectedReservation, "Reservation");

            BigDecimal additionalCharges = Validator.parseBigDecimal(additionalChargesField.getText(), "Additional charges");
            BigDecimal discount = Validator.parseBigDecimal(discountField.getText(), "Discount");
            BigDecimal tax = Validator.parseBigDecimal(taxField.getText(), "Tax");
            PaymentStatus paymentStatus = (PaymentStatus) paymentStatusCombo.getSelectedItem();
            PaymentMethod paymentMethod = (PaymentMethod) paymentMethodCombo.getSelectedItem();

            if (existingBilling == null) {
                billingService.addBilling(selectedReservation.getReservationId(), additionalCharges, discount, tax,
                        paymentStatus, paymentMethod);
                UIUtils.showSuccess(this, "Bill created successfully.");
            } else {
                billingService.updateBilling(existingBilling.getBillId(), additionalCharges, discount, tax,
                        paymentStatus, paymentMethod);
                UIUtils.showSuccess(this, "Bill updated successfully.");
            }
            saved = true;
            dispose();
        } catch (HotelException ex) {
            UIUtils.showError(this, ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
