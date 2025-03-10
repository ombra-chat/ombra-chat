package net.zonia3000.ombrachat.events;

public class AuthenticationPasswordSet implements Event {

    private final String password;

    public AuthenticationPasswordSet(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
