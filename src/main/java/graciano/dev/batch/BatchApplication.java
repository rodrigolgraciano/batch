package graciano.dev.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class BatchApplication {

	public static void main(String[] args) {

		List<String> strings = Arrays.asList(args);

		List<String> finalArgs = new ArrayList<>(strings.size() + 1);
		finalArgs.addAll(strings);
		finalArgs.add("libraryFile=lib10.csv");

		SpringApplication.run(BatchApplication.class,
			finalArgs.toArray(new String[0]));
	}
}
