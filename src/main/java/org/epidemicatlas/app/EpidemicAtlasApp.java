package org.epidemicatlas.app;

import org.epidemicatlas.engine.Lattice;
import org.epidemicatlas.engine.SirParameters;
import org.epidemicatlas.engine.SirSimulator;
import org.epidemicatlas.engine.SpatialParameters;
import org.epidemicatlas.engine.SpatialSimulator;
import org.epidemicatlas.engine.SpatialState;
import org.epidemicatlas.export.CsvExporter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class EpidemicAtlasApp {
    private static final Color PAGE = new Color(242, 246, 250);
    private static final Color SURFACE = Color.WHITE;
    private static final Color LINE = new Color(225, 232, 239);
    private static final Color INK = new Color(27, 43, 58);
    private static final Color MUTED = new Color(113, 129, 145);
    private static final Color BLUE = new Color(40, 119, 164);
    private static final Color TEAL = new Color(33, 143, 132);
    private static final Color RED = new Color(211, 85, 79);
    private static final Color GREEN = new Color(52, 148, 112);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font HEADING_FONT = new Font("Segoe UI", Font.BOLD, 18);

    private final JFrame frame = new JFrame("Epidemic Atlas");
    private final JTextField populationField = new JTextField("24000", 8);
    private final JTextField infectedField = new JTextField("20", 8);
    private final JTextField recoveredField = new JTextField("0", 8);
    private final JTextField betaField = new JTextField("0.32", 8);
    private final JTextField gammaField = new JTextField("0.10", 8);
    private final JTextField movementField = new JTextField("0.08", 8);
    private final JTextField durationField = new JTextField("120", 8);
    private final JTextField stepField = new JTextField("0.2", 8);
    private final JSlider playbackSpeed = new JSlider(1, 10, 5);
    private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(32, 1, 200, 1));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(22, 1, 200, 1));
    private final JComboBox<String> variableChoice = new JComboBox<>(
            new String[] {"Infected (count)", "Infected (proportion)", "Susceptible (proportion)",
                    "Recovered (proportion)", "Population"});
    private final JLabel status = new JLabel("Ready — click a cell to choose the initial infection location.");
    private final JLabel inspector = new JLabel();
    private final JLabel dayMetric = new JLabel("0");
    private final JLabel infectedMetric = new JLabel("20");
    private final JLabel r0Metric = new JLabel("3.2");
    private final JLabel runIndicator = new JLabel(" READY ");
    private JButton runButton;
    private final GridCanvas gridCanvas = new GridCanvas();
    private final TrajectoryChart chart = new TrajectoryChart();
    private final TrajectoryChart comparisonChart = new TrajectoryChart();
    private final Timer timer = new Timer(70, event -> advance());
    private SpatialParameters parameters;
    private SpatialSimulator simulator;
    private SpatialState current;
    private final List<double[]> history = new ArrayList<>();
    private int seedX = 16;
    private int seedY = 11;
    private int selectedX = 16;
    private int selectedY = 11;

    private EpidemicAtlasApp() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            showError("Could not initialize the native desktop theme.", exception);
        }

        buildWindow();
        resetSimulation();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EpidemicAtlasApp app = new EpidemicAtlasApp();
            app.frame.setVisible(true);
        });
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int availableWidth = Math.max(760, screen.width - 80);
        int availableHeight = Math.max(520, screen.height - 80);
        Dimension minimumSize = new Dimension(Math.min(1080, availableWidth), Math.min(680, availableHeight));
        frame.setMinimumSize(minimumSize);
        frame.setSize(Math.min(1500, Math.max(minimumSize.width, availableWidth)),
                Math.min(960, Math.max(minimumSize.height, availableHeight)));
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        root.setBackground(PAGE);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildMain(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        frame.setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        BrandMark mark = new BrandMark();
        JPanel brand = new JPanel(new BorderLayout(13, 0));
        brand.setOpaque(false);
        JLabel title = new JLabel("Epidemic Atlas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("SPATIAL EPIDEMIC LABORATORY");
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        subtitle.setForeground(MUTED);
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 2));
        words.setOpaque(false);
        words.add(title);
        words.add(subtitle);
        brand.add(mark, BorderLayout.WEST);
        brand.add(words, BorderLayout.CENTER);
        JPanel context = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 9));
        context.setOpaque(false);
        JLabel offline = badge("OFFLINE READY", new Color(228, 244, 239), new Color(36, 124, 101));
        JLabel edu = badge("EDUCATIONAL MODEL", new Color(233, 239, 248), BLUE);
        context.add(offline);
        context.add(edu);
        header.add(brand, BorderLayout.WEST);
        header.add(context, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMain() {
        JPanel controls = buildControls();
        controls.setPreferredSize(new Dimension(340, 600));
        JTabbedPane workspace = new JTabbedPane();
        workspace.addTab("Spatial simulator", buildSpatialTab());
        workspace.addTab("Laplacian explorer", buildLaplacianTab());
        workspace.addTab("Model comparison", buildComparisonTab());
        workspace.setFont(new Font("Segoe UI", Font.BOLD, 13));
        workspace.setForeground(INK);
        workspace.setBackground(PAGE);
        workspace.setBorder(BorderFactory.createEmptyBorder());
        workspace.setMinimumSize(new Dimension(0, 0));
        workspace.setPreferredSize(new Dimension(660, 600));
        workspace.setUI(new BasicTabbedPaneUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                contentBorderInsets = new Insets(0, 0, 0, 0);
                tabAreaInsets = new Insets(0, 0, 10, 0);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override protected void paintTabBackground(Graphics graphics, int tabPlacement, int tabIndex,
                                                          int x, int y, int width, int height,
                                                          boolean isSelected) {
                graphics.setColor(isSelected ? SURFACE : PAGE);
                graphics.fillRoundRect(x + 2, y + 1, width - 4, height - 2, 12, 12);
            }

            @Override protected void paintTabBorder(Graphics graphics, int tabPlacement, int tabIndex,
                                                       int x, int y, int width, int height,
                                                       boolean isSelected) { }

            @Override protected void paintContentBorder(Graphics graphics, int tabPlacement, int selectedIndex) { }
        });
        JPanel content = new JPanel(new BorderLayout(14, 0));
        content.setOpaque(false);
        content.setMinimumSize(new Dimension(0, 0));
        content.setPreferredSize(new Dimension(1020, 600));
        content.add(controls, BorderLayout.WEST);
        content.add(workspace, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildControls() {
        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 10));

        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        JLabel eyebrow = new JLabel("BUILD YOUR EXPERIMENT");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 10));
        eyebrow.setForeground(TEAL);
        JLabel title = new JLabel("Model setup");
        title.setFont(HEADING_FONT);
        title.setForeground(INK);
        heading.add(eyebrow, BorderLayout.NORTH);
        heading.add(title, BorderLayout.CENTER);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading);
        content.add(BoxSpacer.vertical(14));

        JPanel transport = new JPanel(new GridLayout(1, 2, 9, 0));
        transport.setOpaque(false);
        runButton = button("Run", TEAL);
        runButton.addActionListener(event -> {
            if (timer.isRunning()) {
                timer.stop();
                status.setText("Paused at day " + format(current.time()) + ".");
                runButton.setText("Resume");
                runIndicator.setText("  PAUSED  ");
                runIndicator.setForeground(new Color(171, 111, 44));
            } else if (current.time() < parameters.duration()) {
                timer.start();
                status.setText("Simulation running from computed state.");
                runButton.setText("Pause");
                runIndicator.setText("  RUNNING  ");
                runIndicator.setForeground(TEAL);
            }
        });
        JButton step = button("Step forward", new Color(238, 243, 248), INK);
        step.addActionListener(event -> advance());
        transport.add(runButton);
        transport.add(step);
        transport.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(transport);
        content.add(BoxSpacer.vertical(12));

        content.add(sectionLabel("MODEL & INITIAL CONDITIONS"));
        content.add(BoxSpacer.vertical(7));
        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 10));
        fields.setOpaque(false);
        addField(fields, "GRID WIDTH", widthSpinner);
        addField(fields, "GRID HEIGHT", heightSpinner);
        addField(fields, "POPULATION", populationField);
        addField(fields, "INFECTED", infectedField);
        addField(fields, "RECOVERED", recoveredField);
        addField(fields, "TRANSMISSION β", betaField);
        addField(fields, "RECOVERY γ", gammaField);
        addField(fields, "MOVEMENT", movementField);
        addField(fields, "DURATION (DAYS)", durationField);
        addField(fields, "TIME STEP (DAYS)", stepField);
        fields.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(fields);
        content.add(BoxSpacer.vertical(18));

        JButton reset = button("Apply parameters", BLUE);
        reset.addActionListener(event -> resetSimulation());
        reset.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(reset);
        content.add(BoxSpacer.vertical(9));

        JLabel speedLabel = sectionLabel("PLAYBACK SPEED");
        content.add(speedLabel);
        content.add(BoxSpacer.vertical(2));
        playbackSpeed.setToolTipText("Playback interval only; changing speed does not alter model time steps.");
        playbackSpeed.addChangeListener(event -> timer.setDelay(605 - playbackSpeed.getValue() * 55));
        playbackSpeed.setOpaque(false);
        playbackSpeed.setAlignmentX(Component.LEFT_ALIGNMENT);
        playbackSpeed.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        content.add(playbackSpeed);
        content.add(BoxSpacer.vertical(18));

        JLabel dataHeading = sectionLabel("SCENARIO & EXPORT");
        content.add(dataHeading);
        content.add(BoxSpacer.vertical(8));
        JPanel utilities = new JPanel(new GridLayout(0, 2, 8, 8));
        utilities.setOpaque(false);
        JButton compare = button("Compare models", new Color(239, 235, 248), new Color(105, 79, 145));
        compare.addActionListener(event -> compareModels());
        JButton save = button("Save scenario", new Color(238, 243, 248), INK);
        save.addActionListener(event -> saveScenario());
        JButton load = button("Load scenario", new Color(238, 243, 248), INK);
        load.addActionListener(event -> loadScenario());
        JButton export = button("Export series", new Color(238, 243, 248), INK);
        export.addActionListener(event -> exportSeries());
        JButton gridExport = button("Export grid", new Color(238, 243, 248), INK);
        gridExport.addActionListener(event -> exportGrid());
        utilities.add(compare);
        utilities.add(save);
        utilities.add(load);
        utilities.add(export);
        utilities.add(gridExport);
        utilities.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(utilities);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(17, 16, 16, 12)));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSpatialTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        panel.setMinimumSize(new Dimension(0, 0));
        JPanel pageHeader = new JPanel(new BorderLayout(0, 4));
        pageHeader.setOpaque(false);
        JLabel title = new JLabel("Spatial simulator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Explore how local infection and neighbour movement shape the outbreak.");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(MUTED);
        pageHeader.add(title, BorderLayout.NORTH);
        pageHeader.add(subtitle, BorderLayout.CENTER);
        panel.add(pageHeader, BorderLayout.NORTH);

        JPanel mapAndChart = new JPanel(new BorderLayout(0, 10));
        mapAndChart.setOpaque(false);
        mapAndChart.setMinimumSize(new Dimension(0, 0));
        JPanel mapAndDetails = new JPanel(new BorderLayout(10, 0));
        mapAndDetails.setOpaque(false);
        mapAndDetails.setMinimumSize(new Dimension(0, 0));

        CardPanel mapCard = new CardPanel();
        mapCard.setLayout(new BorderLayout(0, 10));
        mapCard.setMinimumSize(new Dimension(0, 0));
        mapCard.setPreferredSize(new Dimension(430, 300));
        mapCard.setBorder(BorderFactory.createEmptyBorder(15, 17, 15, 17));
        JPanel mapHeader = new JPanel(new BorderLayout(12, 0));
        mapHeader.setOpaque(false);
        JPanel mapLabels = new JPanel(new GridLayout(2, 1, 0, 3));
        mapLabels.setOpaque(false);
        JLabel mapTitle = new JLabel("Population grid");
        mapTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        mapTitle.setForeground(INK);
        JLabel mapSub = new JLabel("Select a cell to inspect or set the seed");
        mapSub.setFont(SMALL_FONT);
        mapSub.setForeground(MUTED);
        mapLabels.add(mapTitle);
        mapLabels.add(mapSub);
        JPanel mapTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        mapTools.setOpaque(false);
        styleInput(variableChoice);
        variableChoice.setPreferredSize(new Dimension(150, 28));
        JLabel mapVariableLabel = new JLabel("DISPLAY");
        mapVariableLabel.setFont(new Font("Segoe UI", Font.BOLD, 9));
        mapVariableLabel.setForeground(MUTED);
        mapTools.add(mapVariableLabel);
        mapTools.add(variableChoice);
        LegendBar legend = new LegendBar();
        variableChoice.addActionListener(event -> {
            gridCanvas.repaint();
            legend.repaint();
        });
        mapTools.add(legend);
        mapHeader.add(mapLabels, BorderLayout.WEST);
        mapHeader.add(mapTools, BorderLayout.EAST);
        mapCard.add(mapHeader, BorderLayout.NORTH);
        mapCard.add(gridCanvas, BorderLayout.CENTER);
        mapAndDetails.add(mapCard, BorderLayout.CENTER);

        CardPanel chartCard = new CardPanel();
        chartCard.setLayout(new BorderLayout());
        chartCard.setMinimumSize(new Dimension(0, 0));
        chartCard.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        chartCard.setPreferredSize(new Dimension(320, 145));
        chartCard.add(chart, BorderLayout.CENTER);

        CardPanel right = new CardPanel();
        right.setLayout(new BorderLayout(0, 12));
        right.setMinimumSize(new Dimension(0, 0));
        right.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        right.setPreferredSize(new Dimension(210, 300));
        JPanel detailsHeading = new JPanel(new GridLayout(2, 1, 0, 4));
        detailsHeading.setOpaque(false);
        JLabel cellTitle = new JLabel("Cell details");
        cellTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cellTitle.setForeground(INK);
        JLabel cellSubtitle = new JLabel("LIVE SIMULATION STATE");
        cellSubtitle.setFont(new Font("Segoe UI", Font.BOLD, 9));
        cellSubtitle.setForeground(TEAL);
        detailsHeading.add(cellTitle);
        detailsHeading.add(cellSubtitle);
        right.add(detailsHeading, BorderLayout.NORTH);
        inspector.setVerticalAlignment(JLabel.TOP);
        right.add(inspector, BorderLayout.CENTER);
        mapAndDetails.add(right, BorderLayout.EAST);
        mapAndChart.add(mapAndDetails, BorderLayout.CENTER);
        mapAndChart.add(chartCard, BorderLayout.SOUTH);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 10, 0));
        metrics.setOpaque(false);
        metrics.add(metricCard("SIMULATION DAY", dayMetric, BLUE));
        metrics.add(metricCard("TOTAL INFECTED", infectedMetric, RED));
        metrics.add(metricCard("BASIC R₀", r0Metric, TEAL));
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setMinimumSize(new Dimension(0, 0));
        center.add(metrics, BorderLayout.NORTH);
        center.add(mapAndChart, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLaplacianTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        JLabel title = new JLabel("Laplacian explorer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Understand spatial flow through the difference between a cell and its neighbours.");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(MUTED);
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);
        CardPanel explanation = new CardPanel();
        explanation.setLayout(new BorderLayout());
        explanation.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JLabel info = new JLabel("<html><div style='font-family:Segoe UI; font-size:13px; color:#1b2b3a;'>"
                + "<h2 style='margin-top:0;'>Neighbours minus me</h2>"
                + "<p>For infected count I on this four-neighbour grid, "
                + "<b>ΔI = Σ(neighbour − selected cell)</b>. Existing neighbours only are included, "
                + "so the edge is no-flux: cells outside the map do not exchange people.</p>"
                + "<p>A positive value means neighbouring cells have more infections on balance; "
                + "a negative value means the selected cell is higher. The movement rate scales this "
                + "same conservative graph flux for each compartment.</p>"
                + "<p>Choose a cell in the Spatial simulator to see its real neighbour values, individual "
                + "contributions, calculated Laplacian, and movement effect.</p></div></html>");
        explanation.add(info, BorderLayout.CENTER);
        JLabel values = new JLabel();
        values.setName("laplacianValues");
        values.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        CardPanel result = new CardPanel();
        result.setLayout(new BorderLayout());
        result.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JLabel resultHeading = sectionLabel("LIVE CELL CALCULATION");
        resultHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        result.add(resultHeading, BorderLayout.NORTH);
        result.add(values, BorderLayout.CENTER);
        JPanel body = new JPanel(new GridLayout(2, 1, 0, 14));
        body.setOpaque(false);
        body.add(explanation);
        body.add(result);
        panel.add(body, BorderLayout.CENTER);
        panel.putClientProperty("valuesLabel", values);
        return panel;
    }

    private JPanel buildComparisonTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        JLabel title = new JLabel("Model comparison");
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Compare aggregate and spatial assumptions using the same initial totals.");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(MUTED);
        heading.add(title);
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);
        CardPanel explanation = new CardPanel();
        explanation.setLayout(new BorderLayout());
        explanation.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel description = new JLabel("<html><div style='font-family:Segoe UI; font-size:12px; color:#1b2b3a;'>"
                + "<b>Three computed models</b> &nbsp; Well-mixed SIR · Independent cells · Spatially coupled"
                + "<br>Use <b>Compare models</b> to run all three with the same "
                + "population, initial infected/recovered totals, β, γ, duration, and time step.<br>"
                + "The plotted infection trajectories come from engine states. Movement changes timing and "
                + "geography, but does not guarantee every epidemic metric increases.</div></html>");
        explanation.add(description, BorderLayout.CENTER);
        CardPanel plot = new CardPanel();
        plot.setLayout(new BorderLayout());
        plot.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        plot.add(comparisonChart, BorderLayout.CENTER);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(explanation, BorderLayout.NORTH);
        center.add(plot, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private static void addField(JPanel panel, String label, Component field) {
        JPanel tile = new JPanel(new BorderLayout(0, 5));
        tile.setBackground(new Color(247, 249, 252));
        tile.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(
                new Color(235, 239, 244)), BorderFactory.createEmptyBorder(8, 9, 8, 9)));
        JLabel text = new JLabel(label);
        text.setFont(new Font("Segoe UI", Font.BOLD, 9));
        text.setForeground(MUTED);
        text.setToolTipText(label);
        tile.add(text, BorderLayout.NORTH);
        styleInput(field);
        tile.add(field, BorderLayout.CENTER);
        tile.setPreferredSize(new Dimension(136, 60));
        panel.add(tile);
    }

    private static JButton button(String text, Color color) {
        return button(text, color, Color.WHITE);
    }

    private static JButton button(String text, Color color, Color foreground) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getBackground();
                if (getModel().isPressed()) fill = shade(fill, -20);
                else if (getModel().isRollover()) fill = shade(fill, -8);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setUI(new BasicButtonUI());
        button.setBackground(color);
        button.setForeground(foreground);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(120, 40));
        button.setToolTipText(text);
        return button;
    }

    private static Color shade(Color color, int amount) {
        return new Color(Math.max(0, Math.min(255, color.getRed() + amount)),
                Math.max(0, Math.min(255, color.getGreen() + amount)),
                Math.max(0, Math.min(255, color.getBlue() + amount)));
    }

    private static void styleInput(Component component) {
        if (component instanceof JTextField field) {
            field.setFont(new Font("Segoe UI", Font.BOLD, 13));
            field.setForeground(INK);
            field.setBackground(new Color(247, 249, 252));
            field.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
            field.setColumns(7);
        } else if (component instanceof JSpinner spinner) {
            spinner.setFont(new Font("Segoe UI", Font.BOLD, 13));
            spinner.setBackground(new Color(247, 249, 252));
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
                defaultEditor.getTextField().setFont(new Font("Segoe UI", Font.BOLD, 13));
                defaultEditor.getTextField().setForeground(INK);
                defaultEditor.getTextField().setBackground(new Color(247, 249, 252));
                defaultEditor.getTextField().setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
            }
        } else if (component instanceof JComboBox<?> combo) {
            combo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            combo.setForeground(INK);
            combo.setBackground(SURFACE);
            combo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                    BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        }
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(3, 2, 0, 2));
        runIndicator.setFont(new Font("Segoe UI", Font.BOLD, 9));
        runIndicator.setForeground(new Color(36, 124, 101));
        status.setFont(SMALL_FONT);
        status.setForeground(MUTED);
        bar.add(runIndicator, BorderLayout.WEST);
        bar.add(status, BorderLayout.CENTER);
        JLabel version = new JLabel("SIR  ·  RK4  ·  NO-FLUX GRID");
        version.setFont(new Font("Segoe UI", Font.BOLD, 9));
        version.setForeground(MUTED);
        bar.add(version, BorderLayout.EAST);
        return bar;
    }

    private static JLabel badge(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 9));
        label.setForeground(foreground);
        label.setOpaque(true);
        label.setBackground(background);
        label.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        return label;
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(MUTED);
        return label;
    }

    private static JPanel metricCard(String title, JLabel value, Color accent) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 3));
        card.setBorder(BorderFactory.createEmptyBorder(10, 13, 10, 13));
        JLabel caption = new JLabel(title);
        caption.setFont(new Font("Segoe UI", Font.BOLD, 9));
        caption.setForeground(MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 20));
        value.setForeground(accent);
        card.add(caption, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(120, 58));
        return card;
    }

    private static final class BoxSpacer {
        private BoxSpacer() { }
        private static Component vertical(int height) {
            return javax.swing.Box.createRigidArea(new Dimension(1, height));
        }
    }

    private static final class CardPanel extends JPanel {
        private CardPanel() {
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(SURFACE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.setColor(LINE);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class BrandMark extends JPanel {
        private BrandMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(46, 46));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(28, 57, 75));
            g.fillRoundRect(0, 0, 44, 44, 13, 13);
            g.setColor(new Color(110, 190, 188));
            g.fillRoundRect(9, 9, 9, 9, 3, 3);
            g.fillRoundRect(26, 9, 9, 9, 3, 3);
            g.fillRoundRect(9, 26, 9, 9, 3, 3);
            g.fillRoundRect(26, 26, 9, 9, 3, 3);
            g.setColor(new Color(237, 114, 94));
            g.fillOval(20, 20, 9, 9);
            g.dispose();
        }
    }
    private boolean resetSimulation() {
        try {
            timer.stop();
            int width = (Integer) widthSpinner.getValue();
            int height = (Integer) heightSpinner.getValue();
            double population = number(populationField, "Total population");
            if (population <= 0) throw new IllegalArgumentException("Total population must be greater than zero.");
            double infectedTotal = number(infectedField, "Initial infected");
            double recoveredTotal = number(recoveredField, "Initial recovered");
            double beta = number(betaField, "Transmission rate");
            double gamma = number(gammaField, "Recovery rate");
            double movement = number(movementField, "Movement rate");
            double duration = number(durationField, "Duration");
            double timeStep = number(stepField, "Time step");
            double[] people = new double[width * height];
            double[] infected = new double[people.length];
            double[] recovered = new double[people.length];
            java.util.Arrays.fill(people, population / people.length);
            int seed = boundedSeed(width, height);
            if (infectedTotal > people[seed] + 1e-12)
                throw new IllegalArgumentException("The selected seed cell contains less population than initial infected. "
                        + "Choose a larger cell population, smaller grid, or fewer initial infections.");
            infected[seed] = infectedTotal;
            double perCellRecovered = recoveredTotal / people.length;
            java.util.Arrays.fill(recovered, perCellRecovered);
            if (infected[seed] + recovered[seed] > people[seed] + 1e-12)
                throw new IllegalArgumentException("Initial infected and recovered exceed population in the seed cell.");
            parameters = new SpatialParameters(width, height, people, infected, recovered,
                    beta, gamma, movement, duration, timeStep);
            simulator = new SpatialSimulator(parameters);
            current = simulator.state();
            status.setForeground(MUTED);
            runButton.setText("Run");
            runIndicator.setText(" READY ");
            runIndicator.setForeground(new Color(36, 124, 101));
            dayMetric.setText("0");
            infectedMetric.setText(format(infectedTotal));
            r0Metric.setText(format(reproductionNumber(beta, gamma)));
            history.clear();
            history.add(timeSeriesRow(current));
            chart.setSeries(List.of(new TrajectoryChart.Series("Susceptible", BLUE),
                    new TrajectoryChart.Series("Infected", RED),
                    new TrajectoryChart.Series("Recovered", GREEN)));
            chart.add(new double[] {current.totalSusceptible(), current.totalInfected(), current.totalRecovered()},
                    current.time());
            seedX = Math.min(seedX, width - 1);
            seedY = Math.min(seedY, height - 1);
            selectedX = seedX;
            selectedY = seedY;
            updateDisplay();
            status.setText("Reset complete. No-flux boundary; people are continuous model quantities.");
            return true;
        } catch (RuntimeException exception) {
            showError("Could not apply parameters. " + exception.getMessage(), exception);
            return false;
        }
    }

    private int boundedSeed(int width, int height) {
        seedX = Math.max(0, Math.min(seedX, width - 1));
        seedY = Math.max(0, Math.min(seedY, height - 1));
        return seedY * width + seedX;
    }

    private void advance() {
        if (simulator == null || simulator.finished()) {
            timer.stop();
            return;
        }
        try {
            current = simulator.step();
            history.add(timeSeriesRow(current));
            chart.add(new double[] {current.totalSusceptible(), current.totalInfected(), current.totalRecovered()},
                    current.time());
            updateDisplay();
            if (simulator.finished()) {
                timer.stop();
                runButton.setText("Complete");
                runIndicator.setText(" COMPLETE ");
                runIndicator.setForeground(GREEN);
                double peak = history.stream().mapToDouble(row -> row[2]).max().orElse(0);
                double peakTime = history.stream().filter(row -> row[2] == peak).findFirst().orElse(history.get(0))[0];
                status.setText("Completed at day " + format(current.time()) + ". Peak infected "
                        + format(peak) + " at day " + format(peakTime) + "; final size "
                        + format(parametersTotalPopulation() - current.totalSusceptible())
                        + "; R₀ " + format(reproductionNumber(parameters.beta(), parameters.gamma()))
                        + "; population balance " + format(current.totalPopulation() - history.get(0)[4]) + ".");
            }
        } catch (RuntimeException exception) {
            timer.stop();
            runButton.setText("Resume");
            runIndicator.setText("  PAUSED  ");
            runIndicator.setForeground(new Color(171, 111, 44));
            showError("Simulation stopped: " + exception.getMessage(), exception);
        }
    }

    private void updateDisplay() {
        gridCanvas.repaint();
        dayMetric.setText(format(current.time()));
        infectedMetric.setText(format(current.totalInfected()));
        r0Metric.setText(format(reproductionNumber(parameters.beta(), parameters.gamma())));
        int c = selectedY * parameters.width() + selectedX;
        double n = current.total(c);
        double[] infectionValues = current.infectedValues();
        double laplacian = Lattice.laplacian(infectionValues, parameters.width(), parameters.height(),
                selectedX, selectedY);
        StringBuilder text = new StringBuilder("<html><div style='font-family:Segoe UI; font-size:11px; color:#1b2b3a;'>")
                .append("<b>Cell ").append(selectedX).append(", ").append(selectedY)
                .append("</b> &nbsp;·&nbsp; Day ").append(format(current.time()))
                .append("<br>Population ").append(format(n)).append("<br>")
                .append("<b style='color:#2877a4;'>S</b> ").append(format(current.susceptible(c)))
                .append(" (").append(format(proportion(current.susceptible(c), n) * 100)).append("%)<br>")
                .append("<b style='color:#d3554f;'>I</b> ").append(format(current.infected(c)))
                .append(" (").append(format(proportion(current.infected(c), n) * 100)).append("%)<br>")
                .append("<b style='color:#349470;'>R</b> ").append(format(current.recovered(c)))
                .append(" (").append(format(proportion(current.recovered(c), n) * 100)).append("%)<br>")
                .append("β / γ ").append(format(parameters.beta())).append(" / ")
                .append(format(parameters.gamma())).append("<br>ΔI ").append(format(laplacian))
                .append(" · Movement effect ").append(format(parameters.movementRate() * laplacian))
                .append("<br><span style='color:#718191;'>Totals</span> S ")
                .append(format(current.totalSusceptible())).append(" · I ")
                .append(format(current.totalInfected())).append(" · R ")
                .append(format(current.totalRecovered()))
                .append("</div></html>");
        inspector.setText(text.toString());
        JTabbedPane tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, inspector);
        if (tabs != null) {
            JPanel laplacianPanel = (JPanel) tabs.getComponentAt(1);
            JLabel label = (JLabel) laplacianPanel.getClientProperty("valuesLabel");
            label.setText(laplacianText(laplacian));
        }
    }

    private String laplacianText(double laplacian) {
        int width = parameters.width();
        int height = parameters.height();
        double[] values = current.infectedValues();
        int c = selectedY * width + selectedX;
        StringBuilder result = new StringBuilder("<html><h3>Selected cell value: ")
                .append(format(values[c])).append("</h3>");
        if (selectedX > 0) appendNeighbour(result, "West", values[c - 1], values[c]);
        if (selectedX + 1 < width) appendNeighbour(result, "East", values[c + 1], values[c]);
        if (selectedY > 0) appendNeighbour(result, "North", values[c - width], values[c]);
        if (selectedY + 1 < height) appendNeighbour(result, "South", values[c + width], values[c]);
        result.append("<br><b>Discrete Laplacian ΔI = ").append(format(laplacian))
                .append("</b><br>Movement contribution to dI/dt = mΔI = ")
                .append(format(parameters.movementRate() * laplacian))
                .append("<br>For a full step, movement is applied conservatively after local RK4 reaction.")
                .append("</html>");
        return result.toString();
    }

    private static void appendNeighbour(StringBuilder text, String direction, double value, double center) {
        text.append(direction).append(": ").append(format(value)).append(" − ")
                .append(format(center)).append(" = ").append(format(value - center)).append("<br>");
    }

    private void compareModels() {
        try {
            SpatialParameters independentParameters = new SpatialParameters(parameters.width(), parameters.height(),
                    copyPopulation(), initialInfectedGrid(), initialRecoveredGrid(), parameters.beta(),
                    parameters.gamma(), 0, parameters.duration(), parameters.timeStep());
            SpatialParameters coupledParameters = new SpatialParameters(parameters.width(), parameters.height(),
                    copyPopulation(), initialInfectedGrid(), initialRecoveredGrid(), parameters.beta(),
                    parameters.gamma(), parameters.movementRate(), parameters.duration(), parameters.timeStep());
            SirParameters wellMixed = new SirParameters(parametersTotalPopulation(),
                    initialInfectedGridTotal(), initialRecoveredGridTotal(), parameters.beta(),
                    parameters.gamma(), parameters.duration(), parameters.timeStep());
            List<org.epidemicatlas.engine.SirState> aggregate = SirSimulator.simulate(wellMixed);
            comparisonChart.setSeries(List.of(new TrajectoryChart.Series("Well-mixed", new Color(123, 92, 162)),
                    new TrajectoryChart.Series("Independent cells", new Color(38, 116, 170)),
                    new TrajectoryChart.Series("Spatially coupled", RED)));
            comparisonChart.clear();
            SpatialSimulator independentRun = new SpatialSimulator(independentParameters);
            SpatialSimulator coupledRun = new SpatialSimulator(coupledParameters);
            for (int k = 0; k < aggregate.size(); k++) {
                SpatialState independentState = independentRun.state();
                SpatialState coupledState = coupledRun.state();
                comparisonChart.add(new double[] {aggregate.get(k).infected(),
                        independentState.totalInfected(), coupledState.totalInfected()}, aggregate.get(k).time());
                if (k + 1 < aggregate.size()) {
                    independentRun.step();
                    coupledRun.step();
                }
            }
            JTabbedPane tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, comparisonChart);
            if (tabs != null) tabs.setSelectedIndex(2);
            double initial = aggregate.get(0).total();
            double endMixed = aggregate.get(aggregate.size() - 1).susceptible();
            status.setText("Comparison computed. R₀ = " + format(wellMixed.basicReproductionNumber())
                    + "; well-mixed final size = " + format(initial - endMixed) + ".");
        } catch (RuntimeException exception) {
            showError("Could not compare models. " + exception.getMessage(), exception);
        }
    }

    private double[] copyPopulation() {
        double[] population = new double[parameters.size()];
        for (int c = 0; c < population.length; c++) population[c] = parameters.population(c);
        return population;
    }

    private double[] initialInfectedGrid() {
        double[] infected = new double[parameters.size()];
        int cell = seedY * parameters.width() + seedX;
        infected[cell] = parameters.infected(cell);
        return infected;
    }

    private double[] initialRecoveredGrid() {
        double[] recovered = new double[parameters.size()];
        for (int c = 0; c < recovered.length; c++) recovered[c] = parameters.recovered(c);
        return recovered;
    }

    private double initialInfectedGridTotal() {
        double sum = 0;
        for (int c = 0; c < parameters.size(); c++) sum += parameters.infected(c);
        return sum;
    }

    private double initialRecoveredGridTotal() {
        double sum = 0;
        for (int c = 0; c < parameters.size(); c++) sum += parameters.recovered(c);
        return sum;
    }

    private double parametersTotalPopulation() {
        double total = 0;
        for (int c = 0; c < parameters.size(); c++) total += parameters.population(c);
        return total;
    }

    private static double[] timeSeriesRow(SpatialState state) {
        return new double[] {state.time(), state.totalSusceptible(), state.totalInfected(),
                state.totalRecovered(), state.totalPopulation()};
    }

    private void saveScenario() {
        if (parameters == null) return;
        JFileChooser chooser = chooser("Epidemic Atlas scenario (*.properties)", "properties");
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        Properties values = scenarioProperties();
        try {
            Path path = ensureExtension(chooser.getSelectedFile().toPath(), ".properties");
            ScenarioStore.save(path, values);
            status.setText("Scenario saved to " + path.getFileName() + ".");
        } catch (IOException exception) {
            showError("Could not save scenario.", exception);
        }
    }

    private void loadScenario() {
        JFileChooser chooser = chooser("Epidemic Atlas scenario (*.properties)", "properties");
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            Properties values = ScenarioStore.load(chooser.getSelectedFile().toPath());
            widthSpinner.setValue(Integer.parseInt(values.getProperty("width")));
            heightSpinner.setValue(Integer.parseInt(values.getProperty("height")));
            populationField.setText(values.getProperty("population"));
            infectedField.setText(values.getProperty("infected"));
            recoveredField.setText(values.getProperty("recovered"));
            betaField.setText(values.getProperty("beta"));
            gammaField.setText(values.getProperty("gamma"));
            movementField.setText(values.getProperty("movement"));
            durationField.setText(values.getProperty("duration"));
            stepField.setText(values.getProperty("timeStep"));
            seedX = Integer.parseInt(values.getProperty("seedX"));
            seedY = Integer.parseInt(values.getProperty("seedY"));
            if (!resetSimulation()) return;
            status.setText("Loaded deterministic scenario (no random process is used).");
        } catch (IOException | RuntimeException exception) {
            showError("Could not load scenario. Check file contents and parameter values.", exception);
        }
    }

    private Properties scenarioProperties() {
        Properties values = new Properties();
        values.setProperty("model", "spatial-sir");
        values.setProperty("modelVersion", "1.0");
        values.setProperty("width", widthSpinner.getValue().toString());
        values.setProperty("height", heightSpinner.getValue().toString());
        values.setProperty("population", populationField.getText());
        values.setProperty("infected", infectedField.getText());
        values.setProperty("recovered", recoveredField.getText());
        values.setProperty("beta", betaField.getText());
        values.setProperty("gamma", gammaField.getText());
        values.setProperty("movement", movementField.getText());
        values.setProperty("duration", durationField.getText());
        values.setProperty("timeStep", stepField.getText());
        values.setProperty("seedX", Integer.toString(seedX));
        values.setProperty("seedY", Integer.toString(seedY));
        values.setProperty("boundary", "no-flux");
        return values;
    }

    private void exportSeries() {
        if (history.isEmpty()) return;
        JFileChooser chooser = chooser("CSV time-series (*.csv)", "csv");
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            Path path = ensureExtension(chooser.getSelectedFile().toPath(), ".csv");
            CsvExporter.writeTimeSeries(path, List.copyOf(history));
            status.setText("Exported " + history.size() + " computed time points to " + path.getFileName() + ".");
        } catch (IOException exception) {
            showError("Could not export time series.", exception);
        }
    }

    private void exportGrid() {
        if (current == null) return;
        JFileChooser chooser = chooser("CSV grid state (*.csv)", "csv");
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            Path path = ensureExtension(chooser.getSelectedFile().toPath(), ".csv");
            CsvExporter.writeGrid(path, parameters, current);
            status.setText("Exported computed grid at day " + format(current.time()) + ".");
        } catch (IOException exception) {
            showError("Could not export grid state.", exception);
        }
    }

    private static JFileChooser chooser(String description, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        return chooser;
    }

    private static Path ensureExtension(Path path, String extension) {
        return path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(extension)
                ? path : Path.of(path + extension);
    }

    private static double number(JTextField field, String label) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            if (!Double.isFinite(value)) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a finite number.");
        }
    }

    private static double proportion(double value, double total) {
        return total == 0 ? 0 : value / total;
    }

    private static double reproductionNumber(double beta, double gamma) {
        return gamma == 0 ? (beta == 0 ? 0 : Double.POSITIVE_INFINITY) : beta / gamma;
    }

    private static String format(double value) {
        return new DecimalFormat("0.####").format(value);
    }

    private void showError(String message, Exception exception) {
        status.setForeground(RED);
        runIndicator.setText("  CHECK INPUTS  ");
        runIndicator.setForeground(RED);
        status.setText(message);
        if (exception != null) System.err.println(message + " " + exception);
    }

    private final class GridCanvas extends JPanel {
        private GridCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 220));
            setToolTipText("Computed spatial state. Select a cell to inspect values.");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    if (parameters == null) return;
                    int x = Math.min(parameters.width() - 1,
                            Math.max(0, event.getX() * parameters.width() / Math.max(1, getWidth())));
                    int y = Math.min(parameters.height() - 1,
                            Math.max(0, event.getY() * parameters.height() / Math.max(1, getHeight())));
                    selectedX = x;
                    selectedY = y;
                    if (current.time() == 0) {
                        seedX = x;
                        seedY = y;
                        status.setText("Seed location set to cell (" + x + ", " + y
                                + "). Press Reset / apply to rebuild initial conditions.");
                    }
                    updateDisplay();
                }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (current == null || parameters == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(247, 249, 252));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            int variable = variableChoice.getSelectedIndex();
            double maximum = 0;
            for (int c = 0; c < current.size(); c++) maximum = Math.max(maximum, valueAt(c, variable));
            int gap = Math.min(3, Math.max(1, Math.min(getWidth() / parameters.width(),
                    getHeight() / parameters.height()) / 10));
            for (int y = 0; y < parameters.height(); y++) {
                for (int x = 0; x < parameters.width(); x++) {
                    int c = y * parameters.width() + x;
                    double value = valueAt(c, variable);
                    double ratio = maximum <= 0 ? 0 : Math.min(1, value / maximum);
                    g.setColor(colorFor(ratio, variable));
                    int px = x * getWidth() / parameters.width();
                    int py = y * getHeight() / parameters.height();
                    int right = (x + 1) * getWidth() / parameters.width();
                    int bottom = (y + 1) * getHeight() / parameters.height();
                    int cellWidth = right - px;
                    int cellHeight = bottom - py;
                    int inset = gap / 2;
                    g.fillRoundRect(px + inset, py + inset, Math.max(1, cellWidth - gap),
                            Math.max(1, cellHeight - gap), 5, 5);
                    if (x == selectedX && y == selectedY) {
                        g.setColor(new Color(24, 52, 70));
                        g.setStroke(new java.awt.BasicStroke(2f));
                        g.drawRoundRect(px + inset, py + inset, Math.max(1, cellWidth - gap - 1),
                                Math.max(1, cellHeight - gap - 1), 5, 5);
                    }
                    if (x == seedX && y == seedY && current.time() == 0) {
                        g.setColor(Color.WHITE);
                        int marker = Math.max(4, Math.min(cellWidth, cellHeight) / 3);
                        g.fillOval(px + (cellWidth - marker) / 2, py + (cellHeight - marker) / 2,
                                marker, marker);
                        g.setColor(TEAL);
                        g.drawOval(px + (cellWidth - marker) / 2, py + (cellHeight - marker) / 2,
                                marker, marker);
                    }
                }
            }
            g.dispose();
        }

        private double valueAt(int c, int variable) {
            double n = current.total(c);
            return switch (variable) {
                case 1 -> proportion(current.infected(c), n);
                case 2 -> proportion(current.susceptible(c), n);
                case 3 -> proportion(current.recovered(c), n);
                case 4 -> n;
                default -> current.infected(c);
            };
        }

    }

    private Color colorFor(double value, int variable) {
        if (variable == 2) return blend(new Color(244, 249, 255), new Color(38, 116, 170), value);
        if (variable == 3) return blend(new Color(244, 251, 247), GREEN, value);
        if (variable == 4) return blend(new Color(250, 246, 230), new Color(204, 148, 51), value);
        return blend(new Color(255, 246, 240), RED, value);
    }

    private final class LegendBar extends JPanel {
        private LegendBar() {
            setPreferredSize(new Dimension(112, 34));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            int variable = variableChoice.getSelectedIndex();
            int width = Math.max(1, getWidth() - 2);
            for (int x = 0; x < width; x++) {
                g.setColor(colorFor((double) x / Math.max(1, width - 1), variable));
                g.fillRect(x, 2, 1, 10);
            }
            g.setColor(MUTED);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g.drawString("LOW", 1, 25);
            g.drawString("HIGH", Math.max(1, width - 31), 25);
            g.dispose();
        }
    }

    private static Color blend(Color low, Color high, double value) {
        double fraction = Math.max(0, Math.min(1, value));
        return new Color((int) (low.getRed() + (high.getRed() - low.getRed()) * fraction),
                (int) (low.getGreen() + (high.getGreen() - low.getGreen()) * fraction),
                (int) (low.getBlue() + (high.getBlue() - low.getBlue()) * fraction));
    }

    private static final class TrajectoryChart extends JPanel {
        private record Series(String label, Color color) { }
        private final List<double[]> points = new ArrayList<>();
        private final List<Double> times = new ArrayList<>();
        private List<Series> series = List.of();

        private TrajectoryChart() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(5, 6, 4, 6));
        }

        private void setSeries(List<Series> series) {
            this.series = List.copyOf(series);
            clear();
        }

        private void clear() {
            points.clear();
            times.clear();
            repaint();
        }

        private void add(double[] values, double time) {
            points.add(values.clone());
            times.add(time);
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 60, right = 18, top = 38, bottom = 46;
            int plotWidth = Math.max(1, getWidth() - left - right);
            int plotHeight = Math.max(1, getHeight() - top - bottom);
            boolean hasTrajectory = points.size() >= 2;
            g.setColor(INK);
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.drawString("Compartment trajectories", left, 19);
            if (hasTrajectory) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g.setColor(MUTED);
                g.drawString("POPULATION", 8, top + plotHeight / 2);
            }
            g.setColor(new Color(231, 236, 241));
            for (int k = 0; k <= 4; k++) {
                int y = top + k * plotHeight / 4;
                g.drawLine(left, y, left + plotWidth, y);
                if (hasTrajectory) {
                    double max = maximumValue();
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    g.setColor(MUTED);
                    g.drawString(format(max * (4 - k) / 4), 4, y + 3);
                    g.setColor(new Color(231, 236, 241));
                }
            }
            if (!hasTrajectory) {
                g.setColor(MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                String message = "Run or step the model to draw computed trajectories.";
                int messageX = left + Math.max(10, (plotWidth - g.getFontMetrics().stringWidth(message)) / 2);
                g.drawString(message, messageX, top + plotHeight / 2);
            } else {
                double max = maximumValue();
                for (int s = 0; s < series.size(); s++) {
                    g.setColor(series.get(s).color());
                    g.setStroke(new java.awt.BasicStroke(2.5f, java.awt.BasicStroke.CAP_ROUND,
                            java.awt.BasicStroke.JOIN_ROUND));
                    for (int k = 1; k < points.size(); k++) {
                        int x1 = left + (k - 1) * plotWidth / (points.size() - 1);
                        int x2 = left + k * plotWidth / (points.size() - 1);
                        int y1 = top + plotHeight - (int) (points.get(k - 1)[s] / max * plotHeight);
                        int y2 = top + plotHeight - (int) (points.get(k)[s] / max * plotHeight);
                        g.drawLine(x1, y1, x2, y2);
                    }
                }
            }
            g.setColor(MUTED);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g.drawString("0 days", left, top + plotHeight + 17);
            if (hasTrajectory && !times.isEmpty()) {
                String lastDay = format(times.get(times.size() - 1)) + " days";
                g.drawString(lastDay, Math.max(left, left + plotWidth - g.getFontMetrics().stringWidth(lastDay)),
                        top + plotHeight + 17);
            }
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int legendX = left;
            for (Series item : series) {
                g.setColor(item.color());
                g.setStroke(new java.awt.BasicStroke(2.5f, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                g.drawLine(legendX, getHeight() - 10, legendX + 13, getHeight() - 10);
                g.setColor(MUTED);
                g.drawString(item.label(), legendX + 18, getHeight() - 7);
                legendX += 135;
            }
            g.dispose();
        }

        private double maximumValue() {
            double maximum = 0;
            for (double[] point : points)
                for (double value : point) maximum = Math.max(maximum, value);
            return maximum == 0 ? 1 : maximum;
        }
    }
}
