package com.hotel.ui.admin;

import com.hotel.exception.HotelException;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.service.ReservationService;
import com.hotel.service.GuestService;
import com.hotel.service.BillingService;
import com.hotel.ui.common.PlaceholderTextField;
import com.hotel.ui.common.ReadOnlyTableModel;
import com.hotel.ui.common.UIUtils;
import com.hotel.ui.common.DatePickerField;

import javax.swing.*;
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
 * Admin screen for managing reservations: add, edit, soft-delete (archive),
 * view archived reservations (with restore option), and refresh. The table
 * shows full joined details (guest name, room number/type) for every
 * reservation in the database.
 */
public class ReservationManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService = new ReservationService();
    private final GuestService guestService = new GuestService();
    private final BillingService billingService = new BillingService();

    private ReadOnlyTableModel tableModel;
    private JTable table;
    
    private JButton addButton;
    private JButton assignRoomButton;
    private JButton editButton;
    private JButton deleteButton; // Label changes to "Restore" dynamically when viewing archived
    private JButton toggleArchiveButton;
    private JLabel titleLabel;

    // Search and Filters
    private PlaceholderTextField searchField;
    private FilterMode activeFilter = FilterMode.ALL;
    private final List<JButton> filterButtons = new ArrayList<>();
    private boolean viewingArchived = false;

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

    // Master lists
    private List<Reservation> allReservations = new ArrayList<>();
    private List<Reservation> currentFilteredList = new ArrayList<>();

    private static final String[] COLUMNS = {
            "ID", "Guest", "Room", "Room Type", "Check-in", "Check-out", "Nights", "Guests", "Status", "Total Amount"
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

    public ReservationManagementPanel() {
        initComponents();
        loadReservations();
    }

    private void initComponents() {
        setLayout(new BorderLayout(16, 16));
        setBackground(UIUtils.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Top Row: Title & Action Buttons
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        titleLabel = UIUtils.createSectionTitle("Reservation Management");
        headerRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsRow.setOpaque(false);

        addButton = UIUtils.createStyledButton("Add", UIUtils.SUCCESS_COLOR);
        addButton.addActionListener(e -> handleAdd());

        assignRoomButton = UIUtils.createStyledButton("Assign Room", UIUtils.PRIMARY_COLOR);
        assignRoomButton.setEnabled(false);
        assignRoomButton.addActionListener(e -> handleAssignRoom());

        editButton = UIUtils.createStyledButton("Edit", UIUtils.ACCENT_COLOR);
        editButton.setEnabled(false);
        editButton.addActionListener(e -> handleEdit());

        deleteButton = UIUtils.createStyledButton("Delete", UIUtils.DANGER_COLOR);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> handleDelete());

        toggleArchiveButton = UIUtils.createStyledButton("View Archived", UIUtils.PRIMARY_COLOR);
        toggleArchiveButton.addActionListener(e -> toggleArchivedView());

        buttonsRow.add(addButton);
        buttonsRow.add(assignRoomButton);
        buttonsRow.add(editButton);
        buttonsRow.add(deleteButton);
        buttonsRow.add(toggleArchiveButton);
        headerRow.add(buttonsRow, BorderLayout.EAST);

        // Filters and Search Container
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

        // Search Field
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

        JPanel searchWrapper = new JPanel(new BorderLayout(8, 0));
        searchWrapper.setOpaque(false);
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(UIUtils.FONT_BOLD);
        searchWrapper.add(searchLabel, BorderLayout.WEST);
        searchWrapper.add(searchField, BorderLayout.CENTER);
        controlsPanel.add(searchWrapper, gbc);

        // Filter Buttons
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

        JPanel northContainer = new JPanel(new BorderLayout(0, 12));
        northContainer.setOpaque(false);
        northContainer.add(headerRow, BorderLayout.NORTH);
        northContainer.add(controlsPanel, BorderLayout.CENTER);
        add(northContainer, BorderLayout.NORTH);

        // Main workspace split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setDividerLocation(800);
        splitPane.setBorder(null);

        // Table Panel (Left)
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
        table.getColumnModel().getColumn(8).setCellRenderer(new StatusBadgeRenderer());

        // Header clicks for sorting
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

        // Row selection
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailsAndButtonStates();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Pagination Panel
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

        // Details Panel (Right)
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

        JLabel cardTitle = new JLabel("Reservation Details", SwingConstants.CENTER);
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cardTitle.setForeground(UIUtils.PRIMARY_COLOR);
        dcGbc.gridy = dcRow++;
        dcGbc.gridwidth = 2;
        detailsCard.add(cardTitle, dcGbc);

        JSeparator sep = new JSeparator();
        dcGbc.gridy = dcRow++;
        detailsCard.add(sep, dcGbc);
        dcGbc.gridwidth = 1;

        // Group 1: Reservation Information
        addGroupHeader("Reservation Information", dcRow++);
        dcRow++; // spacer for line
        detailIdVal = addDetailRow("Reservation ID:", dcRow++);
        detailGuestVal = addDetailRow("Guest Name:", dcRow++);
        detailPhoneVal = addDetailRow("Phone Number:", dcRow++);

        // Group 2: Room Information
        addGroupHeader("Room Information", dcRow++);
        dcRow++; // spacer for line
        detailRoomVal = addDetailRow("Room Number:", dcRow++);
        detailTypeVal = addDetailRow("Room Type:", dcRow++);
        detailGuestsVal = addDetailRow("Number of Guests:", dcRow++);

        // Group 3: Stay Information
        addGroupHeader("Stay Information", dcRow++);
        dcRow++; // spacer for line
        detailInVal = addDetailRow("Check-in Date:", dcRow++);
        detailOutVal = addDetailRow("Check-out Date:", dcRow++);
        detailNightsVal = addDetailRow("Number of Nights:", dcRow++);

        // Group 4: Payment Information
        addGroupHeader("Payment Information", dcRow++);
        dcRow++; // spacer for line
        
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

        // Document Listener for Search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilterAndSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilterAndSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilterAndSearch(); }
        });

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

    private void loadReservations() {
        UIUtils.runSafely(this, () -> {
            if (viewingArchived) {
                allReservations = reservationService.getAllArchivedReservations();
            } else {
                allReservations = reservationService.getAllActiveReservations();
            }
            applyFilterAndSearch();
        });
    }

    private void applyFilterAndSearch() {
        String query = searchField.getText().trim().toLowerCase();
        List<Reservation> filtered = new ArrayList<>();

        for (Reservation r : allReservations) {
            boolean keep = true;

            // Status filter
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

        // Search query filter
        if (!query.isEmpty()) {
            List<Reservation> searchMatches = new ArrayList<>();
            for (Reservation r : filtered) {
                boolean match = String.valueOf(r.getReservationId()).contains(query)
                        || (r.getGuestName() != null && r.getGuestName().toLowerCase().contains(query))
                        || String.valueOf(r.getRoomNumber()).toLowerCase().contains(query)
                        || String.valueOf(r.getRoomType()).toLowerCase().contains(query);
                if (match) {
                    searchMatches.add(r);
                }
            }
            filtered = searchMatches;
        }

        // Sort list
        sortList(filtered);

        this.currentFilteredList = filtered;

        totalPages = (int) Math.ceil((double) currentFilteredList.size() / PAGE_SIZE);
        if (totalPages <= 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        renderPage();
    }

    private void sortList(List<Reservation> list) {
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
                    cmp = (r1.getGuestName() != null ? r1.getGuestName() : "").compareToIgnoreCase(r2.getGuestName() != null ? r2.getGuestName() : "");
                    break;
                case 2:
                    cmp = r1.getRoomNumber().compareToIgnoreCase(r2.getRoomNumber());
                    break;
                case 3:
                    cmp = String.valueOf(r1.getRoomType()).compareToIgnoreCase(String.valueOf(r2.getRoomType()));
                    break;
                case 4:
                    cmp = r1.getCheckInDate().compareTo(r2.getCheckInDate());
                    break;
                case 5:
                    cmp = r1.getCheckOutDate().compareTo(r2.getCheckOutDate());
                    break;
                case 6:
                    cmp = Long.compare(r1.getNumberOfNights(), r2.getNumberOfNights());
                    break;
                case 7:
                    cmp = Integer.compare(r1.getNumGuests(), r2.getNumGuests());
                    break;
                case 8:
                    cmp = String.valueOf(r1.getStatus()).compareToIgnoreCase(String.valueOf(r2.getStatus()));
                    break;
                case 9:
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
                    r.getGuestName(),
                    r.getRoomNumber(),
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
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            assignRoomButton.setEnabled(false);
            return;
        }

        int modelRowIndex = (currentPage - 1) * PAGE_SIZE + selectedViewRow;
        if (modelRowIndex >= currentFilteredList.size()) {
            clearDetailsCard();
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            assignRoomButton.setEnabled(false);
            return;
        }

        Reservation reservation = currentFilteredList.get(modelRowIndex);
        populateDetailsCard(reservation);

        if (viewingArchived) {
            editButton.setEnabled(false); // Archived cannot be edited
            deleteButton.setEnabled(true); // Can always restore
            assignRoomButton.setEnabled(false);
        } else {
            // Active reservation view rules
            ReservationStatus status = reservation.getStatus();
            deleteButton.setEnabled(true); // Admin can archive active bookings

            if (status == ReservationStatus.CANCELLED || status == ReservationStatus.CHECKED_OUT) {
                editButton.setEnabled(false);
            } else {
                editButton.setEnabled(true);
            }
            
            assignRoomButton.setEnabled(status == ReservationStatus.PENDING);
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
                b -> detailPaymentVal.setText(b.getPaymentStatus().name().replace("_", " ")),
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

    private void toggleArchivedView() {
        viewingArchived = !viewingArchived;
        currentPage = 1;
        
        if (viewingArchived) {
            titleLabel.setText("Reservation Management — Archived");
            toggleArchiveButton.setText("View Active");
            deleteButton.setText("Restore");
            deleteButton.setBackground(UIUtils.SUCCESS_COLOR);
        } else {
            titleLabel.setText("Reservation Management");
            toggleArchiveButton.setText("View Archived");
            deleteButton.setText("Delete");
            deleteButton.setBackground(UIUtils.DANGER_COLOR);
        }
        
        loadReservations();
    }

    public void refreshCurrentView() {
        loadReservations();
    }

    private void handleAssignRoom() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            UIUtils.showInfo(this, "Please select a reservation to assign a room.");
            return;
        }

        int modelRowIndex = (currentPage - 1) * PAGE_SIZE + selectedViewRow;
        if (modelRowIndex >= currentFilteredList.size()) return;

        Reservation reservation = currentFilteredList.get(modelRowIndex);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            UIUtils.showError(this, "Only PENDING reservations can have a room assigned.");
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Assign Room - Reservation #" + reservation.getReservationId(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0;
        int rowIdx = 0;

        panel.add(new JLabel("Guest: " + reservation.getGuestName()), gbc);
        gbc.gridy = ++rowIdx;
        panel.add(new JLabel("Requested Room Type: " + reservation.getRoomType()), gbc);
        gbc.gridy = ++rowIdx;
        panel.add(new JLabel("Dates: " + reservation.getCheckInDate() + " to " + reservation.getCheckOutDate()), gbc);
        gbc.gridy = ++rowIdx;
        panel.add(new JLabel("Guests: " + reservation.getNumGuests()), gbc);
        
        gbc.gridy = ++rowIdx;
        panel.add(new JLabel("Select Available Room:"), gbc);
        
        JComboBox<com.hotel.model.Room> availableRoomsCombo = new JComboBox<>();
        List<com.hotel.model.Room> availableRooms = reservationService.getAvailableRoomsOfType(
                reservation.getRoomType(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getReservationId()
        );
        for (com.hotel.model.Room r : availableRooms) {
            availableRoomsCombo.addItem(r);
        }
        gbc.gridy = ++rowIdx;
        panel.add(availableRoomsCombo, gbc);

        if (availableRooms.isEmpty()) {
            JLabel noRoomsLabel = new JLabel("No available rooms for these dates.");
            noRoomsLabel.setForeground(Color.RED);
            gbc.gridy = ++rowIdx;
            panel.add(noRoomsLabel, gbc);
        }

        dialog.add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(ev -> dialog.dispose());
        
        JButton confirmBtn = UIUtils.createStyledButton("Confirm & Assign", UIUtils.SUCCESS_COLOR);
        confirmBtn.setEnabled(!availableRooms.isEmpty());
        confirmBtn.addActionListener(ev -> {
            com.hotel.model.Room selectedRoom = (com.hotel.model.Room) availableRoomsCombo.getSelectedItem();
            if (selectedRoom != null) {
                try {
                    reservationService.assignRoomAndConfirm(reservation.getReservationId(), selectedRoom.getRoomId());
                    UIUtils.showSuccess(dialog, "Room assigned and reservation confirmed successfully!");
                    dialog.dispose();
                    loadReservations();
                } catch (Exception ex) {
                    UIUtils.showError(dialog, ex.getMessage());
                }
            }
        });
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(confirmBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void handleAdd() {
        ReservationFormDialog dialog = new ReservationFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadReservations();
            if (dialog.getSavedStatus() == ReservationStatus.CHECKED_OUT) {
                Window win = SwingUtilities.getWindowAncestor(this);
                if (win instanceof AdminMainFrame) {
                    List<Reservation> activeRes = reservationService.getAllActiveReservations();
                    int latestResId = activeRes.stream().mapToInt(Reservation::getReservationId).max().orElse(-1);
                    if (latestResId > 0) {
                        ((AdminMainFrame) win).navigateToBillingAndShow(latestResId);
                    }
                }
            }
        }
    }

    private void handleEdit() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            UIUtils.showInfo(this, "Please select a reservation to edit.");
            return;
        }

        int modelRowIndex = (currentPage - 1) * PAGE_SIZE + selectedViewRow;
        if (modelRowIndex >= currentFilteredList.size()) return;

        Reservation reservation = currentFilteredList.get(modelRowIndex);

        if (viewingArchived) {
            UIUtils.showInfo(this, "Archived reservations cannot be edited. Restore the reservation first.");
            return;
        }

        UIUtils.runSafely(this, () -> {
            Reservation latestReservation = reservationService.getReservationOrThrow(reservation.getReservationId());
            ReservationFormDialog dialog = new ReservationFormDialog(SwingUtilities.getWindowAncestor(this), latestReservation);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                loadReservations();
                if (dialog.getSavedStatus() == ReservationStatus.CHECKED_OUT) {
                    Window win = SwingUtilities.getWindowAncestor(this);
                    if (win instanceof AdminMainFrame) {
                        ((AdminMainFrame) win).navigateToBillingAndShow(reservation.getReservationId());
                    }
                }
            }
        });
    }

    private void handleDelete() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            UIUtils.showInfo(this, "Please select a reservation to process.");
            return;
        }

        int modelRowIndex = (currentPage - 1) * PAGE_SIZE + selectedViewRow;
        if (modelRowIndex >= currentFilteredList.size()) return;

        Reservation reservation = currentFilteredList.get(modelRowIndex);
        int reservationId = reservation.getReservationId();

        if (viewingArchived) {
            boolean confirmed = UIUtils.confirm(this,
                    "Restore reservation #" + reservationId + " back to active reservations?", "Confirm Restore");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    reservationService.restoreReservation(reservationId);
                    UIUtils.showSuccess(this, "Reservation restored successfully.");
                    loadReservations();
                });
            }
        } else {
            boolean confirmed = UIUtils.confirm(this,
                    "Move reservation #" + reservationId + " to archive? This is a soft delete.",
                    "Confirm Delete");
            if (confirmed) {
                UIUtils.runSafely(this, () -> {
                    reservationService.softDeleteReservation(reservationId);
                    UIUtils.showSuccess(this, "Reservation archived successfully.");
                    loadReservations();
                });
            }
        }
    }

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
                        setBackground(new Color(255, 243, 205));
                        setForeground(new Color(133, 100, 4));
                    } else if (statusStr.equalsIgnoreCase("CONFIRMED") || statusStr.equalsIgnoreCase("CHECKED_IN")) {
                        setBackground(new Color(212, 239, 223));
                        setForeground(new Color(21, 87, 36));
                    } else if (statusStr.equalsIgnoreCase("CANCELLED")) {
                        setBackground(new Color(248, 215, 218));
                        setForeground(new Color(114, 28, 36));
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
