package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {
    protected String passport;
    protected String name;
    protected String surname;

    protected Map<String, Account> accounts;

    public AbstractPerson(String name, String surname, String passport) {
        // TODO: checcc nulz
        this.name = name;
        this.surname = surname;
        this.passport = passport;
    }

    @Override
    public Account assignAccount(Account acc, String subId) {
        return accounts.putIfAbsent(subId, acc);
    }
    
    // TODO: decide subid vs id
    // бедомба главная тут (перепутаны id и subId в некоторых местах)
    public Account getAccount(String subId) {
        return accounts.getOrDefault(subId, null);
    }

    public String getPassport() {
        return this.passport;
    }

    public String getName() {
        return this.name;
    }

    public String getSurname() {
        return this.surname;
    }


    @Override
    public String toString() {
        return "{" +
            " passport='" + getPassport() + "'" +
            ", name='" + getName() + "'" +
            ", surname='" + getSurname() + "'" +
            "}";
    }
    


}
