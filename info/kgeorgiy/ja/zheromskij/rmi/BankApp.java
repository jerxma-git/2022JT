package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Objects;

public class BankApp {
    
    public static void main(String[] args) {

        if (args == null || args.length != 5) {
            System.out.println("Args: name surname passport accountID newAmount");
        }
        
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Some arguments are null");
        }

        String name = args[0];
        String surname = args[1];
        String passport = args[2];
        String accId = args[3];
        int newAmount = Integer.parseInt(args[4]);

        try {
            Registry registry = LocateRegistry.getRegistry(null, 1099);
            Bank bank = (Bank) registry.lookup("//localhost/bank");
            Person person = bank.getRemotePerson(passport);
            if (person == null) {
                person = bank.createPerson(name, surname, passport);
                System.out.println("No person with passport " + passport + " was found. Creating a new person");
            }
            try {
                Account acc = bank.getOrCreateAccount(accId);
                acc.setAmount(newAmount);
                System.out.println("New amount: " + acc.getAmount());
            } catch (RemoteException e) {
                System.err.println("Couldn't change the amount");
            }
        
        } catch (RemoteException e) {
            System.err.println("Failed to receive the person");
        } catch (NotBoundException e) {
            System.out.println("Bank isn't bound");
        } catch (Exception e) {
            System.out.println("бедомба жесть");
            e.printStackTrace();
        }



    }

    

}
