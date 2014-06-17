package com.obs.integrator;

import java.util.Date;

public class EntitlementsData {

	private  Long id;
	private  String product;
	private Long prdetailsId;
	private String requestType;
	private String hardwareId;
	private String provisioingSystem;
	private Long serviceId;
	
	
	public void setId(Long id) {
		this.id = id;
	}


	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public void setHardwareId(String hardwareId) {
		this.hardwareId = hardwareId;
	}

	public void setProvisioingSystem(String provisioingSystem) {
		this.provisioingSystem = provisioingSystem;
	}
	
	public void setProduct(String product) {
		this.product = product;
	}
	
	public Long getPrdetailsId() {
		return prdetailsId;
	}

	public void setPrdetailsId(Long prdetailsId) {
		this.prdetailsId = prdetailsId;
	}

	public String getProduct() {
		return product;
	}
	
	public Long getId() {
		return id;
	}

	public String getRequestType() {
		return requestType;
	}

	public String getHardwareId() {
		return hardwareId;
	}

	public String getProvisioingSystem() {
		return provisioingSystem;
	}

	public Long getServiceId() {
		return serviceId;
	}

	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}

	
}
