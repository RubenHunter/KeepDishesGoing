package be.kdg.backend.infrastructure.payment;

import be.kdg.backend.application.PaymentProperties;
import be.kdg.backend.application.payment.PaymentGateway;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.CheckoutService;
import com.stripe.service.checkout.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Stripe gateway (US20). The Stripe SDK client is mocked — only the gateway's
 * mapping logic is under test: session id/url mapping, euro→cent conversion, currency lowercasing,
 * and the paid/failed translation.
 */
class StripePaymentGatewayTest {

    private static final String SUCCESS_TEMPLATE = "http://localhost:5173/#/orders/{orderId}/confirmation";

    private StripeClient client;
    private CheckoutService checkoutService;
    private SessionService sessionService;
    private StripePaymentGateway gateway;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties(
                "stripe", "dev-secret", "X-Payment-Signature",
                new PaymentProperties.Stripe("sk_test_x", "whsec_x",
                        SUCCESS_TEMPLATE, "http://localhost:5173/#/checkout"));
        client = mock(StripeClient.class);
        checkoutService = mock(CheckoutService.class);
        sessionService = mock(SessionService.class);
        when(client.checkout()).thenReturn(checkoutService);
        when(checkoutService.sessions()).thenReturn(sessionService);
        gateway = new StripePaymentGateway(properties, client);
    }

    private Session paidSession(String id, String url) {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(id);
        when(session.getUrl()).thenReturn(url);
        return session;
    }

    @Test
    void startPayment_returnsSessionIdAndRedirectUrl() throws Exception {
        Session session = paidSession("cs_test_123", "https://checkout.stripe.com/c/pay/cs_test_123");
        when(sessionService.create(any(SessionCreateParams.class))).thenReturn(session);

        PaymentGateway.StartPaymentResponse result = gateway.startPayment(
                new PaymentGateway.StartPaymentRequest("order-1", 12.34, "EUR"));

        assertThat(result.paymentRef()).isEqualTo("cs_test_123");
        assertThat(result.redirectUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_123");
    }

    @Test
    void startPayment_buildsPaymentSessionInCents() throws Exception {
        Session session = paidSession("cs_test_123", "u");
        when(sessionService.create(any(SessionCreateParams.class))).thenReturn(session);

        gateway.startPayment(new PaymentGateway.StartPaymentRequest("order-1", 9.99, "EUR"));

        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(sessionService).create(captor.capture());
        SessionCreateParams params = captor.getValue();

        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
        assertThat(params.getSuccessUrl()).isEqualTo("http://localhost:5173/#/orders/order-1/confirmation");
        assertThat(params.getClientReferenceId()).isEqualTo("order-1");

        SessionCreateParams.LineItem line = params.getLineItems().get(0);
        assertThat(line.getQuantity()).isEqualTo(1L);
        assertThat(line.getPriceData().getCurrency()).isEqualTo("eur");
        assertThat(line.getPriceData().getUnitAmount()).isEqualTo(999L);
    }

    @Test
    void startPayment_convertsNonIntegerEurosWithoutRoundingError() throws Exception {
        Session session = paidSession("cs_test_123", "u");
        when(sessionService.create(any(SessionCreateParams.class))).thenReturn(session);

        gateway.startPayment(new PaymentGateway.StartPaymentRequest("order-1", 12.34, "EUR"));

        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(sessionService).create(captor.capture());
        assertThat(captor.getValue().getLineItems().get(0).getPriceData().getUnitAmount()).isEqualTo(1234L);
    }

    @Test
    void confirm_mapsPaidStatus() throws Exception {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(sessionService.retrieve(any(String.class))).thenReturn(session);

        PaymentGateway.PaymentConfirmation result = gateway.confirm("cs_test_123");

        assertThat(result.paymentRef()).isEqualTo("cs_test_123");
        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentConfirmation.PaymentStatus.PAID);
    }

    @Test
    void confirm_mapsNonPaidStatusToFailed() throws Exception {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("open");
        when(sessionService.retrieve(any(String.class))).thenReturn(session);

        PaymentGateway.PaymentConfirmation result = gateway.confirm("cs_test_123");

        assertThat(result.status()).isEqualTo(PaymentGateway.PaymentConfirmation.PaymentStatus.FAILED);
    }
}
