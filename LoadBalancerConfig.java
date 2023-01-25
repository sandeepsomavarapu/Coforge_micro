package com.client.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@Component
public class LoadBalancerConfig {
	@Bean
	@Primary
	ServiceInstanceListSupplier serviceInstanceListSupplier() {
		return new DemoServiceInstanceListSuppler("auth-provider");
	}
}

class DemoServiceInstanceListSuppler implements ServiceInstanceListSupplier {
	private final String serviceId;

	DemoServiceInstanceListSuppler(String serviceId) {
		System.out.println("inside constructor");
		this.serviceId = serviceId;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
	
		System.out.println("inside list generation");

		return Flux.just(Arrays.asList(new DefaultServiceInstance(serviceId + "1", serviceId, "localhost", 8090, false),
				new DefaultServiceInstance(serviceId + "2", serviceId, "localhost", 8091, false)));
	}

	@Override
	public String getServiceId() {

		return serviceId;
	}
}
