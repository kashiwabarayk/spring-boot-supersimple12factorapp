package com.example.pcfsample;

import java.time.OffsetDateTime;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableCaching
@EnableRedisHttpSession
public class PcfsampleappApplication {
	private static final Logger logger = LoggerFactory.getLogger(PcfsampleappApplication.class);

	@Autowired
	Greeter greeter;

	@RequestMapping("/")
	String home() {
		return greeter.hello();
		// return "hello";
	}

	@RequestMapping("/kill")
	String kill() {
		System.exit(-1);
		return "Killed";
	}

	@RequestMapping("/put")
	String putSession(HttpSession session) {
		session.setAttribute("username", "tkaburagi");
		return "Generated a session";
	}

	@RequestMapping("/get")
	String getSession(HttpSession session) {
		return session.getAttribute("username").toString();
	}

	@RequestMapping("/remove")
	String removeSession(HttpSession session) {
		session.invalidate();
		return "Removed sessions";
	}

	@RequestMapping("/logger")
	String logger() {
		logger.info("Called Logger");
		logger.error("Error Logger!!");
		return "Output logs";
	}

	@RequestMapping("/bgdemo")
	String bgdemo() {
		return "Hello myapp. This is version 1. Java Version is " + System.getProperty("java.version");
	}

	public static void main(String[] args) {
		SpringApplication.run(PcfsampleappApplication.class, args);
	}
}

@Component
class Greeter {
	@Cacheable("hello") // 実行結果をキャッシュします
	public String hello() {
		return "Hello. It's " + OffsetDateTime.now() + " now.";
	}
}
