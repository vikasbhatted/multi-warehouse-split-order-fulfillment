package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.domain.Product;
import com.fashionhub.fulfillment.domain.Warehouse;
import com.fashionhub.fulfillment.domain.WarehouseStock;
import com.fashionhub.fulfillment.repository.ProductRepository;
import com.fashionhub.fulfillment.repository.WarehouseRepository;
import com.fashionhub.fulfillment.repository.WarehouseStockRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds a small fashion catalogue and three fulfilment centres on first start. */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProductRepository products;
    private final WarehouseRepository warehouses;
    private final WarehouseStockRepository stocks;

    public DataSeeder(ProductRepository products, WarehouseRepository warehouses, WarehouseStockRepository stocks) {
        this.products = products;
        this.warehouses = warehouses;
        this.stocks = stocks;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() > 0) {
            migratePricesToInr();
            return;
        }
        warehouses.save(new Warehouse("WH-ERFURT", "Erfurt Fulfilment Centre", "Erfurt"));
        warehouses.save(new Warehouse("WH-LAHR", "Lahr Fulfilment Centre", "Lahr"));
        warehouses.save(new Warehouse("WH-GLADBACH", "Moenchengladbach Fulfilment Centre", "Moenchengladbach"));

        p("TSHIRT-WHT-M", "Organic Cotton T-Shirt, White, M", "Tops", "1299.00", "TSHIRT-GRY-M");
        p("TSHIRT-GRY-M", "Organic Cotton T-Shirt, Grey, M", "Tops", "1299.00", "TSHIRT-WHT-M");
        p("JEANS-BLU-32", "Slim Fit Jeans, Blue, W32", "Bottoms", "2999.00", "JEANS-BLK-32");
        p("JEANS-BLK-32", "Slim Fit Jeans, Black, W32", "Bottoms", "2999.00", "JEANS-BLU-32");
        p("SNEAK-WHT-42", "Retro Sneakers, White, EU42", "Shoes", "3999.00", "SNEAK-BLK-42");
        p("SNEAK-BLK-42", "Retro Sneakers, Black, EU42", "Shoes", "3999.00", "SNEAK-WHT-42");
        p("JACKET-GRN-L", "Utility Jacket, Green, L", "Outerwear", "5499.00", null);
        p("DRESS-RED-S", "Summer Dress, Red, S", "Dresses", "2499.00", "DRESS-BLU-S");
        p("DRESS-BLU-S", "Summer Dress, Blue, S", "Dresses", "2499.00", "DRESS-RED-S");
        p("SCARF-WOL-OS", "Merino Wool Scarf, One Size", "Accessories", "1499.00", null);

        s("TSHIRT-WHT-M", "WH-ERFURT", 50);
        s("TSHIRT-GRY-M", "WH-ERFURT", 40);
        s("JEANS-BLU-32", "WH-LAHR", 30);
        s("JEANS-BLK-32", "WH-LAHR", 25);
        s("SNEAK-WHT-42", "WH-GLADBACH", 20);
        s("SNEAK-BLK-42", "WH-GLADBACH", 15);
        s("JACKET-GRN-L", "WH-ERFURT", 4);
        s("DRESS-RED-S", "WH-LAHR", 12);
        s("DRESS-BLU-S", "WH-GLADBACH", 8);
        s("SCARF-WOL-OS", "WH-GLADBACH", 40);
        // a few items stocked in two centres so the consolidation strategy can kick in
        s("TSHIRT-WHT-M", "WH-LAHR", 20);
        s("JEANS-BLU-32", "WH-ERFURT", 6);
        log.info("Seeded {} products and {} warehouses", products.count(), warehouses.count());
    }

    /** Catalogue prices are Indian rupees. Databases seeded with the old euro prices are converted once. */
    private static final java.util.Map<String, String> INR_PRICES = java.util.Map.of(
            "TSHIRT-WHT-M", "1299.00", "TSHIRT-GRY-M", "1299.00", "JEANS-BLU-32", "2999.00",
            "JEANS-BLK-32", "2999.00", "SNEAK-WHT-42", "3999.00", "SNEAK-BLK-42", "3999.00",
            "JACKET-GRN-L", "5499.00", "DRESS-RED-S", "2499.00", "DRESS-BLU-S", "2499.00",
            "SCARF-WOL-OS", "1499.00");

    private void migratePricesToInr() {
        int changed = 0;
        for (Product p : products.findAll()) {
            String inr = INR_PRICES.get(p.getSku());
            if (inr != null && p.getPrice().compareTo(new BigDecimal(inr)) != 0) {
                p.setPrice(new BigDecimal(inr));
                changed++;
            }
        }
        if (changed > 0) {
            log.info("Converted {} catalogue prices to INR (existing orders keep their original amounts)", changed);
        }
    }

    private void p(String sku, String name, String cat, String price, String sub) {
        products.save(new Product(sku, name, cat, new BigDecimal(price), sub));
    }

    private void s(String sku, String wh, int qty) {
        stocks.save(new WarehouseStock(sku, wh, qty));
    }
}
