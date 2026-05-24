package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Connection;

public interface HelperInterface extends Remote{

    public boolean addStudentToDb(String name, String dep, String sec, int year) throws RemoteException;

    public boolean addTeacherToDb(String name, String dep, String sec, int year) throws RemoteException;

    public String connectToDatabase() throws RemoteException;

    public Connection getConn() throws RemoteException;
}
