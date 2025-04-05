package net.zonia3000.ombrachat.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
    private PGPPrivateKey privateKey;
    private char[] passphrase;

    public GpgService() {
        settings = ServiceLocator.getService(SettingsService.class);
    }

    public void setPassphrase(char[] passphrase) {
        this.passphrase = passphrase;
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
                logger.error("Unable to create GPG directory {}", dir.toAbsolutePath());
            }
        }
        return dir;
    }

    public String decryptToString(File encryptedData) {
        try (InputStream encryptedIn = new FileInputStream(encryptedData)) {
            JcaPGPObjectFactory pgpObjectFactory = new JcaPGPObjectFactory(encryptedIn);

            Object obj = pgpObjectFactory.nextObject();
            // The first object might be a marker packet
            PGPEncryptedDataList pgpEncryptedDataList = (obj instanceof PGPEncryptedDataList)
                    ? (PGPEncryptedDataList) obj : (PGPEncryptedDataList) pgpObjectFactory.nextObject();

            PGPPrivateKey pgpPrivateKey = null;
            PGPPublicKeyEncryptedData publicKeyEncryptedData = null;

            Iterator<PGPEncryptedData> encryptedDataItr = pgpEncryptedDataList.getEncryptedDataObjects();
            while (pgpPrivateKey == null && encryptedDataItr.hasNext()) {
                publicKeyEncryptedData = (PGPPublicKeyEncryptedData) encryptedDataItr.next();
                pgpPrivateKey = getPrivateKey(publicKeyEncryptedData.getKeyIdentifier().getKeyId());
            }

            if (pgpPrivateKey == null) {
                return null;
            }

            if (publicKeyEncryptedData == null) {
                logger.error("Unable to extract encrypted data");
                return null;
            }

            PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(pgpPrivateKey);
            InputStream decryptedCompressedIn = publicKeyEncryptedData.getDataStream(decryptorFactory);

            JcaPGPObjectFactory decCompObjFac = new JcaPGPObjectFactory(decryptedCompressedIn);
            PGPCompressedData pgpCompressedData = (PGPCompressedData) decCompObjFac.nextObject();

            InputStream compressedDataStream = new BufferedInputStream(pgpCompressedData.getDataStream());
            JcaPGPObjectFactory pgpCompObjFac = new JcaPGPObjectFactory(compressedDataStream);

            Object message = pgpCompObjFac.nextObject();

            String textMessage;
            if (message instanceof PGPLiteralData) {
                PGPLiteralData pgpLiteralData = (PGPLiteralData) message;
                InputStream decDataStream = pgpLiteralData.getInputStream();
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    Streams.pipeAll(decDataStream, outputStream);
                    textMessage = outputStream.toString(StandardCharsets.UTF_8);
                }
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
            return textMessage;
        } catch (IOException | PGPException ex) {
            logger.error("Unable to decrypt file", ex);
            return null;
        }
    }

    private PGPPrivateKey getPrivateKey(long keyID) {
        if (privateKey != null) {
            return privateKey;
        }

        Path privateKeyPath = getPrivateKeyPath();
        if (!Files.exists(privateKeyPath)) {
            return null;
        }

        try (InputStream privateKeyIn = new BufferedInputStream(new FileInputStream(privateKeyPath.toFile()))) {
            var pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyIn),
                    new JcaKeyFingerprintCalculator());

            PGPSecretKey pgpSecretKey = pgpSecretKeyRingCollection.getSecretKey(keyID);
            privateKey = pgpSecretKey == null ? null : pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passphrase));
            return privateKey;
        } catch (IOException | PGPException ex) {
            logger.error("Unable to extract private key", ex);
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

            var ite = pgpSecretKeyRingCollection.iterator();
            if (ite.hasNext()) {
                var secretRing = ite.next();
                PGPSecretKey pgpSecretKey = secretRing.getSecretKey();
                pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passphrase));
                return true;
            }
        } catch (IOException | PGPException ex) {
            logger.error("Unable to extract private key", ex);
        }
        return false;
    }

    private Path getPrivateKeyPath() {
        return Paths.get(settings.getApplicationFolderPath(), "private.asc");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02X", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
