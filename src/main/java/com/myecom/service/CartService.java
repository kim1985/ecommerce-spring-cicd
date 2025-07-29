package com.myecom.service;

import com.myecom.dto.cart.CartItemRequest;
import com.myecom.dto.cart.CartItemResponse;
import com.myecom.dto.cart.CartResponse;
import com.myecom.model.Cart;
import com.myecom.model.CartItem;
import com.myecom.model.Product;
import com.myecom.model.User;
import com.myecom.repository.CartItemRepository;
import com.myecom.repository.CartRepository;
import com.myecom.repository.ProductRepository;
import com.myecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Aggiunge prodotto al carrello
    public CartResponse addToCart(Long userId, CartItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("Quantità non disponibile");
        }

        Cart cart = findOrCreateCart(user);

        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCartResponse(cart);
    }

    // Rimuove prodotto dal carrello
    public CartResponse removeFromCart(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Carrello non trovato"));

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non nel carrello"));

        cartItemRepository.delete(item);
        return getCartResponse(cart);
    }

    // Recupera carrello utente
    public CartResponse getCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Carrello non trovato"));

        return getCartResponse(cart);
    }

    // Svuota carrello
    public void clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Carrello non trovato"));

        cartItemRepository.deleteByCart(cart);
    }

    // Trova o crea carrello
    private Cart findOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    // Crea carrello vuoto
    private Cart createEmptyCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .build();
        return cartRepository.save(cart);
    }

    // Converte Cart a CartResponse
    private CartResponse getCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCart(cart);

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::convertToItemResponse)
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalAmount(cart.getTotalAmount())
                .totalItems(cart.getTotalItems())
                .updatedAt(cart.getUpdatedAt() != null ?
                        cart.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) :
                        cart.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    // Converte CartItem a CartItemResponse
    private CartItemResponse convertToItemResponse(CartItem item) {
        Product product = item.getProduct();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .productImageUrl(product.getImageUrl())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .productInStock(product.isInStock())
                .build();
    }
}
