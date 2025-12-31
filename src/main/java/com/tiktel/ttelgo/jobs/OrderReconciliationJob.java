package com.tiktel.ttelgo.jobs;

import com.tiktel.ttelgo.order.application.OrderService;
import com.tiktel.ttelgo.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job to reconcile stale orders
 * Runs every 10 minutes to retry failed/stuck orders
 */
@Slf4j
@Component
public class OrderReconciliationJob {
    
    private final OrderService orderService;
    
    public OrderReconciliationJob(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Reconcile orders that are stuck in intermediate states
     */
    @Scheduled(cron = "${app.order.reconciliation-cron:0 */10 * * * *}")
    public void reconcileStaleOrders() {
        log.info("Starting order reconciliation job");
        
        try {
            // Find orders that are older than 10 minutes and need attention
            List<Order> staleOrders = orderService.findStaleOrders(10);
            
            if (staleOrders.isEmpty()) {
                log.info("No stale orders found");
                return;
            }
            
            log.info("Found {} stale orders to reconcile", staleOrders.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Order order : staleOrders) {
                try {
                    log.info("Reconciling order: orderId={}, status={}",
                            order.getId(), order.getStatus());
                    
                    // Retry provisioning if order is in failed state
                    if (order.needsProvisioning()) {
                        orderService.provisionOrder(order.getId());
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to reconcile order: orderId={}", order.getId(), e);
                    failureCount++;
                }
            }
            
            log.info("Order reconciliation completed: success={}, failed={}", 
                    successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Error during order reconciliation job", e);
        }
    }
}

