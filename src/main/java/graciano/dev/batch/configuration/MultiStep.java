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
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.RecordFieldSetMapper;
import org.springframework.batch.item.file.transform.PassThroughFieldExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;

@Configuration
@Profile("steps")
public class MultiStep {
  @Bean
  public Job multiSteJob(JobRepository jobRepository) {
    return new JobBuilder("multiStep", jobRepository)
      .incrementer(new RunIdIncrementer())
      .start(firstStep(null, null))
      .next(secondStep(null, null))
      .build();
  }

  @Bean
  public Step firstStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager) {
    return new StepBuilder("firstStep", jobRepository)
      .<Rental, Rental>chunk(500, transactionManager)
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
      .names("title", "isbn", "user")
      .fieldSetMapper(new RecordFieldSetMapper<>(Rental.class))
      .build();
  }

  @Bean
  public JdbcBatchItemWriter<Rental> firstWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Rental>()
      .dataSource(dataSource)
      .beanMapped()
      .sql("INSERT INTO rental (TITLE, ISBN, USER) VALUES (:title, :isbn, :user)")
      .build();
  }

  @Bean
  public Step secondStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("multiStep2", jobRepository)
      .<Rental, Rental>chunk(2, transactionManager)
      .reader(dbReader(null))
      .writer(step2Writer())
      .build();
  }

  @Bean
  public JdbcCursorItemReader<Rental> dbReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<Rental>()
      .name("dbReader")
      .dataSource(dataSource)
      .sql("SELECT TITLE, ISBN, USER FROM rental WHERE user LIKE 'M%'")
      .rowMapper(new DataClassRowMapper<>(Rental.class))
      .build();
  }

  @Bean(name = "step2Writer")
  public FlatFileItemWriter<Rental> step2Writer() {
    return new FlatFileItemWriterBuilder<Rental>()
      .name("step2Writer")
      .resource(new FileSystemResource(String.format("./multiStep_%s.txt", LocalDateTime.now())))
      .delimited()
      .fieldExtractor(new PassThroughFieldExtractor<>())
      .build();
  }
}


