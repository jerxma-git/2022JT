package info.kgeorgiy.ja.zheromskij.rmi;

import java.io.Serializable;
import java.util.HashMap;

public class LocalPerson extends AbstractPerson implements Serializable {
    public LocalPerson(String name, String surname, String passport) {
        super(name, surname, passport);
        this.accounts = new HashMap<>();
    }
}
