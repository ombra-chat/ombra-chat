package net.zonia3000.ombrachat.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
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
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
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

        private String fingerprint;
        private List<String> userIds;
        private PGPPublicKey publicKey;

        public String getFingerprint() {
            return fingerprint;
        }

        public void setFingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        public List<String> getUserIds() {
            return userIds;
        }

        public void setUserIds(List<String> userIds) {
            this.userIds = userIds;
        }

        public PGPPublicKey getEncryptionKey() {
            return publicKey;
        }

        public void setEncryptionKey(PGPPublicKey key) {
            this.publicKey = key;
        }

        @Override
        public String toString() {
            return userIds.isEmpty() ? fingerprint : userIds.get(0) + " " + fingerprint;
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

    public List<GpgPublicKey> listKeys() {

        List<GpgPublicKey> keys = new ArrayList<>();

        Path pubringPath = Paths.get(settings.getPubringPath());
        if (!Files.exists(pubringPath)) {
            return keys;
        }

        try (InputStream keyIn = new FileInputStream(pubringPath.toFile())) {

            KeyBox kbox = new BcKeyBox(keyIn);

            for (var keyBlob : kbox.getKeyBlobs()) {
                if (keyBlob.getType() == BlobType.OPEN_PGP_BLOB) {
                    var publicBlob = ((PublicKeyRingBlob) keyBlob).getPGPPublicKeyRing();
                    var publicKey = publicBlob.getPublicKey();

                    PGPPublicKey encryptionKey = null;
                    Iterator<PGPPublicKey> ite = publicBlob.getPublicKeys();
                    while (ite.hasNext()) {
                        PGPPublicKey k = ite.next();
                        if (k.isEncryptionKey()) {
                            encryptionKey = k;
                            break;
                        }
                    }

                    GpgPublicKey key = new GpgPublicKey();
                    key.setEncryptionKey(encryptionKey);
                    key.setFingerprint(bytesToHex(publicKey.getFingerprint()));
                    List<String> userIds = new ArrayList<>();
                    Iterator<String> userIdsIte = publicKey.getUserIDs();
                    while (userIdsIte.hasNext()) {
                        String user = userIdsIte.next();
                        userIds.add(user);
                    }
                    key.setUserIds(userIds);
                    keys.add(key);
                }
            }
        } catch (IOException ex) {
            throw new IOError(ex);
        }

        return keys;
    }

    public PGPPublicKey getEncryptionKey(String fingerprint) {
        List<GpgPublicKey> keys = listKeys();
        for (var key : keys) {
            if (fingerprint.equals(key.getFingerprint())) {
                return key.getEncryptionKey();
            }
        }
        return null;
    }

    public File createGpgTextFile(PGPPublicKey publicKey, String text) {
        try {
            var tempFile = Files.createTempFile(getGpgDirectoryPath(), GPG_FILE_PREFIX, GPG_TEXT_FILE_SUFFIX).toFile();

            File plaintextFile = Files.createTempFile(getGpgDirectoryPath(), GPG_FILE_PREFIX, ".txt").toFile();
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
            var tempFile = Files.createTempFile(getGpgDirectoryPath(), GPG_FILE_PREFIX, suffix).toFile();
            encrypt(publicKey, plaintextFile, tempFile);
            return tempFile;
        } catch (IOException ex) {
            logger.error("Unable to create encrypted file", ex);
            return null;
        }
    }

    public String encryptText(String plaintext) {
        try {
            File plaintextFile = Files.createTempFile(getGpgDirectoryPath(), GPG_FILE_PREFIX, ".txt").toFile();
            try (FileOutputStream fos = new FileOutputStream(plaintextFile)) {
                fos.write(plaintext.getBytes());
            }
            var encryptedTextFile = Files.createTempFile(getGpgDirectoryPath(), GPG_FILE_PREFIX, GPG_TEXT_FILE_SUFFIX).toFile();
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
                PGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256).setProvider("BC")
                        .setSecureRandom(new SecureRandom())
                        .setWithIntegrityPacket(true);
                PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(encryptorBuilder);
                encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(key).setProvider("BC"));
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

    private Path getGpgDirectoryPath() {
        Path dir = Paths.get(settings.getApplicationFolderPath(), "gpg");
        if (!Files.exists(dir)) {
            if (!dir.toFile().mkdirs()) {
                logger.error("Unable to create GPG directory {}", dir.toFile().getAbsolutePath());
            }
        }
        return dir;
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
            var pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyIn),
                    new JcaKeyFingerprintCalculator());

            for (PGPSecretKeyRing secretRing : pgpSecretKeyRingCollection) {
                var secretKeyIte = secretRing.getSecretKeys();
                while (secretKeyIte.hasNext()) {
                    PGPSecretKey pgpSecretKey = secretKeyIte.next();
                    if (pgpSecretKey.getPublicKey().isEncryptionKey()) {
                        myPrivateKey = pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passphrase));
                        myPublicKey = pgpSecretKey.getPublicKey();
                        return true;
                    }
                }
            }
        } catch (IOException | PGPException ex) {
            logger.error("Unable to extract private key", ex);
        }
        return false;
    }

    private Path getPrivateKeyPath() {
        return Paths.get(settings.getApplicationFolderPath(), "private.asc");
    }

    public void getMyPublicKey(PGPSecretKey pgpSecretKey) {
        pgpSecretKey.getPublicKey().getFingerprint();
    }

    private static String bytesToHex(byte[] bytes) {
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
