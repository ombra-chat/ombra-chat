package net.zonia3000.ombrachat;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CryptoUtilsTest {

    @BeforeAll
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void testGenerateRandomSalt() throws Exception {
        var salt = CryptoUtils.generateRandomSalt();
        Assertions.assertEquals(32, salt.length());
    }

    @Test
    void testGenerateDerivedKey() throws Exception {
        var salt = "f07bc256d5aa50d0e0f6583f69eb48b5";
        var password = "foo".toCharArray();
        var key = CryptoUtils.generateDerivedKey(password, salt);
        var expectedKey = "3778ee07eb1fc2f4c2a7ee0ba5a6269bee45cf14edd5f64ee759812b312f6b03".getBytes();
        Assertions.assertArrayEquals(expectedKey, key);
    }
}
