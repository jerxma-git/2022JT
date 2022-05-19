package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.Naming;
import java.rmi.RemoteException;
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
            Bank bank = (Bank) Naming.lookup("///bank");
            RemotePerson person = bank.getRemotePerson(passport);
            if (person == null) {
                person = bank.createPerson(name, surname, passport);
                System.out.println("No person with passport " + passport + " was found. Creating a new person");
            }
            Account acc = person.assignAccount(new RemoteAccount(accId), accId.split(":")[1]);
            try {
                acc.setAmount(newAmount);
                System.out.println("New amount: " +acc.getAmount());
            } catch (RemoteException e) {
                System.err.println("Couldn't change");
            }
        
        } catch (RemoteException e) {

        } catch (Exception e) {
            // TODO: exceps
        }



    }

    

}
