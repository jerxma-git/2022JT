package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    String getName() throws RemoteException;
    String getSurname() throws RemoteException;
    String getPassport() throws RemoteException;

    Account assignAccount(Account account, String subId) throws RemoteException;
    Account getAccount(String subId) throws RemoteException;
}
