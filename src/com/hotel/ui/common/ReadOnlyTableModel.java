package com.hotel.ui.common;

import javax.swing.table.DefaultTableModel;

/**
 * A read-only {@link DefaultTableModel} variant used throughout the admin
 * management screens. Cells are never directly editable in the table;
 * all edits happen through dedicated add/edit dialogs, which keeps data
 * validation centralized in the service layer rather than scattered
 * across table cell editors.
 */
public class ReadOnlyTableModel extends DefaultTableModel {

    public ReadOnlyTableModel(Object[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
