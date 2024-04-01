package com.teleapps.schedulemate.util;

//AES256 Encryption
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {

	protected String secretKey = "26b334fa9cd83b365742deba3684b2e9e18814558e30b54364f50db3b1897c1cf84179e32ed9bcded356135d7949466d68cc5033f1a3748ae827a68c1cda273e=";
	protected String salt = "A3k#Gvb6@CB#tv$50p!50c35onsM!";

	private String encrypt(String plainText) {
		try {
	        byte[] iv = new byte[16];
	        SecureRandom secureRandom = new SecureRandom();
	        secureRandom.nextBytes(iv);
	        IvParameterSpec ivspec = new IvParameterSpec(iv);
	        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
	        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), 65536, 256);
	        SecretKey tmp = factory.generateSecret(spec);
	        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
	        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
	        cipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
	        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
	        byte[] ivAndEncryptedBytes = new byte[iv.length + encryptedBytes.length];
	        System.arraycopy(iv, 0, ivAndEncryptedBytes, 0, iv.length);
	        System.arraycopy(encryptedBytes, 0, ivAndEncryptedBytes, iv.length, encryptedBytes.length);
	        return Base64.getEncoder().encodeToString(ivAndEncryptedBytes);
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
		return null;
	}

	private String decrypt(String cipherText) {
		try {
	        byte[] encryptedBytes = Base64.getDecoder().decode(cipherText);
	        byte[] iv = new byte[16];
	        byte[] encryptedData = new byte[encryptedBytes.length - iv.length];
	        System.arraycopy(encryptedBytes, 0, iv, 0, iv.length);
	        System.arraycopy(encryptedBytes, iv.length, encryptedData, 0, encryptedData.length);
	        IvParameterSpec ivspec = new IvParameterSpec(iv);
	        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
	        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), 65536, 256);
	        SecretKey tmp = factory.generateSecret(spec);
	        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
	        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
	        cipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
	        byte[] decryptedBytes = cipher.doFinal(encryptedData);
	        String temp = new String(decryptedBytes, StandardCharsets.UTF_8);
			String returnValue = temp.substring(16, temp.length() - 1);
			return returnValue.substring(0, returnValue.length() - 15);
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
		return null;
	}

	public String getPlainText(String cipher) {
		Cryptography security = new Cryptography();
		if (cipher != null && cipher.trim().length() > 0) {
			return security.decrypt(cipher);
		} else {
			return null;
		}
	}

	public String getCipher(String plainText) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		Cryptography security = new Cryptography();
		return security.encrypt(uuid.substring(0, 16).concat(plainText).concat(uuid.substring(16, 32)));
	}
}