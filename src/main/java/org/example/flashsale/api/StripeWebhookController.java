package org.example.flashsale.api;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.example.flashsale.service.FlashSaleService;
import org.example.flashsale.service.StripeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeService stripeService;
    private final FlashSaleService flashSaleService;

    public StripeWebhookController(StripeService stripeService,
                                   FlashSaleService flashSaleService) {
        this.stripeService = stripeService;
        this.flashSaleService = flashSaleService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        log.info("WEBHOOK RECEIVED, payload length={}", payload.length());

        Event event;
        try {
            event = Webhook.constructEvent(
                    payload,
                    signature,
                    stripeService.getWebhookSecret()
            );
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            log.error("Failed to parse webhook event", e);
            return ResponseEntity.status(400).body("Invalid payload");
        }

        String eventType = event.getType();
        log.info("Received Stripe webhook: {}", eventType);

        if ("payment_intent.succeeded".equals(eventType)) {
            try {
                handlePaymentSucceeded(event);
            } catch (Exception e) {
                log.error("Error handling payment_intent.succeeded: {}", e.getMessage(), e);
            }
        }

        return ResponseEntity.ok("ok");
    }

    private void handlePaymentSucceeded(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        Object raw = deserializer.getObject().orElse(null);

        if (raw == null) {
            log.warn("Could not deserialize event object for event type {}", event.getType());
            return;
        }

        if (!(raw instanceof PaymentIntent intent)) {
            log.warn("Event object is not a PaymentIntent: {}", raw.getClass().getName());
            return;
        }

        String orderIdStr = intent.getMetadata().get("orderId");
        if (orderIdStr == null) {
            log.warn("PaymentIntent {} has no orderId metadata", intent.getId());
            return;
        }

        long orderId = Long.parseLong(orderIdStr);
        log.info("Payment succeeded for order {}, intent {}", orderId, intent.getId());

        flashSaleService.markOrderAsPaid(orderId);
        log.info("Order {} marked as CONFIRMED", orderId);
    }
}