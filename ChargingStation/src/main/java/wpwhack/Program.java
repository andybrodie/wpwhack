package wpwhack;

import com.google.gson.Gson;
import com.worldpay.innovation.wpwithin.WPWithinGeneralException;
import com.worldpay.innovation.wpwithin.WPWithinWrapper;
import com.worldpay.innovation.wpwithin.WPWithinWrapperImpl;
import com.worldpay.innovation.wpwithin.eventlistener.EventListener;
import com.worldpay.innovation.wpwithin.rpc.launcher.Listener;
import com.worldpay.innovation.wpwithin.types.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Program {

    public static int GRID_PRICE = 100;
    private static Config config;
    private static String rpcLogFile;

    private WWService elecService;
    private WWService hireService = createHireService();
    private WPWithinWrapper wpw;

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.err.println("Pass parameter whether this is charging station 1 or 2");
            return;
        }
        int stationNo = Integer.parseInt(args[0]);
        System.out.println("Starting charging station " + stationNo);
        loadConfig();

        new Program().go(stationNo);
    }

    private void go(int stationNo) throws Exception {
        // Hire service never changes and never needs to be regenerated.
        //InputStream stream = Config.class.getResourceAsStream("/electricityService.json");
        //List<WWService> services = new ServiceCreator().fromJson(stream);

        //services.stream().filter(t -> t.getName()=="Charging Service")

        if (stationNo == 1) {
            elecService = createElectricityService(GRID_PRICE, 50);
        } else {
            elecService = createElectricityService(GRID_PRICE, 100);
        }

        wpw = createWrapper(stationNo, hireService, elecService);

        mainLoop(stationNo);
    }

    private WPWithinWrapper createWrapper(int stationNo, WWService hireService, WWService elecService) {
        WPWithinWrapper wpw = new WPWithinWrapperImpl(config.getHost(), config.getPort() + (stationNo * 10), true,
                wpWithinEventListener, config.getPort() + 1,
                rpcAgentListener, rpcLogFile);

        wpw.setup("TEAM3CS" + stationNo, "TEAM3's Charging Station No. " + stationNo);
        wpw.addService(elecService);
        wpw.addService(hireService);
        wpw.initProducer(config.getPspConfig());
        return wpw;
    }

    private void mainLoop(int stationNo) throws Exception {
        boolean hasQuit = false;

        Thread t = start(wpw);

        while (!hasQuit) {
            printMenu();

            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine().toUpperCase();
            if ("X".equals(s)) {
                stop(wpw, t);
                hasQuit = true;
            }
            if (s.startsWith("S ")) {
                int newPrice = parsePrice(s);
                System.out.println("Changing solar price to " + newPrice);
                stop(wpw, t);
                elecService = createElectricityService(GRID_PRICE, newPrice);
                wpw = createWrapper(stationNo, hireService, elecService);
                t = start(wpw);
            }
        }
    }

    private Thread start(WPWithinWrapper wpw) {
        Thread t = new Thread(() -> {
            wpw.startServiceBroadcast(0);
        });
        t.start();
        return t;
    }

    private int parsePrice(String s) {
        return Integer.parseInt(s.substring(s.indexOf(" ") + 1));
    }

    private void stop(WPWithinWrapper wpw, Thread t) throws Exception {
        wpw.stopServiceBroadcast();
        ((WPWithinWrapperImpl) wpw).eventServer.stop();
        wpw.stopRPCAgent();
        t.join();
    }

    private void printMenu() {
        System.out.println("MENU:");
        String[] menuItems = new String[]{
                "X = Exit",
                "S <n> = Set solar price to <n>"
        };
        for (String s : menuItems) {
            System.out.println("\t" + s);
        }
        System.out.print(" > ");

    }

    private WWService createHireService() {
        WWService svc = createService("Hire Charge",
                "Pay to hire a bike from this charging service",
                2,
                "Bike hire");

        HashMap<Integer, WWPrice> prices = new HashMap<>(1);

        WWPrice ccPrice = addPrice(1, "Bike Charge", "1 bike", 1, 10, "GBP");
        prices = new HashMap<>(1);
        prices.put(ccPrice.getId(), ccPrice);
        svc.setPrices(prices);

        return svc;
    }

    private WWService createElectricityService(int gridPrice, int solarPrice) {
        WWService svc = createService("Electricity Provision",
                "Provide electricity",
                1,
                "Charger");

        HashMap<Integer, WWPrice> prices = new HashMap<>(1);
        WWPrice ccPrice = addPrice(1, "Kilowatt-hour from the National Grid", "One kilowatt-hour", 1, gridPrice, "GBP");
        prices.put(ccPrice.getId(), ccPrice);
        ccPrice = addPrice(2, "Kilowatt-hour from the Solar Store", "One kilowatt-hour", 1, solarPrice, "GBP");
        prices.put(ccPrice.getId(), ccPrice);
        svc.setPrices(prices);
        return svc;
    }

    private WWPrice addPrice(int id, String description, String unitDescription, int unitId, int amount, String currency) {
        WWPrice ccPrice = new WWPrice();
        ccPrice.setId(id);
        ccPrice.setDescription(description);
        ccPrice.setUnitDescription(unitDescription);
        ccPrice.setUnitId(unitId);
        WWPricePerUnit ppu = new WWPricePerUnit();
        ppu.setAmount(amount);
        ppu.setCurrencyCode(currency);
        ccPrice.setPricePerUnit(ppu);
        return ccPrice;
    }

    private WWService createService(String name, String description, int id, String serviceType) {
        WWService svc = new WWService();
        svc.setId(id);
        svc.setName(name);
        svc.setDescription(description);
        svc.setServiceType(serviceType);
        return svc;
    }

    private final Listener rpcAgentListener = new Listener() {
        @Override
        public void onApplicationExit(int exitCode, String stdOutput, String errOutput) {

            System.out.printf("RPC Agent process did exit.");
            System.out.printf("ExitCode: %d", exitCode);
            System.out.printf("stdout: \n%s\n", stdOutput);
            System.out.printf("stderr: \n%s\n", errOutput);
        }
    };

    /**
     * Loads config and path to logfile
     */
    private static void loadConfig() {
        // define log file name for the rpc agent (based on the package name),
        // e.g. "rpc-within-consumerex.log";
        String[] splitedPkgName = Program.class.getPackage().getName().split("\\.");
        rpcLogFile = "rpc-within-" + splitedPkgName[splitedPkgName.length - 1] + ".log";
        Gson gson = new Gson();
        InputStream stream = Config.class.getResourceAsStream("/sample-producer.json");
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        config = gson.fromJson(result, Config.class);
    }

    private EventListener wpWithinEventListener = new EventListener() {

        @Override
        public void onBeginServiceDelivery(int serviceID, int servicePriceID,
                                           WWServiceDeliveryToken wwServiceDeliveryToken, int unitsToSupply) throws WPWithinGeneralException {

            try {
                System.out.println("event from core - onBeginServiceDelivery()");
                System.out.printf("ServiceID: %d\n", serviceID);
                System.out.printf("UnitsToSupply: %d\n", unitsToSupply);
                System.out.printf("SDT.Key: %s\n", wwServiceDeliveryToken.getKey());
                System.out.printf("SDT.Expiry: %s\n", wwServiceDeliveryToken.getExpiry());
                System.out.printf("SDT.Issued: %s\n", wwServiceDeliveryToken.getIssued());
                System.out.printf("SDT.Signature: %s\n", wwServiceDeliveryToken.getSignature());
                System.out.printf("SDT.RefundOnExpiry: %b\n", wwServiceDeliveryToken.isRefundOnExpiry());
            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        @Override
        public void onEndServiceDelivery(int serviceID, WWServiceDeliveryToken wwServiceDeliveryToken,
                                         int unitsReceived) throws WPWithinGeneralException {
            try {
                System.out.println("event from core - onEndServiceDelivery()");
                System.out.printf("ServiceID: %d\n", serviceID);
                System.out.printf("UnitsReceived: %d\n", unitsReceived);
                System.out.printf("SDT.Key: %s\n", wwServiceDeliveryToken.getKey());
                System.out.printf("SDT.Expiry: %s\n", wwServiceDeliveryToken.getExpiry());
                System.out.printf("SDT.Issued: %s\n", wwServiceDeliveryToken.getIssued());
                System.out.printf("SDT.Signature: %s\n", wwServiceDeliveryToken.getSignature());
                System.out.printf("SDT.RefundOnExpiry: %b\n", wwServiceDeliveryToken.isRefundOnExpiry());
            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDiscoveryEvent(String remoteAddr) throws WPWithinGeneralException {
            System.out.printf("Event: onServiceDiscovery(remoteAddr:%s)\n", remoteAddr);

        }

        @Override
        public void onServicePricesEvent(String remoteAddr, int serviceId) throws WPWithinGeneralException {
            System.out.printf("Event: onServicePricesPresent(remoteAddr:%s, serviceId:%d)\n", remoteAddr, serviceId);
        }

        @Override
        public void onServiceTotalPriceEvent(String remoteAddr, int serviceId, WWTotalPriceResponse t)
                throws WPWithinGeneralException {
            System.out.printf("Event: onServiceTotalPriceEvent(remoteAddr:%s, serviceId:%d)\n", remoteAddr, serviceId);
            System.out.printf("\t(clientId:sd, currentCode:%s, merchantClientKey:%s, paymentReferenceId:%s, serverId:%s, totalPrice:%d, unitsToSupply:%d, priceId:%d",
                    t.getClientId(), t.getCurrencyCode(), t.getMerchantClientKey(), t.getPaymentReferenceId(),
                    t.getServerId(), t.getTotalPrice(), t.getUnitsToSupply(), t.getPriceId());
        }

        @Override
        public void onMakePaymentEvent(int totalPrice, String orderCurrency, String clientToken,
                                       String orderDescription, String uuid) throws WPWithinGeneralException {
            System.out.printf("Event: onMakePayment(totalPrice:%d, orderCurrency:%s, clientToken:%s, orderDescription:%s, uuid:%s\n",
                    totalPrice, orderCurrency, clientToken, orderDescription, uuid);
        }

        @Override
        public void onErrorEvent(String msg) throws WPWithinGeneralException {
            System.out.printf("Event: onError: %s\n", msg);
        }
    };

}