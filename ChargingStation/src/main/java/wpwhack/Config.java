package wpwhack;

import java.util.Map;

import com.worldpay.innovation.wpwithin.types.WWHCECard;

public class Config {
    private int port;
    private String host;
    private Map<String, String> pspConfig;

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

    public Map<String, String> getPspConfig() {
        return pspConfig;
    }

    public void setPspConfig(Map<String, String> pspConfig) {
        this.pspConfig = pspConfig;
    }

}
