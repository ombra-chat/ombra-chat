package net.zonia3000.ombrachat;

import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bouncycastle.gpg.keybox.BlobType;
import org.bouncycastle.gpg.keybox.KeyBox;
import org.bouncycastle.gpg.keybox.PublicKeyRingBlob;
import org.bouncycastle.gpg.keybox.bc.BcKeyBox;

public class GpgUtils {

    public static class GpgPublicKey {

        private String fingerprint;
        private List<String> userIds;

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

        @Override
        public String toString() {
            return userIds.isEmpty() ? fingerprint : userIds.get(0) + " " + fingerprint;
        }
    }

    public static List<GpgPublicKey> listKeys() {

        List<GpgPublicKey> keys = new ArrayList<>();

        Path pubringPath = Paths.get(System.getProperty("user.home"), ".gnupg", "pubring.kbx");
        if (!Files.exists(pubringPath)) {
            return keys;
        }

        try (InputStream keyIn = new FileInputStream(pubringPath.toFile())) {

            KeyBox kbox = new BcKeyBox(keyIn);

            for (var keyBlob : kbox.getKeyBlobs()) {
                if (keyBlob.getType() == BlobType.OPEN_PGP_BLOB) {
                    var publicBlob = ((PublicKeyRingBlob) keyBlob).getPGPPublicKeyRing();
                    var publicKey = publicBlob.getPublicKey();
                    GpgPublicKey key = new GpgPublicKey();
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02X", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
