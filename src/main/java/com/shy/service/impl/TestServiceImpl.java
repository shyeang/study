package com.shy.service.impl;

import com.shy.annotation.Service;
import com.shy.service.TestService;

@Service("testService")
public class TestServiceImpl implements TestService {

	public String eat() {
		System.out.println("eat--------" + "testForTestService");
		return "eat";
	}

}
