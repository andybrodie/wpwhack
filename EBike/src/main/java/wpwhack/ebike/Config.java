package wpwhack.ebike;

import com.worldpay.innovation.wpwithin.types.WWHCECard;

import java.util.Map;

public class Config {
	private int port;
	private String host;
	private WWHCECard hceCard;
	private Map<String, String> pspConfig;

	public String getDeviceNamePrefix() {
		return deviceNamePrefix;
	}

	public void setDeviceNamePrefix(String deviceNamePrefix) {
		this.deviceNamePrefix = deviceNamePrefix;
	}

	private String deviceNamePrefix;

	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public WWHCECard getHceCard() {
		return hceCard;
	}

	public void setHceCard(WWHCECard hceCard) {
		this.hceCard = hceCard;
	}

	public Map<String, String> getPspConfig() {
		return pspConfig;
	}

	public void setPspConfig(Map<String, String> pspConfig) {
		this.pspConfig = pspConfig;
	}
	
}
