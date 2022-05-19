package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemotePerson extends AbstractPerson {
    

    public RemotePerson(String name, String surname, String passport) {
        super(name, surname, passport);
        this.accounts = new ConcurrentHashMap<>();
    }


    public LocalPerson localCopy() {
        return new LocalPerson(name, surname, passport);
    }




  



    

    

}
