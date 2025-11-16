import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Locale;

/**
 * DonutShopApp - single-file demo application.
 *
 * Note: This file includes a simple in-memory DatabaseManager so it can be compiled/run standalone.
 * Replace DatabaseManager with your actual implementation if you have one.
 */
public class DonutShopApp {

    // ----- class fields (must NOT be inside a method) -----
    private LinkedHashMap<String, DatabaseManager.MenuItem> itemMap = new LinkedHashMap<>();
    private final NumberFormat money = NumberFormat.getCurrencyInstance(Locale.US);

    // UI components
    private final JFrame frame;
    private final JList<String> menuList;
    private final JLabel unitPriceLabel;
    private final JSpinner qtySpinner;
    private final JComboBox<String> icingBox;
    private final JComboBox<String> fillingBox;
    private final DefaultTableModel orderTableModel;
    private final JLabel subtotalLabel;
    private final JLabel taxLabel;
    private final JLabel totalLabel;

    private static final double TAX_RATE = 0.06;

    // ----- main - single entry point -----
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager.initializeDatabase();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "DB initialize failed: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
            new DonutShopApp().show();
        });
    }

    // ----- constructor - build UI here -----
    public DonutShopApp() {
        // initialize UI fields that are final
        frame = new JFrame("Oak Donuts – Ordering Mockup");
        menuList = new JList<>(new DefaultListModel<>());
        unitPriceLabel = new JLabel("Unit: $0.00");
        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        icingBox = new JComboBox<>(new String[]{"None", "Chocolate", "Vanilla", "Maple"});
        fillingBox = new JComboBox<>(new String[]{"None", "Custard", "Jam", "Cream"});
        orderTableModel = new DefaultTableModel(new Object[]{"Item", "Options", "Qty", "Price", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        subtotalLabel = new JLabel("Subtotal: " + money.format(0.0));
        taxLabel = new JLabel("Tax (6%): " + money.format(0.0));
        totalLabel = new JLabel("<html><b>Total: " + money.format(0.0) + "</b></html>");

        // frame setup
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 740);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // title and buttons
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 12, 6, 12));
        JLabel title = new JLabel("Oak Donuts OD");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 36f));
        topPanel.add(title, BorderLayout.WEST);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton manageMenuBtn = new JButton("Manage Menu");
        manageMenuBtn.addActionListener(e -> openManageMenuDialog());
        JButton orderHistoryBtn = new JButton("Order History");
        orderHistoryBtn.addActionListener(e -> openOrderHistoryDialog());
        rightTop.add(orderHistoryBtn);
        rightTop.add(manageMenuBtn);
        topPanel.add(rightTop, BorderLayout.EAST);
        frame.add(topPanel, BorderLayout.NORTH);

        // left column filters and options
        JPanel leftCol = new JPanel();
        leftCol.setPreferredSize(new Dimension(240, 0));
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel filtersLabel = new JLabel("Filters");
        filtersLabel.setFont(filtersLabel.getFont().deriveFont(Font.BOLD, 16f));
        leftCol.add(filtersLabel);
        leftCol.add(Box.createRigidArea(new Dimension(0, 8)));

        leftCol.add(new JLabel("Category:"));
        String[] categories = {"All", "Donuts", "Sandwiches", "Drinks"};
        final JComboBox<String> categoryBox = new JComboBox<>(categories);
        categoryBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        leftCol.add(categoryBox);
        leftCol.add(Box.createRigidArea(new Dimension(0, 8)));

        leftCol.add(new JLabel("Search:"));
        final JTextField searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        leftCol.add(searchField);
        leftCol.add(Box.createRigidArea(new Dimension(0, 18)));

        JLabel itemOpts = new JLabel("Item Options");
        itemOpts.setFont(itemOpts.getFont().deriveFont(Font.BOLD, 16f));
        leftCol.add(itemOpts);
        leftCol.add(Box.createRigidArea(new Dimension(0, 8)));

        leftCol.add(new JLabel("Icing:"));
        icingBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        leftCol.add(icingBox);
        leftCol.add(Box.createRigidArea(new Dimension(0, 8)));

        leftCol.add(new JLabel("Filling:"));
        fillingBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        leftCol.add(fillingBox);

        leftCol.add(Box.createVerticalGlue());
        frame.add(leftCol, BorderLayout.WEST);

        // menu center
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(10, 6, 10, 6));
        JLabel menuLabel = new JLabel("Menu");
        menuLabel.setFont(menuLabel.getFont().deriveFont(Font.BOLD, 18f));
        centerPanel.add(menuLabel, BorderLayout.NORTH);

        DefaultListModel<String> menuModel = (DefaultListModel<String>) menuList.getModel();
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setFixedCellHeight(26);
        menuList.setFont(menuList.getFont().deriveFont(14f));
        JScrollPane menuScroll = new JScrollPane(menuList);
        centerPanel.add(menuScroll, BorderLayout.CENTER);

        // add to order section
        JPanel centerBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        centerBottom.setBorder(new EmptyBorder(8, 2, 2, 2));
        centerBottom.add(new JLabel("Qty:"));
        ((JSpinner.DefaultEditor) qtySpinner.getEditor()).getTextField().setColumns(2);
        centerBottom.add(qtySpinner);
        centerBottom.add(Box.createHorizontalStrut(10));
        centerBottom.add(unitPriceLabel);
        centerBottom.add(Box.createHorizontalStrut(12));
        JButton addToOrderBtn = new JButton("Add to Order");
        centerBottom.add(addToOrderBtn);
        centerPanel.add(centerBottom, BorderLayout.SOUTH);
        frame.add(centerPanel, BorderLayout.CENTER);

        // order summary right
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new EmptyBorder(10, 6, 10, 12));
        JLabel orderLabel = new JLabel("Order");
        orderLabel.setFont(orderLabel.getFont().deriveFont(Font.BOLD, 18f));
        rightPanel.add(orderLabel, BorderLayout.NORTH);

        JTable orderTable = new JTable(orderTableModel);
        orderTable.setFillsViewportHeight(true);
        orderTable.setRowHeight(22);
        JScrollPane orderScroll = new JScrollPane(orderTable);
        rightPanel.add(orderScroll, BorderLayout.CENTER);

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        subtotalLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        taxLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        totalLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        summaryPanel.add(Box.createVerticalGlue());
        summaryPanel.add(subtotalLabel);
        summaryPanel.add(Box.createRigidArea(new Dimension(0,4)));
        summaryPanel.add(taxLabel);
        summaryPanel.add(Box.createRigidArea(new Dimension(0,6)));
        summaryPanel.add(totalLabel);

        JPanel rightBottom = new JPanel(new BorderLayout());
        rightBottom.add(summaryPanel, BorderLayout.CENTER);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear");
        JButton checkoutButton = new JButton("Checkout");
        actionBtns.add(clearButton);
        actionBtns.add(checkoutButton);
        rightBottom.add(actionBtns, BorderLayout.SOUTH);

        rightPanel.add(rightBottom, BorderLayout.SOUTH);
        frame.add(rightPanel, BorderLayout.EAST);

        // load menu from DB (or seed if empty)
        try {
            itemMap = DatabaseManager.loadMenuItems();
            if (itemMap.isEmpty()) {
                seedDefaultMenu();
                itemMap = DatabaseManager.loadMenuItems();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed to load menu: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        // refresh menu runnable
        Runnable refreshMenu = () -> {
            String selectedCategory = (String) categoryBox.getSelectedItem();
            String q = searchField.getText().trim().toLowerCase();
            DefaultListModel<String> model = (DefaultListModel<String>) menuList.getModel();
            model.clear();
            for (Map.Entry<String, DatabaseManager.MenuItem> e : itemMap.entrySet()) {
                String name = e.getKey();
                DatabaseManager.MenuItem mi = e.getValue();
                String cat = mi.category == null ? "All" : mi.category;
                boolean catMatch = "All".equals(selectedCategory) || selectedCategory.equalsIgnoreCase(cat);
                boolean searchMatch = q.isEmpty() || name.toLowerCase().contains(q);
                if (catMatch && searchMatch) {
                    model.addElement(String.format("%s — %s", name, money.format(mi.price)));
                }
            }
            if (!model.isEmpty()) menuList.setSelectedIndex(0);
        };

        refreshMenu.run();

        categoryBox.addActionListener(e -> refreshMenu.run());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            void upd(){ refreshMenu.run(); }
            public void insertUpdate(DocumentEvent e){ upd(); }
            public void removeUpdate(DocumentEvent e){ upd(); }
            public void changedUpdate(DocumentEvent e){ upd(); }
        });

        menuList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = getSelectedItemName();
                if (selected != null && itemMap.containsKey(selected)) {
                    unitPriceLabel.setText("Unit: " + money.format(itemMap.get(selected).price));
                } else {
                    unitPriceLabel.setText("Unit: $0.00");
                }
            }
        });

        addToOrderBtn.addActionListener(e -> addSelectedToOrder());
        menuList.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (e.getClickCount()==2) addSelectedToOrder(); } });

        clearButton.addActionListener(e -> { orderTableModel.setRowCount(0); updateTotals(); });

        checkoutButton.addActionListener(e -> {
            if (orderTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(frame, "No items selected.");
                return;
            }
            double subtotal = computeSubtotal();
            double tax = subtotal * TAX_RATE;
            double grand = subtotal + tax;

            String itemsStr = buildItemsStringFromTable();

            String message = String.format("Subtotal: %s\nTax (6%%): %s\nTotal Due: %s\n\nProceed to checkout?",
                    money.format(subtotal), money.format(tax), money.format(grand));
            int choice = JOptionPane.showConfirmDialog(frame, message, "Confirm Checkout", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    int txId = DatabaseManager.saveOrder(itemsStr, subtotal, tax, grand);
                    if (txId > 0) {
                        JOptionPane.showMessageDialog(frame, "Checkout complete!\nTransaction ID: " + txId + "\nAmount: " + money.format(grand));
                        orderTableModel.setRowCount(0);
                        updateTotals();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Failed to save order to DB.", "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to save order: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        orderTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) {
                    int r = orderTable.rowAtPoint(e.getPoint());
                    if (r >= 0) editOrderRowQuantity(r);
                }
            }
            public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int r = orderTable.rowAtPoint(e.getPoint());
                    if (r >= 0) orderTable.setRowSelectionInterval(r, r);
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem remove = new JMenuItem("Remove");
                    remove.addActionListener(a -> {
                        int sel = orderTable.getSelectedRow();
                        if (sel >= 0) { orderTableModel.removeRow(sel); updateTotals(); }
                    });
                    popup.add(remove);
                    popup.show(orderTable, e.getX(), e.getY());
                }
            }
        });
    }

    private void show() { frame.setVisible(true); }

    private void seedDefaultMenu() {
        try {
            DatabaseManager.addMenuItem("Glazed Donut", "Donuts", 1.49);
            DatabaseManager.addMenuItem("Chocolate Sprinkle Donut", "Donuts", 1.79);
            DatabaseManager.addMenuItem("Boston Creme Donut", "Donuts", 1.99);
            DatabaseManager.addMenuItem("Iced Coffee", "Drinks", 2.00);
            DatabaseManager.addMenuItem("Latte", "Drinks", 3.00);
            DatabaseManager.addMenuItem("Tomato & Mozzarella Sandwich", "Sandwiches", 4.50);
            itemMap = DatabaseManager.loadMenuItems();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Failed seeding menu: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openManageMenuDialog() {
        JDialog dlg = new JDialog(frame, "Manage Menu", true);
        dlg.setSize(700, 420);
        dlg.setLocationRelativeTo(frame);
        dlg.setLayout(new BorderLayout());

        DefaultListModel<String> model = new DefaultListModel<>();
        for (DatabaseManager.MenuItem mi : itemMap.values())
            model.addElement(String.format("%d: %s — %s", mi.id, mi.name, money.format(mi.price)));
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dlg.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(8,8,8,8));
        JTextField nameField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField priceField = new JTextField();

        right.add(new JLabel("Name:"));
        right.add(nameField);
        right.add(Box.createRigidArea(new Dimension(0,6)));
        right.add(new JLabel("Category:"));
        right.add(categoryField);
        right.add(Box.createRigidArea(new Dimension(0,6)));
        right.add(new JLabel("Price:"));
        right.add(priceField);
        right.add(Box.createRigidArea(new Dimension(0,12)));

        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JPanel btnRow = new JPanel(new FlowLayout());
        btnRow.add(addBtn);
        btnRow.add(updateBtn);
        btnRow.add(deleteBtn);
        right.add(btnRow);

        dlg.add(right, BorderLayout.EAST);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = list.getSelectedValue();
                if (sel != null) {
                    int colon = sel.indexOf(':');
                    int dash = sel.indexOf('—');
                    if (colon >= 0 && dash > colon) {
                        String idStr = sel.substring(0, colon).trim();
                        try {
                            int id = Integer.parseInt(idStr);
                            DatabaseManager.MenuItem mi = null;
                            for (DatabaseManager.MenuItem m : itemMap.values())
                                if (m.id == id) mi = m;
                            if (mi != null) {
                                nameField.setText(mi.name);
                                categoryField.setText(mi.category);
                                priceField.setText(String.valueOf(mi.price));
                            }
                        } catch (Exception ignore) {}
                    }
                }
            }
        });

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String cat = categoryField.getText().trim();
            String priceS = priceField.getText().trim();
            if (name.isEmpty() || priceS.isEmpty()) { JOptionPane.showMessageDialog(dlg, "Name and price required."); return; }
            try {
                double p = Double.parseDouble(priceS);
                int newId = DatabaseManager.addMenuItem(name, cat.isEmpty() ? "Donuts" : cat, p);
                if (newId > 0) {
                    DatabaseManager.MenuItem mi = new DatabaseManager.MenuItem(newId, name, cat, p);
                    itemMap.put(name, mi);
                    model.addElement(String.format("%d: %s — %s", mi.id, mi.name, money.format(mi.price)));
                    JOptionPane.showMessageDialog(dlg, "Added.");
                } else {
                    JOptionPane.showMessageDialog(dlg, "Failed to add item.", "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dlg, "Invalid price.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error adding item: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        updateBtn.addActionListener(e -> {
            String sel = list.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(dlg, "Select an item to update."); return; }
            int colon = sel.indexOf(':');
            if (colon < 0) return;
            try {
                int id = Integer.parseInt(sel.substring(0, colon).trim());
                String name = nameField.getText().trim();
                String cat = categoryField.getText().trim();
                double p = Double.parseDouble(priceField.getText().trim());
                boolean ok = DatabaseManager.updateMenuItem(id, name, cat, p);
                if (ok) {
                    DatabaseManager.MenuItem toRemove = null;
                    for (DatabaseManager.MenuItem m : itemMap.values()) if (m.id == id) { toRemove = m; break; }
                    if (toRemove != null) itemMap.remove(toRemove.name);
                    DatabaseManager.MenuItem updated = new DatabaseManager.MenuItem(id, name, cat, p);
                    itemMap.put(name, updated);
                    model.clear();
                    for (DatabaseManager.MenuItem mi : itemMap.values()) model.addElement(String.format("%d: %s — %s", mi.id, mi.name, money.format(mi.price)));
                    JOptionPane.showMessageDialog(dlg, "Updated.");
                } else {
                    JOptionPane.showMessageDialog(dlg, "Failed to update.", "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error updating: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteBtn.addActionListener(e -> {
            String sel = list.getSelectedValue();
            if (sel == null) { JOptionPane.showMessageDialog(dlg, "Select an item to delete."); return; }
            int colon = sel.indexOf(':');
            if (colon < 0) return;
            try {
                int id = Integer.parseInt(sel.substring(0, colon).trim());
                int confirm = JOptionPane.showConfirmDialog(dlg, "Delete this item?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean ok = DatabaseManager.deleteMenuItem(id);
                    if (ok) {
                        DatabaseManager.MenuItem toRemove = null;
                        for (DatabaseManager.MenuItem m : itemMap.values()) if (m.id == id) { toRemove = m; break; }
                        if (toRemove != null) itemMap.remove(toRemove.name);
                        model.clear();
                        for (DatabaseManager.MenuItem mi : itemMap.values()) model.addElement(String.format("%d: %s — %s", mi.id, mi.name, money.format(mi.price)));
                        JOptionPane.showMessageDialog(dlg, "Deleted.");
                    } else {
                        JOptionPane.showMessageDialog(dlg, "Failed to delete.", "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error deleting: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dlg.setVisible(true);
        try { itemMap = DatabaseManager.loadMenuItems(); } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Failed to refresh menu after manage: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE); }
    }

    private void openOrderHistoryDialog() {
        JDialog dlg = new JDialog(frame, "Order History", true);
        dlg.setSize(900, 450);
        dlg.setLocationRelativeTo(frame);
        dlg.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Transaction ID", "Date", "Items", "Subtotal", "Tax", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        dlg.add(scroll, BorderLayout.CENTER);

        Runnable loadOrders = () -> {
            model.setRowCount(0);
            try {
                List<DatabaseManager.OrderRow> orders = DatabaseManager.loadOrders();
                for (DatabaseManager.OrderRow o : orders) {
                    model.addRow(new Object[]{o.transactionId, o.orderDate.toString(), o.items, money.format(o.subtotal), money.format(o.tax), money.format(o.total)});
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Failed to load orders: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        loadOrders.run();

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editBtn = new JButton("Edit");
        JButton delBtn = new JButton("Delete");
        JButton closeBtn = new JButton("Close");
        bottom.add(editBtn);
        bottom.add(delBtn);
        bottom.add(closeBtn);
        dlg.add(bottom, BorderLayout.SOUTH);

        // edit selected order
        editBtn.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(dlg, "Select an order to edit."); return; }
            int modelIndex = table.convertRowIndexToModel(sel);
            int txId = (Integer) model.getValueAt(modelIndex, 0);
            String dateStr = (String) model.getValueAt(modelIndex, 1);
            String items = (String) model.getValueAt(modelIndex, 2);
            String subtotalStr = (String) model.getValueAt(modelIndex, 3);
            String taxStr = (String) model.getValueAt(modelIndex, 4);
            String totalStr = (String) model.getValueAt(modelIndex, 5);

            JDialog editDlg = new JDialog(dlg, "Edit Order " + txId, true);
            editDlg.setSize(700, 380);
            editDlg.setLocationRelativeTo(dlg);
            editDlg.setLayout(new BorderLayout());

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBorder(new EmptyBorder(8,8,8,8));
            JTextArea itemsArea = new JTextArea(items, 8, 60);
            JScrollPane itemsScroll = new JScrollPane(itemsArea);
            center.add(new JLabel("Items:"));
            center.add(itemsScroll);
            center.add(Box.createRigidArea(new Dimension(0,8)));

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.add(new JLabel("Subtotal:"));
            JTextField subtField = new JTextField(subtotalStr, 10);
            row.add(subtField);
            row.add(Box.createHorizontalStrut(12));
            row.add(new JLabel("Tax:"));
            JTextField taxField = new JTextField(taxStr, 10);
            row.add(taxField);
            row.add(Box.createHorizontalStrut(12));
            row.add(new JLabel("Total:"));
            JTextField totalField = new JTextField(totalStr, 10);
            row.add(totalField);
            center.add(row);

            editDlg.add(center, BorderLayout.CENTER);

            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveBtn = new JButton("Save");
            JButton cancelBtn = new JButton("Cancel");
            bottomRow.add(saveBtn);
            bottomRow.add(cancelBtn);
            editDlg.add(bottomRow, BorderLayout.SOUTH);

            saveBtn.addActionListener(a -> {
                String newItems = itemsArea.getText().trim();
                String sSub = subtField.getText().trim();
                String sTax = taxField.getText().trim();
                String sTotal = totalField.getText().trim();
                try {
                    double newSub = parseCurrencyOrNumber(sSub);
                    double newTax = parseCurrencyOrNumber(sTax);
                    double newTotal = parseCurrencyOrNumber(sTotal);
                    if (Math.abs((newSub + newTax) - newTotal) > 0.5) {
                        int ok = JOptionPane.showConfirmDialog(editDlg, "Subtotal + Tax does not equal Total. Save anyway?", "Validation", JOptionPane.YES_NO_OPTION);
                        if (ok != JOptionPane.YES_OPTION) return;
                    }
                    boolean updated = DatabaseManager.updateOrder(txId, newItems, newSub, newTax, newTotal);
                    if (updated) {
                        JOptionPane.showMessageDialog(editDlg, "Order updated.");
                        editDlg.dispose();
                        loadOrders.run();
                    } else {
                        JOptionPane.showMessageDialog(editDlg, "Failed to update order.", "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(editDlg, "Invalid numeric values: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelBtn.addActionListener(a -> editDlg.dispose());

            editDlg.setVisible(true);
        });

        // delete selected order
        delBtn.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel < 0) { JOptionPane.showMessageDialog(dlg, "Select an order to delete."); return; }
            int modelIndex = table.convertRowIndexToModel(sel);
            int txId = (Integer) model.getValueAt(modelIndex, 0);
            int confirm = JOptionPane.showConfirmDialog(dlg, "Delete order #" + txId + " ? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    boolean ok = DatabaseManager.deleteOrder(txId);
                    if (ok) {
                        JOptionPane.showMessageDialog(dlg, "Deleted order #" + txId);
                        loadOrders.run();
                    } else {
                        JOptionPane.showMessageDialog(dlg, "Failed to delete order.", "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Error deleting order: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        closeBtn.addActionListener(e -> dlg.dispose());

        dlg.setVisible(true);
    }

    private double parseCurrencyOrNumber(String s) throws Exception {
        if (s == null) throw new Exception("empty");
        String cleaned = s.replaceAll("[,$]", "").trim();
        return Double.parseDouble(cleaned.replaceAll("[^0-9.\\-]", ""));
    }

    private String getSelectedItemName() {
        String raw = menuList.getSelectedValue();
        if (raw == null) return null;
        int dash = raw.indexOf("—");
        if (dash > 0) return raw.substring(0, dash).trim();
        return raw.trim();
    }

    private void addSelectedToOrder() {
        String item = getSelectedItemName();
        if (item == null) { JOptionPane.showMessageDialog(frame, "Please select an item from the menu."); return; }
        DatabaseManager.MenuItem mi = itemMap.get(item);
        if (mi == null) { JOptionPane.showMessageDialog(frame, "Selected item not found."); return; }
        int qty = (Integer) qtySpinner.getValue();
        double unitPrice = mi.price;
        String icing = (String) icingBox.getSelectedItem();
        String filling = (String) fillingBox.getSelectedItem();
        String options = "";
        if (!"None".equals(icing)) options += "Icing: " + icing;
        if (!"None".equals(filling)) {
            if (!options.isEmpty()) options += ", ";
            options += "Filling: " + filling;
        }
        if (options.isEmpty()) options = "-";
        double total = unitPrice * qty;
        orderTableModel.addRow(new Object[]{mi.name, options, qty, money.format(unitPrice), money.format(total)});
        updateTotals();
    }

    private double computeSubtotal() {
        double subtotal = 0.0;
        for (int r = 0; r < orderTableModel.getRowCount(); r++) {
            Object totalObj = orderTableModel.getValueAt(r, 4);
            if (totalObj != null) {
                try {
                    Number n = money.parse(totalObj.toString());
                    subtotal += n.doubleValue();
                } catch (Exception ex) {
                    try {
                        subtotal += Double.parseDouble(totalObj.toString().replaceAll("[^0-9.\\-]", ""));
                    } catch (Exception ignore) {}
                }
            }
        }
        return subtotal;
    }

    private String buildItemsStringFromTable() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < orderTableModel.getRowCount(); r++) {
            String item = (String) orderTableModel.getValueAt(r, 0);
            String opts = (String) orderTableModel.getValueAt(r, 1);
            int qty = (Integer) orderTableModel.getValueAt(r, 2);
            sb.append(item).append(" x").append(qty);
            if (opts != null && !opts.equals("-")) sb.append(" [").append(opts).append("]");
            if (r < orderTableModel.getRowCount() - 1) sb.append("; ");
        }
        return sb.toString();
    }

    private void updateTotals() {
        double subtotal = computeSubtotal();
        double tax = subtotal * TAX_RATE;
        double grand = subtotal + tax;
        subtotalLabel.setText("Subtotal: " + money.format(subtotal));
        taxLabel.setText("Tax (6%): " + money.format(tax));
        totalLabel.setText("<html><b>Total: " + money.format(grand) + "</b></html>");
    }

    private void editOrderRowQuantity(int row) {
        Object qtyObj = orderTableModel.getValueAt(row, 2);
        int current = (qtyObj instanceof Integer) ? (Integer) qtyObj : Integer.parseInt(qtyObj.toString());
        String input = JOptionPane.showInputDialog(frame, "Enter new quantity:", current);
        if (input == null) return;
        try {
            int newQty = Integer.parseInt(input.trim());
            if (newQty < 1) throw new NumberFormatException();
            String priceStr = (String) orderTableModel.getValueAt(row, 3);
            Number pnum = money.parse(priceStr);
            double unitPrice = pnum.doubleValue();
            double newTotal = unitPrice * newQty;
            orderTableModel.setValueAt(newQty, row, 2);
            orderTableModel.setValueAt(money.format(newTotal), row, 4);
            updateTotals();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Invalid quantity.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------
    // Simple in-memory DatabaseManager (for demo/testing)
    // -------------------------
    public static class DatabaseManager {
        private static final Map<Integer, MenuItem> menuById = new LinkedHashMap<>();
        private static final Map<String, MenuItem> menuByName = new LinkedHashMap<>();
        private static final Map<Integer, OrderRow> ordersById = new LinkedHashMap<>();
        private static final AtomicInteger menuIdGen = new AtomicInteger(1);
        private static final AtomicInteger orderIdGen = new AtomicInteger(1000);

        public static void initializeDatabase() {
            // no-op for in-memory demo
        }

        public static LinkedHashMap<String, MenuItem> loadMenuItems() {
            LinkedHashMap<String, MenuItem> copy = new LinkedHashMap<>();
            for (MenuItem m : menuById.values()) copy.put(m.name, new MenuItem(m.id, m.name, m.category, m.price));
            return copy;
        }

        public static int addMenuItem(String name, String category, double price) {
            // prevent duplicate names: if exists, update price/category
            for (MenuItem mi : menuById.values()) {
                if (mi.name.equalsIgnoreCase(name)) {
                    mi.category = category;
                    mi.price = price;
                    menuByName.put(mi.name, mi);
                    return mi.id;
                }
            }
            int id = menuIdGen.getAndIncrement();
            MenuItem mi = new MenuItem(id, name, category, price);
            menuById.put(id, mi);
            menuByName.put(name, mi);
            return id;
        }

        public static boolean updateMenuItem(int id, String name, String category, double price) {
            MenuItem mi = menuById.get(id);
            if (mi == null) return false;
            // remove old name mapping
            menuByName.remove(mi.name);
            mi.name = name;
            mi.category = category;
            mi.price = price;
            menuByName.put(name, mi);
            return true;
        }

        public static boolean deleteMenuItem(int id) {
            MenuItem mi = menuById.remove(id);
            if (mi == null) return false;
            menuByName.remove(mi.name);
            return true;
        }

        public static int saveOrder(String items, double subtotal, double tax, double total) {
            int tx = orderIdGen.getAndIncrement();
            OrderRow o = new OrderRow(tx, new Date(), items, subtotal, tax, total);
            ordersById.put(tx, o);
            return tx;
        }

        public static List<OrderRow> loadOrders() {
            List<OrderRow> out = new ArrayList<>();
            for (OrderRow o : ordersById.values()) out.add(o);
            // sort by transaction id descending (recent first)
            out.sort((a,b) -> Integer.compare(b.transactionId, a.transactionId));
            return out;
        }

        public static boolean updateOrder(int txId, String items, double subtotal, double tax, double total) {
            OrderRow o = ordersById.get(txId);
            if (o == null) return false;
            o.items = items;
            o.subtotal = subtotal;
            o.tax = tax;
            o.total = total;
            o.orderDate = new Date(); // update timestamp to now
            return true;
        }

        public static boolean deleteOrder(int txId) {
            return ordersById.remove(txId) != null;
        }

        // nested data classes
        public static class MenuItem {
            public int id;
            public String name;
            public String category;
            public double price;
            public MenuItem(int id, String name, String category, double price) {
                this.id = id; this.name = name; this.category = category; this.price = price;
            }
        }

        public static class OrderRow {
            public int transactionId;
            public Date orderDate;
            public String items;
            public double subtotal;
            public double tax;
            public double total;
            public OrderRow(int transactionId, Date orderDate, String items, double subtotal, double tax, double total) {
                this.transactionId = transactionId;
                this.orderDate = orderDate;
                this.items = items;
                this.subtotal = subtotal;
                this.tax = tax;
                this.total = total;
            }
        }
    }
}
