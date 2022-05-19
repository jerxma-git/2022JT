package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */

    RemotePerson getRemotePerson(String passport) throws RemoteException;
    LocalPerson getLocalPerson(String passport) throws RemoteException;

    RemotePerson createPerson(String name, String surname, String passport) throws RemoteException;

    Account getAccount(String id) throws RemoteException;

}
