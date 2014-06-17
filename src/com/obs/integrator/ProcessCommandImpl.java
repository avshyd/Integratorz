package com.obs.integrator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

public class ProcessCommandImpl {

	
	private static DataInputStream dataInputStream;
	private static DataOutputStream dataOutputStream;
	private static Socket socket = null;
	static Logger logger = Logger.getLogger(Consumer.class);
	private PropertiesConfiguration prop;
	public Timer timer;
	private String number;
	private Long id;
	public int timePeriod;
	public static int wait;
	private String ROOT_KEY;
	private  ProcessingMessage processingMessage=null;
	private int messageId;
	@SuppressWarnings("static-access")
	public ProcessCommandImpl(Socket requestSocket,PropertiesConfiguration prop) {
		
		try{
			
			this.socket = requestSocket;
			this.prop =prop;
			wait = prop.getInt("ThreadSleep_period");
			timePeriod=prop.getInt("TimePeriod");	
			ROOT_KEY=prop.getString("Root_Key");
			dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
  		    this.processingMessage=new ProcessingMessage();
			byte[] sessionData=processingMessage.createSession(prop);
			send(dataOutputStream, sessionData);
			Reminder(prop.getInt("TimePeriod"));
			receiveResponseAndProcess();		
		}
		 catch (NoSuchAlgorithmException e) {
            logger.info("DES algoritham does not exist");
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("unable to read or write file");
		} catch (Exception e) {
			logger.error(e.getCause().getLocalizedMessage());
		}
		
		
	}

	private String receiveResponseAndProcess() {
		
		//FOR TESTING
		//Receive Server Response
		byte[] byteData = receive(dataInputStream);
		String responseHexString = FileUtils.bytesToHex(byteData,0,byteData.length);
		//System.out.println("Received Response : "+responseHexString);
	//	logger.info("Received Response : "+responseHexString);
		String response=decryptAndProcessServerResponse(responseHexString);
		return response;
	}

	private String decryptAndProcessServerResponse(String responseHexData) {
		
		try {
		String hexDataBody = parseHeadAndReturnDataBody(responseHexData);
		int encryptionScheme = getEncyptionSchemeFromResponse(responseHexData);
		String decryptedData = null;
		if(encryptionScheme == 4 && hexDataBody != null ){
				decryptedData = decryptDataBodyUsingThreeDESAlgorithm(hexDataBody);
			
		}else if(hexDataBody != null){
			decryptedData = decryptDataBodyUsingDESAlgorithm(hexDataBody);
		}
		logger.info("Decrypted Response data:"+decryptedData);
		
		 String responseMessage =null;
		if(decryptedData !=null && decryptedData.length() >= 8){
			
			responseMessage =decryptedData.substring(4,8);
			if(responseMessage.equalsIgnoreCase("8001")){
				processCreateSessionResponse(responseHexData);
				   NSTVConstants.SESSION_KEY=decryptedData.substring(24,40);
			}
		}
		
		
		return responseMessage;
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}

	private void processCreateSessionResponse(String responseHexData) throws Exception{
		

		String hexDataBody = parseHeadAndReturnDataBody(responseHexData);
		
		TripleDES tripleDes = new TripleDES(new String(Hex.encodeHex(ROOT_KEY.getBytes())));
		String decrypted = tripleDes.decrypt(hexDataBody);
		System.out.println("Decrypted Data: " + decrypted);
		logger.info("Create session Response Decrypted Data:"+decrypted);
	}

	private String decryptDataBodyUsingThreeDESAlgorithm(String encryptedHexData)throws Exception{
		
		TripleDES tripleDes = new TripleDES(new String(Hex.encodeHex(ROOT_KEY.getBytes())));
		String decrypted = tripleDes.decrypt(encryptedHexData);
		
		return decrypted;
	}
	
	private String decryptDataBodyUsingDESAlgorithm (String encryptedHexData)throws Exception{
		return DESEncryption.decryptData(encryptedHexData);
	}
	private int getEncyptionSchemeFromResponse(String responseHexData){
		return Integer.parseInt(responseHexData.substring(2,4));
	}
	private String parseHeadAndReturnDataBody(String responseHexData){
		String dataBody = null;
		//Here first 8 bytes are head info, to get the encrypted data we need take substring from 14th index of hex string
		if(responseHexData.length() > 16){
		dataBody = responseHexData.substring(16, responseHexData.length());
		}
		return dataBody;
	}

	private byte[] receive(DataInputStream dataInputStream) {

		try {

			byte[] buffer = new byte[4096];
			int bytesRead;
			int totalLength = 0;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			if (-1 != (bytesRead = dataInputStream.read(buffer))) {
				bos.write(buffer, 0, bytesRead);
				totalLength += bytesRead;
			}

			bos.flush();
			bos.close();
			return bos.toByteArray();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Method used to Send Request to Server
	 */
	public static void send(DataOutputStream os, byte[] byteData) throws Exception
	{
		try
		{
			os.write(byteData);
			os.flush();
			
		}
		catch (Exception exception)
		{
			throw exception;
		}
	}
	public void processRequest(ProcessRequestData processRequestData) {
		
		try {		
				synchronized (prop) {
					id = prop.getLong("DB_ID");					
					id = id + 1;
					prop.setProperty("DB_ID", id);				
						prop.save();					 					
				}
				
				if (processRequestData.getRequestType().equalsIgnoreCase(NSTVConstants.REQ_ACTIVATION) || 
						processRequestData.getRequestType().equalsIgnoreCase(NSTVConstants.REQ_RECONNECT)) {

					Reminder(timePeriod);
					 messageId=513;
					 byte[] bytedataforopenAcc=this.processingMessage.processAccountRequest(processRequestData,messageId);
					   send(dataOutputStream, bytedataforopenAcc);
					   String response=receiveResponseAndProcess();
					   
					   if(response.equalsIgnoreCase(NSTVConstants.RES_OPENACCOUNT)){
							Reminder(timePeriod);
						   logger.error("Open Account is Created with card "+processRequestData.getSmartcardId());  
							messageId=525;
					      byte[] EntitlmentByteArrayData=	this.processingMessage.processEntiltmentData(processRequestData,messageId);
					      send(dataOutputStream, EntitlmentByteArrayData);
					       String responseentitlment=receiveResponseAndProcess();
					       
					   if(responseentitlment.equalsIgnoreCase(NSTVConstants.RES_CREATEENTITLMENT)){
							
						  logger.error("Entitlment is done with card "+processRequestData.getSmartcardId());  
						  process("Success",processRequestData.getId(), processRequestData.getPrdetailsId());
					
					   }else{
						logger.error("Problem to create Entitlment "+processRequestData.getSmartcardId());
					    process("failure",processRequestData.getId(), processRequestData.getPrdetailsId());
					   }
					
					}else{
						 logger.error("Open Account is not Created..");  
					   }
					   
				 }else if(processRequestData.getRequestType().equalsIgnoreCase(NSTVConstants.REQ_RENEWAL)){
						Reminder(timePeriod);
					   messageId=526;
					   byte[] bytedatafEntitlmentData = this.processingMessage.processEntiltmentData(processRequestData,messageId);
					   send(dataOutputStream, bytedatafEntitlmentData);
					    String response=receiveResponseAndProcess();
					   if(response.equalsIgnoreCase(NSTVConstants.RES_EXTENTENTITLMENT)){
						   logger.info("Entitlment Extension is done sucessfully");
						   process("Success",processRequestData.getId(), processRequestData.getPrdetailsId());
					   }else{
						   logger.info("Entitlment Extension failure");
						   process("failure",processRequestData.getId(), processRequestData.getPrdetailsId());
					   }
					 
				 }else if (processRequestData.getRequestType().equalsIgnoreCase(NSTVConstants.REQ_DISCONNECTION)) {
					Reminder(timePeriod);
					   messageId=514;
					  byte[] bytedataforopenAcc=this.processingMessage.processAccountRequest(processRequestData,messageId);
					  send(dataOutputStream, bytedataforopenAcc);
					  String response=receiveResponseAndProcess();

					  if(response.equalsIgnoreCase(NSTVConstants.RES_CANCELACCOUNT)){
						  
						   logger.info("Account is stopped successfully ..."+processRequestData.getSmartcardId());  
							process("Success",processRequestData.getId(), processRequestData.getPrdetailsId());

					  }else{
						   logger.error("Account Disconnection is failed. "+processRequestData.getSmartcardId());
						    process("failure",processRequestData.getId(), processRequestData.getPrdetailsId());
					   }
					  
				}else if (processRequestData.getRequestType().equalsIgnoreCase(NSTVConstants.REQ_MESSAGE)) {
				   	 Reminder(timePeriod);
					   messageId=772;
					  byte[] bytedataforOsd=this.processingMessage.processOsdRequest(processRequestData,messageId);
					  send(dataOutputStream, bytedataforOsd);
					  String response=receiveResponseAndProcess();

					  if(response.equalsIgnoreCase(NSTVConstants.RES_OSD)){
						  
						   logger.info("OSD sent successfully ..."+processRequestData.getSmartcardId());  
							process("Success",processRequestData.getId(), processRequestData.getPrdetailsId());

					  }else{
						   logger.error("OSD sent failed. "+processRequestData.getSmartcardId());
						    process("failure",processRequestData.getId(), processRequestData.getPrdetailsId());
					   }
				}else{
					System.out.println("RequestType="+processRequestData.getRequestType()+" . Invalid Request Type");
					logger.info("RequestType="+processRequestData.getRequestType()+" . Invalid Request Type");
				}
			 
		} catch (ConfigurationException e) {
			logger.error("(ConfigurationException) Properties file loading error.... : " + e.getMessage());
			
		} catch (Exception e) {
			e.printStackTrace();
		} 

	}
	
	

	public void connectionHolding() {
		try {
			if (socket != null) {
				
				String heartBeatMessage="0004FFFFFFFF0000"; 
					byte[] heartbeatBytes=FileUtils.hexStringToByteArray(heartBeatMessage);
					logger.info("Sending Hearbeat Message for Connectionhold");
					send(dataOutputStream, heartbeatBytes);
					receiveResponseAndProcess();
			} else {
				ProcessCommandImpl.closeIOFile();
			}
		} catch (Exception e) {
			logger.error("(ConfigurationException) Properties file loading error.... : " + e.getMessage());
		} 
	}



	public static void process(String value, Long id, Long prdetailsId){
		
		try{		
			logger.info("output from CAS Server is :" +value);
			if(value==null){
				throw new NullPointerException();
			}else{	
				Consumer.sendResponse(value,id,prdetailsId);	
			}		
		} catch(NullPointerException e){
			logger.error("NullPointerException : Output from the Oss System Server is : " + value);
			
		} catch (Exception e) {
		    logger.error("Exception : " + e.getMessage());
	    }
		
	}
	
	public void Reminder(int seconds) {
		
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		timer.schedule(new RemindTask(), seconds);
	}

	class RemindTask extends TimerTask {
		public void run() {
			connectionHolding();
			Reminder(timePeriod);
		}
	}
	
	public static String ReadOutput(String output){
		try {
		
			  CSVReader reader = new CSVReader(new StringReader(output));
			  String[] tokens;
			  String message = "";
			  tokens = reader.readNext();
			
			  if (tokens.length > 1) {
				  String mes = tokens[1];
				
				if (mes.equalsIgnoreCase("0")) {
					message = "success";
				}else {
					String errorid= tokens[1];
					String error = tokens[2];
					message = "failure : Exception error code is : " + errorid + " , Exception/Error Message is : " + error;
				}
			  }
			  return message;
	    } catch (IOException e) {
		      return null;
	    }
	}
	
	public static void closeIOFile(){
		try{
		dataInputStream.close();
		dataOutputStream.close();
		Thread.sleep(wait);
		throw new IOException();
		} catch(InterruptedException e){
			logger.error("thread is Interrupted for the : " + e.getCause().getLocalizedMessage());
		} catch (IOException e) {
			logger.error("The Socket server connection is DisConnected, ReConnect to the Server");
			Consumer.getConnection();
		}
	}
}
