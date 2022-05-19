package info.kgeorgiy.ja.zheromskij.rmi.test;

import static org.junit.Assert.assertEquals;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.kgeorgiy.ja.zheromskij.rmi.BankApp;


public class BankAppTest extends BaseTest{
    
    private static String[] args0 = {"Zack", "Snyder", "2344325875", "2344325875:oiasudhf", "4000"};
    private static String[] args1 = {"Hu", "Tao", "674385372", "674385372:asd", "250"};
    // private static String[] args2 = {"Hu", "Tao", "674385372", "674385372:asd", "832648"};
    // private static String[] args3 = {"Hu", "Tao", "674385372", "674385372:asd", "-1"};
    // private static List<String[]> argss = List.of(args1, args2, args3);
    private static List<String[]> data = List.of(args0, args1);

    @BeforeEach
    void initBank() throws RemoteException{
        for (String[] args : data) {
            bank.createPerson(args[0], args[1], args[2]);
            bank.getOrCreateAccount(args[3]);
        }
    }

    @Test
    void mainTest() throws RemoteException {
        BankApp.main(args1);
        assertEquals(args1[4], bank.getAccount(args1[3]).getAmount());

    }


    // @Test
    // void parallel() throws RemoteException, InterruptedException {
    //     var pool = Executors.newFixedThreadPool(100);
    //     for (int i = 0; i < 100; i++) {
    //         pool.submit(() -> {
    //             BankApp.main(args1);
    //         });
    //     }
    //     pool.shutdown();
    //     pool.awaitTermination(10, TimeUnit.SECONDS);
    //     assertEquals(args1[4], bank.getRemotePerson(args1[2]).getAccount(args1[3]).getAmount());
    // }

    
    
}
