package com.obs.integrator;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * 
 * @author Umar Farooq
 * 
 */
public class DESEncryption {

	private static Cipher encryptCipher;
	private static Cipher decryptCipher;
	
	
	public static void initDesAlgorithm() throws Exception{
		String myEncryptionKey =NSTVConstants.SESSION_KEY;
        String myEncryptionScheme = "DES";
        byte[] keyAsBytes = h2b(myEncryptionKey);
        DESKeySpec myKeySpec = new DESKeySpec(keyAsBytes);
		
        SecretKeyFactory mySecretKeyFactory = SecretKeyFactory.getInstance(myEncryptionScheme);
        SecretKey secretKey = mySecretKeyFactory.generateSecret(myKeySpec);

         //Perform encrpting data
		encryptCipher = Cipher.getInstance("DES/ECB/NoPadding");
		encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
		
		  //Perform decrypting data
		decryptCipher = Cipher.getInstance("DES/ECB/NoPadding");
		decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
		
	}

	/**
	 * Encrypt Data
	 * @param data
	 * @return
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] encryptData(String data)
	throws IllegalBlockSizeException, BadPaddingException {
		
		
		byte[] dataToEncrypt = h2b(data);//data.getBytes();
		byte[] encryptedData = encryptCipher.doFinal(dataToEncrypt);
		System.out.println("Encryted Data: " + encryptedData);

		return encryptedData;
	}

	/**
	 * Decrypt Data
	 * @param data
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static String decryptData(String data)
	throws IllegalBlockSizeException, BadPaddingException {
		byte[] arr = h2b(data);
		
		byte[] textDecrypted = decryptCipher.doFinal(arr);
		String decrypted = b2h(textDecrypted);
		System.out.println("Decryted Data: " + decrypted);
		
		return decrypted;
	}
	
	private static byte[] h2b(String hex)
	{
		if ((hex.length() & 0x01) == 0x01)
			throw new IllegalArgumentException();
		byte[] bytes = new byte[hex.length() / 2];
		for (int idx = 0; idx < bytes.length; ++idx) {
			int hi = Character.digit((int) hex.charAt(idx * 2), 16);
			int lo = Character.digit((int) hex.charAt(idx * 2 + 1), 16);
			if ((hi < 0) || (lo < 0))
				throw new IllegalArgumentException();
			bytes[idx] = (byte) ((hi << 4) | lo);
		}
		return bytes;
	}
	
	private static String b2h(byte[] bytes)
	{
		char[] hex = new char[bytes.length * 2];
		for (int idx = 0; idx < bytes.length; ++idx) {
			int hi = (bytes[idx] & 0xF0) >>> 4;
			int lo = (bytes[idx] & 0x0F);
			hex[idx * 2] = (char) (hi < 10 ? '0' + hi : 'A' - 10 + hi);
			hex[idx * 2 + 1] = (char) (lo < 10 ? '0' + lo : 'A' - 10 + lo);
		}
		return new String(hex);
	}
}