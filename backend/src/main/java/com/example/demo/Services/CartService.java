package com.example.demo.Services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.Dto.AddCartDto;
import com.example.demo.Entity.CartItems;
import com.example.demo.Entity.Products;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.CartItemRepository;
import com.example.demo.Repository.ProductImgRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;

@Service
public class CartService {
    
	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductImgRepository productImgRepository;

	public CartService(UserRepository userRepository, CartItemRepository cartItemRepository, ProductRepository productRepository, ProductImgRepository productImgRepository) {
		this.userRepository = userRepository;
		this.cartItemRepository = cartItemRepository;
		this.productRepository = productRepository;
		this.productImgRepository = productImgRepository;
	}
	
	public int addOrUpdateCartItem(int userId, int productId, int quantity) {
		Users user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Invalid User"));

		Products product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Invalid Product"));

		Optional<CartItems> cartItemOptional = cartItemRepository.findByUserIdAndProductId(userId, productId);

		int currentQty = cartItemOptional.isPresent() ? cartItemOptional.get().getQuantity() : 0;
		int requestedTotal = currentQty + quantity;

		if (product.getStock() != null && requestedTotal > product.getStock()) {
			throw new IllegalArgumentException("Cannot add more than available stock (" + product.getStock() + " available)");
		}

		if (cartItemOptional.isPresent()) {
			cartItemOptional.get().setQuantity(requestedTotal);
			cartItemRepository.save(cartItemOptional.get());
		} else {
			CartItems cartItem = new CartItems(user, product, quantity);
			cartItemRepository.save(cartItem);
		}
		return countCartItem(userId);
	}

	public int addOrUpdateCartItem(AddCartDto addCartDto) {
		Users user = userRepository.findByUsername(addCartDto.getUsername())
				.orElseThrow(() -> new RuntimeException("Invalid User"));
		return addOrUpdateCartItem(user.getUser_id(), addCartDto.getProduct_id(), addCartDto.getQuantity());
	}
	
	public int countCartItem(int userId) {
	   return cartItemRepository.countCartItems(userId);
	}	
	
	public int getTotalCartQuantity(String username) {
	    Users user = userRepository.findByUsername(username)
	        .orElseThrow(() -> new RuntimeException("Invalid User"));

	    return countCartItem(user.getUser_id());
	}
	
	public Map<String, Object> fetchAllCartItemsByUserName(String username) {
		Map<String, Object> responseMap = new HashMap<>();
		
		Users users = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not Found!"));
		List<CartItems> cartItems = cartItemRepository.fetchAllCartItems(users.getUser_id());
		
		List<Integer> allProductId = cartItems.stream().map(ci -> ci.getProduct().getProduct_id()).distinct().toList();
		
		
		Map<Integer, String> allProductImgMap = productImgRepository.findImageByListOfProductId(allProductId)
				.stream()
				.collect(Collectors.toMap(
						pid -> pid.getProducts().getProduct_id(),
						pimg -> pimg.getImageUrl(),
						(existingUrl, duplicateUrl) -> existingUrl
				));
		
		 BigDecimal overallTotal = BigDecimal.ZERO;
		responseMap.put("user", users.getUsername());
		
		List<Map<String, Object>> cartList = new ArrayList<>();
		
		for(CartItems cartItem: cartItems) {
			
			Map<String, Object> cartProductInfo = new HashMap<>();

	        BigDecimal price = cartItem.getProduct().getPrice();
	        BigDecimal quantity = BigDecimal.valueOf(cartItem.getQuantity());
	        BigDecimal totalPrice = price.multiply(quantity);
	        
	        cartProductInfo.put("cart_id", cartItem.getId());
			cartProductInfo.put("product_id", cartItem.getProduct().getProduct_id());
			cartProductInfo.put("product_name", cartItem.getProduct().getName());
			cartProductInfo.put("product_description", cartItem.getProduct().getDescription());
			cartProductInfo.put("product_stock", cartItem.getProduct().getStock());
			cartProductInfo.put("quantity", cartItem.getQuantity());
			cartProductInfo.put("price_per_unit", price);
		    cartProductInfo.put("total_price", totalPrice);
		    cartProductInfo.put("product_img", allProductImgMap.get(cartItem.getProduct().getProduct_id()));
		    overallTotal = overallTotal.add(totalPrice);
		    cartList.add(cartProductInfo);
		    
		}
		responseMap.put("cart_items", cartList);
		responseMap.put("overall_totalprice", overallTotal);
		
		return responseMap;
	}
	
	public void updateCartItem(int userId, int productId, int quantity) {
	        Optional<CartItems> cartItemOptional = cartItemRepository.findByUserIdAndProductId(userId, productId);
	        
	        if(cartItemOptional.isPresent()) {
	        	CartItems cartItem = cartItemOptional.get();
				if (cartItem.getProduct().getStock() != null && quantity > cartItem.getProduct().getStock()) {
					throw new IllegalArgumentException("Cannot exceed available stock (" + cartItem.getProduct().getStock() + " available)");
				}
	        	cartItem.setQuantity(quantity);
	        	cartItemRepository.save(cartItem);
	        }
	}
	
	public void deleteCart(int userId, int productId) {
		Optional<CartItems> cartOptional = cartItemRepository.findByUserIdAndProductId(userId, productId);
		
		if(cartOptional.isPresent()) {
			cartItemRepository.deleteTheCartItem(userId, productId);
		} else {
			throw new RuntimeException("No Product Found in Cart");
		}
	}

}