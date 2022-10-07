import java.util.Random;

class DoesSomething {

    static {
        new Random();
    }

    private Object fieldMember = new Random();

    public void foo() {
        Random r = new Random();
    }

    public void foo2() {
        Random r = new java.util.Random();
    }
}