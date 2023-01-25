package com.client.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.client.models.ApplicationUser;
import com.client.models.Cart;
import com.client.models.CartDTO;
import com.client.models.Order;
import com.client.models.OrderItem;
import com.client.models.Product;
import com.client.models.ProductDTO;

@Controller
@LoadBalancerClient(name="MyloadBalancer", configuration=LoadBalancerConfig.class)
public class ClientController {

	@Autowired
	private RestTemplate restTemplate;

	
	@GetMapping("/")
	public String home() {
		return "home";
	}
	
	@PostMapping("/userlogin")
	public String userLogin(@RequestParam("username") String username,@RequestParam("password") String password , Model model,HttpSession session) {
		System.out.println(username);
		System.out.println(password);
		ApplicationUser user=new ApplicationUser();
		
		user.setUsername(username);
		user.setPassword(password);
		int status;
		try {
		ResponseEntity<ApplicationUser> res=restTemplate.postForEntity("http://MyloadBalancer/authservice/auth", user, ApplicationUser.class);
		ApplicationUser userObj=(ApplicationUser) res.getBody();
		System.out.println(userObj);
		session.setAttribute("userId", userObj.getUserId());
		status=res.getStatusCodeValue();
		System.out.println(res.getStatusCodeValue());
		return "redirect:/products";
		}
		catch(Exception e)
		{
			status=400;
			model.addAttribute("message","UserName and Password invalid"+e);		
			
			return "home";
		}	
	}
	
	@GetMapping("/products")
	public String getproducts(HttpSession session) {
		ProductDTO dto=restTemplate.getForObject("http://localhost:8083/productservice/products",ProductDTO.class);
		//model.addAttribute("ProductList",dto.getList());
		session.setAttribute("ProductList", dto.getList());
		return "productList";
	}
	
	@PostMapping("/addToCart/{productId}")
	public String addToCart(@PathVariable("productId") int productId,HttpSession session) {
		int  userId=0;
		userId=(int) session.getAttribute("userId");
		if(userId==0) {
			return "SessionExpired";
		}
		List<Product> list=(List<Product>) session.getAttribute("ProductList");
		for(Product p:list) {
			if(p.getProductId()==productId) {
			Cart cart=new Cart();
			cart.setUserid(userId);
			cart.setProductId(productId);
			cart.setProductname(p.getProductName());
			cart.setProductPrice(p.getProductPrice());
			cart.setQuantity(1);
			cart.setSumTotal(p.getProductPrice());
			ResponseEntity<Object> res=restTemplate.postForEntity("http://localhost:8084/cartservice/addItem",cart,Object.class);
			System.out.println(res.getStatusCodeValue());
			}
		}
		return "redirect:/products";
	}
	
	@GetMapping("/viewCart")
	public String getCartItems(HttpSession session) {
		CartDTO dto= new CartDTO();
		try {
		dto=restTemplate.getForObject("http://localhost:8084/cartservice/getCartItems",CartDTO.class);
		
		List<Cart> list=dto.getList();
		System.out.println(list);
		session.setAttribute("cartItems", list);
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
			return "Cart";
		}
		return "Cart";
	}
	
	@GetMapping("/deleteFromCart/{id}")
	public String deleteFromCart(@PathVariable("id") int id) {
		restTemplate.delete("http://localhost:8084/cartservice/delete/"+id);
		return"redirect:/viewCart";
	}
	
	@GetMapping("/placeOrder")
	public String placeOrder(HttpSession session) {
		List<Cart> cartList=(List<Cart>) session.getAttribute("cartItems");
		List<OrderItem> items=new ArrayList();
		int grandTotal=0;
		for(Cart c:cartList) {
			OrderItem o=new OrderItem();
			//o.setOrderId(0);
			o.setProductid(c.getProductId());
			o.setProductName(c.getProductname());
			o.setProductPrice(c.getProductPrice());
			o.setQuantity(c.getQuantity());
			o.setSubTotal(c.getSumTotal());
			grandTotal+=c.getSumTotal();
			items.add(o);
		
		}
		session.setAttribute("orderItems",items);
		session.setAttribute("grandTotal",grandTotal);
		
		return "PlaceOrder";
	}
	
	@PostMapping("/confirmOrder")
	public String confirmOrder(@RequestParam("address") String address,@RequestParam("mode") String mode, HttpSession session) {
		
		long millis=System.currentTimeMillis();  
        java.sql.Date date=new java.sql.Date(millis);  
        if(session.getAttribute("userId")==null) {
        	return "SessionExpired";
        }
		
		Order order=new Order();
		order.setDeliveryAddress(address);
		order.setPaymentMode(mode);
		order.setOrderAmount((int) session.getAttribute("grandTotal"));
		order.setUserid((int) session.getAttribute("userId"));
		order.setOrderItems((List<OrderItem>) session.getAttribute("orderItems"));
		order.setOrderDate(date);
		System.out.println(order.getOrderItems());
		ResponseEntity<Object> res=restTemplate.postForEntity("http://localhost:8085/orderservice/placeOrder", order, Object.class);
		System.out.println(res.getStatusCodeValue());
		restTemplate.delete("http://localhost:8084/cartservice/deleteAll");
		
		
		List<Cart> cartList=null;
		session.setAttribute("cartItems", cartList);
		
		return "Thanks";
	}
	
}
