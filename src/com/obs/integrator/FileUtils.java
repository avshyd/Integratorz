package com.obs.integrator;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtils {
	
	static char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	

	public static String decToHex(int dec, int hexStringLength) {
		
		StringBuilder hexBuilder = new StringBuilder(hexStringLength);
		hexBuilder.setLength(hexStringLength);
		for (int i = hexStringLength - 1; i >= 0; --i)
		{
			int j = dec & 0x0F;
			hexBuilder.setCharAt(i, hexDigit[j]);
			dec >>= 4;
		}
		return hexBuilder.toString(); 
	}

	public static byte[] hexStringToByteArray(String hexEncodedBinary) {
		
		if (hexEncodedBinary.length() % 2 == 0) {
			char[] sc = hexEncodedBinary.toCharArray();
			byte[] ba = new byte[sc.length / 2];

			for (int i = 0; i < ba.length; i++) {
				int nibble0 = Character.digit(sc[i * 2], 16);
				int nibble1 = Character.digit(sc[i * 2 + 1], 16);
				if (nibble0 == -1 || nibble1 == -1){
					throw new IllegalArgumentException(
							"Hex-encoded binary string contains an invalid hex digit in '"+sc[i * 2]+sc[i * 2 + 1]+"'");
				}
				ba[i] = (byte) ((nibble0 << 4) | (nibble1));
			}

			return ba;
		} else {
			throw new IllegalArgumentException(
			"Hex-encoded binary string contains an uneven no. of digits");
		}
	}

	
	public static String bytesToHex(byte[] b, int off, int len) {
		
		StringBuffer buf = new StringBuffer();
		for (int j=0; j<len; j++)
			buf.append(byteToHex(b[off+j]));
		return buf.toString();
	}
	
	public static String byteToHex(byte b) {
		char[] a = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };

		return new String(a);
	}

	public static String dateToHex(Date date) throws Exception {
		
		Long decimal=date.getTime();
	 	int new_x = Integer.parseInt(Long.toString(decimal).substring(0,10));
		return Long.toHexString(new_x).toUpperCase();
	}
}
