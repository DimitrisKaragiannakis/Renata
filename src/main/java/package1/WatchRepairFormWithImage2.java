package package1;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

// Apache PDFBox Imports
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.apache.poi.xwpf.usermodel.*;
import java.io.FileOutputStream;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class WatchRepairFormWithImage2 extends JFrame {

    public static String getMachineID() {
        try {
            // Gets the network interface (Ethernet or Wi-Fi card)
            java.net.InetAddress ip = java.net.InetAddress.getLocalHost();
            java.net.NetworkInterface network = java.net.NetworkInterface.getByInetAddress(ip);
            
            // If the first one is null, grab the first available active interface
            if (network == null) {
                java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
                while (en.hasMoreElements()) {
                    java.net.NetworkInterface ni = en.nextElement();
                    if (ni.getHardwareAddress() != null) {
                        network = ni;
                        break;
                    }
                }
            }

            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString(); // Returns something like: 00-1A-2B-3C-4D-5E
        } catch (Exception e) {
            return "DEFAULT_KEY_001"; // Fallback if no network card is found
        }
    }
    public static void main(String[] args) {
    	
    	// 1. Get the current computer's ID
        String id = getMachineID();
        System.out.println("tSystemMotherboardSerial is :"+id);
        // 2. THE LOCK: Replace "YOUR_CLIENT_ID" with the actual ID 
        // you get when you run it on their computer for the first time.
		
		/*
		 * if (!id.equals("18-C0-4D-60-2E-99")) { JOptionPane.showMessageDialog(null,
		 * "Unauthorized Computer.\nHardware ID: " + id, "License Error",
		 * JOptionPane.ERROR_MESSAGE); System.exit(0); }
		 */
        
     // 1. ΠΡΩΤΑ ΑΠΟ ΟΛΑ: Δημιουργία/Έλεγχος του πίνακα
        DatabaseHelper.initializeDatabase();
        
        try { 
        	UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new WatchRepairFormWithImage2().setVisible(true));
    }
    private int currentLoadedId = -1; // -1 σημαίνει ότι η φόρμα είναι άδεια ή νέα
    private JLabel imagePreviewLabel;
    private JTextField nameField, addressField, cityField, brandField, telField, refField, modelField;
    private File selectedImageFile;

    private JTable serviceTable;

    private JButton uploadButton, saveBtn, loadBtn, printBtn;

    public WatchRepairFormWithImage2() {
    	
        setTitle("ΔΕΛΤΙΟ ΠΑΡΑΛΑΒΗΣ ΕΠΙΣΚΕΥΗΣ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 900);
        setLocationRelativeTo(null);

        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Header Section
        mainContentPanel.add(createHeaderPanel());
        mainContentPanel.add(Box.createVerticalStrut(15));

        // 2. Image Section
        mainContentPanel.add(createImageUploadPanel());
        mainContentPanel.add(Box.createVerticalStrut(15));

        // 3. Services Section
        mainContentPanel.add(createServicesTablePanel());
        mainContentPanel.add(Box.createVerticalStrut(15));

        // 4. Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        saveBtn = new JButton("Save Record");
        saveBtn.setFont(new Font("Arial", Font.BOLD, 12));
        saveBtn.addActionListener(e -> saveFullRecord());

        loadBtn = new JButton("Load Record");
        loadBtn.addActionListener(e -> showSearchDialog());

        printBtn = new JButton("Print PDF");
        printBtn.setFont(new Font("Arial", Font.BOLD, 12));
        printBtn.setBackground(new Color(220, 240, 255));
        printBtn.addActionListener(e -> generateDocx());
        
        styleButton(saveBtn, new Color(46, 204, 113)); // Emerald Green
        styleButton(printBtn, new Color(52, 152, 219)); // Peter River Blue
        styleButton(loadBtn, new Color(149, 165, 166)); // Concrete Gray

        buttonPanel.add(saveBtn);
        buttonPanel.add(loadBtn);
        buttonPanel.add(printBtn);
        
        JButton btnExport = new JButton("Εξαγωγή σε TXT");
        btnExport.setIcon(new ImageIcon("export_icon.png")); // Προαιρετικά, αν έχεις εικονίδιο

        btnExport.addActionListener(e -> exportToTXT());

        // Πρόσθεσε το κουμπί στο panel σου (π.χ. buttonPanel)
        buttonPanel.add(btnExport);
        
     // Inside the WatchRepairFormWithImage2 constructor:

        JButton clearBtn = new JButton("Νέα Εγγραφή (Clear)");
        styleButton(clearBtn, new Color(231, 76, 60)); // Alizarin Red
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Είστε σίγουροι ότι θέλετε να καθαρίσετε τη φόρμα;", 
                "Επιβεβαίωση", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                clearForm();
            }
        });

        // Add it to your existing button panel
        buttonPanel.add(clearBtn);
        
        mainContentPanel.add(buttonPanel);

        JScrollPane mainScrollPane = new JScrollPane(mainContentPanel);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScrollPane);
        
     // Πρόσθεσε το δεξί κλικ σε κάθε πεδίο
        addContextMenu(nameField);
        addContextMenu(brandField);
        addContextMenu(modelField);
        addContextMenu(telField);
        addContextMenu(addressField);
        addContextMenu(cityField);
        addContextMenu(refField);
        
        setupMenuBar();
    }

    private void addContextMenu(JTextField field) {
        JPopupMenu menu = new JPopupMenu();
        
        // Επιλογή: Cut
        JMenuItem cut = new JMenuItem("Αποκοπή (Cut)");
        cut.addActionListener(e -> field.cut());
        
        // Επιλογή: Copy
        JMenuItem copy = new JMenuItem("Αντιγραφή (Copy)");
        copy.addActionListener(e -> field.copy());
        
        // Επιλογή: Paste
        JMenuItem paste = new JMenuItem("Επικόλληση (Paste)");
        paste.addActionListener(e -> field.paste());
        
        // Επιλογή: Select All
        JMenuItem selectAll = new JMenuItem("Επιλογή όλων");
        selectAll.addActionListener(e -> field.selectAll());

        menu.add(cut);
        menu.add(copy);
        menu.add(paste);
        menu.addSeparator();
        menu.add(selectAll);

        field.setComponentPopupMenu(menu);
    }

    private void addLabelField(JPanel panel, String labelText, JTextField field, GridBagConstraints gbc, int y, int xStart, int width) {
        gbc.gridx = xStart; gbc.gridy = y; gbc.gridwidth = 1;
        panel.add(new JLabel(labelText), gbc);
        gbc.gridx = xStart + 1; gbc.gridwidth = width; gbc.weightx = 1.0;
        panel.add(field, gbc);
        gbc.weightx = 0; gbc.gridwidth = 1;
    }

    private void clearForm() {
        // 1. Clear all text fields
        nameField.setText("");
        addressField.setText("");
        cityField.setText("");
        brandField.setText("");
        telField.setText("");
        refField.setText("");
        modelField.setText("");
        currentLoadedId = -1; // Επαναφορά σε "Νέα Εγγραφή"

        // 2. Uncheck all checkboxes in the table and clear notes
        DefaultTableModel model = (DefaultTableModel) serviceTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(false, i, 1); // Column 1 is the Boolean
            model.setValueAt("", i, 2);    // Column 2 is the Notes
        }

        // 3. Reset the Image Preview
        selectedImageFile = null;
        imagePreviewLabel.setIcon(null);
        imagePreviewLabel.setText("No Image Selected");

        // 4. Feedback
        System.out.println("Form cleared for next entry.");
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Στοιχεία Πελάτη & Ωρολογίου"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        addLabelField(panel, "Ονοματεπώνυμο:", nameField = new JTextField(20), gbc, 0, 0, 3);
        addLabelField(panel, "Διεύθυνση:", addressField = new JTextField(15), gbc, 1, 0, 1);
        gbc.gridx = 2; panel.add(new JLabel(" Πόλη:"), gbc);
        gbc.gridx = 3; cityField = new JTextField(10); panel.add(cityField, gbc);
        addLabelField(panel, "Μάρκα ωρολογίου:", brandField = new JTextField(15), gbc, 2, 0, 1);
        gbc.gridx = 2; panel.add(new JLabel(" Τηλ:"), gbc);
        gbc.gridx = 3; telField = new JTextField(10); panel.add(telField, gbc);
        addLabelField(panel, "REF:", refField = new JTextField(15), gbc, 3, 0, 1);
        gbc.gridx = 2; panel.add(new JLabel(" Model:"), gbc);
        gbc.gridx = 3; modelField = new JTextField(10); panel.add(modelField, gbc);
        return panel;
    }

    private JPanel createImageUploadPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Φωτογραφία Ωρολογίου"));
        imagePreviewLabel = new JLabel("No Image Selected", SwingConstants.CENTER);
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imagePreviewLabel.setPreferredSize(new Dimension(300, 250));
        imagePreviewLabel.setMaximumSize(new Dimension(300, 250));
        imagePreviewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        uploadButton = new JButton("Select Picture");
        uploadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser("/home/dimitris/Pictures/");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedImageFile = fc.getSelectedFile();
                displayScaledImage(selectedImageFile);
            }
        });

        panel.add(Box.createVerticalStrut(5));
        panel.add(imagePreviewLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(uploadButton);
        return panel;
    }
    
    
    
    private JPanel createServicesTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("ΕΡΓΑΣΙΕΣ"));
        String[] columnNames = {"Τύπος Εργασίας", "Επιλογή", "Σημειώσεις"};
        Object[][] data = {{"SERVICE", false, ""}, {"Κύκλωμα", false, ""}, {"Πηνίο", false, ""},
                           {"Κορώνα", false, ""}, {"Μηχανή", false, ""}, {"Κρύσταλο / Ζελατίνα", false, ""}};
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? Boolean.class : String.class; }
        };
        serviceTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(serviceTable);
        scrollPane.setPreferredSize(new Dimension(600, 150));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private void displayScaledImage(File file) {
        ImageIcon icon = new ImageIcon(file.getAbsolutePath());
        Image img = icon.getImage().getScaledInstance(300, 250, Image.SCALE_SMOOTH);
        imagePreviewLabel.setIcon(new ImageIcon(img));
        imagePreviewLabel.setText("");
        imagePreviewLabel.revalidate();
        imagePreviewLabel.repaint();
    }
    
    private void exportToTXT() {
        JFileChooser fileChooser = new JFileChooser(".");
        fileChooser.setDialogTitle("Αποθήκευση Εξαγωγής");
        
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Προσθήκη κατάληξης .txt αν δεν υπάρχει
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".txt")) {
                filePath += ".txt";
            }

            String sql = "SELECT * FROM repairs"; // Βεβαιώσου ότι ο πίνακας λέγεται repairs
            String url = "jdbc:sqlite:repairs.db";

            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql);
                 PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {

                writer.println("=== ΑΡΧΕΙΟ ΕΠΙΣΚΕΥΩΝ ΡΟΛΟΓΙΩΝ ===");
                writer.println("Ημερομηνία Εξαγωγής: " + new java.util.Date());
                writer.println("---------------------------------------------------------------------------");
                // Προσθέσαμε το %-15s για το Τηλέφωνο στην επικεφαλίδα
                writer.printf("%-5s | %-20s | %-15s | %-15s | %-20s%n", "ID", "Πελάτης", "Μοντέλο", "Τηλέφωνο", "Εργασίες");
                writer.println("---------------------------------------------------------------------------");

                while (rs.next()) {
                    // Εδώ τραβάμε και το rs.getString("tel")
                    writer.printf("%-5d | %-20s | %-15s | %-15s | %-20s%n", 
                        rs.getInt("id"), 
                        rs.getString("customer_name"), 
                        rs.getString("watch_model"),
                        (rs.getString("tel") != null ? rs.getString("tel") : "-"), // Έλεγχος αν είναι κενό
                        rs.getString("services"));
                }

                writer.println("---------------------------------------------------------------------------");
                writer.flush();

                JOptionPane.showMessageDialog(this, "Η εξαγωγή ολοκληρώθηκε επιτυχώς!");

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Σφάλμα κατά την εξαγωγή: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void generateDocx() {
        if (!validateForm()) return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm"));
        String fileName = "Repair_" + nameField.getText().replaceAll("\\s+", "_") + "_" + timestamp + ".docx";

        try (XWPFDocument document = new XWPFDocument()) {
            
            // --- 1. ΤΙΤΛΟΣ ---
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("ΔΕΛΤΙΟ ΠΟΣΟΤΙΚΗΣ ΠΑΡΑΛΑΒΗΣ & ΕΠΙΣΚΕΥΗΣ");
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.addBreak();

            // --- 2. ΠΙΝΑΚΑΣ ΣΤΟΙΧΕΙΩΝ (Για τέλεια στοίχηση) ---
            // Δημιουργούμε έναν πίνακα 4 σειρών και 2 στηλών
            XWPFTable infoTable = document.createTable(4, 2);
            infoTable.setWidth("100%");

         // Πλήρης αφαίρεση περιγραμμάτων (Borders) για όλες τις πλευρές
         org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders borders = infoTable.getCTTbl().getTblPr().addNewTblBorders();
         borders.addNewBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
         borders.addNewTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
         borders.addNewLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
         borders.addNewRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
         borders.addNewInsideH().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
         borders.addNewInsideV().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);

            // Σειρά 0: Όνομα | Τηλέφωνο
            setTableCellText(infoTable.getRow(0).getCell(0), "Ονοματεπώνυμο: " + nameField.getText(), true);
            setTableCellText(infoTable.getRow(0).getCell(1), "Τηλέφωνο: " + telField.getText(), true);

            // Σειρά 1: Διεύθυνση | Πόλη
            setTableCellText(infoTable.getRow(1).getCell(0), "Διεύθυνση: " + addressField.getText(), false);
            setTableCellText(infoTable.getRow(1).getCell(1), "Πόλη: " + cityField.getText(), false);

            // Σειρά 2: Μάρκα | Μοντέλο
            setTableCellText(infoTable.getRow(2).getCell(0), "Μάρκα: " + brandField.getText(), true);
            setTableCellText(infoTable.getRow(2).getCell(1), "Model: " + modelField.getText(), true);

            // Σειρά 3: Ημερομηνία | REF
            setTableCellText(infoTable.getRow(3).getCell(0), "Ημερομηνία: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), false);
            setTableCellText(infoTable.getRow(3).getCell(1), "REF: " + refField.getText(), false);

            document.createParagraph().createRun().addBreak(); // Κενό
            document.createParagraph().createRun().addBreak(); // Κενό

            // --- 3. ΕΡΓΑΣΙΕΣ ---
            XWPFParagraph sTitle = document.createParagraph();
            XWPFRun sRun = sTitle.createRun();
            sRun.setText("ΕΡΓΑΣΙΕΣ ΠΡΟΣ ΕΚΤΕΛΕΣΗ:");
            sRun.setBold(true);
            sRun.setUnderline(UnderlinePatterns.SINGLE);

            DefaultTableModel model = (DefaultTableModel) serviceTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((Boolean) model.getValueAt(i, 1)) {
                    XWPFParagraph p = document.createParagraph();
                    p.setIndentationLeft(720); // 0.5 inch tab
                    XWPFRun r = p.createRun();
                    String service = model.getValueAt(i, 0).toString();
                    String notes = model.getValueAt(i, 2).toString();
                    r.setText("• " + service + (notes.isEmpty() ? "" : " [" + notes + "]"));
                }
            }
            document.createParagraph().createRun().addBreak(); // Κενό

            // --- 4. ΦΩΤΟΓΡΑΦΙΑ ---
            if (selectedImageFile != null && selectedImageFile.exists()) {
                document.createParagraph().createRun().addBreak();
                XWPFParagraph imgPara = document.createParagraph();
                imgPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun imgRun = imgPara.createRun();
                
                try (FileInputStream is = new FileInputStream(selectedImageFile)) {
                    // Μετατροπή pixel σε EMU (300x225 περίπου)
                    imgRun.addPicture(is, XWPFDocument.PICTURE_TYPE_JPEG, selectedImageFile.getName(), 
                                     org.apache.poi.util.Units.toEMU(280), org.apache.poi.util.Units.toEMU(210));
                }
            }

            // Αποθήκευση και Άνοιγμα
            try (FileOutputStream out = new FileOutputStream(fileName)) {
                document.write(out);
            }
            
            JOptionPane.showMessageDialog(this, "Το έγγραφο Word δημιουργήθηκε!");
            Desktop.getDesktop().open(new File(fileName));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Σφάλμα Word: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generatePDF() {
        if (!validateForm()) return;
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "Receipt_" + nameField.getText().replaceAll("\\s+", "") + "_" + timestamp + ".pdf";

        // 1. Άνοιγμα του Document
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // --- LOAD FONTS ---
            File regularFile = new File("arial.ttf");
            File boldFile = new File("arialbd.ttf");
            org.apache.pdfbox.pdmodel.font.PDFont font;
            org.apache.pdfbox.pdmodel.font.PDFont fontBold;

            if (regularFile.exists()) {
                font = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, regularFile);
                fontBold = boldFile.exists() ? org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, boldFile) : font;
            } else {
                JOptionPane.showMessageDialog(this, "Το αρχείο arial.ttf λείπει!", "Font Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2. Άνοιγμα του ContentStream
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                
                // --- HEADER ---
                contentStream.beginText();
                contentStream.setFont(fontBold, 18);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("ΔΕΛΤΙΟ ΠΑΡΑΛΑΒΗΣ ΕΠΙΣΚΕΥΗΣ");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.setLeading(18f);
                contentStream.newLineAtOffset(50, 710);

                // --- DATA ---
                contentStream.showText("Πελάτης: " + nameField.getText().trim());
                contentStream.newLine();
                contentStream.showText("Τηλέφωνο: " + telField.getText().trim());
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText("Μάρκα: " + brandField.getText().trim() + " | Model: " + modelField.getText().trim());
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText("ΕΡΓΑΣΙΕΣ:");
                contentStream.newLine();

                DefaultTableModel model = (DefaultTableModel) serviceTable.getModel();
                for (int i = 0; i < model.getRowCount(); i++) {
                    if ((Boolean) model.getValueAt(i, 1)) {
                        String service = model.getValueAt(i, 0).toString();
                        String notes = model.getValueAt(i, 2).toString();
                        contentStream.showText("- " + service + (notes.isEmpty() ? "" : " (" + notes + ")"));
                        contentStream.newLine();
                    }
                }
                contentStream.endText();

                // --- IMAGE ---
                if (selectedImageFile != null && selectedImageFile.exists()) {
                    java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(selectedImageFile);
                    if (bimg != null) {
                        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage = 
                            org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, bimg);
                        
                        float panelWidth = (float) imagePreviewLabel.getWidth();
                        float panelHeight = (float) imagePreviewLabel.getHeight();
                        contentStream.drawImage(pdImage, 50, 150, panelWidth, panelHeight);
                    }
                }
            } // Εδώ κλείνει αυτόματα το contentStream

            // 3. Σώζουμε το έγγραφο ΑΦΟΥ κλείσει το contentStream αλλά ΠΡΙΝ κλείσει το document
            document.save(fileName);
            JOptionPane.showMessageDialog(this, "Το PDF δημιουργήθηκε: " + fileName);
            Desktop.getDesktop().open(new File(fileName));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Σφάλμα PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private String getSelectedServices() {
        StringBuilder sb = new StringBuilder();
        DefaultTableModel model = (DefaultTableModel) serviceTable.getModel();
        
        for (int i = 0; i < model.getRowCount(); i++) {
            // Η στήλη 1 (Index 1) είναι το Boolean (το checkbox)
            Boolean isSelected = (Boolean) model.getValueAt(i, 1);
            
            if (isSelected != null && isSelected) {
                if (sb.length() > 0) {
                    sb.append(", "); // Χωρίζουμε τις εργασίες με κόμμα
                }
                // Η στήλη 0 είναι το όνομα της εργασίας (π.χ. "SERVICE")
                String serviceName = model.getValueAt(i, 0).toString();
                // Η στήλη 2 είναι οι σημειώσεις (Notes)
                String notes = model.getValueAt(i, 2).toString().trim();
                
                sb.append(serviceName);
                if (!notes.isEmpty()) {
                    sb.append(" (").append(notes).append(")");
                }
            }
        }
        
        return sb.length() == 0 ? "Καμία Εργασία" : sb.toString();
    }
           

    private void loadRecordFromSQLite(int id) {
        String sql = "SELECT * FROM repairs WHERE id = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
            	currentLoadedId = id; // Τώρα η εφαρμογή "ξέρει" ποιον επεξεργαζόμαστε
                // 1. Γέμισμα απλών πεδίων κειμένου
                nameField.setText(rs.getString("customer_name"));
                brandField.setText(rs.getString("brand"));
                modelField.setText(rs.getString("watch_model"));
                // Αν έχεις προσθέσει address, city, tel, ref στη βάση, τα βάζεις κι αυτά εδώ
                
                // 2. Διαχείριση Εικόνας
                String path = rs.getString("image_path");
                if (path != null && !path.equals("None")) {
                    selectedImageFile = new File(path);
                    if (selectedImageFile.exists()) {
                        displayScaledImage(selectedImageFile);
                    } else {
                        imagePreviewLabel.setIcon(null);
                        imagePreviewLabel.setText("Το αρχείο εικόνας χάθηκε");
                    }
                } else {
                    imagePreviewLabel.setIcon(null);
                    imagePreviewLabel.setText("No Image Selected");
                }

                // 3. Διαχείριση Checkboxes (Εργασίες)
                String savedServices = rs.getString("services"); // π.χ. "SERVICE, Κύκλωμα (Αλλαγή)"
                DefaultTableModel model = (DefaultTableModel) serviceTable.getModel();
                
                for (int i = 0; i < model.getRowCount(); i++) {
                    String serviceName = model.getValueAt(i, 0).toString();
                    
                    // Έλεγχος αν το όνομα της εργασίας περιέχεται στο String της βάσης
                    if (savedServices != null && savedServices.contains(serviceName)) {
                        model.setValueAt(true, i, 1); // Τσεκάρισμα
                        
                        // Προαιρετικό: Αν θέλεις να ανακτήσεις και τα Notes μέσα στις παρενθέσεις
                        // είναι λίγο πιο σύνθετο, αλλά για αρχή το contains δουλεύει για το checkbox.
                    } else {
                        model.setValueAt(false, i, 1); // Ξετσεκάρισμα
                    }
                }
                
                //JOptionPane.showMessageDialog(this, "Η εγγραφή #" + id + " φορτώθηκε επιτυχώς!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Σφάλμα κατά την ανάκτηση: " + e.getMessage());
        }
    }
    
    private void populateForm(String[] data) {
        nameField.setText(data[1]);
        brandField.setText(data[2]);
        modelField.setText(data[3]);
        String savedServices = data[4];
        DefaultTableModel model = (DefaultTableModel) serviceTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(savedServices.contains(model.getValueAt(i, 0).toString()), i, 1);
        }
        if (!data[5].equals("None")) {
            selectedImageFile = new File(data[5]);
            if (selectedImageFile.exists()) displayScaledImage(selectedImageFile);
        }
    }
    
    
    private void saveFullRecord() {
        if (!validateForm()) return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String imagePath = (selectedImageFile != null) ? saveImageLocally(selectedImageFile) : "None";
        String servicesData = getSelectedServices();

        String sql;
        if (currentLoadedId == -1) {
            sql = "INSERT INTO repairs(timestamp, customer_name, brand, watch_model, services, image_path, tel) VALUES(?,?,?,?,?,?,?)";
        } else {
            // ΠΡΟΣΟΧΗ: watch_model εδώ!
            sql = "UPDATE repairs SET timestamp=?, customer_name=?, brand=?, watch_model=?, services=?, image_path=?, tel=? WHERE id=?";
        }

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, timestamp);
            pstmt.setString(2, nameField.getText());
            pstmt.setString(3, brandField.getText());
            pstmt.setString(4, modelField.getText());
            pstmt.setString(5, servicesData);
            pstmt.setString(6, imagePath);
            pstmt.setString(7, telField.getText());

            if (currentLoadedId != -1) {
                pstmt.setInt(8, currentLoadedId);
            }
            
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, (currentLoadedId == -1 ? "Αποθηκεύτηκε!" : "Ενημερώθηκε!"));
            clearForm(); 
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Σφάλμα SQL: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private String saveImageLocally(File sourceFile) {
        try {
            // 1. Δημιουργία φακέλου "repairs_images" αν δεν υπάρχει
            Path dir = Paths.get("repairs_images");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 2. Δημιουργία μοναδικού ονόματος αρχείου (Timestamp + Όνομα)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = timestamp + "_" + sourceFile.getName();
            Path targetPath = dir.resolve(fileName);

            // 3. Αντιγραφή του αρχείου στον φάκελο της εφαρμογής
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Επιστροφή του path για να αποθηκευτεί στην SQLite
            return targetPath.toString();
            
        } catch (IOException e) {
            e.printStackTrace();
            return "Error saving image";
        }
    }

    // Βοηθητική μέθοδος για γρήγορο γέμισμα κελιών πίνακα
    private void setTableCellText(XWPFTableCell cell, String text, boolean bold) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontFamily("Arial");
        r.setFontSize(11);
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // --- ΜΕΝΟΥ: ΑΡΧΕΙΟ ---
        JMenu fileMenu = new JMenu("Αρχείο");
        
        JMenuItem newItem = new JMenuItem("Νέα Εγγραφή (Clear)");
        newItem.addActionListener(e -> clearForm());
        
        JMenuItem saveItem = new JMenuItem("Αποθήκευση");
        saveItem.addActionListener(e -> saveFullRecord());
        
        JMenuItem exitItem = new JMenuItem("Έξοδος");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator(); // Μια γραμμή διαχωρισμού
        fileMenu.add(exitItem);

        // --- ΜΕΝΟΥ: ΑΝΑΖΗΤΗΣΗ ---
        JMenu searchMenu = new JMenu("Αναζήτηση");
        JMenuItem openSearch = new JMenuItem("Εύρεση Εγγραφής...");
        openSearch.addActionListener(e -> showSearchDialog());
        searchMenu.add(openSearch);

        // --- ΜΕΝΟΥ: ΕΞΑΓΩΓΗ ---
        JMenu exportMenu = new JMenu("Εξαγωγή");
        
        JMenuItem wordItem = new JMenuItem("Εκτύπωση σε Word / Εκτυπωτή");
        wordItem.addActionListener(e -> generateDocx());
        
        JMenuItem txtItem = new JMenuItem("Εξαγωγή Λίστας σε TXT");
        txtItem.addActionListener(e -> exportToTXT());

        exportMenu.add(wordItem);
        exportMenu.add(txtItem);

        // Προσθήκη όλων στο MenuBar
        menuBar.add(fileMenu);
        menuBar.add(searchMenu);
        menuBar.add(exportMenu);

        // Τοποθέτηση του MenuBar στο JFrame
        setJMenuBar(menuBar);
    }
    
    private void showSearchDialog() {
        // 1. Δημιουργία του UI του διαλόγου
        JTextField searchField = new JTextField(20);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Αναζήτηση (Όνομα, Μάρκα ή Τηλέφωνο):"), BorderLayout.NORTH);
        panel.add(searchField, BorderLayout.CENTER);

        // 2. Πίνακας αποτελεσμάτων
        String[] columns = {"ID", "Ημερομηνία", "Πελάτης", "Μάρκα", "Τηλέφωνο"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        panel.add(scrollPane, BorderLayout.SOUTH);

        // Λειτουργία αναζήτησης καθώς πληκτρολογεί ο χρήστης (Real-time)
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateSearchTable(tableModel, searchField.getText().trim());
            }
        });

        // Αρχικό γέμισμα πίνακα (δείξε τα τελευταία 20)
        updateSearchTable(tableModel, "");

        int result = JOptionPane.showConfirmDialog(this, panel, "Αναζήτηση Επισκευών", 
                     JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && table.getSelectedRow() != -1) {
            int id = (int) table.getValueAt(table.getSelectedRow(), 0);
            loadRecordFromSQLite(id);
        }
    }

    
   

    private void styleButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }
    
    private void updateSearchTable(DefaultTableModel model, String searchText) {
        model.setRowCount(0); // Καθαρισμός πίνακα
        
        // Το query ψάχνει παντού! Το % είναι το "μπαλαντέρ" της SQL
        String sql = "SELECT id, timestamp, customer_name, brand, tel FROM repairs " +
                     "WHERE customer_name LIKE ? OR brand LIKE ? OR tel LIKE ? " +
                     "ORDER BY id DESC LIMIT 50";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String wildCard = "%" + searchText + "%";
            pstmt.setString(1, wildCard);
            pstmt.setString(2, wildCard);
            pstmt.setString(3, wildCard);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("timestamp"),
                    rs.getString("customer_name"),
                    rs.getString("brand"),
                    rs.getString("tel")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Το όνομα είναι υποχρεωτικό!", "Missing Data", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (brandField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Η μάρκα είναι υποχρεωτική!", "Missing Data", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
    
}