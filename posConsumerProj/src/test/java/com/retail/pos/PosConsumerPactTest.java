package com.retail.pos;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "oms-provider", pactVersion = PactSpecVersion.V4)
class PosConsumerPactTest {

    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact getOrder(PactDslWithProvider builder) {
        return builder
                .given("order 123 exists")
                .uponReceiving("a request for order 123")
                .path("/orders/123")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 123)
                        .stringType("status", "CONFIRMED")
                        .numberType("total", 42.0))
                .toPact(V4Pact.class);
    }

    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact createOrder(PactDslWithProvider builder) {
        return builder
                .given("inventory available for SKU-9")
                .uponReceiving("a request to create an order")
                .path("/orders")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("sku", "SKU-9")
                        .integerType("qty", 1))
                .willRespondWith()
                .status(201)
                .matchHeader("Location", "/orders/\\d+", "/orders/777")
                .body(new PactDslJsonBody()
                        .integerType("id", 777)
                        .stringType("status", "PENDING"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrder")
    void getsOrder(MockServer mockServer) {
        given()
                .baseUri(mockServer.getUrl())
                .when()
                .get("/orders/123")
                .then()
                .statusCode(200)
                .body("id", equalTo(123))
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @PactTestFor(pactMethod = "createOrder")
    void createsOrder(MockServer mockServer) {
        given()
                .baseUri(mockServer.getUrl())
                .contentType("application/json")
                .body("""
                        {
                          "sku": "SKU-9",
                          "qty": 1
                        }
                        """)
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("status", equalTo("PENDING"));
    }
}
