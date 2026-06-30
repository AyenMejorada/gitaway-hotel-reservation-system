package com.hotel.ui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Reusable dropdown calendar picker component.
 */
public class DatePickerField extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JTextField displayField = new JTextField();
    private final JButton dropButton = new JButton("▼");
    private final JPopupMenu popup = new JPopupMenu();
    private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 2, 2));
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate;
    private Runnable changeListener;
    private java.util.function.Predicate<LocalDate> dateValidator;

    public DatePickerField() {
        super(new BorderLayout(4, 0));
        setOpaque(false);

        displayField.setEditable(false);
        displayField.setPreferredSize(new Dimension(0, 36));
        displayField.setText("Select date...");

        dropButton.setFocusable(false);
        dropButton.setPreferredSize(new Dimension(36, 36));

        add(displayField, BorderLayout.CENTER);
        add(dropButton, BorderLayout.EAST);

        buildPopup();

        dropButton.addActionListener(e -> showPopup());
        displayField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup();
            }
        });
    }

    private void buildPopup() {
        JPanel popupContent = new JPanel(new BorderLayout(4, 4));
        popupContent.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        popupContent.setBackground(Color.WHITE);
        popupContent.setPreferredSize(new Dimension(260, 220));

        JPanel navRow = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        prevButton.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            renderMonth();
        });
        nextButton.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            renderMonth();
        });
        monthLabel.setFont(UIUtils.FONT_BOLD);
        navRow.add(prevButton, BorderLayout.WEST);
        navRow.add(monthLabel, BorderLayout.CENTER);
        navRow.add(nextButton, BorderLayout.EAST);

        calendarGrid.setBackground(Color.WHITE);

        popupContent.add(navRow, BorderLayout.NORTH);
        popupContent.add(calendarGrid, BorderLayout.CENTER);

        popup.add(popupContent);
    }

    private void showPopup() {
        currentMonth = selectedDate != null ? YearMonth.from(selectedDate) : YearMonth.now();
        renderMonth();
        popup.show(this, 0, getHeight());
    }

    private void renderMonth() {
        calendarGrid.removeAll();
        monthLabel.setText(currentMonth.getMonth().toString() + " " + currentMonth.getYear());

        String[] dayHeaders = { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
        for (String d : dayHeaders) {
            JLabel header = new JLabel(d, SwingConstants.CENTER);
            header.setFont(UIUtils.FONT_BOLD);
            calendarGrid.add(header);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int leadingBlanks = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        for (int i = 0; i < leadingBlanks; i++) {
            calendarGrid.add(new JLabel(""));
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            JButton dayButton = new JButton(String.valueOf(day));
            dayButton.setMargin(new Insets(2, 2, 2, 2));
            dayButton.setFocusable(false);
            if (date.equals(selectedDate)) {
                dayButton.setBackground(UIUtils.ACCENT_COLOR);
                dayButton.setForeground(Color.WHITE);
            }
            if (dateValidator != null && !dateValidator.test(date)) {
                dayButton.setEnabled(false);
                dayButton.setToolTipText("This date is unavailable.");
            } else {
                dayButton.addActionListener(e -> {
                    setDate(date);
                    popup.setVisible(false);
                });
            }
            calendarGrid.add(dayButton);
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private String placeholder = "Select date...";

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        if (selectedDate == null) {
            displayField.setText(placeholder);
        }
    }

    public void setDate(LocalDate date) {
        this.selectedDate = date;
        displayField.setText(date == null ? placeholder : date.format(DATE_FORMAT));
        if (changeListener != null) {
            changeListener.run();
        }
    }

    public LocalDate getDate() {
        return selectedDate;
    }

    public void addChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    public void setDateValidator(java.util.function.Predicate<LocalDate> validator) {
        this.dateValidator = validator;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        displayField.setEnabled(enabled);
        dropButton.setEnabled(enabled);
    }
}
