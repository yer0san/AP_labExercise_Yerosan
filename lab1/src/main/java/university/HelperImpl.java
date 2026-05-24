package university;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import shared.HelperInterface;

public class HelperImpl extends  UnicastRemoteObject implements HelperInterface {
    
    // yeah i know i'm pushing my credentials, sue me
    private static final String DB_URL  = "jdbc:mariadb://localhost:3306/university";
    private static final String DB_USER = "uni_manager";
    private static final String DB_PASS = "1234";

    private Connection conn;

    public HelperImpl() throws RemoteException {
        super();
    }
    
    public boolean addStudentToDb(String name, String dep, String sec, int year) throws RemoteException {
        return new Student(name, dep, sec, year).addToDb(conn);
    }

    public boolean addTeacherToDb(String name, String dep, String sec, int year) throws RemoteException {
        return new Teacher(name, dep, sec, year).addToDb(conn);
    }

    public String connectToDatabase() throws RemoteException{
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("DB connected.");
            return "connected";

        } catch (SQLException e) {
            System.out.println("DB failed: " + e.getMessage());
            
            return "Db failed: " + e.getMessage();
            
        }
    }

    public Connection getConn() throws RemoteException{
        return conn;
    }
}
