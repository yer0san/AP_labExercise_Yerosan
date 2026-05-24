package university;

import java.sql.*;

public class Teacher {
    public String name;
    public String dep;
    public String sec;
    public int year;
    
    public Teacher(String name, String dep, String sec, int year) {
        this.name = name;
        this.dep  = dep;
        this.sec  = sec;
        this.year = year;
    }

    public boolean addToDb(Connection conn){
        String sql = "INSERT INTO teachers (name, dep, sec, year) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, dep);
            ps.setString(3, sec);
            ps.setInt(4, year);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error saving teacher: " + e.getMessage());
            return false;
        }
    }
}