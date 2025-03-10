package net.zonia3000.ombrachat.events;

public class PhoneNumberSet implements Event {

    private final String phoneNumber;

    public PhoneNumberSet(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
