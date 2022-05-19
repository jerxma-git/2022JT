package info.kgeorgiy.ja.zheromskij.rmi.test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.rmi.RemoteException;
import java.util.List;
import java.util.stream.IntStream;

import javax.print.attribute.standard.MediaSize.NA;

import org.junit.Test;

import info.kgeorgiy.ja.zheromskij.rmi.Person;

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
    
    /** 
     * @throws RemoteException
     */
    @Test
    void createPersonsTest() throws RemoteException {
        for (String passport : PASSPORTS) {
            assertEquals(null, bank.getLocalPerson(passport));
        }

        for (int i = 0; i < PASSPORTS.size(); i++) {
            bank.createPerson(NAMES.get(i), SURNAMES.get(i), PASSPORTS.get(i));
        }

        for (int i = 0; i < PASSPORTS.size(); i++) {
            Person person = bank.getRemotePerson(PASSPORTS.get(i));
            assertEquals(NAMES.get(i), person.getName());
            assertEquals(SURNAMES.get(i), person.getSurname());
            assertEquals(PASSPORTS.get(i), person.getPassport());
        }
    }

    


    
}
