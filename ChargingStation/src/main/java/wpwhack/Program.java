package wpwhack;

import com.google.gson.Gson;
import com.worldpay.innovation.wpwithin.WPWithinWrapper;
import com.worldpay.innovation.wpwithin.WPWithinWrapperImpl;
import com.worldpay.innovation.wpwithin.rpc.launcher.Listener;
import com.worldpay.innovation.wpwithin.types.WWPrice;
import com.worldpay.innovation.wpwithin.types.WWPricePerUnit;
import com.worldpay.innovation.wpwithin.types.WWService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Program {

    public static int GRID_PRICE = 100;
    private static Config config;
    private static String rpcLogFile;

    private static WWService elecService;
    private static WWService hireService = createHireService();
    private static WPWithinWrapper wpw;

    public static void main(String[] args) throws Exception {
d
        if (args.length == 0) {
            System.err.println("Pass parameter whether this is charging station 1 or 2");
            return;
        }
        int stationNo = Integer.parseInt(args[0]);
        System.out.println("Starting charging station " + stationNo);
        loadConfig();

        // Hire service never changes and never needs to be regenerated.
        if (stationNo == 1) {
            elecService = createElectricityService(GRID_PRICE, 50);
        } else {
            elecService = createElectricityService(GRID_PRICE, 100);
        }

        wpw = createWrapper(stationNo, hireService, elecService);

        mainLoop(stationNo);
    }

    private static WPWithinWrapper createWrapper(int stationNo, WWService hireService, WWService elecService) {
        WPWithinWrapper wpw = new WPWithinWrapperImpl(config.getHost(), config.getPort() + (stationNo * 10), true, rpcAgentListener, rpcLogFile);

        wpw.setup("TEAM3CS" + stationNo, "TEAM3's Charging Station No. " + stationNo);
        wpw.addService(elecService);
        wpw.addService(hireService);
        wpw.initProducer(config.getPspConfig());
        return wpw;
    }

    private static void mainLoop(int stationNo) throws Exception {
        boolean hasQuit = false;

        Thread t = start(wpw);

        while (!hasQuit) {
            printMenu();

            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
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

    private static Thread start(WPWithinWrapper wpw) {
        Thread t = new Thread(() -> {
            wpw.startServiceBroadcast(0);
        });
        t.start();
        return t;
    }

    private static int parsePrice(String s) {
        return Integer.parseInt(s.substring(s.indexOf(" ")+1));
    }

    private static void stop(WPWithinWrapper wpw, Thread t) throws Exception {
        wpw.stopServiceBroadcast();
        wpw.stopRPCAgent();
        t.join();
    }

    private static void printMenu() {
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

    private static WWService createHireService() {
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

    private static WWService createDistanceService() {
        WWService svc = createService("Standing Charge",
                "Bike Standing charge",
                2,
                "Bike hire");

        HashMap<Integer, WWPrice> prices = new HashMap<>(1);

        WWPrice ccPrice = addPrice(1, "Standing Charge", "1 kilometer", 1, 1, "GBP");
        prices = new HashMap<>(1);
        prices.put(ccPrice.getId(), ccPrice);
        svc.setPrices(prices);

        return svc;
    }

    private static WWService createElectricityService(int gridPrice, int solarPrice) {
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

    private static WWPrice addPrice(int id, String description, String unitDescription, int unitId, int amount, String currency) {
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

    private static WWService createService(String name, String description, int id, String serviceType) {
        WWService svc = new WWService();
        svc.setId(id);
        svc.setName(name);
        svc.setDescription(description);
        svc.setServiceType(serviceType);
        return svc;
    }

    private static final Listener rpcAgentListener = new Listener() {
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
}