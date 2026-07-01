package com.hotel.ui.customer;

import com.hotel.exception.HotelException;
import com.hotel.exception.ValidationException;
import com.hotel.model.Guest;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.service.ReservationService;
import com.hotel.service.RoomService;
import com.hotel.service.GuestService;
import com.hotel.service.BillingService;
import com.hotel.ui.common.PlaceholderTextField;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;
import com.hotel.ui.common.DatePickerField;
import com.hotel.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified customer screen for viewing, updating, and cancelling their reservations.
 * Consolidates all reservation-management capabilities in a single tab.
 */
public class ViewReservationPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();
    private final RoomService roomService = new RoomService();
    private final GuestService guestService = new GuestService();
    private final BillingService billingService = new BillingService();

    private ReadOnlyTableModel tableModel;
    private JTable table;

    private JButton updateButton;
    private JButton cancelButton;

    // Search and Filters
    private PlaceholderTextField searchField;
    private FilterMode activeFilter = FilterMode.ALL;
    private final List<JButton> filterButtons = new ArrayList<>();

    // Sorting
    private int currentSortColumn = 0;
    private boolean isAscending = true;

    // Pagination
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private int totalPages = 1;
    private JLabel pageLabel;
    private JButton prevPageButton;
    private JButton nextPageButton;

    // Details Panel Elements
    private JPanel detailsCard;
    private JLabel detailIdVal;
    private JLabel detailGuestVal;
    private JLabel detailPhoneVal;
    private JLabel detailRoomVal;
    private JLabel detailTypeVal;
    private JLabel detailGuestsVal;
    private JLabel detailInVal;
    private JLabel detailOutVal;
    private JLabel detailNightsVal;
    private JPanel detailStatusPanel;
    private JLabel detailPaymentVal;
    private JLabel detailTotalVal;

    // Master list of reservations for current customer
    private List<Reservation> allReservations = new ArrayList<>();
    private List<Reservation> currentFilteredList = new ArrayList<>();

    private static final String[] COLUMNS = {
            "Reservation ID", "Room", "Room Type", "Check-in", "Check-out", "Nights", "Guests", "Status", "Total Amount"
    };

    private enum FilterMode {
        ALL("All Reservations"),
        LATEST("Latest"),
        OLDEST("Oldest"),
        PENDING("Pending"),
        CONFIRMED("Confirmed"),
        CANCELLED("Cancelled"),
        UPCOMING("Upcoming"),
        COMPLETED("Completed");

        private final String displayName;

        FilterMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public ViewReservationPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Top Row: Section Title and Action Buttons
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UIUtils.createSectionTitle("My Reservations"), BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        updateButton = UIUtils.createStyledButton("Update Selected", UIUtils.ACCENT_COLOR);
        updateButton.setEnabled(false);
        updateButton.addActionListener(e -> handleUpdate());

        cancelButton = UIUtils.createStyledButton("Cancel Selected", UIUtils.DANGER_COLOR);
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> handleCancel());

        buttonsRow.add(updateButton);
        buttonsRow.add(cancelButton);
        headerRow.add(buttonsRow, BorderLayout.EAST);

        // Filters and Search Bar Container
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

        // Search Bar Row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        searchField = new PlaceholderTextField("Search by Reservation ID, Guest Name, or Room Number...");
        searchField.setPreferredSize(new Dimension(0, 36));
        searchField.setFont(UIUtils.FONT_REGULAR);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        
        // Search Label / Field wrapper
        JPanel searchWrapper = new JPanel(new BorderLayout(8, 0));
        searchWrapper.setOpaque(false);
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(UIUtils.FONT_BOLD);
        searchWrapper.add(searchLabel, BorderLayout.WEST);
        searchWrapper.add(searchField, BorderLayout.CENTER);
        controlsPanel.add(searchWrapper, gbc);

        // Filters Row
        gbc.gridy = 1;
        JPanel filtersWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filtersWrapper.setOpaque(false);

        for (FilterMode mode : FilterMode.values()) {
            JButton filterBtn = new JButton(mode.getDisplayName());
            filterBtn.setFont(UIUtils.FONT_REGULAR);
            filterBtn.setFocusPainted(false);
            filterBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            filterBtn.setPreferredSize(new Dimension(130, 28));
            filterBtn.addActionListener(e -> {
                setActiveFilter(mode);
                applyFilterAndSearch();
            });
            filterButtons.add(filterBtn);
            filtersWrapper.add(filterBtn);
        }
        updateFilterButtonStyles();
        controlsPanel.add(filtersWrapper, gbc);

        // North Panel wrapping title and controls
        JPanel northContainer = new JPanel(new BorderLayout(0, 12));
        northContainer.setOpaque(false);
        northContainer.add(headerRow, BorderLayout.NORTH);
        northContainer.add(controlsPanel, BorderLayout.CENTER);
        add(northContainer, BorderLayout.NORTH);

        // Workspace Split Pane (Table on Left, Details Card on Right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setDividerLocation(800);
        splitPane.setBorder(null);

        // Left Side Panel: Table + Pagination
        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setOpaque(false);

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
        
        // Custom Badge Renderer for Status Column (index 7)
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());

        // Header click listener for sorting
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col >= 0) {
                    if (currentSortColumn == col) {
                        isAscending = !isAscending;
                    } else {
                        currentSortColumn = col;
                        isAscending = true;
                    }
                    applyFilterAndSearch();
                }
            }
        });

        // Row Selection Listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailsAndButtonStates();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom Pagination Panel
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        paginationPanel.setOpaque(false);

        prevPageButton = new JButton("◀ Prev");
        prevPageButton.setFont(UIUtils.FONT_BOLD);
        prevPageButton.setPreferredSize(new Dimension(80, 28));
        prevPageButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderPage();
            }
        });

        pageLabel = new JLabel("Page 1 of 1");
        pageLabel.setFont(UIUtils.FONT_BOLD);

        nextPageButton = new JButton("Next ▶");
        nextPageButton.setFont(UIUtils.FONT_BOLD);
        nextPageButton.setPreferredSize(new Dimension(80, 28));
        nextPageButton.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                renderPage();
            }
        });

        paginationPanel.add(prevPageButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextPageButton);
        leftPanel.add(paginationPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // Right Side Panel: Details Card
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));

        detailsCard = new JPanel(new GridBagLayout());
        detailsCard.setBackground(Color.WHITE);
        detailsCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(210, 220, 235), 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        GridBagConstraints dcGbc = new GridBagConstraints();
        dcGbc.fill = GridBagConstraints.HORIZONTAL;
        dcGbc.insets = new Insets(8, 0, 8, 0);
        dcGbc.gridx = 0;
        int dcRow = 0;

        // Card Header Title
        JLabel cardTitle = new JLabel("Reservation Details", SwingConstants.CENTER);
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cardTitle.setForeground(UIUtils.PRIMARY_COLOR);
        dcGbc.gridy = dcRow++;
        dcGbc.gridwidth = 2;
        detailsCard.add(cardTitle, dcGbc);

        // Separator
        JSeparator sep = new JSeparator();
        dcGbc.gridy = dcRow++;
        detailsCard.add(sep, dcGbc);
        dcGbc.gridwidth = 1;

        // Group 1: Reservation Information
        addGroupHeader("Reservation Information", dcRow++);
        dcRow++; // spacer for separator
        detailIdVal = addDetailRow("Reservation ID:", dcRow++);
        detailGuestVal = addDetailRow("Guest Name:", dcRow++);
        detailPhoneVal = addDetailRow("Phone Number:", dcRow++);

        // Group 2: Room Information
        addGroupHeader("Room Information", dcRow++);
        dcRow++; // spacer for separator
        detailRoomVal = addDetailRow("Room Number:", dcRow++);
        detailTypeVal = addDetailRow("Room Type:", dcRow++);
        detailGuestsVal = addDetailRow("Number of Guests:", dcRow++);

        // Group 3: Stay Information
        addGroupHeader("Stay Information", dcRow++);
        dcRow++; // spacer for separator
        detailInVal = addDetailRow("Check-in Date:", dcRow++);
        detailOutVal = addDetailRow("Check-out Date:", dcRow++);
        detailNightsVal = addDetailRow("Number of Nights:", dcRow++);

        // Group 4: Payment Information
        addGroupHeader("Payment Information", dcRow++);
        dcRow++; // spacer for separator

        // Status Row
        dcGbc.gridy = dcRow++;
        dcGbc.gridx = 0;
        dcGbc.weightx = 0;
        dcGbc.anchor = GridBagConstraints.WEST;
        JLabel statusLbl = new JLabel("Reservation Status:");
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        detailsCard.add(statusLbl, dcGbc);

        dcGbc.gridx = 1;
        dcGbc.weightx = 1.0;
        dcGbc.anchor = GridBagConstraints.EAST;
        detailStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        detailStatusPanel.setOpaque(false);
        detailsCard.add(detailStatusPanel, dcGbc);

        detailPaymentVal = addDetailRow("Payment Status:", dcRow++);

        // Total Amount Row (larger and bold)
        dcGbc.gridy = dcRow++;
        dcGbc.gridx = 0;
        dcGbc.weightx = 0;
        dcGbc.anchor = GridBagConstraints.WEST;
        JLabel totalLbl = new JLabel("Total Amount:");
        totalLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        detailsCard.add(totalLbl, dcGbc);

        dcGbc.gridx = 1;
        dcGbc.weightx = 1.0;
        dcGbc.anchor = GridBagConstraints.EAST;
        detailTotalVal = new JLabel("-");
        detailTotalVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        detailTotalVal.setForeground(UIUtils.PRIMARY_COLOR);
        detailsCard.add(detailTotalVal, dcGbc);

        rightPanel.add(detailsCard, BorderLayout.NORTH);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        // Hook up search listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilterAndSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilterAndSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilterAndSearch(); }
        });

        // Initialize state
        clearDetailsCard();
    }

    private JLabel addDetailRow(String labelText, int rowNum) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = rowNum;
        gbc.insets = new Insets(6, 0, 6, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        detailsCard.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel val = new JLabel("-");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        detailsCard.add(val, gbc);

        return val;
    }

    private void addGroupHeader(String title, int rowNum) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = rowNum;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(16, 0, 4, 0);

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(UIUtils.ACCENT_COLOR);
        detailsCard.add(lbl, gbc);

        gbc.gridy = rowNum + 1;
        JSeparator groupSep = new JSeparator();
        groupSep.setForeground(new Color(230, 235, 245));
        detailsCard.add(groupSep, gbc);
    }

    private void setActiveFilter(FilterMode mode) {
        this.activeFilter = mode;
        updateFilterButtonStyles();
    }

    private void updateFilterButtonStyles() {
        for (int i = 0; i < FilterMode.values().length; i++) {
            FilterMode mode = FilterMode.values()[i];
            JButton btn = filterButtons.get(i);
            if (mode == activeFilter) {
                btn.setBackground(UIUtils.PRIMARY_COLOR);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(UIUtils.PRIMARY_COLOR));
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.DARK_GRAY);
                btn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            }
        }
    }

    public void refresh() {
        UIUtils.runSafely(this, () -> {
            Guest guest = CustomerContext.getOrCreateCurrentGuest();
            allReservations = reservationService.getReservationsForGuest(guest.getGuestId());
            applyFilterAndSearch();
        });
    }

    private void applyFilterAndSearch() {
        String query = searchField.getText().trim().toLowerCase();
        List<Reservation> filtered = new ArrayList<>();

        // 1. Filter by Active Filter
        for (Reservation r : allReservations) {
            boolean keep = true;

            // Status filters
            if (activeFilter == FilterMode.PENDING && r.getStatus() != ReservationStatus.PENDING) keep = false;
            else if (activeFilter == FilterMode.CONFIRMED && r.getStatus() != ReservationStatus.CONFIRMED) keep = false;
            else if (activeFilter == FilterMode.CANCELLED && r.getStatus() != ReservationStatus.CANCELLED) keep = false;
            
            // Time filters
            else if (activeFilter == FilterMode.UPCOMING) {
                if (r.getStatus() == ReservationStatus.CANCELLED || r.getCheckInDate().isBefore(LocalDate.now())) {
                    keep = false;
                }
            } else if (activeFilter == FilterMode.COMPLETED) {
                if (r.getStatus() != ReservationStatus.CHECKED_OUT && !r.getCheckOutDate().isBefore(LocalDate.now())) {
                    keep = false;
                }
            }

            if (keep) {
                filtered.add(r);
            }
        }

        // 2. Filter by Search Query
        if (!query.isEmpty()) {
            List<Reservation> searchMatches = new ArrayList<>();
            for (Reservation r : filtered) {
                boolean match = String.valueOf(r.getReservationId()).contains(query)
                        || String.valueOf(r.getRoomNumber()).toLowerCase().contains(query)
                        || String.valueOf(r.getRoomType()).toLowerCase().contains(query);
                if (match) {
                    searchMatches.add(r);
                }
            }
            filtered = searchMatches;
        }

        // 3. Sort List
        sortList(filtered);

        this.currentFilteredList = filtered;
        
        // Calculate pagination properties
        totalPages = (int) Math.ceil((double) currentFilteredList.size() / PAGE_SIZE);
        if (totalPages <= 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        renderPage();
    }

    private void sortList(List<Reservation> list) {
        // Handle filter modes "LATEST" and "OLDEST" which override manual sort column
        if (activeFilter == FilterMode.LATEST) {
            list.sort((r1, r2) -> r2.getCheckInDate().compareTo(r1.getCheckInDate()));
            return;
        } else if (activeFilter == FilterMode.OLDEST) {
            list.sort((r1, r2) -> r1.getCheckInDate().compareTo(r2.getCheckInDate()));
            return;
        }

        if (currentSortColumn < 0) return;

        list.sort((r1, r2) -> {
            int cmp = 0;
            switch (currentSortColumn) {
                case 0:
                    cmp = Integer.compare(r1.getReservationId(), r2.getReservationId());
                    break;
                case 1:
                    cmp = r1.getRoomNumber().compareToIgnoreCase(r2.getRoomNumber());
                    break;
                case 2:
                    cmp = String.valueOf(r1.getRoomType()).compareToIgnoreCase(String.valueOf(r2.getRoomType()));
                    break;
                case 3:
                    cmp = r1.getCheckInDate().compareTo(r2.getCheckInDate());
                    break;
                case 4:
                    cmp = r1.getCheckOutDate().compareTo(r2.getCheckOutDate());
                    break;
                case 5:
                    cmp = Long.compare(r1.getNumberOfNights(), r2.getNumberOfNights());
                    break;
                case 6:
                    cmp = Integer.compare(r1.getNumGuests(), r2.getNumGuests());
                    break;
                case 7:
                    cmp = String.valueOf(r1.getStatus()).compareToIgnoreCase(String.valueOf(r2.getStatus()));
                    break;
                case 8:
                    cmp = r1.getTotalAmount().compareTo(r2.getTotalAmount());
                    break;
            }
            return isAscending ? cmp : -cmp;
        });
    }

    private void renderPage() {
        tableModel.setRowCount(0);
        
        int startIdx = (currentPage - 1) * PAGE_SIZE;
        int endIdx = Math.min(startIdx + PAGE_SIZE, currentFilteredList.size());

        for (int i = startIdx; i < endIdx; i++) {
            Reservation r = currentFilteredList.get(i);
            tableModel.addRow(new Object[]{
                    r.getReservationId(),
                    r.getRoomNumber() == null ? "-" : r.getRoomNumber(),
                    r.getRoomType(),
                    r.getCheckInDate().format(DATE_FORMAT),
                    r.getCheckOutDate().format(DATE_FORMAT),
                    r.getNumberOfNights(),
                    r.getNumGuests(),
                    r.getStatus(),
                    String.format("₱%,.2f", r.getTotalAmount())
            });
        }

        pageLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);

        UIUtils.formatTableColumns(table);
        updateDetailsAndButtonStates();
    }

    private void updateDetailsAndButtonStates() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            clearDetailsCard();
            updateButton.setEnabled(false);
            cancelButton.setEnabled(false);
            return;
        }

        // Get reservation object from selected row
        int modelRowIndex = (currentPage - 1) * PAGE_SIZE + selectedViewRow;
        if (modelRowIndex >= currentFilteredList.size()) {
            clearDetailsCard();
            updateButton.setEnabled(false);
            cancelButton.setEnabled(false);
            return;
        }

        Reservation reservation = currentFilteredList.get(modelRowIndex);
        populateDetailsCard(reservation);

        // Enable/Disable buttons based on reservation status
        ReservationStatus status = reservation.getStatus();
        if (status == ReservationStatus.CANCELLED) {
            updateButton.setEnabled(false);
            cancelButton.setEnabled(false);
        } else if (status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED) {
            updateButton.setEnabled(true);
            cancelButton.setEnabled(true);
        } else {
            // CHECKED_IN or CHECKED_OUT
            updateButton.setEnabled(false);
            cancelButton.setEnabled(false);
        }
    }

    private void populateDetailsCard(Reservation r) {
        detailIdVal.setText("#" + r.getReservationId());
        detailGuestVal.setText(r.getGuestName() != null ? r.getGuestName() : "-");
        
        UIUtils.runSafely(this, () -> {
            try {
                com.hotel.model.Guest g = guestService.getGuestOrThrow(r.getGuestId());
                detailPhoneVal.setText(g.getPhone() != null ? g.getPhone() : "-");
            } catch (Exception e) {
                detailPhoneVal.setText("-");
            }
        });

        detailRoomVal.setText(r.getRoomNumber());
        detailTypeVal.setText(String.valueOf(r.getRoomType()));
        detailGuestsVal.setText(String.valueOf(r.getNumGuests()));
        detailInVal.setText(r.getCheckInDate().format(DATE_FORMAT));
        detailOutVal.setText(r.getCheckOutDate().format(DATE_FORMAT));
        detailNightsVal.setText(String.valueOf(r.getNumberOfNights()));

        detailStatusPanel.removeAll();
        detailStatusPanel.add(UIUtils.createStatusBadge(r.getStatus().name()));
        detailStatusPanel.revalidate();
        detailStatusPanel.repaint();

        UIUtils.runSafely(this, () -> {
            billingService.findByReservationId(r.getReservationId()).ifPresentOrElse(
                b -> detailPaymentVal.setText(b.getBillStatus().getDisplayName()),
                () -> detailPaymentVal.setText("UNPAID")
            );
        });

        detailTotalVal.setText(String.format("₱%,.2f", r.getTotalAmount()));
    }

    private void clearDetailsCard() {
        detailIdVal.setText("-");
        detailGuestVal.setText("-");
        detailPhoneVal.setText("-");
        detailRoomVal.setText("-");
        detailTypeVal.setText("-");
        detailGuestsVal.setText("-");
        detailInVal.setText("-");
        detailOutVal.setText("-");
        detailNightsVal.setText("-");
        
        detailStatusPanel.removeAll();
        JLabel noStatus = new JLabel("-");
        noStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        detailStatusPanel.add(noStatus);
        detailStatusPanel.revalidate();
        detailStatusPanel.repaint();

        detailPaymentVal.setText("-");
        detailTotalVal.setText("-");
    }

    private void handleUpdate() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            UIUtils.showInfo(this, "Please select a reservation to update.");
            return;
        }

        int modelRowIndex = (currentPage - 1) * PAGE_SIZE + selectedViewRow;
        if (modelRowIndex >= currentFilteredList.size()) return;

        Reservation reservation = currentFilteredList.get(modelRowIndex);

        UIUtils.runSafely(this, () -> {
            Reservation latestReservation = reservationService.getReservationOrThrow(reservation.getReservationId());

            if (latestReservation.getStatus() == ReservationStatus.CANCELLED
                    || latestReservation.getStatus() == ReservationStatus.CHECKED_OUT) {
                UIUtils.showError(this, "This reservation can no longer be updated (status: " + latestReservation.getStatus() + ").");
                return;
            }

            showUpdateDialog(latestReservation);
        });
    }

    private void showUpdateDialog(Reservation reservation) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Update Reservation #" + reservation.getReservationId(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 680);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        int row = 0;

        // Room Type Selection
        gbc.gridy = row++;
        form.add(boldLabel("Room Type"), gbc);
        JComboBox<RoomType> roomTypeCombo = new JComboBox<>(RoomType.values());
        roomTypeCombo.setSelectedItem(reservation.getRoomType());
        roomTypeCombo.setPreferredSize(new Dimension(0, 36));
        roomTypeCombo.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(roomTypeCombo, gbc);

        // Room Details Panel inside Edit Dialog
        gbc.gridy = row++;
        form.add(boldLabel("Room Type Details"), gbc);
        
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        GridBagConstraints dGbc = new GridBagConstraints();
        dGbc.fill = GridBagConstraints.HORIZONTAL;
        dGbc.weightx = 1.0;
        dGbc.gridx = 0;
        dGbc.gridy = 0;
        
        JLabel priceLbl = new JLabel("Price per Night: ₱0.00");
        priceLbl.setFont(UIUtils.FONT_BOLD);
        priceLbl.setForeground(UIUtils.PRIMARY_COLOR);
        detailsPanel.add(priceLbl, dGbc);
        
        JLabel capacityLbl = new JLabel("Max Guest Capacity: 0");
        capacityLbl.setFont(UIUtils.FONT_BOLD);
        capacityLbl.setForeground(Color.DARK_GRAY);
        dGbc.gridy = 1;
        dGbc.insets = new Insets(4, 0, 0, 0);
        detailsPanel.add(capacityLbl, dGbc);
        
        JTextArea descArea = new JTextArea(2, 20);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setFont(UIUtils.FONT_REGULAR);
        descArea.setBackground(Color.WHITE);
        descArea.setForeground(Color.GRAY);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(null);
        dGbc.gridy = 2;
        dGbc.insets = new Insets(4, 0, 0, 0);
        detailsPanel.add(descScroll, dGbc);
        
        gbc.gridy = row++;
        form.add(detailsPanel, gbc);

        // Date Pickers
        gbc.gridy = row++;
        form.add(boldLabel("Check-in Date"), gbc);
        DatePickerField checkInPicker = new DatePickerField();
        LocalDate initialCheckIn = reservation.getCheckInDate();
        checkInPicker.setDateValidator(date -> !date.isBefore(LocalDate.now()) || date.equals(initialCheckIn));
        gbc.gridy = row++;
        form.add(checkInPicker, gbc);

        gbc.gridy = row++;
        form.add(boldLabel("Check-out Date"), gbc);
        DatePickerField checkOutPicker = new DatePickerField();
        LocalDate initialCheckOut = reservation.getCheckOutDate();
        checkOutPicker.setDateValidator(date -> !date.isBefore(LocalDate.now()) || date.equals(initialCheckOut));
        gbc.gridy = row++;
        form.add(checkOutPicker, gbc);

        // Read-only Nights Field
        gbc.gridy = row++;
        form.add(boldLabel("Number of Nights (Calculated)"), gbc);
        JTextField nightsField = new JTextField();
        nightsField.setEditable(false);
        nightsField.setBackground(new Color(240, 240, 240));
        nightsField.setPreferredSize(new Dimension(0, 36));
        nightsField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(nightsField, gbc);

        // Read-only Total Field
        gbc.gridy = row++;
        form.add(boldLabel("Total Amount (Calculated)"), gbc);
        JTextField totalAmountField = new JTextField();
        totalAmountField.setEditable(false);
        totalAmountField.setBackground(new Color(240, 240, 240));
        totalAmountField.setPreferredSize(new Dimension(0, 36));
        totalAmountField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(totalAmountField, gbc);

        // Guests Field
        gbc.gridy = row++;
        form.add(boldLabel("Number of Guests"), gbc);
        JTextField numGuestsField = new JTextField(String.valueOf(reservation.getNumGuests()));
        numGuestsField.setPreferredSize(new Dimension(0, 36));
        numGuestsField.setFont(UIUtils.FONT_REGULAR);
        gbc.gridy = row++;
        form.add(numGuestsField, gbc);

        // Update fields initially
        checkInPicker.setDate(reservation.getCheckInDate());
        checkOutPicker.setDate(reservation.getCheckOutDate());

        // Calculation helper
        Runnable updateCalcs = () -> {
            RoomType selectedType = (RoomType) roomTypeCombo.getSelectedItem();
            LocalDate inDate = checkInPicker.getDate();
            LocalDate outDate = checkOutPicker.getDate();
            if (selectedType != null) {
                int capacity = reservationService.getCapacityForType(selectedType);
                java.math.BigDecimal price = reservationService.getPriceForType(selectedType);
                capacityLbl.setText("Max Guest Capacity: " + capacity);
                priceLbl.setText(String.format("Price per Night: ₱%,.2f", price));
                
                String desc = roomService.getAllActiveRooms().stream()
                        .filter(rm -> rm.getRoomType() == selectedType)
                        .map(Room::getDescription)
                        .filter(d -> d != null && !d.trim().isEmpty())
                        .findFirst()
                        .orElseGet(() -> {
                            switch (selectedType) {
                                case SINGLE: return "Cozy single room, perfect for solo travelers.";
                                case DOUBLE: return "Comfortable double room for two guests.";
                                case DELUXE: return "Deluxe room offering premium comfort and amenities.";
                                case SUITE: return "Luxurious executive suite with premium services.";
                                default: return "";
                            }
                        });
                descArea.setText(desc);

                if (inDate != null && outDate != null) {
                    long nights = outDate.toEpochDay() - inDate.toEpochDay();
                    if (nights > 0) {
                        nightsField.setText(String.valueOf(nights));
                        java.math.BigDecimal total = price.multiply(java.math.BigDecimal.valueOf(nights));
                        totalAmountField.setText(String.format("₱%,.2f", total));
                        return;
                    }
                }
            }
            nightsField.setText("");
            totalAmountField.setText("₱0.00");
        };

        // Hook up listeners for calculations
        roomTypeCombo.addActionListener(e -> updateCalcs.run());
        
        checkInPicker.addChangeListener(() -> {
            LocalDate inDate = checkInPicker.getDate();
            if (inDate != null) {
                checkOutPicker.setEnabled(true);
                checkOutPicker.setDateValidator(date -> (!date.isBefore(LocalDate.now()) && date.isAfter(inDate)) || date.equals(initialCheckOut));
                LocalDate outDate = checkOutPicker.getDate();
                if (outDate != null && !outDate.isAfter(inDate) && !outDate.equals(initialCheckOut)) {
                    checkOutPicker.setDate(null);
                }
            } else {
                checkOutPicker.setDate(null);
                checkOutPicker.setEnabled(false);
            }
            updateCalcs.run();
        });

        checkOutPicker.addChangeListener(updateCalcs);

        // Trigger first calculation check
        updateCalcs.run();

        // Lock fields if reservation is already confirmed (keep selection and dates locked)
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            checkInPicker.setEnabled(false);
            checkOutPicker.setEnabled(false);
            roomTypeCombo.setEnabled(false);
        }

        dialog.add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        JButton saveBtn = UIUtils.createStyledButton("Save Changes", UIUtils.SUCCESS_COLOR);
        saveBtn.addActionListener(e -> {
            try {
                RoomType selectedType = (RoomType) roomTypeCombo.getSelectedItem();
                Validator.requireNonNull(selectedType, "Room Type");

                LocalDate checkIn = checkInPicker.getDate();
                if (checkIn == null) {
                    throw new ValidationException("Please select a check-in date.");
                }
                LocalDate checkOut = checkOutPicker.getDate();
                if (checkOut == null) {
                    throw new ValidationException("Please select a check-out date.");
                }

                // Date ordering validation
                if (!checkOut.isAfter(checkIn)) {
                    throw new ValidationException("Check-out date must be after the check-in date.");
                }

                // Limit stays to 30 nights
                long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
                if (nights > 30) {
                    throw new ValidationException("Stays cannot exceed 30 nights.");
                }

                // Guest count validations
                int numGuests = Validator.parseInt(numGuestsField.getText(), "Number of guests");
                if (numGuests <= 0) {
                    throw new ValidationException("Number of guests must be greater than zero.");
                }
                int capacity = reservationService.getCapacityForType(selectedType);
                if (numGuests > capacity) {
                    throw new ValidationException("Number of guests exceeds the room type's maximum capacity of " + capacity + ".");
                }

                reservationService.updateReservation(reservation.getReservationId(), selectedType, 0,
                        checkIn, checkOut, numGuests, reservation.getStatus(), reservation.getNotes());

                UIUtils.showSuccess(dialog, "Reservation updated successfully.");
                dialog.dispose();
                refresh();
            } catch (HotelException ex) {
                UIUtils.showError(dialog, ex.getMessage());
            }
        });
        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void handleCancel() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            UIUtils.showInfo(this, "Please select a reservation to cancel.");
            return;
        }

        int modelRowIndex = (currentPage - 1) * PAGE_SIZE + selectedViewRow;
        if (modelRowIndex >= currentFilteredList.size()) return;

        Reservation reservation = currentFilteredList.get(modelRowIndex);
        int reservationId = reservation.getReservationId();
        ReservationStatus currentStatus = reservation.getStatus();

        if (currentStatus == ReservationStatus.CANCELLED) {
            UIUtils.showInfo(this, "This reservation is already cancelled.");
            return;
        }
        if (currentStatus == ReservationStatus.CHECKED_OUT) {
            UIUtils.showInfo(this, "A checked-out reservation cannot be cancelled.");
            return;
        }

        boolean confirmed = UIUtils.confirm(this,
                "Are you sure you want to cancel reservation #" + reservationId + "?", "Confirm Cancellation");
        if (confirmed) {
            UIUtils.runSafely(this, () -> {
                reservationService.cancelReservation(reservationId);
                UIUtils.showSuccess(this, "Reservation cancelled successfully.");
                refresh();
            });
        }
    }

    private JLabel boldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIUtils.FONT_BOLD);
        return label;
    }

    /**
     * Custom TableCellRenderer to display status as nice color badges.
     */
    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value != null) {
                String statusStr = value.toString();
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(UIUtils.FONT_BOLD);
                
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                } else {
                    if (statusStr.equalsIgnoreCase("PENDING")) {
                        setBackground(new Color(255, 243, 205)); // Light yellow
                        setForeground(new Color(133, 100, 4));    // Dark yellow/brown
                    } else if (statusStr.equalsIgnoreCase("CONFIRMED") || statusStr.equalsIgnoreCase("CHECKED_IN")) {
                        setBackground(new Color(212, 239, 223)); // Light green
                        setForeground(new Color(21, 87, 36));     // Dark green
                    } else if (statusStr.equalsIgnoreCase("CANCELLED")) {
                        setBackground(new Color(248, 215, 218)); // Light red
                        setForeground(new Color(114, 28, 36));    // Dark red
                    } else {
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                }
            }
            return c;
        }
    }
}
