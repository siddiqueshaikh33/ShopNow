package com.example.demo.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.Dto.CreateOrderRequest;
import com.example.demo.Entity.OrderItems;
import com.example.demo.Entity.Orders;
import com.example.demo.Entity.Status;
import com.example.demo.Entity.Users;
import com.example.demo.Repository.CartItemRepository;
import com.example.demo.Repository.OrderItemsRepository;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.ProductRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final int MAX_RETRIES = 3;

	@Value("${razorpay.key_id}")
	String razorpayKeyId;

	@Value("${razorpay.key_secret}")
	String razorpaySecret;

	private final OrderItemsRepository orderItemsRepository;
	private final OrderRepo orderRepo;
	private final CartItemRepository cartItemRepository;
	private final ProductRepository productRepository;
	private final OrderTransactionService orderTransactionService;

	public OrderService(OrderItemsRepository orderItemsRepository, OrderRepo orderRepo,
			CartItemRepository cartItemRepository, ProductRepository productRepository,
			OrderTransactionService orderTransactionService) {
		super();
		this.orderItemsRepository = orderItemsRepository;
		this.orderRepo = orderRepo;
		this.cartItemRepository = cartItemRepository;
		this.productRepository = productRepository;
		this.orderTransactionService = orderTransactionService;
	}

	protected RazorpayClient createRazorpayClient() throws RazorpayException {
		return new RazorpayClient(razorpayKeyId, razorpaySecret);
	}

	protected boolean verifyPaymentSignature(JSONObject payload, String secret) {
		try {
			return com.razorpay.Utils.verifyPaymentSignature(payload, secret);
		} catch (RazorpayException e) {
			return false;
		}
	}

	public String createOrder(Users user, CreateOrderRequest request) throws RazorpayException {
		RazorpayClient razorpayClient = createRazorpayClient();
		int attempt = 0;

		while (attempt < MAX_RETRIES) {
			attempt++;
			try {
				return orderTransactionService.executeOrderCreation(user, request, razorpayClient);
			} catch (OptimisticLockingFailureException ex) {
				log.warn("⚠️ Optimistic lock conflict on checkout attempt {} of {}: {}", attempt, MAX_RETRIES, ex.getMessage());
				if (attempt >= MAX_RETRIES) {
					log.error("❌ Checkout failed after {} attempts due to concurrent updates", MAX_RETRIES);
					throw ex;
				}
				try {
					Thread.sleep(50L * attempt);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw ex;
				}
			}
		}
		throw new IllegalStateException("Failed to complete order creation after retries");
	}

	public void cancelOrder(String orderId, Users user) {
		Orders order = orderRepo.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		if (order.getUser() == null || order.getUser().getUser_id() != user.getUser_id()) {
			throw new RuntimeException("Unauthorized order access");
		}

		orderTransactionService.cancelAndRestoreStock(orderId);
	}

	@Scheduled(fixedRate = 60000)
	public void cancelExpiredPendingOrders() {
		LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
		List<Orders> expiredOrders = orderRepo.findPendingOrdersBefore(Status.PENDING, cutoffTime);
		for (Orders order : expiredOrders) {
			try {
				orderTransactionService.cancelAndRestoreStock(order.getId());
				log.info("⏰ Auto-cancelled expired pending order {} and restored stock", order.getId());
			} catch (Exception e) {
				log.error("Failed to auto-cancel pending order {}", order.getId(), e);
			}
		}
	}

	@Scheduled(cron = "0 0 0 * * ?")
	@Transactional
	public void handleOrderPendingDeletion() {
		LocalDateTime sevenDayAgoDateTime = LocalDateTime.now().minusDays(7);
		Status status = Status.PENDING;

		// 1. Delete child order items first to avoid foreign key constraint failure
		int deletedItemsCount = orderItemsRepository.deleteByOrdersStatusAndCreatedAtBefore(status, sevenDayAgoDateTime);

		// 2. Safely delete parent pending orders
		int deletedCount = orderRepo.deletePendingStatus(status, sevenDayAgoDateTime);

		log.info("Deleted {} pending orders and {} order items older than 7 days", deletedCount, deletedItemsCount);
	}

	@Transactional
	public boolean verifyOrder(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature, Users user) {
		try {
			JSONObject payload = new JSONObject();
			payload.put("razorpay_payment_id", razorpayPaymentId);
			payload.put("razorpay_order_id", razorpayOrderId);
			payload.put("razorpay_signature", razorpaySignature);

			boolean verifySignature = verifyPaymentSignature(payload, razorpaySecret);

			if (verifySignature) {
				Orders order = orderRepo.findById(razorpayOrderId)
						.orElseThrow(() -> new RuntimeException("Order not found!"));

				if (order.getUser().getUser_id() != user.getUser_id()) {
					throw new RuntimeException("Unauthorized order access");
				}

				order.setStatus(Status.SUCCESS);
				orderRepo.save(order);

				List<OrderItems> orderItems = orderItemsRepository.findByOrders_Id(razorpayOrderId);
				List<Integer> purchasedProductIds = orderItems.stream()
						.map(oi -> oi.getProducts().getProduct_id())
						.distinct()
						.toList();

				if (!purchasedProductIds.isEmpty()) {
					cartItemRepository.deleteCartItemsByUserIdAndProductIds(user.getUser_id(), purchasedProductIds);
				}

				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error("Payment verification error", e);
			return false;
		}
	}
}
