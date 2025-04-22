package com.example.git_action;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GitActionApplication {

	public static void check() {
		System.out.println("Hello Saddam. Are you okay? Yes");
	}

	public static void main(String[] args) {
		SpringApplication.run(GitActionApplication.class, args);
		check();
	}

}
