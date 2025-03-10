package net.zonia3000.ombrachat.events;

public class AuthenticationCodeSet implements Event {

    private final String code;

    public AuthenticationCodeSet(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
