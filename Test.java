import java.util.stream.*;
import java.util.*;


public class Test extends Test2{
    // public Test(int i) {
    //     super(i);
    // }

    public static void main(String[] args) {
        Integer[] ints = {1,2,3,4};
        Double[] doubles = {5.6, 7.8}; 
        for (Object o : Stream.of(ints, doubles).map(Arrays::stream).flatMap(i->i).collect(Collectors.toList())) {
            System.out.println(o);
        }
    }    
}
