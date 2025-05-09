package net.zonia3000.ombrachat;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

public class CryptoUtils {

    private static final int RANDOM_PASSWORD_LENGTH = 25;

    public static byte[] generateRandomBytes(int length) {
        byte[] randomData = new byte[length];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomData);
        return randomData;
    }

    /**
     * Generates random salt and returns it as a hexadecimal string.
     *
     * @return the salt in hex format
     */
    public static String generateRandomSalt() {
        byte[] randomData = generateRandomBytes(16);
        return Hex.toHexString(randomData);
    }

    public static String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[RANDOM_PASSWORD_LENGTH];
        random.nextBytes(bytes);
        StringBuilder password = new StringBuilder();
        for (byte b : bytes) {
            // Maps to printable ASCII characters (33-126)
            password.append((char) (Math.abs(b % 94) + 32));
        }
        return password.toString();
    }

    /**
     * Generates the derived key for encrypting local tdlib database. Using
     * PBKDF2-HMAC-SHA256 on a password provided by the user is acceptable for
     * desktop applications where the user doesn't rely on notifications
     * arriving in the background when they are not using the app. See
     * https://github.com/tdlib/td/issues/188
     *
     * @param password
     * @param salt
     * @return the derived key
     */
    public static byte[] generateDerivedKey(char[] password, String salt) {
        try {
            int iterations = 100000; // Number of iterations
            int keyLength = 256; // 256 bits

            // Convert salt to byte array
            byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factoryBC = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256", BouncyCastleProvider.PROVIDER_NAME);
            KeySpec keyspecBC = new PBEKeySpec(password, saltBytes, iterations, keyLength);
            SecretKey keyBC = factoryBC.generateSecret(keyspecBC);
            return Hex.toHexString(keyBC.getEncoded()).getBytes(StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }
}
