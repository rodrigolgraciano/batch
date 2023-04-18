package graciano.dev.batch.configuration;

import graciano.dev.batch.domain.Rental;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.RecordFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Profile("first")
public class FirstBatchJob {

  @Bean
  public Job firstJob(JobRepository jobRepository) {
    return new JobBuilder("first", jobRepository)
      .incrementer(new RunIdIncrementer())
      .start(firstStep(null, null))
      .build();
  }

  @Bean
  public Step firstStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager) {
    return new StepBuilder("firstStep", jobRepository)
      .<Rental, Rental>chunk(2, transactionManager)
      .reader(firstReader(null))
      .writer(firstWriter(null))
      .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<Rental> firstReader(
    @Value("#{jobParameters['libraryFile']}") Resource resource) {
    return new FlatFileItemReaderBuilder<Rental>()
      .name("firstReader")
      .resource(resource)
      .delimited()
      .names("id", "title", "isbn", "user")
      .fieldSetMapper(new RecordFieldSetMapper<>(Rental.class))
      .build();
  }

  @Bean
  public JdbcBatchItemWriter<Rental> firstWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Rental>()
      .dataSource(dataSource)
      .beanMapped()
      .sql("INSERT INTO rental (ID, TITLE, ISBN, USER) VALUES (:id, :title, :isbn, :user)")
      .build();
  }
}
