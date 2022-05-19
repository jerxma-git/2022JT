package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, RemoteAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RemotePerson> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public RemotePerson createPerson(final String name, final String surname, final String passport) {
        return persons.computeIfAbsent(passport, p -> {
            RemotePerson rp = new RemotePerson(name, surname, passport);

            try {
                UnicastRemoteObject.exportObject(rp, port);
            } catch (RemoteException e) {
                // TODO: remove bedomba
                System.err.println("Error: person export failed");
                return null;
            }
            return rp;
        });
    }

    @Override
    public RemotePerson getRemotePerson(final String passport) {
        // TODO: checks
        return persons.get(passport);
    }

    @Override
    public LocalPerson getLocalPerson(final String passport) {
        RemotePerson rp = getRemotePerson(passport);
        return rp == null ? null : rp.localCopy();
    }


    private String[] parseId(String id) {
        String[] parts = id.split(":");
        if (parts.length != 2) {
            System.err.println("Invalid id: " + id);
            return null;
        }
        return parts;
    }

    @Override
    public Account getOrCreateAccount(String id) {
        String[] parts = parseId(id);
        if (parts == null) {
            return null;
        }
        String passport = parts[0];
        String subId = parts[1];
        RemotePerson person = getRemotePerson(passport);

        if (person == null) {
            System.err.println("Person with passport " + passport + " wasn't found");
            return null;
        }

        RemoteAccount acc = accounts.computeIfAbsent(id, ignored -> {
            RemoteAccount racc = new RemoteAccount(id);
            try {
                UnicastRemoteObject.exportObject(racc, port);
            } catch (RemoteException e) {
                System.err.println("Couldn't export remote object");
                return null;
            }
            return racc;
        });
        person.assignAccount(acc, subId);
        return acc;
    }

    @Override
    public Account getAccount(String id) {
        String[] parts = parseId(id);
        if (parts == null) {
            return null;
        }
        String passport = parts[0];
        String subId = parts[1];
        RemotePerson person = getRemotePerson(passport);
        if (person == null) {
            System.out.println("Incorrect uid");
            return null;
        }
        return person.getAccount(subId);
    }


   
}
