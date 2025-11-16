import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Database manager using embedded Derby.
 * Provides table creation, menu CRUD, and saving/updating/deleting orders.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:derby:oakdonutsdb;create=true";

    // Initialize database and create tables if missing
    public static void initializeDatabase() throws SQLException {
        // ensure driver loaded (optional)
        try { Class.forName("org.apache.derby.jdbc.EmbeddedDriver"); } catch (ClassNotFoundException ignored) {}

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            DatabaseMetaData md = conn.getMetaData();

            // create menu_items table
            try (ResultSet rs = md.getTables(null, null, "MENU_ITEMS", null)) {
                if (!rs.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate(
                                "CREATE TABLE menu_items (" +
                                        "id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                                        "name VARCHAR(200) UNIQUE NOT NULL," +
                                        "category VARCHAR(50)," +
                                        "price DOUBLE" +
                                        ")"
                        );
                    }
                }
            }

            // create orders table
            try (ResultSet rs = md.getTables(null, null, "ORDERS", null)) {
                if (!rs.next()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate(
                                "CREATE TABLE orders (" +
                                        "transaction_id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                                        "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                        "items VARCHAR(4000)," +
                                        "subtotal DOUBLE," +
                                        "tax DOUBLE," +
                                        "total DOUBLE" +
                                        ")"
                        );
                    }
                }
            }
        }
    }

    // load menu items into a LinkedHashMap (insertion order)
    public static LinkedHashMap<String, MenuItem> loadMenuItems() throws SQLException {
        LinkedHashMap<String, MenuItem> map = new LinkedHashMap<>();
        String sql = "SELECT id, name, category, price FROM menu_items ORDER BY id";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String category = rs.getString("category");
                double price = rs.getDouble("price");
                map.put(name, new MenuItem(id, name, category, price));
            }
        }
        return map;
    }

    // add a new menu item, returns the generated id or -1 on error
    public static int addMenuItem(String name, String category, double price) throws SQLException {
        String sql = "INSERT INTO menu_items (name, category, price) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // update menu item by id
    public static boolean updateMenuItem(int id, String name, String category, double price) throws SQLException {
        String sql = "UPDATE menu_items SET name=?, category=?, price=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    // delete menu item by id
    public static boolean deleteMenuItem(int id) throws SQLException {
        String sql = "DELETE FROM menu_items WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // save an order; returns generated transaction id or -1 on failure
    public static int saveOrder(String items, double subtotal, double tax, double total) throws SQLException {
        String sql = "INSERT INTO orders (items, subtotal, tax, total) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, items);
            ps.setDouble(2, subtotal);
            ps.setDouble(3, tax);
            ps.setDouble(4, total);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // load all orders as a list of OrderRow
    public static List<OrderRow> loadOrders() throws SQLException {
        List<OrderRow> list = new ArrayList<>();
        String sql = "SELECT transaction_id, order_date, items, subtotal, tax, total FROM orders ORDER BY transaction_id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("transaction_id");
                Timestamp ts = rs.getTimestamp("order_date");
                String items = rs.getString("items");
                double subtotal = rs.getDouble("subtotal");
                double tax = rs.getDouble("tax");
                double total = rs.getDouble("total");
                list.add(new OrderRow(id, ts, items, subtotal, tax, total));
            }
        }
        return list;
    }

    // update an order by transaction_id
    public static boolean updateOrder(int transactionId, String items, double subtotal, double tax, double total) throws SQLException {
        String sql = "UPDATE orders SET items=?, subtotal=?, tax=?, total=? WHERE transaction_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, items);
            ps.setDouble(2, subtotal);
            ps.setDouble(3, tax);
            ps.setDouble(4, total);
            ps.setInt(5, transactionId);
            return ps.executeUpdate() > 0;
        }
    }

    // delete an order by transaction_id
    public static boolean deleteOrder(int transactionId) throws SQLException {
        String sql = "DELETE FROM orders WHERE transaction_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            return ps.executeUpdate() > 0;
        }
    }

    // small helper class to hold menu item data (id included)
    public static class MenuItem {
        public final int id;
        public String name;
        public String category;
        public double price;

        public MenuItem(int id, String name, String category, double price) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
        }

        @Override
        public String toString() {
            return name + " (" + category + ") - " + price;
        }
    }

    // helper class to represent orders
    public static class OrderRow {
        public final int transactionId;
        public final Timestamp orderDate;
        public String items;
        public double subtotal;
        public double tax;
        public double total;

        public OrderRow(int transactionId, Timestamp orderDate, String items, double subtotal, double tax, double total) {
            this.transactionId = transactionId;
            this.orderDate = orderDate;
            this.items = items;
            this.subtotal = subtotal;
            this.tax = tax;
            this.total = total;
        }
    }
}
