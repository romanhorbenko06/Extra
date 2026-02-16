package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


class Hyperedge {
    private final Set<Integer> nodes;
    private final int id;

    public Hyperedge(int id, Set<Integer> nodes) {
        this.id = id;
        this.nodes = new HashSet<>(nodes);
    }

    public Set<Integer> getNodes() { return nodes; }
    public int getId() { return id; }
    public boolean contains(int node) { return nodes.contains(node); }
    public int size() { return nodes.size(); }
}

class Hypergraph {
    private final int numNodes;
    private final List<Hyperedge> hyperedges;
    private final Map<Integer, Set<Integer>> nodeToEdges;

    public Hypergraph(int numNodes) {
        this.numNodes = numNodes;
        this.hyperedges = new ArrayList<>();
        this.nodeToEdges = new HashMap<>();
        for (int i = 0; i < numNodes; i++) {
            nodeToEdges.put(i, new HashSet<>());
        }
    }

    public void addHyperedge(Set<Integer> nodes) {
        int edgeId = hyperedges.size();
        Hyperedge edge = new Hyperedge(edgeId, nodes);
        hyperedges.add(edge);

        for (int node : nodes) {
            nodeToEdges.get(node).add(edgeId);
        }
    }

    public int getNumNodes() { return numNodes; }
    public List<Hyperedge> getHyperedges() { return hyperedges; }

    public Set<Integer> getNeighbors(int node) {
        Set<Integer> neighbors = new HashSet<>();
        for (int edgeId : nodeToEdges.get(node)) {
            neighbors.addAll(hyperedges.get(edgeId).getNodes());
        }
        neighbors.remove(node);
        return neighbors;
    }

    public Set<Integer> getEdgesContaining(int node) {
        return nodeToEdges.get(node);
    }
}

class HypergraphTraversal {
    private final Hypergraph graph;
    private final Set<Integer> visitedNodes;
    private final Map<String, Integer> edgeTransitions; // (from,to) -> count
    private final List<Integer> path;

    public HypergraphTraversal(Hypergraph graph) {
        this.graph = graph;
        this.visitedNodes = new HashSet<>();
        this.edgeTransitions = new HashMap<>();
        this.path = new ArrayList<>();
    }

    public List<Integer> traverse(int startNode) {
        visitedNodes.clear();
        edgeTransitions.clear();
        path.clear();

        int current = startNode;
        visitedNodes.add(current);
        path.add(current);

        while (visitedNodes.size() < graph.getNumNodes()) {
            int next = selectNextNode(current);
            if (next == -1) break;

            String transition = current + "->" + next;
            edgeTransitions.put(transition, edgeTransitions.getOrDefault(transition, 0) + 1);

            current = next;
            visitedNodes.add(current);
            path.add(current);
        }

        return new ArrayList<>(path);
    }

    private int selectNextNode(int current) {
        Set<Integer> neighbors = graph.getNeighbors(current);

        List<Integer> unvisited = new ArrayList<>();
        for (int neighbor : neighbors) {
            if (!visitedNodes.contains(neighbor)) {
                unvisited.add(neighbor);
            }
        }

        if (!unvisited.isEmpty()) {
            return selectBestUnvisited(current, unvisited);
        }

        int best = -1;
        int minTransitions = Integer.MAX_VALUE;

        for (int neighbor : neighbors) {
            String transition = current + "->" + neighbor;
            int count = edgeTransitions.getOrDefault(transition, 0);
            if (count < minTransitions) {
                minTransitions = count;
                best = neighbor;
            }
        }

        return best;
    }

    private int selectBestUnvisited(int current, List<Integer> candidates) {
        int best = candidates.get(0);
        int maxUnvisitedNeighbors = 0;

        for (int candidate : candidates) {
            Set<Integer> neighbors = graph.getNeighbors(candidate);
            int unvisitedCount = 0;
            for (int n : neighbors) {
                if (!visitedNodes.contains(n)) unvisitedCount++;
            }

            if (unvisitedCount > maxUnvisitedNeighbors) {
                maxUnvisitedNeighbors = unvisitedCount;
                best = candidate;
            }
        }

        return best;
    }

    public int getTotalTransitions() {
        return edgeTransitions.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getRepeatedTransitions() {
        return edgeTransitions.values().stream().filter(c -> c > 1).mapToInt(c -> c - 1).sum();
    }
}

class HypergraphVisualizer extends JPanel {
    private Hypergraph graph;
    private List<Integer> path;
    private Map<Integer, Point> nodePositions;
    private int currentStep = 0;

    public HypergraphVisualizer() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
        nodePositions = new HashMap<>();
    }

    public void setGraph(Hypergraph graph) {
        this.graph = graph;
        calculateNodePositions();
        repaint();
    }

    public void setPath(List<Integer> path) {
        this.path = path;
        this.currentStep = 0;
        repaint();
    }

    public void nextStep() {
        if (path != null && currentStep < path.size() - 1) {
            currentStep++;
            repaint();
        }
    }

    public void reset() {
        currentStep = 0;
        repaint();
    }

    private void calculateNodePositions() {
        if (graph == null) return;

        int n = graph.getNumNodes();
        int centerX = 400, centerY = 300;
        int radius = 200;

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            int x = centerX + (int)(radius * Math.cos(angle));
            int y = centerY + (int)(radius * Math.sin(angle));
            nodePositions.put(i, new Point(x, y));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (graph == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color[] edgeColors = {
                new Color(255, 200, 200), new Color(200, 255, 200),
                new Color(200, 200, 255), new Color(255, 255, 200),
                new Color(255, 200, 255), new Color(200, 255, 255)
        };

        for (Hyperedge edge : graph.getHyperedges()) {
            Set<Integer> nodes = edge.getNodes();
            if (nodes.size() < 2) continue;

            int cx = 0, cy = 0;
            for (int node : nodes) {
                Point p = nodePositions.get(node);
                cx += p.x;
                cy += p.y;
            }
            cx /= nodes.size();
            cy /= nodes.size();

            g2.setColor(edgeColors[edge.getId() % edgeColors.length]);
            for (int node : nodes) {
                Point p = nodePositions.get(node);
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(cx, cy, p.x, p.y);
            }

            g2.fillOval(cx - 5, cy - 5, 10, 10);
        }

        if (path != null && currentStep > 0) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
            for (int i = 0; i < Math.min(currentStep, path.size() - 1); i++) {
                Point p1 = nodePositions.get(path.get(i));
                Point p2 = nodePositions.get(path.get(i + 1));
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);

                drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
            }
        }

        for (int i = 0; i < graph.getNumNodes(); i++) {
            Point p = nodePositions.get(i);

            if (path != null && currentStep < path.size() && path.get(currentStep) == i) {
                g2.setColor(Color.GREEN);
            } else if (path != null && path.subList(0, currentStep + 1).contains(i)) {
                g2.setColor(Color.ORANGE); // Відвіданий вузол
            } else {
                g2.setColor(Color.LIGHT_GRAY);
            }

            g2.fillOval(p.x - 20, p.y - 20, 40, 40);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(p.x - 20, p.y - 20, 40, 40);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            String label = String.valueOf(i);
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(label);
            g2.drawString(label, p.x - w/2, p.y + 5);
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 10;

        int midX = (x1 + x2) / 2;
        int midY = (y1 + y2) / 2;

        int[] xPoints = {
                midX,
                midX - (int)(arrowSize * Math.cos(angle - Math.PI/6)),
                midX - (int)(arrowSize * Math.cos(angle + Math.PI/6))
        };
        int[] yPoints = {
                midY,
                midY - (int)(arrowSize * Math.sin(angle - Math.PI/6)),
                midY - (int)(arrowSize * Math.sin(angle + Math.PI/6))
        };

        g2.fillPolygon(xPoints, yPoints, 3);
    }
}

public class HypergraphTraversalApp extends JFrame {
    private Hypergraph graph;
    private HypergraphVisualizer visualizer;
    private HypergraphTraversal traversal;
    private JTextArea statsArea;
    private JButton startButton, stepButton, resetButton, generateButton;

    public HypergraphTraversalApp() {
        setTitle("Обхід гіперграфа з мінімізацією повторних відвідувань");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        visualizer = new HypergraphVisualizer();
        add(visualizer, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());
        generateButton = new JButton("Згенерувати граф");
        startButton = new JButton("Почати обхід");
        stepButton = new JButton("Наступний крок");
        resetButton = new JButton("Скинути");

        controlPanel.add(generateButton);
        controlPanel.add(startButton);
        controlPanel.add(stepButton);
        controlPanel.add(resetButton);

        add(controlPanel, BorderLayout.NORTH);

        statsArea = new JTextArea(6, 30);
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(statsArea);
        add(scrollPane, BorderLayout.SOUTH);

        generateButton.addActionListener(e -> generateGraph());
        startButton.addActionListener(e -> startTraversal());
        stepButton.addActionListener(e -> visualizer.nextStep());
        resetButton.addActionListener(e -> visualizer.reset());

        pack();
        setLocationRelativeTo(null);

        generateGraph();
    }

    private void generateGraph() {
        Random rand = new Random();
        int numNodes = 8;
        graph = new Hypergraph(numNodes);

        for (int i = 0; i < 6; i++) {
            int edgeSize = 2 + rand.nextInt(3); // 2-4 вузли
            Set<Integer> nodes = new HashSet<>();
            while (nodes.size() < edgeSize) {
                nodes.add(rand.nextInt(numNodes));
            }
            graph.addHyperedge(nodes);
        }

        visualizer.setGraph(graph);
        statsArea.setText("Граф згенеровано!\nВузлів: " + numNodes +
                "\nГіперребер: " + graph.getHyperedges().size());
    }

    private void startTraversal() {
        traversal = new HypergraphTraversal(graph);
        List<Integer> path = traversal.traverse(0);

        visualizer.setPath(path);

        StringBuilder stats = new StringBuilder();
        stats.append("=== Результати обходу ===\n");
        stats.append("Шлях: ").append(path).append("\n");
        stats.append("Відвідано вузлів: ").append(path.size()).append("/").append(graph.getNumNodes()).append("\n");
        stats.append("Всього переходів: ").append(traversal.getTotalTransitions()).append("\n");
        stats.append("Повторних переходів: ").append(traversal.getRepeatedTransitions()).append("\n");
        stats.append("\nНатисніть 'Наступний крок' для візуалізації");

        statsArea.setText(stats.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HypergraphTraversalApp().setVisible(true);
        });
    }
}