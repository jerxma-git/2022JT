package info.kgeorgiy.ja.zheromskij.rmi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.kgeorgiy.ja.zheromskij.rmi.BankApp;
import info.kgeorgiy.ja.zheromskij.rmi.Person;

public class BankAppTest extends BaseTest {


    private static List<List<String>> PERSONS_LATIN = List.of(
            List.of("Zack", "Snyder", "2344325875"),
            List.of("Hu", "Tao", "674385372"));

    private static List<List<String>> PERSONS_CYRILLIC = List.of(
            List.of("Никита", "Жмышенко", "42069228"),
            List.of("Алишьеьр", "Моргьеьштьеьн", "666666666"));

    private static List<List<String>> PERSONS_ARABIC = List.of(

    );

    private static List<List<String>> PERSONS_ALL = Stream.of(
            PERSONS_LATIN,
            PERSONS_CYRILLIC,
            PERSONS_ARABIC)
            .flatMap(List::stream).toList();

    private List<String> DEFAULT_ACCOUNTS = PERSONS_ALL
            .stream()
            .map(list -> list.get(2) + ":" + list.hashCode())
            .toList();

    private static final List<String> SPECIAL_PERSON = PERSONS_ALL.get(0); 


    @BeforeEach
    void initBank() throws RemoteException {
        for (List<String> personData : PERSONS_ALL) {
            bank.createPerson(personData.get(0), personData.get(1), personData.get(2));
        }
        for (String accountId : DEFAULT_ACCOUNTS) {
            bank.getOrCreateAccount(accountId);
        }
    }

    void createSpecialPerson() throws RemoteException {
        bank.createPerson(SPECIAL_PERSON.get(0), SPECIAL_PERSON.get(1), SPECIAL_PERSON.get(2));
    }

    @Test
    void existingAccountsTest() throws RemoteException {
        for (int index = 0; index < PERSONS_ALL.size(); index++) {
            List<String> personData = PERSONS_ALL.get(index);
            String[] args = {
                personData.get(0), 
                personData.get(1), 
                personData.get(2), 
                DEFAULT_ACCOUNTS.get(index), 
                String.valueOf(index)};
            BankApp.main(args);
            assertEquals(index, bank.getAccount(DEFAULT_ACCOUNTS.get(index)).getAmount());
        }
    }


    private static final List<List<String>> UNREGISTERED_PERSONS = List.of(
        List.of("Imnot","Registered","0000000"),
        List.of("Imalso", "Notregistered", "00000001")
    );

    @Test 
    void nonExistentPersonsAndAccountsTest() throws RemoteException {
        for (List<String> personData : UNREGISTERED_PERSONS) {
            String[] args = {
                personData.get(0), 
                personData.get(1), 
                personData.get(2),
                personData.get(2) + ":111",
                "15"
            };
            System.out.println(Arrays.stream(args).collect(Collectors.joining(", ", "running main with args: ", ".")));
            BankApp.main(args);
            Person person = bank.getRemotePerson(personData.get(2));
            assertNotNull(person);
            // assertEquals(15, person.getAccount("111").getAmount());
        }

    }


    @Test
    void parallel() throws RemoteException, InterruptedException {
        createSpecialPerson();
        String[] args = {
            SPECIAL_PERSON.get(0), 
            SPECIAL_PERSON.get(1), 
            SPECIAL_PERSON.get(2), 
            SPECIAL_PERSON.get(2) + ":12345", 
            "123"};
        var pool = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 100; i++) {
            pool.submit(() -> {
                BankApp.main(args);
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(123, bank.getAccount(args[3]).getAmount());
    }

    static String getSubId(String id) {
        return id.substring(id.indexOf(":") + 1);
    }

}
