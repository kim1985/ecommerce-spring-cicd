package com.myecom.service.validation.impl;

import com.myecom.model.CartItem;
import com.myecom.model.Order;
import com.myecom.model.User;
import com.myecom.service.validation.OrderValidator;
import com.myecom.exception.BusinessException;
import com.myecom.repository.OrderRepository;
import com.myecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Validatore per il limite giornaliero di ordini per utente.
 *
 * Previene spam e abusi limitando il numero massimo di ordini
 * che un utente può creare in un giorno.
 *
 * Il limite è configurabile tramite property: order.daily-limit
 *
 * Ordine di esecuzione: 30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyLimitValidator implements OrderValidator {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Value("${order.daily-limit:10}")
    private int dailyOrderLimit;

    @Override
    public void validate(Long userId, List<CartItem> cartItems) throws BusinessException {
        log.debug("Validating daily limit for user: {}, limit: {}", userId, dailyOrderLimit);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("Utente non trovato"));

            // Calcola inizio e fine giornata
            LocalDateTime startOfDay = LocalDateTime.now()
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = LocalDateTime.now()
                    .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            // Conta ordini di oggi
            List<Order> todayOrders = orderRepository.findUserOrdersInPeriod(user, startOfDay, endOfDay);

            log.debug("User: {} has {} orders today, limit: {}",
                    userId, todayOrders.size(), dailyOrderLimit);

            if (todayOrders.size() >= dailyOrderLimit) {
                log.warn("Daily limit validation failed for user: {}. Orders today: {}, Limit: {}",
                        userId, todayOrders.size(), dailyOrderLimit);

                throw new BusinessException(
                        String.format("Raggiunto il limite massimo di %d ordini al giorno. " +
                                        "Se hai necessità particolari, contatta il supporto clienti.",
                                dailyOrderLimit)
                );
            }

            log.debug("Daily limit validation passed for user: {} ({}/{} orders today)",
                    userId, todayOrders.size(), dailyOrderLimit);

        } catch (BusinessException e) {
            // Re-lancia le BusinessException
            throw e;
        } catch (Exception e) {
            // Per altri errori, non bloccare l'ordine ma logga l'errore
            log.error("Error checking daily limit for user: {} - {}", userId, e.getMessage());
            log.debug("Daily limit validation skipped due to error for user: {}", userId);
        }
    }

    @Override
    public String getName() {
        return "DailyLimit";
    }

    @Override
    public int getOrder() {
        return 30; // Dopo le validazioni base
    }
}