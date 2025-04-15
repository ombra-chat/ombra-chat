package net.zonia3000.ombrachat.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.zonia3000.ombrachat.ServiceLocator;
import net.zonia3000.ombrachat.UiException;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.gpg.keybox.BlobType;
import org.bouncycastle.gpg.keybox.KeyBox;
import org.bouncycastle.gpg.keybox.PublicKeyRingBlob;
import org.bouncycastle.gpg.keybox.bc.BcKeyBox;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Credits: part of the code has been copied from
 * https://www.baeldung.com/java-bouncy-castle-pgp-encryption
 */
public class GpgService {

    private static final Logger logger = LoggerFactory.getLogger(GpgService.class);

    private static final String GPG_FILE_PREFIX = "ombra-chat-";
    private static final String GPG_TEXT_FILE_SUFFIX = ".txt.gpg";
    private static final String GPG_GENERIC_FILE_SUFFIX = ".gpg";

    public static class GpgPublicKey {

        private final PGPPublicKey masterKey;
        private final List<PGPPublicKey> availableEncryptionKeys;
        private final List<String> userIds;

        private PGPPublicKey encryptionKey;

        public GpgPublicKey(PGPPublicKey masterKey, List<PGPPublicKey> encryptionKeys) {
            this.masterKey = masterKey;
            this.availableEncryptionKeys = encryptionKeys;
            if (encryptionKeys.size() == 1) {
                encryptionKey = encryptionKeys.get(0);
            }

            userIds = new ArrayList<>();
            Iterator<String> userIdsIte = masterKey.getUserIDs();
            while (userIdsIte.hasNext()) {
                String user = userIdsIte.next();
                userIds.add(user);
            }
        }

        public String getFingerprint() {
            return encryptionKey == null ? null : bytesToHex(encryptionKey.getFingerprint());
        }

        public List<String> getUserIds() {
            return userIds;
        }

        public PGPPublicKey getMasterKey() {
            return masterKey;
        }

        public PGPPublicKey getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(PGPPublicKey key) {
            this.encryptionKey = key;
        }

        public void setEncryptionKey(String fingerprint) {
            this.encryptionKey = availableEncryptionKeys.stream()
                    .filter(k -> fingerprint.equals(bytesToHex(k.getFingerprint())))
                    .findFirst().orElse(null);
        }

        public List<PGPPublicKey> getAvailableEncryptionKeys() {
            return availableEncryptionKeys;
        }

        @Override
        public String toString() {
            var user = userIds.isEmpty() ? "" : userIds.get(0);
            if (encryptionKey == null) {
                return user + " (" + availableEncryptionKeys.size() + " encryption keys)";
            }
            return user + " " + getFingerprint();
        }
    }

    public static class GpgPrivateKey {

        private final PGPPublicKeyEncryptedData pbe;
        private final PGPPrivateKey key;

        public GpgPrivateKey(PGPPublicKeyEncryptedData pbe, PGPPrivateKey key) {
            this.pbe = pbe;
            this.key = key;
        }

        public PGPPublicKeyEncryptedData getPbe() {
            return pbe;
        }

        public PGPPrivateKey getKey() {
            return key;
        }
    }

    private final SettingsService settings;
    private PGPPrivateKey myPrivateKey;
    private PGPPublicKey myPublicKey;

    public GpgService() {
        settings = ServiceLocator.getService(SettingsService.class);
    }

    public boolean hasPrivateKey() {
        return Files.exists(getPrivateKeyPath());
    }

    public List<GpgPublicKey> listKeys() throws UiException {

        Path pubringPath = Paths.get(settings.getPubringPath());
        if (!Files.exists(pubringPath)) {
            throw new UiException("GPG pubring file not found");
        }

        try (InputStream keyIn = new FileInputStream(pubringPath.toFile())) {

            KeyBox kbox = new BcKeyBox(keyIn);

            List<GpgPublicKey> keys = new ArrayList<>();
            for (var keyBlob : kbox.getKeyBlobs()) {
                if (keyBlob.getType() == BlobType.OPEN_PGP_BLOB) {
                    var publicBlob = ((PublicKeyRingBlob) keyBlob).getPGPPublicKeyRing();
                    var key = loadGpgPublicKey(publicBlob.getPublicKeys());
                    if (key != null) {
                        keys.add(key);
                    }
                }
            }
            return keys;
        } catch (IOException ex) {
            logger.error("Error reading public key ring", ex);
            throw new UiException("Error reading public key ring");
        }
    }

    public GpgPublicKey loadPublicKeyFromFile(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            logger.warn("Public key file not found {}", filePath);
            return null;
        }
        logger.debug("Loading public key from file {}", filePath);
        try (InputStream publicKeyIn = new BufferedInputStream(new FileInputStream(filePath))) {

            PGPObjectFactory factory = new PGPObjectFactory(
                    PGPUtil.getDecoderStream(publicKeyIn), new JcaKeyFingerprintCalculator()
            );

            for (var data : factory) {
                if (data instanceof PGPPublicKeyRing publicRing) {
                    var key = loadGpgPublicKey(publicRing.getPublicKeys());
                    if (key != null) {
                        return key;
                    }
                }
            }

            logger.warn("No encryption key found on file {}", filePath);
        } catch (IOException ex) {
            logger.error("Error reading public key file", ex);
        }
        return null;
    }

    private GpgPublicKey loadGpgPublicKey(Iterator<PGPPublicKey> ite) {
        PGPPublicKey masterKey = null;
        List<PGPPublicKey> encryptionKeys = new ArrayList<>();
        while (ite.hasNext()) {
            PGPPublicKey k = ite.next();
            if (k.isMasterKey()) {
                masterKey = k;
            } else if (k.isEncryptionKey()) {
                encryptionKeys.add(k);
            }
        }

        if (masterKey == null || encryptionKeys.isEmpty()) {
            return null;
        }

        return new GpgPublicKey(masterKey, encryptionKeys);
    }

    public PGPPublicKey getEncryptionKey(String fingerprint) {
        Path path = getGpgKeysDirectoryPath().resolve(fingerprint + ".asc");
        if (!Files.exists(path)) {
            logger.error("Key file {} not found", path.toFile().getAbsolutePath());
            return null;
        }
        var key = loadPublicKeyFromFile(path.toFile().getAbsolutePath());
        if (key == null) {
            return null;
        }
        return key.getEncryptionKey();
    }

    public void saveKeyToFile(GpgPublicKey publicKey) throws UiException {
        if (publicKey.getEncryptionKey() == null) {
            throw new UiException("No encryption key selected");
        }
        Path path = getGpgKeysDirectoryPath().resolve(publicKey.getFingerprint() + ".asc");
        PGPPublicKeyRing keyRing = new PGPPublicKeyRing(
                List.of(publicKey.getMasterKey(), publicKey.getEncryptionKey())
        );
        try (ArmoredOutputStream os = new ArmoredOutputStream(new FileOutputStream(path.toFile()))) {
            keyRing.encode(os);
            logger.debug("Saved key to file {}", path.toFile().getAbsolutePath());
        } catch (IOException ex) {
            throw new UiException("Unable to save public key to file");
        }
    }

    public File createGpgTextFile(PGPPublicKey publicKey, String text) {
        try {
            var messagesDir = getGpgMessagesDirectoryPath();
            var tempFile = Files.createTempFile(messagesDir, GPG_FILE_PREFIX, GPG_TEXT_FILE_SUFFIX).toFile();

            File plaintextFile = Files.createTempFile(messagesDir, GPG_FILE_PREFIX, ".txt").toFile();
            try (FileOutputStream fos = new FileOutputStream(plaintextFile)) {
                fos.write(text.getBytes());
            }
            encrypt(publicKey, plaintextFile, tempFile);
            Files.delete(plaintextFile.toPath());
            return tempFile;
        } catch (IOException ex) {
            logger.error("Unable to create encrypted file", ex);
            return null;
        }
    }

    public File createGpgFile(PGPPublicKey publicKey, File plaintextFile) {
        try {
            var fileExtensionPosition = plaintextFile.getName().indexOf(".");
            var suffix = fileExtensionPosition == -1 ? GPG_GENERIC_FILE_SUFFIX
                    : plaintextFile.getName().substring(fileExtensionPosition) + GPG_GENERIC_FILE_SUFFIX;
            var tempFile = Files.createTempFile(getGpgMessagesDirectoryPath(), GPG_FILE_PREFIX, suffix).toFile();
            encrypt(publicKey, plaintextFile, tempFile);
            return tempFile;
        } catch (IOException ex) {
            logger.error("Unable to create encrypted file", ex);
            return null;
        }
    }

    public String encryptText(String plaintext) {
        try {
            File plaintextFile = Files.createTempFile(getGpgMessagesDirectoryPath(), GPG_FILE_PREFIX, ".txt").toFile();
            try (FileOutputStream fos = new FileOutputStream(plaintextFile)) {
                fos.write(plaintext.getBytes());
            }
            var encryptedTextFile = Files.createTempFile(getGpgMessagesDirectoryPath(), GPG_FILE_PREFIX, GPG_TEXT_FILE_SUFFIX).toFile();
            encrypt(myPublicKey, plaintextFile, encryptedTextFile);
            byte[] encryptedData = Files.readAllBytes(encryptedTextFile.toPath());
            String encryptedString = bytesToHex(encryptedData);
            Files.delete(plaintextFile.toPath());
            Files.delete(encryptedTextFile.toPath());
            return encryptedString;
        } catch (IOException ex) {
            logger.error("Unable to create encrypted text", ex);
            return null;
        }
    }

    public String decryptText(String encryptedText) {
        var bytes = hexToBytes(encryptedText);
        try (InputStream encryptedIn = new ByteArrayInputStream(bytes)) {
            return decrypt(encryptedIn, decDataStream -> {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    Streams.pipeAll(decDataStream, outputStream);
                    return outputStream.toString(StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    logger.error("Unable to decrypt string", ex);
                    return null;
                }
            });
        } catch (IOException ex) {
            logger.error("Unable to decrypt string", ex);
            return null;
        }
    }

    private void encrypt(PGPPublicKey key, File plaintext, File destination) {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            try (ByteArrayOutputStream bOut = new ByteArrayOutputStream()) {
                PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
                PGPUtil.writeFileToLiteralData(comData.open(bOut), PGPLiteralData.BINARY, plaintext);
                comData.close();
                byte[] bytes = bOut.toByteArray();
                PGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256).setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .setSecureRandom(new SecureRandom())
                        .setWithIntegrityPacket(true);
                PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(encryptorBuilder);
                encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(key).setProvider(BouncyCastleProvider.PROVIDER_NAME));
                if (key.getKeyID() != myPublicKey.getKeyID()) {
                    encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(myPublicKey).setProvider(BouncyCastleProvider.PROVIDER_NAME));
                }
                try (OutputStream cOut = encGen.open(out, bytes.length)) {
                    cOut.write(bytes);
                }
            }
        } catch (IOException | PGPException ex) {
            logger.error("Unable to create encrypted file", ex);
        }
    }

    public boolean isGpgMessage(TdApi.MessageDocument messageDocument) {
        return messageDocument.document.fileName.startsWith(GPG_FILE_PREFIX) && messageDocument.document.fileName.endsWith(GPG_GENERIC_FILE_SUFFIX);
    }

    public boolean isGpgTextMessage(TdApi.MessageDocument messageDocument) {
        return isGpgMessage(messageDocument) && messageDocument.document.fileName.endsWith(GPG_TEXT_FILE_SUFFIX);
    }

    private Path getGpgMessagesDirectoryPath() {
        Path dir = getGpgDirectoryPath().resolve("messages");
        createDirectoryIfNeeded(dir);
        return dir;
    }

    private Path getGpgKeysDirectoryPath() {
        Path dir = getGpgDirectoryPath().resolve("keys");
        createDirectoryIfNeeded(dir);
        return dir;
    }

    public Path getGpgDirectoryPath() {
        Path dir = Paths.get(settings.getApplicationFolderPath(), "gpg");
        createDirectoryIfNeeded(dir);
        return dir;
    }

    private void createDirectoryIfNeeded(Path dir) {
        if (!Files.exists(dir)) {
            if (!dir.toFile().mkdirs()) {
                logger.error("Unable to create directory {}", dir.toFile().getAbsolutePath());
            }
        }
    }

    public String decryptToString(File encryptedData) {
        try (InputStream encryptedIn = new FileInputStream(encryptedData)) {
            return decrypt(encryptedIn, decDataStream -> {
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    Streams.pipeAll(decDataStream, outputStream);
                    return outputStream.toString(StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    logger.error("Unable to decrypt file", ex);
                    return null;
                }
            });
        } catch (IOException ex) {
            logger.error("Unable to decrypt file", ex);
            return null;
        }
    }

    public File decryptToFile(File encryptedData) {
        try (InputStream encryptedIn = new FileInputStream(encryptedData)) {
            return decrypt(encryptedIn, decDataStream -> {
                var encryptedFilePath = encryptedData.getAbsolutePath();
                var plaintextFilePath = encryptedFilePath.substring(0, encryptedFilePath.length() - GPG_GENERIC_FILE_SUFFIX.length());
                var plaintextFile = new File(plaintextFilePath);
                try (FileOutputStream outputStream = new FileOutputStream(plaintextFile)) {
                    Streams.pipeAll(decDataStream, outputStream);
                    return plaintextFile;
                } catch (IOException ex) {
                    logger.error("Unable to decrypt file", ex);
                    return null;
                }
            });
        } catch (IOException ex) {
            logger.error("Unable to decrypt file", ex);
            return null;
        }
    }

    private <T> T decrypt(InputStream encryptedIn, Function<InputStream, T> func) {
        if (myPrivateKey == null) {
            logger.error("Missing private key");
            return null;
        }

        try {
            JcaPGPObjectFactory pgpObjectFactory = new JcaPGPObjectFactory(encryptedIn);

            Object obj = pgpObjectFactory.nextObject();
            // The first object might be a marker packet
            PGPEncryptedDataList pgpEncryptedDataList = (obj instanceof PGPEncryptedDataList)
                    ? (PGPEncryptedDataList) obj : (PGPEncryptedDataList) pgpObjectFactory.nextObject();

            PGPPublicKeyEncryptedData publicKeyEncryptedData = null;

            Iterator<PGPEncryptedData> encryptedDataItr = pgpEncryptedDataList.getEncryptedDataObjects();
            var keyMatches = false;
            while (!keyMatches && encryptedDataItr.hasNext()) {
                publicKeyEncryptedData = (PGPPublicKeyEncryptedData) encryptedDataItr.next();
                keyMatches = publicKeyEncryptedData.getKeyIdentifier().getKeyId() == myPublicKey.getKeyID();
            }

            if (!keyMatches || publicKeyEncryptedData == null) {
                logger.error("Unable to extract encrypted data");
                return null;
            }

            PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(myPrivateKey);
            InputStream decryptedCompressedIn = publicKeyEncryptedData.getDataStream(decryptorFactory);

            JcaPGPObjectFactory decCompObjFac = new JcaPGPObjectFactory(decryptedCompressedIn);
            PGPCompressedData pgpCompressedData = (PGPCompressedData) decCompObjFac.nextObject();

            InputStream compressedDataStream = new BufferedInputStream(pgpCompressedData.getDataStream());
            JcaPGPObjectFactory pgpCompObjFac = new JcaPGPObjectFactory(compressedDataStream);

            Object message = pgpCompObjFac.nextObject();

            T result;
            if (message instanceof PGPLiteralData) {
                PGPLiteralData pgpLiteralData = (PGPLiteralData) message;
                InputStream decDataStream = pgpLiteralData.getInputStream();
                result = func.apply(decDataStream);
            } else {
                logger.error("Invalid message");
                return null;
            }
            // Performing Integrity check
            if (publicKeyEncryptedData.isIntegrityProtected()) {
                if (!publicKeyEncryptedData.verify()) {
                    logger.error("Message failed integrity check");
                    return null;
                }
            }
            return result;
        } catch (IOException | PGPException ex) {
            logger.error("Unable to decrypt file", ex);
            return null;
        }
    }

    public boolean checkSecretKey(char[] passphrase) {
        Path privateKeyPath = getPrivateKeyPath();
        if (!Files.exists(privateKeyPath)) {
            return false;
        }
        try (InputStream privateKeyIn = new BufferedInputStream(new FileInputStream(privateKeyPath.toFile()))) {
            PGPObjectFactory factory = new PGPObjectFactory(
                    PGPUtil.getDecoderStream(privateKeyIn), new JcaKeyFingerprintCalculator()
            );

            for (var data : factory) {
                if (data instanceof PGPSecretKeyRing secretRing) {
                    var secretKeyIte = secretRing.getSecretKeys();
                    PGPSecretKey masterKey = null;
                    PGPSecretKey encryptionKey = null;
                    while (secretKeyIte.hasNext()) {
                        PGPSecretKey pgpSecretKey = secretKeyIte.next();
                        if (pgpSecretKey.getPublicKey().isMasterKey()) {
                            masterKey = pgpSecretKey;
                        } else if (pgpSecretKey.getPublicKey().isEncryptionKey()) {
                            encryptionKey = pgpSecretKey;
                            break;
                        }
                    }

                    // always prefer non master key for encryption
                    var privateKey = encryptionKey != null ? encryptionKey
                            : masterKey != null && masterKey.getPublicKey().isEncryptionKey() ? masterKey : null;
                    if (privateKey != null) {
                        myPrivateKey = privateKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passphrase));
                        myPublicKey = privateKey.getPublicKey();
                        return true;
                    }
                }
            }
        } catch (IOException | PGPException ex) {
            logger.error("Unable to extract private key", ex);
        }
        return false;
    }

    public Path getPrivateKeyPath() {
        return getGpgDirectoryPath().resolve("private.asc");
    }

    public void getMyPublicKey(PGPSecretKey pgpSecretKey) {
        pgpSecretKey.getPublicKey().getFingerprint();
    }

    public GpgPublicKey getKeyFromFingerprint(GpgPublicKey key, String fingerprint) {
        if (fingerprint.equals(key.getFingerprint())) {
            return key;
        } else {
            for (var ek : key.getAvailableEncryptionKeys()) {
                if (fingerprint.equals(bytesToHex(ek.getFingerprint()))) {
                    return new GpgPublicKey(key.getMasterKey(), List.of(ek));
                }
            }
        }
        return null;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02X", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] bytes = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }
}
