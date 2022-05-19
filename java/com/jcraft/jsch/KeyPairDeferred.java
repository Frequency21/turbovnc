package com.jcraft.jsch;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Arrays;

/**
 * A {@link KeyPair} which can only reveal its type and content after it was decrypted using KeyPairDeferred{@link #decrypt(byte[])}.
 */
public class KeyPairDeferred extends KeyPair {

    private KeyPair delegate;

    KeyPairDeferred(JSch jsch) {
        super(jsch);
    }

    @Override
    public boolean decrypt(byte[] _passphrase) {
        try {
            if (!encrypted) {
                return true;
            }
            if (_passphrase == null) {
                JSch.getLogger().log(Logger.ERROR, "no passphrase set.");
                return false;
            }

            initCipher(_passphrase);

            byte[] plain = new byte[data.length];
            cipher.update(data, 0, data.length, plain, 0);

            // now we have decrypted key and can determine type
            int type = readOpenSSHKeyv1(plain);

            delegate = getKeyPair(jsch, null, null, null, false, plain, getPublicKeyBlob(), type, VENDOR_OPENSSH_V1, publicKeyComment, cipher, null, null);

            return delegate != null;


        } catch (Exception e) {
            throw new IllegalArgumentException("Could not sucessfully decrypt openssh v1 key", e);
        }

    }

    private void initCipher(byte[] _passphrase) throws Exception {

        // the encrypted private key is here:
        if ("bcrypt".equals(kdfName)) {
            Buffer opts = new Buffer(kdfOptions);

            byte[] keyiv = new byte[48];

            new BCrypt().pbkdf(_passphrase, opts.getString(), opts.getInt(), keyiv);

            Arrays.fill(_passphrase, (byte) 0);
            byte[] key = Arrays.copyOfRange(keyiv, 0, 32);
            byte[] iv = Arrays.copyOfRange(keyiv, 32, 48);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
        } else {
            throw new IllegalStateException("No support for KDF '" + kdfName + "'.");
        }
    }

    @Override
    void generate(int key_size) throws JSchException {
        throw new UnsupportedOperationException();
    }

    @Override
    byte[] getBegin() {
        return delegate.getBegin();
    }

    @Override
    byte[] getEnd() {
        return delegate.getEnd();
    }

    @Override
    int getKeySize() {
        return delegate.getKeySize();
    }

    @Override
    public byte[] getSignature(byte[] data) {
        return delegate.getSignature(data);
    }

    @Override
    public Signature getVerifier() {
        return delegate.getVerifier();
    }

    @Override
    public byte[] forSSHAgent() throws JSchException {
        return delegate.forSSHAgent();
    }

    @Override
    byte[] getPrivateKey() {
        return delegate.getPrivateKey();
    }

    @Override
    byte[] getKeyTypeName() {
        return delegate.getKeyTypeName();
    }

    @Override
    public int getKeyType() {
        return delegate.getKeyType();
    }

    @Override
    boolean parse(byte[] data) {
        return delegate.parse(data);
    }

    @Override
    public byte[] getPublicKeyBlob() {
        return delegate != null ? delegate.getPublicKeyBlob() : null;
    }

    @Override
    public String getPublicKeyComment() {
        return delegate.getPublicKeyComment();
    }

    @Override
    public String getFingerPrint() {
        return delegate.getFingerPrint();
    }

    @Override
    public boolean isEncrypted() {
        return delegate != null ? delegate.isEncrypted() : super.isEncrypted();
    }
}
