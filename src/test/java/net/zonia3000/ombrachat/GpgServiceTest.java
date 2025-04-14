package net.zonia3000.ombrachat;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import net.zonia3000.ombrachat.services.GpgService;
import net.zonia3000.ombrachat.services.SettingsService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class GpgServiceTest {

    private static final String TEST_SECRET_KEY = """
-----BEGIN PGP PRIVATE KEY BLOCK-----

lIYEZ/GtahYJKwYBBAHaRw8BAQdAC8A66aczoI4L6XQ0yPksmPUh8NuDTuApbYwp
vFxMlGn+BwMClgM2NQ+22J3/I0lKROIGldbtmmaTS8A09xAdtsigMDhwJPnnWmPH
Ot/FUflD4Fs3BuLF7jbwn5lCZSyMrba2QpwJkdtsmLXcRUorxkDBCrQcT21icmEg
Q2hhdCA8b21icmFAbG9jYWxob3N0PoiTBBMWCgA7FiEEr0Z18j1sMP6qQNxKI3oX
EBkgYNcFAmfxrWoCGwMFCwkIBwICIgIGFQoJCAsCBBYCAwECHgcCF4AACgkQI3oX
EBkgYNdiMAD+JTEagCagfC7+B0T8jKjVaswhoa5TBiH562aCbx8bJbgA/1qI8orx
0fMUm300xg57HuGScE0i2DlXoDAnuIlJAFoKnIsEZ/GtahIKKwYBBAGXVQEFAQEH
QOBwXcUPoC07phPjICm+M+APB91dy7Boljg/KFaQlD1vAwEIB/4HAwKLjkR7ka+7
O/9182jsCDQCcg9E9+xa+82pcQ5jXMDQnEKaOlPXkl+8othU+xNU1TRfjmczVkjM
qGzNzbaa0CZ9/DR1k9daFQLxmC5q2alFiHgEGBYKACAWIQSvRnXyPWww/qpA3Eoj
ehcQGSBg1wUCZ/GtagIbDAAKCRAjehcQGSBg13qyAQDqHPQEQxV7uAErZfiSVDx/
BBk9OLZ5csa8GOb0Hq/BbAD9G8e59ucHxcAvljgVfW9Z2T5qeJ+odNGggjqXqeUV
hQk=
=RCCN
-----END PGP PRIVATE KEY BLOCK-----
""";

    private static final String TEST_PUBLIC_KEY = """
-----BEGIN PGP PUBLIC KEY BLOCK-----
                                                  
mDMEZ/GtahYJKwYBBAHaRw8BAQdAC8A66aczoI4L6XQ0yPksmPUh8NuDTuApbYwp
vFxMlGm0HE9tYnJhIENoYXQgPG9tYnJhQGxvY2FsaG9zdD6IkwQTFgoAOxYhBK9G
dfI9bDD+qkDcSiN6FxAZIGDXBQJn8a1qAhsDBQsJCAcCAiICBhUKCQgLAgQWAgMB
Ah4HAheAAAoJECN6FxAZIGDXYjAA/iUxGoAmoHwu/gdE/Iyo1WrMIaGuUwYh+etm
gm8fGyW4AP9aiPKK8dHzFJt9NMYOex7hknBNItg5V6AwJ7iJSQBaCrg4BGfxrWoS
CisGAQQBl1UBBQEBB0DgcF3FD6AtO6YT4yApvjPgDwfdXcuwaJY4PyhWkJQ9bwMB
CAeIeAQYFgoAIBYhBK9GdfI9bDD+qkDcSiN6FxAZIGDXBQJn8a1qAhsMAAoJECN6
FxAZIGDXerIBAOoc9ARDFXu4AStl+JJUPH8EGT04tnlyxrwY5vQer8FsAP0bx7n2
5wfFwC+WOBV9b1nZPmp4n6h00aCCOpep5RWFCQ==
=ogV1
-----END PGP PUBLIC KEY BLOCK-----
""";

    private static final String TEST_SECRET_KEY_PASSWORD = "test";
    private static final String TEST_KEY_FINGERPRINT = "49A0C0308A6AC2B19B8459B59AB4084AD167DFCA";

    private static Path tmpAppFolder;
    private static Path pubring;
    private static Path publicKeyPath;

    @Mock
    private SettingsService settings;

    private GpgService gpgService;

    @BeforeAll
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
        try {
            tmpAppFolder = Files.createTempDirectory("gpg-test");
            var gpgFolder = tmpAppFolder.resolve("gpg");
            gpgFolder.toFile().mkdirs();

            var privateKeyPath = gpgFolder.resolve("private.asc");
            Files.writeString(privateKeyPath, TEST_SECRET_KEY);
            publicKeyPath = tmpAppFolder.resolve("public.asc");
            Files.writeString(publicKeyPath, TEST_PUBLIC_KEY);

            pubring = tmpAppFolder.resolve("pubring.kbx");
            try (InputStream in = GpgServiceTest.class.getClassLoader().getResourceAsStream("pubring.kbx")) {
                Files.copy(in, pubring);
            }
        } catch (IOException ex) {
            throw new IOError(ex);
        }
    }

    @BeforeEach
    public void initServices() {
        MockitoAnnotations.openMocks(this);
        ServiceLocator.registerService(SettingsService.class, settings);
        Mockito.when(settings.getApplicationFolderPath()).thenReturn(tmpAppFolder.toFile().getAbsolutePath());
        Mockito.when(settings.getPubringPath()).thenReturn(pubring.toFile().getAbsolutePath());

        gpgService = new GpgService();
        Assertions.assertTrue(gpgService.checkSecretKey(TEST_SECRET_KEY_PASSWORD.toCharArray()));
    }

    @Test
    void testEncryptAndDecryptTextFile() throws Exception {
        var keys = gpgService.listKeys();
        var key = keys.get(0);
        var plaintext = "Encryption test";
        File file = gpgService.createGpgTextFile(key.getEncryptionKey(), plaintext);
        Assertions.assertTrue(file.length() > 0);
        var result = gpgService.decryptToString(file);
        Assertions.assertEquals(plaintext, result);
    }

    @Test
    void testEncryptAndDecryptText() {
        var plaintext = "Encryption test";
        var encrypted = gpgService.encryptText(plaintext);
        var decrypted = gpgService.decryptText(encrypted);
        Assertions.assertEquals(plaintext, decrypted);
    }

    @Test
    void testReadPublicKeyFromFile() {
        var key = gpgService.loadPublicKeyFromFile(publicKeyPath.toFile().getAbsolutePath());
        Assertions.assertNotNull(key);
        Assertions.assertEquals(TEST_KEY_FINGERPRINT, key.getFingerprint());
    }

    @Test
    void testExtractKeyFromPubring() throws Exception {
        var keys = gpgService.listKeys();
        var key = keys.get(0);
        Assertions.assertEquals(TEST_KEY_FINGERPRINT, key.getFingerprint());
        gpgService.saveKeyToFile(key);
        var readedKey = gpgService.getEncryptionKey(key.getFingerprint());
        Assertions.assertNotNull(readedKey);
    }
}
