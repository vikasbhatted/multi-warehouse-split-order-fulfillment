package com.fashionhub.fulfillment.web;

import com.fashionhub.fulfillment.dto.CreateOrderRequest;
import com.fashionhub.fulfillment.dto.OrderViews.OrderDetail;
import com.fashionhub.fulfillment.dto.OrderViews.OrderSummary;
import com.fashionhub.fulfillment.dto.TrackerViews.TrackerResponse;
import com.fashionhub.fulfillment.service.OrderService;
import com.fashionhub.fulfillment.service.TrackerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    private final TrackerService tracker;

    public OrderController(OrderService orders, TrackerService tracker) {
        this.orders = orders;
        this.tracker = tracker;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderSummary create(@Valid @RequestBody CreateOrderRequest request) {
        return orders.createOrder(request);
    }

    @GetMapping
    public List<OrderSummary> list() {
        return orders.list();
    }

    @GetMapping("/{id}")
    public OrderDetail get(@PathVariable String id) {
        return orders.getOrder(id);
    }

    @GetMapping("/{id}/tracker")
    public TrackerResponse track(@PathVariable String id) {
        return tracker.track(id);
    }
}
