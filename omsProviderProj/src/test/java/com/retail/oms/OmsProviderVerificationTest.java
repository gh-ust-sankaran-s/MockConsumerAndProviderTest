package com.retail.oms;


import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@Provider("oms-provider")
//@PactFolder("../target/pacts")
@PactBroker(
        url = "http://127.0.0.1:9292"
)
public class OmsProviderVerificationTest {

    @RegisterExtension
    static final WireMockExtension wireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().port(4010))
                    .build();

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(
                new HttpTestTarget(
                        "127.0.0.1",
                        4010
                )
        );
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("order 123 exists")
    void orderExists() {

        wireMock.stubFor(
                get(urlEqualTo("/orders/123"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json")
                                        .withBody("""
                                                {
                                                  "id": 123,
                                                  "status": "CONFIRMED",
                                                  "total": 42.0
                                                }
                                                """)
                        )
        );
    }

    @State("inventory available for SKU-9")
    void inventoryAvailable() {

        wireMock.stubFor(
                post(urlEqualTo("/orders"))
                        .withHeader(
                                "Content-Type",
                                matching("application/json.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json")
                                        .withHeader(
                                                "Location",
                                                "/orders/777")
                                        .withBody("""
                                                {
                                                  "id": 777,
                                                  "status": "PENDING"
                                                }
                                                """)
                        )
        );
    }

    @State("order 123 exists and Update order")
    void orderUpdate() {

        wireMock.stubFor(
                put(urlEqualTo("/orders/123/status"))
                        .withHeader(
                                "Content-Type",
                                matching("application/json.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json")
                                        .withBody("""
                                                {
                                                  "id": 123,
                                                  "status": "SHIPPED"
                                                }
                                                """)
                        )
        );
    }

    @State("SKU-9 has stock")
    void skuHasStock() {

        wireMock.stubFor(
                get(urlEqualTo("/inventory/SKU-9"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json")
                                        .withBody("""
                                                {
                                                  "sku": "SKU-9",
                                                  "qty": 5
                                                }
                                                """)
                        )
        );
    }

    @State("order 123 exists and can be cancelled")
    void orderCancel() {

        wireMock.stubFor(
                delete(urlEqualTo("/orders/123"))
                        .willReturn(
                                aResponse()
                                        .withStatus(204)
                        )
        );
    }
}
