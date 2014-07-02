package com.obs.integrator;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class ProcessingMessage {
	

	
	private  PropertiesConfiguration prop;
	static Logger logger = Logger.getLogger(NSTVConsumer.class);
	
	//Method for Create session
	public byte[] createSession(PropertiesConfiguration prop) throws NoSuchAlgorithmException {
		
		this.prop=prop;
		StringBuilder createSessionRequestData = new StringBuilder();
		createSessionRequestData.append(FileUtils.decToHex(1, 2)); //Proto_ver
		createSessionRequestData.append(FileUtils.decToHex(6, 2)); //Crypt_ver (1) + Key_type (10) = 00000110 = 6
		createSessionRequestData.append(FileUtils.decToHex(this.prop.getInt("Oper_Id"), 4)); //op_id
		createSessionRequestData.append(FileUtils.decToHex(this.prop.getInt("Sms_Id"), 4)); //sms_id

		String dataBody = createDataBodyForSessionRequest();
		if(dataBody.length() > 0){
			createSessionRequestData.append(FileUtils.decToHex(dataBody.length()/2,4)); //DB_Len
		}
		createSessionRequestData.append(dataBody);
		return FileUtils.hexStringToByteArray(createSessionRequestData.toString());
	}

	private String createDataBodyForSessionRequest() throws NoSuchAlgorithmException {

		StringBuilder dataBody = new StringBuilder();
		dataBody.append(FileUtils.decToHex(this.prop.getInt("DB_ID") + 1, 4)); //Db_id
		dataBody.append(FileUtils.decToHex(1, 4)); //Msg_id
		dataBody.append(FileUtils.decToHex(0, 4)); //Data_len (zero for create session request)
		dataBody.append("F0"); //padding bytes
		dataBody.append("F0");

		//create MAC using data_body
		String dataBodyHexString = dataBody.toString();
		dataBody.append(createMessageAuthCodeUsingDataBody(dataBodyHexString));
		return dataBody.toString();
	}

	private Object createMessageAuthCodeUsingDataBody(String dataBodyHexString) throws NoSuchAlgorithmException {
		
		MessageDigest testDigest = MessageDigest.getInstance("MD5");
		byte[] testDigestArr = testDigest.digest(FileUtils.hexStringToByteArray(dataBodyHexString));
		return FileUtils.bytesToHex(testDigestArr,0,testDigestArr.length);
	}

     public byte[] processAccountRequest(ProcessRequestData processRequestData, int messageId) throws Exception {
    	 
    	 
		try{
		StringBuilder createOpenAccountData = new StringBuilder();
		createOpenAccountData.append(FileUtils.decToHex(1, 2)); //Proto_ver
		createOpenAccountData.append(FileUtils.decToHex(5, 2)); //Crypt_ver (1) + Key_type (10) = 00000110 = 6
		createOpenAccountData.append(FileUtils.decToHex(prop.getInt("Oper_Id"), 4)); //op_id
		createOpenAccountData.append(FileUtils.decToHex(prop.getInt("Sms_Id"), 4)); //sms_id
		String dataBody =null;
		//For Open Account
		if(messageId == 513){
		   dataBody = createDataBodyForAccountRequest(processRequestData,messageId);
		
		//For Cancel Account
		}else{
		   dataBody = createDataBodyForCancelAccountRequest(processRequestData,messageId);
		}
		if(dataBody.length() > 0){
			createOpenAccountData.append(FileUtils.decToHex(dataBody.length()/2,4)); //DB_Len
		}
		createOpenAccountData.append(dataBody);
		
		logger.info("Sending Data for Open Account"+createOpenAccountData.toString());
		
	byte[] byteArrayData = FileUtils.hexStringToByteArray(createOpenAccountData.toString());
		return byteArrayData;
	}catch(Exception exception){
		logger.info("Error.:"+exception.getMessage());
		return null;
	}
	}
     
     //Databody For Create account
	private String createDataBodyForAccountRequest(ProcessRequestData processRequestData, int messageId) throws Exception {
		
		try{
		StringBuilder dataBody = new StringBuilder();
		dataBody.append(FileUtils.decToHex(prop.getInt("DB_ID") + 1, 4)); //Db_id
		dataBody.append(FileUtils.decToHex(messageId, 4)); //Msg_id
		//Card SN manipulation
		String strCardSN =processRequestData.getSmartcardId(); 
		char[] charArrCardSN = Hex.encodeHex(strCardSN.getBytes());
		dataBody.append(FileUtils.decToHex(strCardSN.length(), 4)); //Data_len (zero for create session request)
		dataBody.append(charArrCardSN);
		
		while(dataBody.length()%8 != 0){
			dataBody.append("F0"); //padding bytes
		}
		
		//create MAC using data_body
		String dataBodyHexString = dataBody.toString();
		dataBody.append(createMessageAuthCodeUsingDataBody(dataBodyHexString));
		logger.info("Open Account Card No:"+strCardSN+",MessageId:"+messageId);
		System.out.println("Open Request Data Body Before Encryption :"+dataBody.toString());
		logger.info("Open Request Data Body Before Encryption :"+dataBody.toString());
		//Encrypt Data_Body
		DESEncryption encryption = new DESEncryption();
		encryption.initDesAlgorithm();
		byte[] encryptedData = encryption.encryptData(dataBody.toString());
		
		return new String(Hex.encodeHex(encryptedData));
	}catch(Exception exception){
		logger.info("Error.:"+exception.getMessage());
		return null;
	}
	}
	
	
	//For Cancel Account
	private String createDataBodyForCancelAccountRequest(ProcessRequestData processRequestData, int messageId) throws Exception {
		
		
	try{
		StringBuilder dataBody = new StringBuilder();
		dataBody.append(FileUtils.decToHex(prop.getInt("DB_ID") + 1, 4)); //Db_id
		dataBody.append(FileUtils.decToHex(messageId,4)); //Msg_id
		
		//Card SN manipulation
		String strCardSN = processRequestData.getSmartcardId();
		char[] charArrCardSN = Hex.encodeHex(strCardSN.getBytes());
		dataBody.append(FileUtils.decToHex(strCardSN.length(), 4)); //Data_len (zero for create session request)
		dataBody.append(charArrCardSN);
		while(dataBody.length()%8 != 0){
			dataBody.append("F0"); //padding bytes
		}
		logger.info("Cancel Account Card No:"+strCardSN+",MessageId:"+messageId);
		//create MAC using data_body
		String dataBodyHexString = dataBody.toString();
		dataBody.append(createMessageAuthCodeUsingDataBody(dataBodyHexString));
		System.out.println("Cancel Account Data Body Before Encryption :"+dataBody.toString());
		
		//Encrypt Data_Body
		DESEncryption encryption = new DESEncryption();
		encryption.initDesAlgorithm();
		byte[] encryptedData = encryption.encryptData(dataBody.toString());
		return new String(Hex.encodeHex(encryptedData));
	}catch(Exception exception){
		logger.info("Error.:"+exception.getMessage());
		return null;
	}
	}

	public byte[] processEntiltmentData(ProcessRequestData processRequestData, int messageId) throws Exception {
		
		StringBuilder createEntitlmentData = new StringBuilder();
		createEntitlmentData.append(FileUtils.decToHex(1, 2)); //Proto_ver
		createEntitlmentData.append(FileUtils.decToHex(5, 2)); //Crypt_ver (1) + Key_type (10) = 00000110 = 6
		createEntitlmentData.append(FileUtils.decToHex(this.prop.getInt("Oper_Id"), 4)); //op_id
		createEntitlmentData.append(FileUtils.decToHex(this.prop.getInt("Sms_Id"), 4)); //sms_id
		String dataBody=null;

		//for Create Entitlment
		if(messageId == 525){
		dataBody = createDataBodyForCreateEntitlment(processRequestData,messageId);

		//Extend Entitlment
		}else{
			dataBody = createDataBodyForEntitlmentExtent(processRequestData,messageId);
		}
		if(dataBody.length() > 0){
			createEntitlmentData.append(FileUtils.decToHex(dataBody.length()/2,4)); //DB_Len
		}
		createEntitlmentData.append(dataBody);
		logger.info("Sending data for Create entitlment :"+createEntitlmentData.toString());
		return FileUtils.hexStringToByteArray(createEntitlmentData.toString());
	}

	//For Entitlement databody 
	private String createDataBodyForEntitlmentExtent(ProcessRequestData processRequestData, int messageId) throws Exception {
		
		try{
		StringBuilder dataBody = new StringBuilder();
		dataBody.append(FileUtils.decToHex(1, 4)); //Db_id
		dataBody.append(FileUtils.decToHex(messageId, 4)); //Msg_id
		StringBuilder dataCont = new StringBuilder();
		dataCont.append(Hex.encodeHex(processRequestData.getSmartcardId().getBytes()));//Card No
		dataCont.append(FileUtils.decToHex(384,4));//ProductCount(1)+Entity_tpye(1)+TF_Reserved(0)
		dataCont.append(FileUtils.decToHex(1,4)); //Product Id
		dataCont.append(FileUtils.decToHex(1, 2)); //Tape_ctrl
		dataCont.append(FileUtils.dateToHex(processRequestData.getStartDate()));// //Start_time
		dataCont.append(FileUtils.dateToHex(processRequestData.getEndDate()));//End_time  
		dataBody.append(FileUtils.decToHex(dataCont.length()/2,4));
		dataBody.append(dataCont);
		while(dataBody.length()%8 != 0){
			dataBody.append("F0"); //padding bytes
		}
		dataBody.append("F0");
		dataBody.append("F0");
		dataBody.append("F0");
		dataBody.append("F0");
		//create MAC using data_body
		String dataBodyHexString = dataBody.toString();
		dataBody.append(createMessageAuthCodeUsingDataBody(dataBodyHexString));
		logger.info("Extent Entitlment Card No:"+processRequestData.getSmartcardId()+",MessageId:"+messageId+",Start date:"+processRequestData.getStartDate()
				+",End date:"+processRequestData.getEndDate());
		System.out.println("Extend Entitlment Data Body Before Encryption :"+dataBody.toString());
		logger.info("Extend Entitlment Data Body Before Encryption :"+dataBody.toString());
		
		//Encrypt Data_Body
		DESEncryption encryption = new DESEncryption();
		encryption.initDesAlgorithm();
		byte[] encryptedData = encryption.encryptData(dataBody.toString());
		
		return new  String(Hex.encodeHex(encryptedData)); //bytesToHex(encryptedData, 0, encryptedData.length);
	//return dataBody.toString();

	}catch(Exception exception){
		logger.info("Error.:"+exception.getMessage());
		return null;
	}
	}

	private String createDataBodyForCreateEntitlment(ProcessRequestData processRequestData, int messageId) throws Exception {
		
		try{
		
		StringBuilder dataBody = new StringBuilder();
		dataBody.append(FileUtils.decToHex(1, 4)); //Db_id
		dataBody.append(FileUtils.decToHex(messageId, 4)); //Msg_id
		StringBuilder dataCont = new StringBuilder();
		dataCont.append(Hex.encodeHex(processRequestData.getSmartcardId().getBytes()));//"8122303556028037".getBytes()));
		dataCont.append(FileUtils.decToHex(1, 2)); //Product Count
		dataCont.append(FileUtils.decToHex(32769, 4)); //Entity_type(1)+TF_reserved(0)+Product Id(1)
		dataCont.append(FileUtils.decToHex(0, 2)); //Tape_ctrl
		dataCont.append(FileUtils.dateToHex(processRequestData.getStartDate()));// //Start_time
		dataCont.append(FileUtils.dateToHex(processRequestData.getEndDate()));//End_time  
		dataBody.append(FileUtils.decToHex(dataCont.length()/2,4));
		dataBody.append(dataCont);
		while(dataBody.length()%8 != 0){
			dataBody.append("F0"); //padding bytes
		}
		dataBody.append("F0");
		dataBody.append("F0");
		dataBody.append("F0");
		dataBody.append("F0");
		//create MAC using data_body
		String dataBodyHexString = dataBody.toString();
		logger.info("Entitlment Card No:"+processRequestData.getSmartcardId()+",MessageId:"+messageId+",Start date:"+processRequestData.getStartDate()
				+",End date:"+processRequestData.getEndDate());
		dataBody.append(createMessageAuthCodeUsingDataBody(dataBodyHexString));
		
		System.out.println("Create Entitlment Data Body Before Encryption :"+dataBody.toString());
		logger.info("Create Entitlment Data Body Before Encryption :"+dataBody.toString());
		
		//Encrypt Data_Body
		DESEncryption encryption = new DESEncryption();
		encryption.initDesAlgorithm();
		byte[] encryptedData = encryption.encryptData(dataBody.toString());
		
		return new  String(Hex.encodeHex(encryptedData)); //bytesToHex(encryptedData, 0, encryptedData.length);
	//return dataBody.toString();

	}catch(Exception exception){
		logger.info("Error.:"+exception.getMessage());
		return null;
	}
	}

	public byte[] processOsdRequest(ProcessRequestData processRequestData,int messageId) throws Exception {
		
		StringBuilder createEntitlmentData = new StringBuilder();
		createEntitlmentData.append(FileUtils.decToHex(1, 2)); //Proto_ver
		createEntitlmentData.append(FileUtils.decToHex(5, 2)); //Crypt_ver (1) + Key_type (10) = 00000110 = 6
		createEntitlmentData.append(FileUtils.decToHex(this.prop.getInt("Oper_Id"), 4)); //op_id
		createEntitlmentData.append(FileUtils.decToHex(this.prop.getInt("Sms_Id"), 4)); //sms_id
		String dataBody=null;
		
		dataBody = createDataBodyForOsd(processRequestData,messageId);


		if(dataBody.length() > 0){
			createEntitlmentData.append(FileUtils.decToHex(dataBody.length()/2,4)); //DB_Len
		}
		createEntitlmentData.append(dataBody);
		logger.info("Sending data for Create entitlment :"+createEntitlmentData.toString());
		return FileUtils.hexStringToByteArray(createEntitlmentData.toString());
		
	}

	private String createDataBodyForOsd(ProcessRequestData processRequestData,int messageId) throws Exception {
		
		try{
		
		StringBuilder dataBody = new StringBuilder();
		dataBody.append(FileUtils.decToHex(1, 4)); //Db_id
		dataBody.append(FileUtils.decToHex(messageId, 4)); //Msg_id
		StringBuilder dataCont = new StringBuilder();
		StringBuilder ExpressionBuilder = new StringBuilder();
		ExpressionBuilder.append(String.format("%010x", new BigInteger(1,"card=".getBytes())));//Exp
		ExpressionBuilder.append(Hex.encodeHex(processRequestData.getSmartcardId().getBytes()));
		dataCont.append(FileUtils.decToHex(ExpressionBuilder.length()/2,2));
		dataCont.append(ExpressionBuilder);
		String MsgHex=String.format("%010x", new BigInteger(1,processRequestData.getProduct().getBytes()));//Text
		dataCont.append(FileUtils.decToHex(MsgHex.length()/2,2));//Text_length
		dataCont.append(MsgHex);
		dataCont.append(FileUtils.decToHex(1, 2)); //Display_type
		dataCont.append(FileUtils.decToHex(16,8));//Duration
		dataBody.append(FileUtils.decToHex(dataCont.length()/2,4));
		dataBody.append(dataCont);
		logger.info("OSD Account Card No:"+processRequestData.getSmartcardId()+",MessageId:"+messageId+",Message :"+processRequestData.getProduct());
		while(dataBody.length()%8 != 0 || (dataBody.length()/2)%8 != 0){
			dataBody.append("F0"); //padding bytes
		}
		
		//create MAC using data_body
		String dataBodyHexString = dataBody.toString();
		dataBody.append(createMessageAuthCodeUsingDataBody(dataBodyHexString));
		
		System.out.println("OSD Data Body Before Encryption :"+dataBody.toString());
		logger.info("OSD Data Body Before Encryption :"+dataBody.toString());
		
		//Encrypt Data_Body
		DESEncryption encryption = new DESEncryption();
		encryption.initDesAlgorithm();
		byte[] encryptedData = encryption.encryptData(dataBody.toString());
		
		return new  String(Hex.encodeHex(encryptedData)); 
		}catch(Exception exception){
			logger.info("Error.:"+exception.getMessage());
			return null;
		
	}

	}
	

}
