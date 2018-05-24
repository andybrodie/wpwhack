package wpwhack;

import com.google.gson.Gson;
import com.worldpay.innovation.wpwithin.types.WWPrice;
import com.worldpay.innovation.wpwithin.types.WWPricePerUnit;
import com.worldpay.innovation.wpwithin.types.WWService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceCreator {

    public ServiceCreator() {
    }

    public List<WWService> fromJson(InputStream stream) {
        Gson gson = new Gson();
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        ServiceDescriptions sd = gson.fromJson(result, ServiceDescriptions.class);
        return createWWService(sd);
    }

    private List<WWService> createWWService(ServiceDescriptions sds) {
        List<WWService> services = new ArrayList<>();
        int serviceIdCount = 1;
        for (ServiceDescription sd : sds.services) {
            WWService svc = new WWService();

            svc.setId(serviceIdCount);
            svc.setDescription(sd.description);
            svc.setName(sd.name);
            svc.setServiceType(sd.serviceType);

            svc.setPrices(createWWPrices(sd.prices));
            services.add(svc);
            serviceIdCount++;
        }
        return services;
    }

    private HashMap<Integer,WWPrice> createWWPrices(PriceDescription[] prices) {
        HashMap<Integer, WWPrice> wwPrices = new HashMap<Integer, WWPrice>();
        int idCount = 1;
        for (PriceDescription price : prices) {
            WWPrice wwPrice = new WWPrice();
            wwPrice.setId(idCount);
            wwPrice.setDescription(price.description);
            WWPricePerUnit wwppu = new WWPricePerUnit();
            wwppu.setAmount(price.amount);
            wwppu.setCurrencyCode(price.currencyCode);
            wwPrice.setPricePerUnit(wwppu);
            wwPrice.setUnitDescription(price.unitDescription);
            wwPrice.setUnitId(1);

            wwPrices.put(idCount, wwPrice);

            idCount++;
        }
        return wwPrices;
    }

    private static class ServiceDescriptions {

        private ServiceDescription[] services;
    }

    private static class ServiceDescription {
        private String name;
        private String description;
        private String serviceType;
        private PriceDescription[] prices;
    }

    private static class PriceDescription {
        public String pricePerUnit;
        public String unitDescription;
        private String description;
        private int amount;
        private String currencyCode;

    }



}
