package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */

    Person getRemotePerson(String passport) throws RemoteException;
    Person getLocalPerson(String passport) throws RemoteException;

    RemotePerson createPerson(String name, String surname, String passport) throws RemoteException;

    Account getAccount(String id) throws RemoteException;
    Account getOrCreateAccount(String id) throws RemoteException;

}
