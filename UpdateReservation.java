import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UpdateReservation extends JFrame {

    // Search Components
    private JTextField txtSearch;
    private JButton btnSearch;

    // Reservation Table
    private JTable reservationTable;
    private DefaultTableModel tableModel;

    // Details Fields (Part 2)
    private JTextField txtReservationID;
    private JTextField txtRoomNumber;
    private JComboBox<String> cmbRoomType;
    private JTextField txtCheckIn;
    private JTextField txtCheckOut;
    private JComboBox<String> cmbStatus;
    private JTextField txtTotalAmount;

    // Buttons (Part 3)
    private JButton btnUpdate;
    private JButton btnClear;

    public UpdateReservation() {

        setTitle("Update Reservation");
        setSize(1200,750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout(10,10));

        //---------------- HEADER ----------------//

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(34,49,63));
        header.setBorder(new EmptyBorder(15,20,15,20));

        JLabel lblTitle = new JLabel("UPDATE RESERVATION");
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI",Font.BOLD,28));

        header.add(lblTitle,BorderLayout.WEST);

        add(header,BorderLayout.NORTH);

        //---------------- CENTER ----------------//

        JPanel centerPanel = new JPanel(new BorderLayout(10,10));
        centerPanel.setBorder(new EmptyBorder(10,10,10,10));

        //---------------- SEARCH PANEL ----------------//

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel lblSearch = new JLabel("Reservation ID");

        lblSearch.setFont(new Font("Segoe UI",Font.BOLD,16));

        txtSearch = new JTextField(20);

        btnSearch = new JButton("Search");

        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        centerPanel.add(searchPanel,BorderLayout.NORTH);

        //---------------- TABLE ----------------//

        String[] columns = {
                "Reservation ID",
                "Room No.",
                "Guest",
                "Room Type",
                "Check-In",
                "Check-Out",
                "Status",
                "Total Amount"
        };

        tableModel = new DefaultTableModel(columns,0);

        reservationTable = new JTable(tableModel);

        reservationTable.setRowHeight(28);
        reservationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        reservationTable.getTableHeader().setFont(
                new Font("Segoe UI",Font.BOLD,14));

        loadSampleData();

        JScrollPane scroll = new JScrollPane(reservationTable);

        centerPanel.add(scroll,BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        centerPanel,
        createDetailsPanel());

        splitPane.setResizeWeight(0.55);

        add(splitPane, BorderLayout.CENTER);
        

        // ---------------- TABLE CLICK ----------------

reservationTable.getSelectionModel().addListSelectionListener(e -> {

    if (!e.getValueIsAdjusting()) {

        int row = reservationTable.getSelectedRow();

        if (row != -1) {

            txtReservationID.setText(tableModel.getValueAt(row,0).toString());
            txtRoomNumber.setText(tableModel.getValueAt(row,1).toString());

            cmbRoomType.setSelectedItem(tableModel.getValueAt(row,3).toString());

            txtCheckIn.setText(tableModel.getValueAt(row,4).toString());
            txtCheckOut.setText(tableModel.getValueAt(row,5).toString());

            cmbStatus.setSelectedItem(tableModel.getValueAt(row,6).toString());

            txtTotalAmount.setText(tableModel.getValueAt(row,7).toString());

        }

    }

});

// ---------------- SEARCH BUTTON ----------------

btnSearch.addActionListener(e -> searchReservation());

// ---------------- UPDATE BUTTON ----------------

btnUpdate.addActionListener(e -> updateReservation());

// ---------------- CLEAR BUTTON ----------------

btnClear.addActionListener(e -> clearFields());
        setVisible(true);

    }

    //--------------------------------------------------

    private void loadSampleData(){

        tableModel.addRow(new Object[]{
                "RES001",
                "101",
                "Juan Dela Cruz",
                "Deluxe",
                "07/01/2026",
                "07/05/2026",
                "Confirmed",
                "₱12,500"
        });

        tableModel.addRow(new Object[]{
                "RES002",
                "205",
                "Maria Santos",
                "Suite",
                "07/03/2026",
                "07/06/2026",
                "Pending",
                "₱18,000"
        });

        tableModel.addRow(new Object[]{
                "RES003",
                "310",
                "John Reyes",
                "Standard",
                "07/08/2026",
                "07/10/2026",
                "Checked In",
                "₱6,000"
        });

        tableModel.addRow(new Object[]{
                "RES004",
                "402",
                "Anne Cruz",
                "Deluxe",
                "07/11/2026",
                "07/13/2026",
                "Cancelled",
                "₱8,500"
        });}

        private void searchReservation(){

        String id = txtSearch.getText().trim();

        if(id.isEmpty()){

        JOptionPane.showMessageDialog(
                this,
                "Please enter a Reservation ID.");

        return;
    }

    boolean found = false;

    for(int i=0;i<tableModel.getRowCount();i++){

        String reservationID = tableModel.getValueAt(i,0).toString();

        if(reservationID.equalsIgnoreCase(id)){

            reservationTable.setRowSelectionInterval(i,i);

            reservationTable.scrollRectToVisible(
                    reservationTable.getCellRect(i,0,true));

            found = true;

            break;

        }

    }

    if(!found){

        JOptionPane.showMessageDialog(
                this,
                "No reservation found.",
                "Search",
                JOptionPane.INFORMATION_MESSAGE);

        clearFields();

    }

}
    

        private void updateReservation(){

        int row = reservationTable.getSelectedRow();

        if(row==-1){

        JOptionPane.showMessageDialog(
                this,
                "Please select a reservation first.");

        return;

    }

    tableModel.setValueAt(
            txtReservationID.getText(),row,0);

    tableModel.setValueAt(
            txtRoomNumber.getText(),row,1);

    tableModel.setValueAt(
            cmbRoomType.getSelectedItem(),row,3);

    tableModel.setValueAt(
            txtCheckIn.getText(),row,4);

    tableModel.setValueAt(
            txtCheckOut.getText(),row,5);

    tableModel.setValueAt(
            cmbStatus.getSelectedItem(),row,6);

    tableModel.setValueAt(
            txtTotalAmount.getText(),row,7);

    JOptionPane.showMessageDialog(
            this,
            "Reservation updated successfully!");

}

        private void clearFields(){

    txtSearch.setText("");

    txtReservationID.setText("");

    txtRoomNumber.setText("");

    txtCheckIn.setText("");

    txtCheckOut.setText("");

    txtTotalAmount.setText("");

    cmbRoomType.setSelectedIndex(0);

    cmbStatus.setSelectedIndex(0);

    reservationTable.clearSelection();

        }

    

    private JPanel createDetailsPanel(){

    JPanel panel = new JPanel(new BorderLayout());

    panel.setBorder(new EmptyBorder(15,15,15,15));

    JLabel title = new JLabel("Reservation Details");

    title.setFont(new Font("Segoe UI", Font.BOLD,22));

    panel.add(title, BorderLayout.NORTH);

    JPanel form = new JPanel(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();

    gbc.insets = new Insets(8,8,8,8);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    txtReservationID = new JTextField(20);
    txtRoomNumber = new JTextField(20);

    cmbRoomType = new JComboBox<>();

    cmbRoomType.addItem("Standard");
    cmbRoomType.addItem("Deluxe");
    cmbRoomType.addItem("Suite");

    txtCheckIn = new JTextField(20);
    txtCheckOut = new JTextField(20);

    cmbStatus = new JComboBox<>();

    cmbStatus.addItem("Pending");
    cmbStatus.addItem("Confirmed");
    cmbStatus.addItem("Checked In");
    cmbStatus.addItem("Checked Out");
    cmbStatus.addItem("Cancelled");

    txtTotalAmount = new JTextField(20);

    int row = 0;

    addField(form, gbc, row++, "Reservation ID", txtReservationID);
    addField(form, gbc, row++, "Room Number", txtRoomNumber);
    addField(form, gbc, row++, "Room Type", cmbRoomType);
    addField(form, gbc, row++, "Check-In Date", txtCheckIn);
    addField(form, gbc, row++, "Check-Out Date", txtCheckOut);
    addField(form, gbc, row++, "Reservation Status", cmbStatus);
    addField(form, gbc, row++, "Total Amount", txtTotalAmount);

    panel.add(form, BorderLayout.CENTER);

    JPanel buttons = new JPanel();

    btnUpdate = new JButton("Update Reservation");

    btnClear = new JButton("Clear");

    buttons.add(btnUpdate);
    buttons.add(btnClear);

    panel.add(buttons, BorderLayout.SOUTH);

    return panel;

}

private void addField(JPanel panel,
                      GridBagConstraints gbc,
                      int row,
                      String label,
                      Component field){

    gbc.gridx = 0;
    gbc.gridy = row;
    gbc.weightx = 0;

    JLabel lbl = new JLabel(label);

    lbl.setFont(new Font("Segoe UI",Font.BOLD,15));

    panel.add(lbl, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1;

    panel.add(field, gbc);

}

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() ->
                new UpdateReservation());

    }

}
