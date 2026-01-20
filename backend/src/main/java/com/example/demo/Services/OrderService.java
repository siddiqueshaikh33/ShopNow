package com.example.demo.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.demo.Entity.CartItems;
import com.example.demo.Entity.OrderItems;
import com.example.demo.Entity.Orders;
import com.example.demo.Entity.Status;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.CartItemRepository;
import com.example.demo.Repository.OrderItemsRepository;
import com.example.demo.Repository.OrderRepo;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

	@Value("${razorpay.key_id}")
	String razorpayKeyId;

	@Value("${razorpay.key_secret}")
	String razorpaySecret;

	OrderItemsRepository orderItemsRepository;

	OrderRepo orderRepo;

	CartItemRepository cartItemRepository;

	public OrderService(OrderItemsRepository orderItemsRepository, OrderRepo orderRepo,
			CartItemRepository cartItemRepository) {
		super();
		this.orderItemsRepository = orderItemsRepository;
		this.orderRepo = orderRepo;
		this.cartItemRepository = cartItemRepository;
	}

	@Transactional
	public String createOrder(Users user, BigDecimal totalPrice, List<OrderItems> cartItems) throws RazorpayException {

		RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpaySecret);

		JSONObject preparePayload = new JSONObject();
		preparePayload.put("amount", totalPrice.multiply(BigDecimal.valueOf(100)).intValue());
		preparePayload.put("currency", "INR");
		preparePayload.put("receipt", "txn_" + System.currentTimeMillis());

		Order razorpayorder = razorpayClient.orders.create(preparePayload);

		Orders order = new Orders();
		order.setId(razorpayorder.get("id"));
		order.setUser(user);
		order.setTotal_amount(totalPrice);
		order.setStatus(Status.PENDING);

		orderRepo.save(order);
		return razorpayorder.get("id");
	}
	
	@Scheduled(cron = "0 0 0 * * ?")	
    public void handleOrderPendingDeletion() {
    	 LocalDateTime sevenDayAgoDateTime = LocalDateTime.now().minusDays(7);
    	 Status status = Status.PENDING;
    	 int deletedCount = orderRepo.deletePendingStatus(status, sevenDayAgoDateTime);
    	 System.out.println("Deleted " + deletedCount + " pending orders older than 7 days");
    }

	@Transactional
	public boolean verifyOrder(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature, Users user) {
		try {
			JSONObject payload = new JSONObject();
			payload.put("razorpay_payment_id", razorpayPaymentId);
			payload.put("razorpay_order_id", razorpayOrderId);
			payload.put("razorpay_signature", razorpaySignature);

			boolean verifySignature;

			verifySignature = com.razorpay.Utils.verifyPaymentSignature(payload, razorpaySecret);

			if (verifySignature) {
				Orders order = orderRepo.findById(razorpayOrderId)
						.orElseThrow(() -> new RuntimeException("Order not found!"));

				if (order.getUser().getUser_id() !=   user.getUser_id()) {
					throw new RuntimeException("Unauthorize order access");
				}
				order.setStatus(Status.SUCCESS);
				orderRepo.save(order);

				List<CartItems> cartList = cartItemRepository.fetchAllCartItems(user.getUser_id());

				List<OrderItems> orderItemsList = cartList.stream().map(item -> {
					OrderItems orderItems = new OrderItems();
					orderItems.setOrders(order);
					orderItems.setProducts(item.getProduct());
					orderItems.setQuantity(item.getQuantity());
					orderItems.setPricePerUnit(item.getProduct().getPrices());

					BigDecimal totalPrice = item.getProduct().getPrices()
							.multiply(BigDecimal.valueOf(item.getQuantity()));
					orderItems.setTotalPrice(totalPrice);

					return orderItems;
				}).toList();

				orderItemsRepository.saveAll(orderItemsList); // ✅ batch save

				cartItemRepository.deleteAll(cartList);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
