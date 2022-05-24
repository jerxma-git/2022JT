package info.kgeorgiy.ja.zheromskij.rmi;

import java.rmi.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RemotePerson extends AbstractPerson {
    

    public RemotePerson(String name, String surname, String passport) {
        super(name, surname, passport);
        this.accounts = new ConcurrentHashMap<>();
    }


    public LocalPerson localCopy() {
        return new LocalPerson(name, surname, passport);
    }

    @Override
    public String toString() {
        return "accs:" + this.accounts.keySet().stream().collect(Collectors.joining("_"));
    }




  



    

    

}
