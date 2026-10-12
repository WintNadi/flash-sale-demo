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

        Event event;
        try {
            event = Webhook.constructEvent(
                    payload,
                    signature,
                    stripeService.getWebhookSecret()
            );
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.status(400).body("Invalid signature");
        }

        log.info("Received Stripe webhook: {}", event.getType());

        if ("payment_intent.succeeded".equals(event.getType())) {
            handlePaymentSucceeded(event);
        }

        return ResponseEntity.ok("ok");
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("No PaymentIntent in event"));

        String orderIdStr = intent.getMetadata().get("orderId");
        if (orderIdStr == null) {
            log.warn("PaymentIntent {} has no orderId metadata", intent.getId());
            return;
        }

        long orderId = Long.parseLong(orderIdStr);
        log.info("Payment succeeded for order {}, intent {}", orderId, intent.getId());

        try {
            flashSaleService.markOrderAsPaid(orderId);
            log.info("Order {} marked as CONFIRMED", orderId);
        } catch (Exception e) {
            log.error("Failed to mark order {} as paid: {}", orderId, e.getMessage(), e);
        }
    }
}
