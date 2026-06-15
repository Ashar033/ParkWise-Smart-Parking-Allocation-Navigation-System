import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ==================== MODEL CLASSES ====================

class ParkingSlot implements Comparable<ParkingSlot> {
    private String id;
    private String location;
    private double distanceFromUser;
    private double trafficDensity;
    private double costPerHour;
    private boolean isAvailable;
    private int priorityScore;

    public ParkingSlot(String id, String location, double distance,
                       double traffic, double cost, boolean available) {
        this.id = id;
        this.location = location;
        this.distanceFromUser = distance;
        this.trafficDensity = traffic;
        this.costPerHour = cost;
        this.isAvailable = available;
        calculatePriorityScore();
    }

    private void calculatePriorityScore() {
        double normalizedDistance = distanceFromUser / 10.0;
        double normalizedCost = costPerHour / 20.0;
        this.priorityScore = (int)((normalizedDistance * 0.4 +
                trafficDensity * 0.3 +
                normalizedCost * 0.3) * 100);
    }

    public String getId() { return id; }
    public String getLocation() { return location; }
    public double getDistanceFromUser() { return distanceFromUser; }
    public double getTrafficDensity() { return trafficDensity; }
    public double getCostPerHour() { return costPerHour; }
    public boolean isAvailable() { return isAvailable; }
    public int getPriorityScore() { return priorityScore; }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    @Override
    public int compareTo(ParkingSlot other) {
        return Integer.compare(this.priorityScore, other.priorityScore);
    }

    @Override
    public String toString() {
        return String.format("%s - %s | Distance: %.1fkm | Traffic: %.0f%% | Cost: $%.2f | %s",
                id, location, distanceFromUser, trafficDensity * 100,
                costPerHour, isAvailable ? "Available" : "Occupied");
    }
}

// ==================== GRAPH FOR ROAD NETWORK ====================

class Graph {
    private Map<String, List<Edge>> adjacencyList;
    private Map<String, String> nodeLocations;

    public Graph() {
        adjacencyList = new HashMap<>();
        nodeLocations = new HashMap<>();
        initializeGraph();
    }

    static class Edge {
        String destination;
        double weight;

        Edge(String dest, double w) {
            this.destination = dest;
            this.weight = w;
        }
    }

    private void initializeGraph() {
        addNode("A", "North Entrance");
        addNode("B", "South Mall");
        addNode("C", "East Plaza");
        addNode("D", "West Garage");
        addNode("E", "Central Square");
        addNode("F", "Shopping Complex");
        addNode("G", "Business District");
        addNode("H", "Residential Area");

        addEdge("A", "B", 0.8);
        addEdge("A", "C", 1.2);
        addEdge("B", "D", 0.5);
        addEdge("B", "E", 0.7);
        addEdge("C", "E", 0.6);
        addEdge("C", "F", 1.0);
        addEdge("D", "G", 0.9);
        addEdge("E", "F", 0.4);
        addEdge("E", "G", 0.8);
        addEdge("F", "H", 1.1);
        addEdge("G", "H", 0.7);
        addEdge("A", "D", 1.5);
        addEdge("B", "F", 1.3);
    }

    private void addNode(String id, String location) {
        adjacencyList.putIfAbsent(id, new ArrayList<>());
        nodeLocations.put(id, location);
    }

    private void addEdge(String from, String to, double weight) {
        adjacencyList.get(from).add(new Edge(to, weight));
        adjacencyList.get(to).add(new Edge(from, weight));
    }

    public Map<String, Double> dijkstra(String start) {
        Map<String, Double> distances = new HashMap<>();
        PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<>(
                Map.Entry.comparingByValue()
        );
        Set<String> visited = new HashSet<>();

        for (String node : adjacencyList.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        pq.add(new AbstractMap.SimpleEntry<>(start, 0.0));

        while (!pq.isEmpty()) {
            String current = pq.poll().getKey();
            if (visited.contains(current)) continue;
            visited.add(current);

            for (Edge edge : adjacencyList.get(current)) {
                double newDist = distances.get(current) + edge.weight;
                if (newDist < distances.get(edge.destination)) {
                    distances.put(edge.destination, newDist);
                    pq.add(new AbstractMap.SimpleEntry<>(edge.destination, newDist));
                }
            }
        }
        return distances;
    }

    public String getNodeLocation(String nodeId) {
        return nodeLocations.getOrDefault(nodeId, "Unknown Location");
    }

    public Set<String> getAllNodes() {
        return adjacencyList.keySet();
    }
}

// ==================== PARKING MANAGER ====================

class ParkingManager {
    private List<ParkingSlot> allSlots;
    private Graph roadNetwork;
    private PriorityQueue<ParkingSlot> availableSlotsPQ;
    private Map<String, ParkingSlot> slotMap;
    private List<String> allocationHistory;

    public ParkingManager() {
        allSlots = new ArrayList<>();
        roadNetwork = new Graph();
        availableSlotsPQ = new PriorityQueue<>();
        slotMap = new HashMap<>();
        allocationHistory = new ArrayList<>();
        initializeParkingSlots();
        updateAvailableSlotsQueue();
    }

    private void initializeParkingSlots() {
        allSlots.add(new ParkingSlot("P001", "North Entrance", 0.5, 0.3, 5.0, true));
        allSlots.add(new ParkingSlot("P002", "North Entrance", 0.6, 0.3, 4.5, true));
        allSlots.add(new ParkingSlot("P003", "South Mall", 1.2, 0.7, 8.0, true));
        allSlots.add(new ParkingSlot("P004", "South Mall", 1.3, 0.7, 7.5, false));
        allSlots.add(new ParkingSlot("P005", "East Plaza", 0.8, 0.4, 6.0, true));
        allSlots.add(new ParkingSlot("P006", "East Plaza", 0.9, 0.4, 5.5, true));
        allSlots.add(new ParkingSlot("P007", "West Garage", 1.5, 0.6, 4.0, true));
        allSlots.add(new ParkingSlot("P008", "West Garage", 1.6, 0.6, 3.5, false));
        allSlots.add(new ParkingSlot("P009", "Central Square", 0.3, 0.5, 10.0, true));
        allSlots.add(new ParkingSlot("P010", "Shopping Complex", 2.0, 0.8, 7.0, true));
        allSlots.add(new ParkingSlot("P011", "Business District", 1.8, 0.9, 12.0, true));
        allSlots.add(new ParkingSlot("P012", "Residential Area", 2.5, 0.2, 3.0, true));

        for (ParkingSlot slot : allSlots) {
            slotMap.put(slot.getId(), slot);
        }
    }

    private void updateAvailableSlotsQueue() {
        availableSlotsPQ.clear();
        for (ParkingSlot slot : allSlots) {
            if (slot.isAvailable()) {
                availableSlotsPQ.offer(slot);
            }
        }
    }

    public List<ParkingSlot> findNearestParking(String userLocation, int maxResults) {
        Map<String, Double> distances = roadNetwork.dijkstra(userLocation);
        List<ParkingSlot> sortedSlots = new ArrayList<>();

        for (ParkingSlot slot : allSlots) {
            if (!slot.isAvailable()) continue;

            double minDistToSlot = Double.MAX_VALUE;
            for (String node : roadNetwork.getAllNodes()) {
                if (roadNetwork.getNodeLocation(node).equals(slot.getLocation())) {
                    double dist = distances.getOrDefault(node, Double.MAX_VALUE);
                    if (dist < minDistToSlot) {
                        minDistToSlot = dist;
                    }
                }
            }

            if (minDistToSlot < Double.MAX_VALUE) {
                ParkingSlot updatedSlot = new ParkingSlot(
                        slot.getId(), slot.getLocation(), minDistToSlot,
                        slot.getTrafficDensity(), slot.getCostPerHour(), true
                );
                sortedSlots.add(updatedSlot);
            }
        }

        sortedSlots.sort(Comparator.comparingDouble(ParkingSlot::getDistanceFromUser));
        return sortedSlots.subList(0, Math.min(maxResults, sortedSlots.size()));
    }

    public List<ParkingSlot> getBestAvailableSlots(int count) {
        updateAvailableSlotsQueue();
        List<ParkingSlot> bestSlots = new ArrayList<>();
        PriorityQueue<ParkingSlot> tempQueue = new PriorityQueue<>(availableSlotsPQ);

        for (int i = 0; i < count && !tempQueue.isEmpty(); i++) {
            bestSlots.add(tempQueue.poll());
        }
        return bestSlots;
    }

    public boolean allocateSlot(String slotId, String userName) {
        ParkingSlot slot = slotMap.get(slotId);
        if (slot != null && slot.isAvailable()) {
            slot.setAvailable(false);
            updateAvailableSlotsQueue();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            allocationHistory.add(String.format("[%s] %s allocated to %s", timestamp, slotId, userName));
            return true;
        }
        return false;
    }

    public boolean freeSlot(String slotId) {
        ParkingSlot slot = slotMap.get(slotId);
        if (slot != null && !slot.isAvailable()) {
            slot.setAvailable(true);
            updateAvailableSlotsQueue();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            allocationHistory.add(String.format("[%s] %s freed", timestamp, slotId));
            return true;
        }
        return false;
    }

    public List<ParkingSlot> getAllSlots() {
        return new ArrayList<>(allSlots);
    }

    public List<String> getAllocationHistory() {
        return new ArrayList<>(allocationHistory);
    }

    public int getAvailableCount() {
        int count = 0;
        for (ParkingSlot slot : allSlots) {
            if (slot.isAvailable()) count++;
        }
        return count;
    }

    public Graph getRoadNetwork() {
        return roadNetwork;
    }
}

// ==================== MAIN GUI CLASS ====================

public class ParkWiseGUI extends JFrame {
    private ParkingManager parkingManager;
    private JTabbedPane tabbedPane;
    private JTable slotsTable;
    private DefaultTableModel tableModel;
    private JTextArea historyArea;
    private JComboBox<String> locationCombo;
    private JTextArea resultsArea;
    private JLabel statsLabel;
    private JLabel headerAvailableLabel;

    public ParkWiseGUI() {
        parkingManager = new ParkingManager();
        initializeUI();
        updateStats();
    }

    private void initializeUI() {
        setTitle("ParkWise - Smart Parking Allocation System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255));

        mainPanel.add(createHeader(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Dashboard", createDashboardPanel());
        tabbedPane.addTab("Find Parking", createFindParkingPanel());
        tabbedPane.addTab("All Slots", createAllSlotsPanel());
        tabbedPane.addTab("History", createHistoryPanel());
        tabbedPane.addTab("About", createAboutPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        statsLabel = new JLabel("System Ready");
        statsLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statsLabel.setBackground(new Color(70, 130, 200));
        statsLabel.setForeground(Color.WHITE);
        statsLabel.setOpaque(true);
        mainPanel.add(statsLabel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(70, 130, 200));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("ParkWise - Smart Parking Allocation System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Find the best parking spot using intelligent algorithms");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.WHITE);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        header.add(textPanel, BorderLayout.WEST);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statsPanel.setOpaque(false);
        headerAvailableLabel = new JLabel("Available: " + parkingManager.getAvailableCount());
        headerAvailableLabel.setForeground(Color.WHITE);
        headerAvailableLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statsPanel.add(headerAvailableLabel);
        header.add(statsPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(240, 248, 255));

        panel.add(createCard("Quick Stats",
                "Total Slots: " + parkingManager.getAllSlots().size() + "\n" +
                        "Available: " + parkingManager.getAvailableCount() + "\n" +
                        "Occupied: " + (parkingManager.getAllSlots().size() - parkingManager.getAvailableCount()) + "\n" +
                        "Allocations: " + parkingManager.getAllocationHistory().size()
        ));

        panel.add(createCard("Algorithms Used",
                "• Dijkstra's Shortest Path\n" +
                        "• Priority Queue (Heap)\n" +
                        "• Weighted Scoring System\n" +
                        "• Graph Traversal\n" +
                        "• BFS for route finding"
        ));

        panel.add(createCard("Features",
                "✓ Real-time availability\n" +
                        "✓ Distance-based ranking\n" +
                        "✓ Traffic consideration\n" +
                        "✓ Cost optimization\n" +
                        "✓ Historical tracking"
        ));

        JPanel quickActions = createCard("Quick Actions", "");
        quickActions.setLayout(new GridLayout(3, 1, 5, 5));

        JButton findParkingBtn = new JButton("Find Best Parking");
        findParkingBtn.addActionListener(e -> {
            tabbedPane.setSelectedIndex(1);
            findBestParking();
        });

        JButton viewAllSlotsBtn = new JButton("View All Slots");
        viewAllSlotsBtn.addActionListener(e -> tabbedPane.setSelectedIndex(2));

        JButton refreshBtn = new JButton("Refresh Status");
        refreshBtn.addActionListener(e -> {
            updateStats();
            refreshTables();
        });

        quickActions.add(findParkingBtn);
        quickActions.add(viewAllSlotsBtn);
        quickActions.add(refreshBtn);
        panel.add(quickActions);

        return panel;
    }

    private JPanel createCard(String title, String content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(70, 130, 200));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JTextArea contentArea = new JTextArea(content);
        contentArea.setEditable(false);
        contentArea.setBackground(Color.WHITE);
        contentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(contentArea, BorderLayout.CENTER);

        return card;
    }

    private JPanel createFindParkingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(240, 248, 255));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createTitledBorder("Search Parameters"));

        inputPanel.add(new JLabel("Your Location:"));
        locationCombo = new JComboBox<>(new String[]{"A", "B", "C", "D", "E", "F", "G", "H"});
        locationCombo.setPreferredSize(new Dimension(100, 25));

        JButton searchBtn = new JButton(" Find Best Parking");
        searchBtn.setBackground(new Color(70, 130, 200));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.addActionListener(e -> findBestParking());

        JButton allocateBtn = new JButton("Allocate Selected");
        allocateBtn.setBackground(new Color(60, 179, 113));
        allocateBtn.setForeground(Color.WHITE);
        allocateBtn.addActionListener(e -> allocateSelectedSlot());

        inputPanel.add(new JLabel("Location (A-H):"));
        inputPanel.add(locationCombo);
        inputPanel.add(searchBtn);
        inputPanel.add(allocateBtn);

        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void findBestParking() {
        String location = (String) locationCombo.getSelectedItem();
        if (location == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== SEARCH RESULTS for Location ").append(location).append(" ===\n\n");
        sb.append("Using Dijkstra's Algorithm for Shortest Path...\n");
        sb.append("Applying Priority Queue for optimal ranking...\n\n");

        List<ParkingSlot> nearestSlots = parkingManager.findNearestParking(location, 5);

        sb.append(" TOP 5 PARKING SPOTS (by distance):\n");
        sb.append("-------------------------------------------------------------\n");
        for (int i = 0; i < nearestSlots.size(); i++) {
            ParkingSlot slot = nearestSlots.get(i);
            sb.append(String.format("%d. %s\n", i+1, slot.toString()));
        }

        sb.append("\n\n BEST OVERALL SPOTS (considering distance, traffic & cost):\n");
        sb.append("-------------------------------------------------------------\n");
        List<ParkingSlot> bestSlots = parkingManager.getBestAvailableSlots(5);
        for (int i = 0; i < bestSlots.size(); i++) {
            ParkingSlot slot = bestSlots.get(i);
            sb.append(String.format("%d. %s | Score: %d\n",
                    i+1, slot.toString(), slot.getPriorityScore()));
        }

        resultsArea.setText(sb.toString());
        updateStats();
    }

    private void allocateSelectedSlot() {
        String slotId = JOptionPane.showInputDialog(this, "Enter Slot ID to allocate:");
        if (slotId != null && !slotId.trim().isEmpty()) {
            String userName = JOptionPane.showInputDialog(this, "Enter your name:");
            if (userName != null && !userName.trim().isEmpty()) {
                if (parkingManager.allocateSlot(slotId.toUpperCase(), userName)) {
                    JOptionPane.showMessageDialog(this,
                            "Slot " + slotId + " allocated successfully to " + userName + "!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    findBestParking();
                    refreshTables();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Slot not available or invalid!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private JPanel createAllSlotsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"ID", "Location", "Distance", "Traffic", "Cost/Hour", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        slotsTable = new JTable(tableModel);
        slotsTable.setRowHeight(25);
        slotsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        slotsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) tableModel.getValueAt(row, 5);
                if (!isSelected) {
                    if ("Available".equals(status)) {
                        c.setBackground(new Color(144, 238, 144));
                    } else {
                        c.setBackground(new Color(255, 182, 193));
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(slotsTable);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshTables());

        JButton freeSlotBtn = new JButton("Free Selected Slot");
        freeSlotBtn.addActionListener(e -> freeSelectedSlot());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(freeSlotBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshTables();
        return panel;
    }

    private void refreshTables() {
        tableModel.setRowCount(0);
        for (ParkingSlot slot : parkingManager.getAllSlots()) {
            tableModel.addRow(new Object[]{
                    slot.getId(),
                    slot.getLocation(),
                    String.format("%.1f km", slot.getDistanceFromUser()),
                    String.format("%.0f%%", slot.getTrafficDensity() * 100),
                    String.format("$%.2f", slot.getCostPerHour()),
                    slot.isAvailable() ? "Available" : "Occupied"
            });
        }
        updateStats();
    }

    private void freeSelectedSlot() {
        int selectedRow = slotsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String slotId = (String) tableModel.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Free slot " + slotId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (parkingManager.freeSlot(slotId)) {
                    JOptionPane.showMessageDialog(this, "Slot freed successfully!");
                    refreshTables();
                    findBestParking();
                } else {
                    JOptionPane.showMessageDialog(this, "Error freeing slot!");
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a slot first!");
        }
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Allocation History"));

        JButton refreshHistoryBtn = new JButton("Refresh History");
        refreshHistoryBtn.addActionListener(e -> refreshHistory());

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(refreshHistoryBtn, BorderLayout.SOUTH);

        refreshHistory();
        return panel;
    }

    private void refreshHistory() {
        StringBuilder sb = new StringBuilder();
        for (String record : parkingManager.getAllocationHistory()) {
            sb.append(record).append("\n");
        }
        if (sb.length() == 0) {
            sb.append("No allocations made yet.");
        }
        historyArea.setText(sb.toString());
    }

    private JPanel createAboutPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));

        JTextArea aboutText = new JTextArea(
                "ParkWise - Smart Parking Allocation System\n\n" +
                        "Version: 1.0\n\n" +
                        "This system demonstrates the practical application of Data Structures\n" +
                        "and Algorithms in solving real-world parking and traffic problems.\n\n" +
                        "Algorithms Implemented:\n" +
                        "• Dijkstra's Shortest Path Algorithm - for finding nearest parking\n" +
                        "• Priority Queue (Heap) - for ranking parking spots\n" +
                        "• Graph Data Structure - for road network representation\n" +
                        "• Weighted Scoring System - for multi-factor optimization\n\n" +
                        "Features:\n" +
                        "• Real-time parking availability tracking\n" +
                        "• Distance-based search using road networks\n" +
                        "• Traffic density consideration\n" +
                        "• Cost optimization\n" +
                        "• Historical allocation tracking\n\n" +
                        "Developed as a comprehensive demonstration of Java DSA concepts."
        );
        aboutText.setEditable(false);
        aboutText.setBackground(new Color(240, 248, 255));
        aboutText.setFont(new Font("Arial", Font.PLAIN, 13));
        aboutText.setMargin(new Insets(20, 20, 20, 20));

        panel.add(aboutText);
        return panel;
    }

    private void updateStats() {
        // Update status bar
        if (statsLabel != null) {
            statsLabel.setText(String.format("Available Slots: %d | Total Slots: %d | Allocations: %d | System Active",
                    parkingManager.getAvailableCount(),
                    parkingManager.getAllSlots().size(),
                    parkingManager.getAllocationHistory().size()));
        }

        // Update header label
        if (headerAvailableLabel != null) {
            headerAvailableLabel.setText("Available: " + parkingManager.getAvailableCount());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ParkWiseGUI().setVisible(true);
        });
    }
}