package main.view;

import main.controller.AIClient;
import main.model.Choice;
import main.model.DBManager;
import main.model.ProCon;
import main.model.Decision;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class WrapLayout extends FlowLayout {
    private Dimension preferredLayoutSize;

    public WrapLayout() {
        super(FlowLayout.LEFT, 10, 10);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        Dimension preferred = layoutSize(target, true);
        preferredLayoutSize = preferred;
        return preferred;
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getParent().getWidth();
            if (targetWidth == 0) {
                targetWidth = target.getParent().getParent().getWidth();
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();
            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = d.width;
                        rowHeight = d.height;
                    } else {
                        rowWidth += d.width + hgap;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
            }

            dim.width = Math.max(dim.width, rowWidth);
            dim.height += rowHeight + insets.top + insets.bottom;

            return dim;
        }
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getParent().getWidth();
            if (targetWidth == 0) {
                targetWidth = target.getParent().getParent().getWidth();
            }

            Insets insets = target.getInsets();
            int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);
            int x = insets.left + getHgap();
            int y = insets.top;
            int rowHeight = 0;
            int start = 0;

            int nmembers = target.getComponentCount();
            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();
                    m.setSize(d.width, d.height);

                    if (x + d.width > maxWidth) {
                        layoutRow(target, insets.left + getHgap(), y, rowHeight, start, i);
                        x = insets.left + getHgap();
                        y += rowHeight + getVgap();
                        rowHeight = d.height;
                        start = i;
                    } else {
                        x += d.width + getHgap();
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
            }
            layoutRow(target, insets.left + getHgap(), y, rowHeight, start, nmembers);
        }
    }

    private void layoutRow(Container target, int x, int y, int height, int start, int end) {
        int hgap = getHgap();
        for (int i = start; i < end; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                m.setLocation(x, y);
                x += m.getWidth() + hgap;
            }
        }
    }
}

public class MainWindow extends JFrame {
    private final DBManager dbManager;
    private final AIClient aiClient;
    private JPanel choicesPanel;
    private JTextField newChoiceField;
    private List<Choice> selectedChoices = new ArrayList<>();
    private Decision currentDecision; // Track the current decision

    public MainWindow() {
        this.dbManager = new DBManager();
        this.aiClient = new AIClient();
        
        setTitle("DecisionWise");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        initializeUI();
        // Do not load any choices or decision by default
        // User must select from history or create new
    }

    private void initializeUI() {
        // Top toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JButton newDecisionBtn = new JButton("New Decision");
        JButton compareBtn = new JButton("Compare Selected");
        JButton deleteSelectedBtn = new JButton("Delete Selected");
        JButton historyBtn = new JButton("History");
        
        toolbar.add(newDecisionBtn);
        toolbar.add(compareBtn);
        toolbar.add(deleteSelectedBtn);
        toolbar.add(historyBtn);

        // New choice input panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        inputPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        newChoiceField = new JTextField();
        newChoiceField.setPreferredSize(new Dimension(250, 28));
        JButton addChoiceBtn = new JButton("Add Choice");
        addChoiceBtn.setPreferredSize(new Dimension(100, 28));
        
        inputPanel.add(newChoiceField);
        inputPanel.add(addChoiceBtn);

        // Choices panel with grid layout (2 columns)
        choicesPanel = new JPanel(new GridLayout(0, 2, 20, 20));
        choicesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(choicesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setBorder(null);

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(toolbar, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);
        
        // Adjust the split between input and cards
        scrollPane.setPreferredSize(new Dimension(getWidth(), 680));

        setLayout(new BorderLayout());
        add(mainPanel);

        // Event handlers
        addChoiceBtn.addActionListener(e -> addNewChoice());
        newDecisionBtn.addActionListener(e -> startNewDecision());
        compareBtn.addActionListener(e -> compareSelectedChoices());
        deleteSelectedBtn.addActionListener(e -> deleteSelectedChoices());
        newChoiceField.addActionListener(e -> addNewChoice());
        historyBtn.addActionListener(e -> openDecisionHistory());

        // Add component listener to handle resize
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                updateScrollPanePolicy();
            }
        });
    }

    private void updateScrollPanePolicy() {
        Component[] components = choicesPanel.getComponents();
        Container parent = choicesPanel.getParent();
        if (parent != null) {
            Container scrollPane = parent.getParent();
            if (scrollPane instanceof JScrollPane) {
                if (components.length > 4) { // Show scroll when more than 2 rows (4 cards)
                    ((JScrollPane) scrollPane).setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                } else {
                    ((JScrollPane) scrollPane).setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
                }
            }
        }
    }

    private void startNewDecision() {
        String title = JOptionPane.showInputDialog(this, "Enter a name for your new decision:", "New Decision", JOptionPane.PLAIN_MESSAGE);
        if (title != null && !title.trim().isEmpty()) {
            Decision decision = new Decision(title.trim());
            decision = dbManager.createDecision(decision);
            if (decision != null && decision.getId() > 0) {
                currentDecision = decision;
                clearChoices();
                setTitle("DecisionWise - " + decision.getTitle());
            }
        }
    }

    private void openDecisionHistory() {
        DecisionHistoryDialog dialog = new DecisionHistoryDialog(this, dbManager);
        dialog.setVisible(true);
        Decision selected = dialog.getSelectedDecision();
        if (selected != null) {
            currentDecision = selected;
            loadChoicesForDecision(selected.getId());
            setTitle("DecisionWise - " + selected.getTitle());
        }
    }

    private void loadChoicesForDecision(int decisionId) {
        choicesPanel.removeAll();
        List<Choice> choices = dbManager.getChoicesForDecision(decisionId);
        if (choices.isEmpty()) {
            // Show an empty page (no cards, no message)
            choicesPanel.revalidate();
            choicesPanel.repaint();
            updateScrollPanePolicy();
            return;
        }
        choices.forEach(this::addChoiceCard);
        updateScrollPanePolicy();
    }

    private void addNewChoice() {
        if (currentDecision == null) {
            JOptionPane.showMessageDialog(this, "Please start a new decision first.", "No Decision Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String title = newChoiceField.getText().trim();
        if (!title.isEmpty()) {
            Choice choice = new Choice(title, currentDecision.getId());
            // Generate pros and cons using AI
            List<ProCon> prosAndCons = aiClient.generateProsAndCons(title);
            for (ProCon proCon : prosAndCons) {
                choice.addProCon(proCon);
            }
            // Save to database
            choice = dbManager.saveChoice(choice);
            if (choice != null && choice.getId() > 0) {
                addChoiceCard(choice);
                newChoiceField.setText("");
            }
        }
    }

    private void addChoiceCard(Choice choice) {
        ChoiceCard card = new ChoiceCard(choice, dbManager, selected -> {
            if (selected) {
                selectedChoices.add(choice);
            } else {
                selectedChoices.remove(choice);
            }
        });
        
        // Set fixed, wider size for the card
        card.setPreferredSize(new Dimension(550, 300));
        card.setMaximumSize(new Dimension(550, 300));
        
        choicesPanel.add(card);
        choicesPanel.revalidate();
        choicesPanel.repaint();
        updateScrollPanePolicy();
    }

    private void loadChoices() {
        choicesPanel.removeAll();
        List<Choice> choices = dbManager.getAllChoices();
        choices.forEach(this::addChoiceCard);
        updateScrollPanePolicy();
    }

    private void clearChoices() {
        choicesPanel.removeAll();
        choicesPanel.revalidate();
        choicesPanel.repaint();
        selectedChoices.clear();
        updateScrollPanePolicy();
    }

    private void compareSelectedChoices() {
        if (selectedChoices.size() != 2) {
            JOptionPane.showMessageDialog(this,
                "Please select exactly 2 choices to compare.",
                "Compare Choices",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Choice choice1 = selectedChoices.get(0);
        Choice choice2 = selectedChoices.get(1);
        List<ProCon> comparison = aiClient.compareChoices(choice1, choice2);
        
        StringBuilder message = new StringBuilder();
        message.append("Comparison between ").append(choice1.getTitle())
               .append(" and ").append(choice2.getTitle()).append(":\n\n");
        
        for (ProCon proCon : comparison) {
            message.append(proCon.getType() == ProCon.Type.PRO ? "✓ " : "✗ ")
                   .append(proCon.getContent()).append("\n");
        }
        
        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(this,
            scrollPane,
            "Comparison Results",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedChoices() {
        if (selectedChoices.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select at least one choice to delete.",
                "Delete Choices",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete " + selectedChoices.size() + " selected choice(s)?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            // Create a copy of the list to avoid concurrent modification
            List<Choice> choicesToDelete = new ArrayList<>(selectedChoices);
            
            // Clear the selection first
            selectedChoices.clear();
            
            // Delete each choice
            for (Choice choice : choicesToDelete) {
                dbManager.deleteChoice(choice.getId());
            }
            
            // Refresh the UI for the current decision only
            loadChoicesForDecision(currentDecision != null ? currentDecision.getId() : -1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainWindow().setVisible(true);
        });
    }
} 