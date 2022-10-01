package dev.graciano.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BatchApplication {

	public static void main(String[] args) {
		String [] newArgs = new String[] {"inputFlatFile=classpath:/data/race1_results.csv"};
		SpringApplication.run(BatchApplication.class, newArgs);
	}

}
