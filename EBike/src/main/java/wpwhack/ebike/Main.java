package wpwhack.ebike;

import com.google.gson.Gson;
import com.worldpay.innovation.wpwithin.WPWithinGeneralException;
import com.worldpay.innovation.wpwithin.WPWithinWrapper;
import com.worldpay.innovation.wpwithin.WPWithinWrapperImpl;
import com.worldpay.innovation.wpwithin.rpc.launcher.Listener;
import com.worldpay.innovation.wpwithin.types.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    private static WPWithinWrapper wpw;
    private static WWDevice wpwDevice;
    private static Config config;
    private static String rpcLogFile;
    public static void main(String[] args) {

        System.out.println("Electric Bike Consumer");
        loadConfig();
        wpw = new WPWithinWrapperImpl(config.getHost(), config.getPort(), true, rpcAgentListener, rpcLogFile);

        try {

            wpw.setup("TEAM3 Bike", "A cool electric bike");
            wpwDevice = wpw.getDevice();

            Set<WWServiceMessage> devices = discoverDevices();

            if(devices != null && devices.iterator().hasNext()) {

                // Will pick the first device discovered
                WWServiceMessage svcMsg = devices.iterator().next();

                connectToDevice(svcMsg);

                Set<WWServiceDetails> svcDetails = getAvailableServices();

                if(svcDetails != null && svcDetails.iterator().hasNext()) {

                    // Select the first service and get a list of prices for that service
                    WWServiceDetails svcDetail = svcDetails.iterator().next();

                    Set<WWPrice> svcPrices = getServicePrices(svcDetail.getServiceId());

                    if(svcPrices != null && svcPrices.iterator().hasNext()) {

                        // Select the first price in the list
                        WWPrice svcPrice = svcPrices.iterator().next();

                        WWTotalPriceResponse tpr = getServicePriceQuote(svcDetail.getServiceId(), 5, svcPrice.getId());

                        System.out.printf("Client ID: %s\n", tpr.getClientId());
                        System.out.printf("Server ID: %s\n", tpr.getServerId());

                        WWPaymentResponse paymentResponse = purchaseService(svcDetail.getServiceId(), tpr);
                    }
                }
            }

            wpw.stopRPCAgent();

        } catch(WPWithinGeneralException wpge) {

            wpge.printStackTrace();
        }
    }

    private static Set<WWServiceMessage> discoverDevices() throws WPWithinGeneralException {

        Set<WWServiceMessage> devices = wpw.deviceDiscovery(15000);

        if(devices.size() > 0) {

            System.out.printf("%d services found:\n", devices.size());
            for (WWServiceMessage svcMsg : devices) {
                if (svcMsg.getDeviceName().startsWith(config.getDeviceNamePrefix())) {
                    System.out.printf("Device Description: %s\n", svcMsg.getDeviceDescription());
                    System.out.printf("Hostname: %s\n", svcMsg.getHostname());
                    System.out.printf("Port: %d\n", svcMsg.getPortNumber());
                    System.out.printf("URL Prefix: %s\n", svcMsg.getUrlPrefix());
                    System.out.printf("ServerId: %s\n", svcMsg.getServerId());
                    System.out.printf("Scheme: %s\n", svcMsg.getScheme());
                    devices.add(svcMsg);
                    for (String s: svcMsg.getServiceTypes()) {
                        System.out.printf("ServiceTypes: %s\n", s);
                    }
                    System.out.println("--------");
                } else {
                    System.out.println("Ignoring service that doesn't start with " + config.getDeviceNamePrefix());
                }
            }

        } else {

            System.out.println("No services found..");
        }

        return devices;
    }

    private static void connectToDevice(WWServiceMessage svcMsg) throws WPWithinGeneralException {
//		System.out.println(config.getPspConfig().get("psp_name"));
//
//        Map<String, String> pspConfig = new HashMap<>();
//
//        // Worldpay Online Payments
//        pspConfig.put(PSPConfig.PSP_NAME, PSPConfig.WORLDPAY_ONLINE_PAYMENTS);
//        pspConfig.put(PSPConfig.API_ENDPOINT, "https://api.worldpay.com/v1");

        // Worldpay Total US / SecureNet
//        pspConfig.put(PSPConfig.PSP_NAME, PSPConfig.SECURE_NET);
//        pspConfig.put(PSPConfig.API_ENDPOINT, "https://gwapi.demo.securenet.com/api");
//        pspConfig.put(PSPConfig.APP_VERSION, "0.1");
//        pspConfig.put(PSPConfig.DEVELOPER_ID, "12345678");

        wpw.initConsumer(svcMsg.getScheme(), svcMsg.getHostname(), svcMsg.getPortNumber(), svcMsg.getUrlPrefix(), wpwDevice.getUid(), config.getHceCard(), config.getPspConfig());
    }

    private static Set<WWServiceDetails> getAvailableServices() throws WPWithinGeneralException {

        Set<WWServiceDetails> services = wpw.requestServices();

        System.out.printf("%d services found\n", services.size());

        if(services != null && services.size() > 0) {

            Iterator<WWServiceDetails> svcIterator = services.iterator();

            while(svcIterator.hasNext()) {

                WWServiceDetails svc = svcIterator.next();

                System.out.println("Service:");
                System.out.printf("Id: %d\n", svc.getServiceId());
                System.out.printf("Description: %s\n", svc.getServiceDescription());
                System.out.println("------");
            }
        }

        return services;
    }

    private static Set<WWPrice> getServicePrices(int serviceId) throws WPWithinGeneralException {

        Set<WWPrice> prices = wpw.getServicePrices(serviceId);

        System.out.printf("%d prices found for service id %d\n", prices.size(), serviceId);

        if(prices != null && prices.size() > 0) {

            Iterator<WWPrice> priceIterator = prices.iterator();

            while(priceIterator.hasNext()) {

                WWPrice price = priceIterator.next();

                System.out.println("Price:");
                System.out.printf("Id: %d\n", price.getId());
                System.out.printf("Description: %s\n", price.getDescription());
                System.out.printf("UnitId: %d\n", price.getUnitId());
                System.out.printf("UnitDescription: %s\n", price.getUnitDescription());
                System.out.printf("Unit Price Amount: %d\n", price.getPricePerUnit().getAmount());
                System.out.printf("Unit Price CurrencyCode: %s\n", price.getPricePerUnit().getCurrencyCode());
                System.out.println("------");

            }
        }

        return prices;
    }

    private static WWTotalPriceResponse getServicePriceQuote(int serviceId, int numberOfUnits, int priceId) throws WPWithinGeneralException {

        WWTotalPriceResponse tpr = wpw.selectService(serviceId, numberOfUnits, priceId);

        if(tpr != null ) {

            System.out.println("Did retrieve price quote:");
            System.out.printf("Merchant client key: %s\n", tpr.getMerchantClientKey());
            System.out.printf("Payment reference id: %s\n", tpr.getPaymentReferenceId());
            System.out.printf("Units to supply: %d\n", tpr.getUnitsToSupply());
            System.out.printf("Currency code: %s\n", tpr.getCurrencyCode());
            System.out.printf("Total price: %d\n", tpr.getTotalPrice());

        } else {

            System.out.println("Result of select service is null..");
        }

        return tpr;
    }

    private static WWPaymentResponse purchaseService(int serviceID, WWTotalPriceResponse pReq) throws WPWithinGeneralException {

        WWPaymentResponse pResp = wpw.makePayment(pReq);

        if(pResp != null) {

            System.out.printf("Payment response: ");
            System.out.printf("Total paid: %d\n", pResp.getTotalPaid());
            System.out.printf("ServiceDeliveryToken.issued: %s\n", pResp.getServiceDeliveryToken().getIssued());
            System.out.printf("ServiceDeliveryToken.expiry: %s\n", pResp.getServiceDeliveryToken().getExpiry());
            System.out.printf("ServiceDeliveryToken.key: %s\n", pResp.getServiceDeliveryToken().getKey());
            System.out.printf("ServiceDeliveryToken.signature: %s\n", pResp.getServiceDeliveryToken().getSignature());
            System.out.printf("ServiceDeliveryToken.refundOnExpiry: %b\n", pResp.getServiceDeliveryToken().isRefundOnExpiry());

            beginServiceDelivery(serviceID, pResp.getServiceDeliveryToken(), 5);

        } else {

            System.out.println("Result of make payment is null..");
        }

        return pResp;
    }

    private static void beginServiceDelivery(int serviceID, WWServiceDeliveryToken token, int unitsToSupply) throws WPWithinGeneralException {

        System.out.println("Calling beginServiceDelivery()");

        wpw.beginServiceDelivery(serviceID, token, unitsToSupply);

        try {
            System.out.println("Sleeping 10 seconds..");
            Thread.sleep(10000);
            endServiceDelivery(serviceID, token, unitsToSupply);
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
    }

    private static void endServiceDelivery(int serviceID, WWServiceDeliveryToken token, int unitsReceived) throws WPWithinGeneralException {

        System.out.println("Calling endServiceDelivery()");

        wpw.endServiceDelivery(serviceID, token, unitsReceived);
    }

    private static final Listener rpcAgentListener = new Listener() {
        @Override
        public void onApplicationExit(int exitCode, String stdOutput, String errOutput) {

            System.out.printf("RPC Agent process did exit.");
//            System.out.printf("ExitCode: %d", exitCode);
//            System.out.printf("stdout: \n%s\n", stdOutput);
//            System.out.printf("stderr: \n%s\n", errOutput);
        }
    };
    
    /**
     * Loads config and path to logfile
     */
    private static void loadConfig() {
        // define log file name for the rpc agent (based on the package name),
        // e.g. "rpc-within-consumerex.log";
        String[] splitedPkgName = Main.class.getPackage().getName().split("\\.");
        rpcLogFile = "rpc-within-" + splitedPkgName[splitedPkgName.length-1] + ".log";
		Gson gson = new Gson();
		InputStream stream = Config.class.getResourceAsStream("/sample-consumer.json");
		String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
		// System.out.println(result);
		config = gson.fromJson(result, Config.class);
    }
}
