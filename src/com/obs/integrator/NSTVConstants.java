package com.obs.integrator;

public class NSTVConstants {
	
	
	//NSTV Commands

	//For request Type
	public static final String REQ_ACTIVATION="ACTIVATION";
	public static final String REQ_RENEWAL_BE="RENEWAL_BE";
	public static final String REQ_RENEWAL_AE="RENEWAL_AE";
	public static final String REQ_DISCONNECTION="DISCONNECTION";
	public static final String REQ_RECONNECT="RECONNECTION";
    public static final String REQ_MESSAGE="MESSAGE";
    public static final String REQ_STB_CHANGE="DEVICE_SWAP";
    public static String SESSION_KEY=null;
    
    
    //Response Messages
    public static final String RES_ACTIVATION="8001";
    public static final String RES_OPENACCOUNT="8201";
    public static final String RES_CANCELACCOUNT="8202";
    public static final String RES_CREATEENTITLMENT="820D";
    public static final String RES_EXTENTENTITLMENT="820E";
    public static final String RES_OSD="8304";
}
