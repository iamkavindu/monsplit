package dev.iamkavindu.monsplit;

import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MonsplitApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(MonsplitApplication.class, args);
    }
}
