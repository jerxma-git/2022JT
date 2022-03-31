package info.kgeorgiy.ja.zheromskij.walk;

public class WalkException extends Exception {

    @Override 
    public String getMessage() {
        return String.format("Something went wrong while walking files:%n%s",  super.getMessage());
    }
}

