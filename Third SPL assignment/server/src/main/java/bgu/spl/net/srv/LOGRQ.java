package bgu.spl.net.srv;

public class LOGRQ extends packets {
    private String userName;

    public LOGRQ(short opcode, String userName) {
        super((short) 7);
        this.userName = userName;
    }

    public String getUsername() {
        return this.userName;
    }
}