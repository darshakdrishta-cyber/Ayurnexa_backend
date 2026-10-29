package com.Ayurnexa.Ayurnexa.model.cart;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "carts")
public class Cart {
    @Id private String id;
    @Indexed(unique = true) private String userId;   // one active cart per user
    private List<CartItem> items = new ArrayList<>();
    @Version private Long version;
    @LastModifiedDate private Instant updatedAt;
}
