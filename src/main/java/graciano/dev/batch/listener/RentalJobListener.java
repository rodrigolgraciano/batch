package graciano.dev.batch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.stereotype.Component;

@Component
public class RentalJobListener {

  @BeforeJob
  public void beforeJob() {
    System.out.println("Job is about to start...");
  }

  @AfterJob
  public void afterJob(JobExecution execution) {
    System.out.printf("*** Job completed with status %s",
      execution.getExitStatus());
  }
}
