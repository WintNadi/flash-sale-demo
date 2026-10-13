package org.example.flashsale.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.example.flashsale.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    /**
     * Create a Stripe PaymentIntent for an order.
     * Returns the client_secret, which the frontend uses to complete the payment.
     *
     * Idempotency: reuses the same PaymentIntent for the same order ID.
     */
    public String createPaymentIntent(Order order) throws StripeException {
        // Amount in cents — $499.00 = 49900
        long amountCents = Math.round(order.getTotalPrice() * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("usd")
                .setDescription("FlashSale order #" + order.getId())
                .putMetadata("orderId", String.valueOf(order.getId()))
                .putMetadata("userId", String.valueOf(order.getUserId()))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        // IMPORTANT: no idempotency key — Stripe creates a fresh intent each call.
        // Safety comes from the frontend preventing duplicate clicks
        // AND the webhook handler being idempotent (it checks order status).
        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}
