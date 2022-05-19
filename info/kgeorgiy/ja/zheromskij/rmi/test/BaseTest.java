package info.kgeorgiy.ja.zheromskij.rmi.test;



import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import info.kgeorgiy.ja.zheromskij.rmi.*;

public class BaseTest {
    protected Bank bank;

    private static final String REGISTRY_URL = "//localhost/bank";
    private static Registry registry;
    private static final int REGISTRY_PORT = 1099;
    private static final int BANK_PORT = 1337;
    
    
    @BeforeAll
    static void startRegistry() throws RemoteException {
        try {
            registry = LocateRegistry.createRegistry(REGISTRY_PORT);
        } catch (ExportException ignored) {
            registry = LocateRegistry.getRegistry(REGISTRY_PORT);
        }
    }

    @BeforeEach
    void startBank() throws RemoteException {
        bank = new RemoteBank(BANK_PORT);
        UnicastRemoteObject.exportObject(bank, BANK_PORT);
        registry.rebind(REGISTRY_URL, bank);
    }

    @AfterEach
    void stopBank() throws RemoteException, NotBoundException {
        registry.unbind(REGISTRY_URL);
        UnicastRemoteObject.unexportObject(bank, false);
    }
}
