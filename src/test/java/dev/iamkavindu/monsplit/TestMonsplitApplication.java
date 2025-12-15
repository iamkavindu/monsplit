package dev.iamkavindu.monsplit;

import org.springframework.boot.SpringApplication;

public class TestMonsplitApplication {

	public static void main(String[] args) {
		SpringApplication.from(MonsplitApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
