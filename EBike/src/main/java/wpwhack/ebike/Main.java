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

    private static Config config;
    private static String rpcLogFile;
    public static void main(String[] args) throws Exception  {
        System.out.println("Electric Bike Consumer");
        loadConfig();

        WPWithinWrapper wpw = new WPWithinWrapperImpl(config.getHost(), config.getPort(), true, rpcAgentListener, rpcLogFile);
        BikeConsumer c1 = new BikeConsumer(wpw, config);

        c1.connectDevice("TEAM3CS1", 1000);
        c1.consume();

        wpw.stopRPCAgent();
    }


    /**
     * Loads config and path to logfile
     */
    private static void loadConfig() {
        // define log file name for the rpc agent (based on the package name),
        // e.g. "rpc-within-consumerex.log";
        String[] splitedPkgName = Main.class.getPackage().getName().split("\\.");
        rpcLogFile = "rpc-within-" + splitedPkgName[splitedPkgName.length-1] + ".log";
		Gson gson = new Gson();
		InputStream stream = Config.class.getResourceAsStream("/ebike.json");
		String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
		// System.out.println(result);
		config = gson.fromJson(result, Config.class);
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


}
