package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.common.OrderRejectedException;
import com.fashionhub.fulfillment.domain.CustomerOrder;
import com.fashionhub.fulfillment.domain.ItemStatus;
import com.fashionhub.fulfillment.domain.OrderItem;
import com.fashionhub.fulfillment.domain.WarehouseStock;
import com.fashionhub.fulfillment.repository.WarehouseStockRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides which warehouse ships which item.
 * <ol>
 *   <li>If one warehouse holds the complete order, the order is consolidated into one parcel.</li>
 *   <li>Otherwise each item goes to a warehouse with enough stock, preferring warehouses that are
 *       already used by the order (fewest parcels), then the warehouse with most stock.</li>
 * </ol>
 * Stock is reserved (decremented) as part of the allocation.
 */
@Service
public class AllocationService {

    private final WarehouseStockRepository stocks;

    public AllocationService(WarehouseStockRepository stocks) {
        this.stocks = stocks;
    }

    @Transactional
    public void allocate(CustomerOrder order) {
        List<OrderItem> items = order.getItems();
        if (items.isEmpty()) {
            throw new OrderRejectedException("Order has no items");
        }

        Map<String, Map<String, Integer>> stockBySku = new HashMap<>();
        for (OrderItem it : items) {
            Map<String, Integer> perWarehouse = new TreeMap<>();
            for (WarehouseStock s : stocks.findBySku(it.getSku())) {
                perWarehouse.put(s.getWarehouseId(), s.getQuantity());
            }
            stockBySku.put(it.getSku(), perWarehouse);
        }

        Map<String, String> chosen = new LinkedHashMap<>();

        // Strategy 1: consolidate in a single warehouse
        Set<String> candidates = new TreeSet<>();
        stockBySku.values().forEach(m -> candidates.addAll(m.keySet()));
        String single = candidates.stream()
                .filter(wh -> items.stream().allMatch(
                        it -> stockBySku.get(it.getSku()).getOrDefault(wh, 0) >= it.getQuantity()))
                .findFirst().orElse(null);

        if (single != null) {
            items.forEach(it -> chosen.put(it.getSku(), single));
        } else {
            // Strategy 2: split shipment, minimise number of parcels
            Set<String> used = new LinkedHashSet<>();
            // most constrained items first -> greedy choice yields the fewest parcels
            List<OrderItem> byConstraint = items.stream()
                    .sorted(Comparator.comparingInt((OrderItem it) -> (int) stockBySku.get(it.getSku()).values().stream()
                            .filter(q -> q >= it.getQuantity()).count()))
                    .toList();
            for (OrderItem it : byConstraint) {
                Map<String, Integer> perWarehouse = stockBySku.get(it.getSku());
                Optional<String> best = perWarehouse.entrySet().stream()
                        .filter(e -> e.getValue() >= it.getQuantity())
                        .max(Comparator.<Map.Entry<String, Integer>>comparingInt(e -> used.contains(e.getKey()) ? 1 : 0)
                                .thenComparingInt(Map.Entry::getValue))
                        .map(Map.Entry::getKey);
                if (best.isEmpty()) {
                    throw new OrderRejectedException("Insufficient stock for " + it.getSku()
                            + " (requested " + it.getQuantity() + ")");
                }
                chosen.put(it.getSku(), best.get());
                used.add(best.get());
            }
        }

        // Reserve stock
        for (OrderItem it : items) {
            String wh = chosen.get(it.getSku());
            WarehouseStock stock = stocks.findBySkuAndWarehouseId(it.getSku(), wh)
                    .orElseThrow(() -> new OrderRejectedException("No stock record for " + it.getSku()));
            stock.setQuantity(stock.getQuantity() - it.getQuantity());
            it.setWarehouseId(wh);
            it.setStatus(ItemStatus.ALLOCATED);
        }
    }
}
