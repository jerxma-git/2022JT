package info.kgeorgiy.ja.zheromskij.rmi.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import info.kgeorgiy.ja.zheromskij.rmi.Account;
import info.kgeorgiy.ja.zheromskij.rmi.LocalPerson;
import info.kgeorgiy.ja.zheromskij.rmi.Person;
import info.kgeorgiy.ja.zheromskij.rmi.RemotePerson;

public class BankTest extends BaseTest {

    private static final List<String> NAMES = List.of(
        "Александр",
        "Владимир",
        "John",
        "Hu",
        "Zack"
    );

    private static List<String> SURNAMES = List.of(
        "Пашук",
        "Волочай",
        "Doe",
        "Tao",
        "Snyder"
    );

    private static List<String> PASSPORTS = List.of(
        "401253534",
        "301198981",
        "281377236",
        "674385372",
        "234435875"
    );
    

    private void createPersons() throws RemoteException {
        for (int i = 0; i < PASSPORTS.size(); i++) {
            bank.createPerson(NAMES.get(i), SURNAMES.get(i), PASSPORTS.get(i));
        }
    }

    /** 
     * @throws RemoteException
     */
    @Test
    void createPersonsTest() throws RemoteException {
        for (String passport : PASSPORTS) {
            assertEquals(null, bank.getLocalPerson(passport));
        }

        createPersons();

        for (int i = 0; i < PASSPORTS.size(); i++) {
            Person person = bank.getRemotePerson(PASSPORTS.get(i));
            assertEquals(NAMES.get(i), person.getName());
            assertEquals(SURNAMES.get(i), person.getSurname());
            assertEquals(PASSPORTS.get(i), person.getPassport());
        }
    }

    private static final List<String> subIds = List.of(
        "asuhsfiuhsdfg",
        "jsdfjo82348234o",
        "234",
        "asd",
        "oiasudhf"
    );

    private static final List<String> ids = IntStream.range(0, subIds.size())
            .boxed()
            .map(i -> PASSPORTS.get(i) + ":" + subIds.get(i)).toList();

    @Test
    void createAccountsTest() throws RemoteException {
        for (String id : ids) {
            assertEquals(null, bank.getAccount(id));
        }

        for (String id : ids) {
            Account acc = bank.getOrCreateAccount(id);
            assertEquals(acc, bank.getAccount(id));
        }
    }
    
    

    @Test
    void personsType() throws RemoteException {
        createPersons();
        for (String passportNumber : PASSPORTS) {
            assertInstanceOf(LocalPerson.class, bank.getLocalPerson(passportNumber));
            assertInstanceOf(RemotePerson.class, bank.getRemotePerson(passportNumber));
        }
    }

    @Test
    void setAmountTest() throws RemoteException {
        createPersons();
        for (String id : ids) {
            bank.getOrCreateAccount(id);
        }

        for (String id : ids) {
            Account acc = bank.getAccount(id);
            System.out.println(acc);
            assertEquals(0, acc.getAmount());
            acc.setAmount(1000);
            assertEquals(1000, acc.getAmount());
        }

    }

    
}
