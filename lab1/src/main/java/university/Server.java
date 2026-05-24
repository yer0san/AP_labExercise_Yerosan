package university;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Server {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            
            HelperImpl helper = new HelperImpl();

            Naming.rebind("rmi://localhost/helper", helper);

            System.out.println("Server started at 1099 :: rmi://localhost/helper...");
            
        } catch(Exception e){
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
