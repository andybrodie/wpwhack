package wpwhack;

import com.google.gson.Gson;
import com.worldpay.innovation.wpwithin.WPWithinGeneralException;
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
import java.util.stream.Collectors;

public class Program {

    private static Config config;
    private static String rpcLogFile;
    public static void main(String[] args) {

        try {

            System.out.println("WorldpayWithin Sample Producer...");
            loadConfig();
            WPWithinWrapper wpw = new WPWithinWrapperImpl(config.getHost(), config.getPort(), true, rpcAgentListener, rpcLogFile);

            wpw.setup("Producer Example", "Example WorldpayWithin producer");

            WWService svc = new WWService();
            svc.setName("Car charger");
            svc.setDescription("Can charge your hybrid / electric car");
            svc.setId(1);
            svc.setServiceType("Charger");

            WWPrice ccPrice = new WWPrice();
            ccPrice.setId(1);
            ccPrice.setDescription("Kilowatt-hour");
            ccPrice.setUnitDescription("One kilowatt-hour");
            ccPrice.setUnitId(1);
            WWPricePerUnit ppu = new WWPricePerUnit();
            ppu.setAmount(25);
            ppu.setCurrencyCode("GBP");
            ccPrice.setPricePerUnit(ppu);

            HashMap<Integer, WWPrice> prices = new HashMap<>(1);
            prices.put(ccPrice.getId(), ccPrice);

            svc.setPrices(prices);
            wpw.addService(svc);

            wpw.initProducer(config.getPspConfig());

            wpw.startServiceBroadcast(0);

        } catch (WPWithinGeneralException e) {

            e.printStackTrace();
        }
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
        rpcLogFile = "rpc-within-" + splitedPkgName[splitedPkgName.length-1] + ".log";
        Gson gson = new Gson();
        InputStream stream = Config.class.getResourceAsStream("/sample-producer.json");
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        config = gson.fromJson(result, Config.class);
    }
}