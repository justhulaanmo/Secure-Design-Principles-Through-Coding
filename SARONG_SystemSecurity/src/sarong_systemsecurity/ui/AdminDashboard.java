package sarong_systemsecurity.ui;

import java.awt.GridLayout;
import sarong_systemsecurity.database.DBConnection;
import sarong_systemsecurity.models.User;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class AdminDashboard extends javax.swing.JFrame {
    
        User user;
        

    public AdminDashboard(User user) {
        this.user = user;
        initComponents();
        setLocationRelativeTo(null);
        
        labelRole.setText(user.getUsername() + " (admin)");

        loadStudents();
        loadInstructors();
        loadSubjects();
        
        students.setDefaultEditor(Object.class, null);
        instructor.setDefaultEditor(Object.class, null);
        subject.setDefaultEditor(Object.class, null);
    }
    
        private void loadStudents() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Username", "Full Name", "2FA Code"}, 0);
        students.setModel(model);

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT u.username, u.full_name, u.twofa_code FROM users u " +
                         "JOIN students s ON u.id = s.user_id WHERE u.role='student'";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("twofa_code")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInstructors() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Instructor Name", "Full Name", "2FA Code"}, 0);
        instructor.setModel(model);

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT i.instructor_id, i.instructor_name, u.full_name, u.twofa_code " +
                         "FROM instructors i JOIN users u ON i.user_id = u.id";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("instructor_id"),
                        rs.getString("instructor_name"),
                        rs.getString("full_name"),
                        rs.getString("twofa_code")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadSubjects() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Subject Name", "Assigned Instructor"}, 0);
        subject.setModel(model);

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT s.subject_name, i.instructor_name " +
                         "FROM subjects s LEFT JOIN instructors i ON s.subject_id = i.instructor_id"; 
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("subject_name"),
                        rs.getString("instructor_name")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
        
        
        // ==================== ADD BUTTON ====================
    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {
        int selectedTab = jTabbedPane1.getSelectedIndex();
        switch (selectedTab) {
            case 0: addStudentPopup(); break;
            case 1: addInstructorPopup(); break;
            case 2: addSubjectPopup(); break;
        }
    }

    // ==================== EDIT BUTTON ====================
    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {
        int selectedTab = jTabbedPane1.getSelectedIndex();
        switch (selectedTab) {
            case 0: editStudentPopup(); break;
            case 1: editInstructorPopup(); break;
            case 2: editSubjectPopup(); break;
        }
    }

    // ==================== POPUPS ====================
    private void addStudentPopup() {
        JTextField tfFullname = new JTextField();
        JTextField tfUsername = new JTextField();
        JPasswordField pfPassword = new JPasswordField();
        JComboBox<String> cbSubjects = new JComboBox<>();

        cbSubjects.addItem("None");
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT subject_name FROM subjects ORDER BY subject_name");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) cbSubjects.addItem(rs.getString("subject_name"));
        } catch (Exception ex) { ex.printStackTrace(); }

        JPanel panel = new JPanel(new GridLayout(0,1,5,5));
        panel.add(new JLabel("Full Name:")); panel.add(tfFullname);
        panel.add(new JLabel("Username:")); panel.add(tfUsername);
        panel.add(new JLabel("Password:")); panel.add(pfPassword);
        panel.add(new JLabel("Subject (optional):")); panel.add(cbSubjects);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = tfFullname.getText().trim();
            String username = tfUsername.getText().trim();
            String password = new String(pfPassword.getPassword()).trim();
            String subject = cbSubjects.getSelectedItem().toString();
            if (subject.equals("None")) subject = null;

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete required fields");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement chk = con.prepareStatement("SELECT id FROM users WHERE username = ?");
                chk.setString(1, username);
                ResultSet rschk = chk.executeQuery();
                if (rschk.next()) { JOptionPane.showMessageDialog(this, "Username already exists"); return; }

                String twofa = String.valueOf((int)(Math.random()*900000+100000));
                PreparedStatement insUser = con.prepareStatement(
                        "INSERT INTO users (username, full_name, password, role, twofa_code) VALUES (?, ?, ?, 'student', ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                insUser.setString(1, username); insUser.setString(2, name);
                insUser.setString(3, password); insUser.setString(4, twofa);
                insUser.executeUpdate();
                ResultSet gk = insUser.getGeneratedKeys(); gk.next();
                int userId = gk.getInt(1);

                PreparedStatement insStudent = con.prepareStatement(
                        "INSERT INTO students (user_id, student_name, subject_name) VALUES (?,?,?)"
                );
                insStudent.setInt(1, userId); insStudent.setString(2, name);
                if (subject != null) insStudent.setString(3, subject); else insStudent.setNull(3, Types.VARCHAR);
                insStudent.executeUpdate();

                JOptionPane.showMessageDialog(this, "Student added!");
                loadStudents();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void addInstructorPopup() {
        JTextField tfFullname = new JTextField();
        JTextField tfUsername = new JTextField();
        JPasswordField pfPassword = new JPasswordField();
        JTextField tfInstructorName = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0,1,5,5));
        panel.add(new JLabel("Full Name:")); panel.add(tfFullname);
        panel.add(new JLabel("Username:")); panel.add(tfUsername);
        panel.add(new JLabel("Password:")); panel.add(pfPassword);
        panel.add(new JLabel("Instructor Name:")); panel.add(tfInstructorName);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Instructor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String full = tfFullname.getText().trim();
            String username = tfUsername.getText().trim();
            String password = new String(pfPassword.getPassword()).trim();
            String instructorName = tfInstructorName.getText().trim();

            if (full.isEmpty() || username.isEmpty() || password.isEmpty() || instructorName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete required fields");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement chk = con.prepareStatement("SELECT id FROM users WHERE username = ?");
                chk.setString(1, username); ResultSet rschk = chk.executeQuery();
                if (rschk.next()) { JOptionPane.showMessageDialog(this, "Username already exists"); return; }

                String twofa = String.valueOf((int)(Math.random()*900000+100000));
                PreparedStatement insUser = con.prepareStatement(
                        "INSERT INTO users (username, full_name, password, role, twofa_code) VALUES (?, ?, ?, 'teacher', ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                insUser.setString(1, username); insUser.setString(2, full);
                insUser.setString(3, password); insUser.setString(4, twofa);
                insUser.executeUpdate(); ResultSet gk = insUser.getGeneratedKeys(); gk.next();
                int userId = gk.getInt(1);

                PreparedStatement insInstructor = con.prepareStatement(
                        "INSERT INTO instructors (user_id, instructor_name) VALUES (?,?)"
                );
                insInstructor.setInt(1, userId); insInstructor.setString(2, instructorName);
                insInstructor.executeUpdate();

                JOptionPane.showMessageDialog(this, "Instructor added!");
                loadInstructors();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void addSubjectPopup() {
        JTextField tfSubject = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0,1,5,5));
        panel.add(new JLabel("Subject Name:")); panel.add(tfSubject);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Subject", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String subj = tfSubject.getText().trim();
            if (subj.isEmpty()) { JOptionPane.showMessageDialog(this, "Subject name required"); return; }

            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement chk = con.prepareStatement("SELECT subject_id FROM subjects WHERE subject_name = ?");
                chk.setString(1, subj); ResultSet rs = chk.executeQuery();
                if (rs.next()) { JOptionPane.showMessageDialog(this, "Subject already exists"); return; }

                PreparedStatement ins = con.prepareStatement("INSERT INTO subjects (subject_name) VALUES (?)");
                ins.setString(1, subj); ins.executeUpdate();
                JOptionPane.showMessageDialog(this, "Subject added!");
                loadSubjects();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

        
                
            private void editStudentPopup() {
    int selectedRow = students.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Select a student to edit");
        return;
    }

    String oldUsername = students.getValueAt(selectedRow, 0).toString();

    JTextField tfFullname = new JTextField(students.getValueAt(selectedRow, 1).toString());
    JTextField tfUsername = new JTextField(oldUsername);
    JPasswordField pfPassword = new JPasswordField();
    JComboBox<String> cbSubjects = new JComboBox<>();

    cbSubjects.addItem("None");
    try (Connection con = DBConnection.getConnection()) {
        ResultSet rs = con.createStatement().executeQuery("SELECT subject_name FROM subjects ORDER BY subject_name");
        while (rs.next()) cbSubjects.addItem(rs.getString("subject_name"));
        String currentSubj = students.getValueAt(selectedRow, 1).toString();
        if (currentSubj != null) cbSubjects.setSelectedItem(currentSubj);
    } catch (Exception ex) { ex.printStackTrace(); }

    JPanel panel = new JPanel(new GridLayout(0,1,5,5));
    panel.add(new JLabel("Full Name:")); panel.add(tfFullname);
    panel.add(new JLabel("Username:")); panel.add(tfUsername);
    panel.add(new JLabel("Password (leave blank to keep):")); panel.add(pfPassword);
    panel.add(new JLabel("Subject (optional):")); panel.add(cbSubjects);

    int result = JOptionPane.showConfirmDialog(this, panel, "Edit Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
        String name = tfFullname.getText().trim();
        String username = tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword()).trim();
        String subject = cbSubjects.getSelectedItem().toString();
        if (subject.equals("None")) subject = null;

        if (name.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Username required");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // get user_id
            PreparedStatement pst = con.prepareStatement("SELECT id FROM users WHERE username=?");
            pst.setString(1, oldUsername);
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) return;
            int userId = rs.getInt("id");

            // Check if username changed and exists
            if (!username.equals(oldUsername)) {
                PreparedStatement chk = con.prepareStatement("SELECT id FROM users WHERE username=?");
                chk.setString(1, username);
                ResultSet rschk = chk.executeQuery();
                if (rschk.next()) { JOptionPane.showMessageDialog(this, "Username already exists"); return; }
            }

            // Update users
            if (!password.isEmpty()) {
                pst = con.prepareStatement("UPDATE users SET username=?, full_name=?, password=? WHERE id=?");
                pst.setString(1, username); pst.setString(2, name); pst.setString(3, password); pst.setInt(4, userId);
            } else {
                pst = con.prepareStatement("UPDATE users SET username=?, full_name=? WHERE id=?");
                pst.setString(1, username); pst.setString(2, name); pst.setInt(3, userId);
            }
            pst.executeUpdate();

            // Update student table
            pst = con.prepareStatement("UPDATE students SET student_name=?, subject_name=? WHERE user_id=?");
            pst.setString(1, name);
            if (subject != null) pst.setString(2, subject); else pst.setNull(2, Types.VARCHAR);
            pst.setInt(3, userId);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Student updated!");
            loadStudents();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}

            
            private void editInstructorPopup() {
    int selectedRow = instructor.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Select an instructor to edit");
        return;
    }

    // Get instructor_id and current data from JTable
    int instructorId = Integer.parseInt(instructor.getValueAt(selectedRow, 0).toString());
    String instructorName = instructor.getValueAt(selectedRow, 1).toString();
    String fullName = instructor.getValueAt(selectedRow, 2).toString();

    // Fetch the username from database using instructor_id
    String username = "";
    try (Connection con = DBConnection.getConnection()) {
        PreparedStatement pst = con.prepareStatement(
            "SELECT u.username FROM users u JOIN instructors i ON u.id = i.user_id WHERE i.instructor_id=?"
        );
        pst.setInt(1, instructorId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            username = rs.getString("username");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }

    // Create popup fields
    JTextField tfFullname = new JTextField(fullName);
    JTextField tfUsername = new JTextField(username);
    JPasswordField pfPassword = new JPasswordField();
    JTextField tfInstructorName = new JTextField(instructorName);

    JPanel panel = new JPanel(new GridLayout(0,1,5,5));
    panel.add(new JLabel("Full Name:")); panel.add(tfFullname);
    panel.add(new JLabel("Username:")); panel.add(tfUsername);
    panel.add(new JLabel("Password (leave blank to keep):")); panel.add(pfPassword);
    panel.add(new JLabel("Instructor Name:")); panel.add(tfInstructorName);

    int result = JOptionPane.showConfirmDialog(this, panel, "Edit Instructor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) return;

    // Get updated values
    String newFullName = tfFullname.getText().trim();
    String newUsername = tfUsername.getText().trim();
    String password = new String(pfPassword.getPassword()).trim();
    String newInstructorName = tfInstructorName.getText().trim();

    if (newFullName.isEmpty() || newUsername.isEmpty() || newInstructorName.isEmpty()) {
        JOptionPane.showMessageDialog(this, "All fields except password are required");
        return;
    }

    try (Connection con = DBConnection.getConnection()) {
        // Check if username changed and already exists
        if (!newUsername.equals(username)) {
            PreparedStatement chk = con.prepareStatement("SELECT id FROM users WHERE username=?");
            chk.setString(1, newUsername);
            ResultSet rsChk = chk.executeQuery();
            if (rsChk.next()) {
                JOptionPane.showMessageDialog(this, "Username already exists");
                return;
            }
        }

        // Get user_id
        PreparedStatement pst = con.prepareStatement("SELECT user_id FROM instructors WHERE instructor_id=?");
        pst.setInt(1, instructorId);
        ResultSet rsUser = pst.executeQuery();
        if (!rsUser.next()) return;
        int userId = rsUser.getInt("user_id");

        // Update users table
        if (!password.isEmpty()) {
            pst = con.prepareStatement("UPDATE users SET username=?, full_name=?, password=? WHERE id=?");
            pst.setString(1, newUsername);
            pst.setString(2, newFullName);
            pst.setString(3, password);
            pst.setInt(4, userId);
        } else {
            pst = con.prepareStatement("UPDATE users SET username=?, full_name=? WHERE id=?");
            pst.setString(1, newUsername);
            pst.setString(2, newFullName);
            pst.setInt(3, userId);
        }
        pst.executeUpdate();

        // Update instructors table
        pst = con.prepareStatement("UPDATE instructors SET instructor_name=? WHERE instructor_id=?");
        pst.setString(1, newInstructorName);
        pst.setInt(2, instructorId);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Instructor updated successfully!");
        loadInstructors(); // refresh table
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}


            private void editSubjectPopup() {
    int selectedRow = subject.getSelectedRow();
    if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Select a subject to edit"); return; }

    String oldSubject = subject.getValueAt(selectedRow, 0).toString();

    JTextField tfSubject = new JTextField(oldSubject);
    JComboBox<String> cbInstructors = new JComboBox<>();
    cbInstructors.addItem("None");
    try (Connection con = DBConnection.getConnection()) {
        ResultSet rs = con.createStatement().executeQuery("SELECT instructor_name FROM instructors ORDER BY instructor_name");
        while (rs.next()) cbInstructors.addItem(rs.getString("instructor_name"));
    } catch (Exception ex) { ex.printStackTrace(); }

    String currentInstructor = subject.getValueAt(selectedRow, 1) != null ? subject.getValueAt(selectedRow, 1).toString() : "None";
    cbInstructors.setSelectedItem(currentInstructor);

    JPanel panel = new JPanel(new GridLayout(0,1,5,5));
    panel.add(new JLabel("Subject Name:")); panel.add(tfSubject);
    panel.add(new JLabel("Assign Instructor (optional):")); panel.add(cbInstructors);

    int result = JOptionPane.showConfirmDialog(this, panel, "Edit Subject", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
        String newSubject = tfSubject.getText().trim();
        String instructorName = cbInstructors.getSelectedItem().toString();
        if (instructorName.equals("None")) instructorName = null;

        if (newSubject.isEmpty()) { JOptionPane.showMessageDialog(this, "Subject name required"); return; }

        try (Connection con = DBConnection.getConnection()) {
            // Check duplicate subject if changed
            if (!newSubject.equals(oldSubject)) {
                PreparedStatement chk = con.prepareStatement("SELECT subject_id FROM subjects WHERE subject_name=?");
                chk.setString(1, newSubject); ResultSet rs = chk.executeQuery();
                if (rs.next()) { JOptionPane.showMessageDialog(this, "Subject already exists"); return; }
            }

            // Update subject table
            PreparedStatement pst = con.prepareStatement("UPDATE subjects SET subject_name=? WHERE subject_name=?");
            pst.setString(1, newSubject); pst.setString(2, oldSubject);
            pst.executeUpdate();

            // Assign instructor if selected
            if (instructorName != null) {
                // get instructor_id
                PreparedStatement pstIns = con.prepareStatement("SELECT instructor_id FROM instructors WHERE instructor_name=?");
                pstIns.setString(1, instructorName); ResultSet rs = pstIns.executeQuery();
                int instructorId = 0;
                if (rs.next()) instructorId = rs.getInt("instructor_id");

                // Optional: assign instructor to subject
                // you can implement your own logic here if subject table has instructor_id field
            }

            JOptionPane.showMessageDialog(this, "Subject updated!");
            loadSubjects();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}

            private void deleteRow(JTable table, String dbTable, String column, int columnIndex) {
    int row = table.getSelectedRow();
    if (row == -1) { 
        JOptionPane.showMessageDialog(this, "Select a row to delete"); 
        return; 
    }

    String value = table.getValueAt(row, columnIndex).toString();
    int confirm = JOptionPane.showConfirmDialog(this, "Delete " + value + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
    if (confirm != JOptionPane.YES_OPTION) return;

    try (Connection con = DBConnection.getConnection()) {
        PreparedStatement pst = con.prepareStatement("DELETE FROM " + dbTable + " WHERE " + column + "=?");
        pst.setString(1, value);
        pst.executeUpdate();
        JOptionPane.showMessageDialog(this, "Deleted successfully!");
    } catch (Exception e) {
        e.printStackTrace();
    }

    // Reload corresponding table
    switch (table.getName()) {
        case "students": loadStudents(); break;
        case "instructor": loadInstructors(); break;
        case "subject": loadSubjects(); break;
    }
}


        
        

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        labelRole = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        Students = new javax.swing.JScrollPane();
        students = new javax.swing.JTable();
        Instructor = new javax.swing.JScrollPane();
        instructor = new javax.swing.JTable();
        Subjects = new javax.swing.JScrollPane();
        subject = new javax.swing.JTable();
        btnADD = new javax.swing.JButton();
        btnEDIT = new javax.swing.JButton();
        btnDELETE = new javax.swing.JButton();
        btnLOGOUT = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 204, 204));

        jLabel1.setText("Logged in as:");

        jPanel2.setBackground(new java.awt.Color(255, 204, 204));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTabbedPane1.setBackground(new java.awt.Color(255, 204, 204));

        students.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Student ID", "Name of Student"
            }
        ));
        Students.setViewportView(students);

        jTabbedPane1.addTab("Students", Students);

        instructor.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "ID", "Instructor's Name"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        instructor.getTableHeader().setReorderingAllowed(false);
        Instructor.setViewportView(instructor);
        if (instructor.getColumnModel().getColumnCount() > 0) {
            instructor.getColumnModel().getColumn(0).setResizable(false);
            instructor.getColumnModel().getColumn(1).setResizable(false);
        }

        jTabbedPane1.addTab("Instructor", Instructor);

        subject.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Subject's Name", "Instructor"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        Subjects.setViewportView(subject);

        jTabbedPane1.addTab("Subjects", Subjects);

        jPanel2.add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 426, 243));

        btnADD.setText("Add");
        btnADD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnADDActionPerformed(evt);
            }
        });

        btnEDIT.setText("Edit");
        btnEDIT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEDITActionPerformed(evt);
            }
        });

        btnDELETE.setText("Delete");
        btnDELETE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDELETEActionPerformed(evt);
            }
        });

        btnLOGOUT.setText("LOGOUT");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(labelRole, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnLOGOUT))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnADD)
                            .addComponent(btnEDIT)
                            .addComponent(btnDELETE))))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(labelRole, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(50, 50, 50)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(btnLOGOUT)
                        .addGap(82, 82, 82)
                        .addComponent(btnADD)
                        .addGap(18, 18, 18)
                        .addComponent(btnEDIT)
                        .addGap(18, 18, 18)
                        .addComponent(btnDELETE)))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnDELETEActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDELETEActionPerformed
        int selectedTab = jTabbedPane1.getSelectedIndex();
    switch (selectedTab) {
        case 0: deleteRow(students, "users", "username", 0); break;        // Students
        case 1: deleteRow(instructor, "users", "username", 2); break;      // Instructors
        case 2: deleteRow(subject, "subjects", "subject_name", 0); break;  // Subjects
    }
    
    }//GEN-LAST:event_btnDELETEActionPerformed

    private void btnADDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnADDActionPerformed
        int selectedTab = jTabbedPane1.getSelectedIndex();
        switch (selectedTab) {
            case 0: addStudentPopup(); break;
            case 1: addInstructorPopup(); break;
            case 2: addSubjectPopup(); break;
        }
    }//GEN-LAST:event_btnADDActionPerformed

    private void btnEDITActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEDITActionPerformed
        int selectedTab = jTabbedPane1.getSelectedIndex();
        switch (selectedTab) {
            case 0: editStudentPopup(); break;
            case 1: editInstructorPopup(); break;
            case 2: editSubjectPopup(); break;
        }
    }//GEN-LAST:event_btnEDITActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane Instructor;
    private javax.swing.JScrollPane Students;
    private javax.swing.JScrollPane Subjects;
    private javax.swing.JButton btnADD;
    private javax.swing.JButton btnDELETE;
    private javax.swing.JButton btnEDIT;
    private javax.swing.JButton btnLOGOUT;
    private javax.swing.JTable instructor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelRole;
    private javax.swing.JTable students;
    private javax.swing.JTable subject;
    // End of variables declaration//GEN-END:variables
}
