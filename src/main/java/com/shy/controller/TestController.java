package com.shy.controller;

import com.shy.annotation.Controller;
import com.shy.annotation.Qualifier;
import com.shy.annotation.RequestMapping;
import com.shy.service.TestService;

@Controller
@RequestMapping("/test")
public class TestController {

	@Qualifier("testService")
	TestService testService;

	@RequestMapping("/eat")
	public String eatOnControl(){
		String eat = testService.eat();
		System.out.println(eat);
		return "eatOnControl----------------" + "";
	}
}
