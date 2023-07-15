package graciano.dev.batch.configuration;

import graciano.dev.batch.domain.Rental;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.RecordFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Profile("multi")
public class MultiFile {

  @Value("classpath:lib10*.csv")
  private Resource[] inputResources;

  @Bean
  public Job multiFileJob(JobRepository jobRepository, Step multiFileStep) {
    return new JobBuilder("multiFileJob", jobRepository)
      .incrementer(new RunIdIncrementer())
      .start(multiFileStep)
      .build();
  }

  @Bean
  public Step multiFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("multiFileStep", jobRepository)
      .<Rental, Rental>chunk(2, transactionManager)
      .reader(multiFileReader())
      .writer(raceDBWriter(null))
      .build();
  }

  @Bean
  public MultiResourceItemReader<Rental> multiFileReader() {
    return new MultiResourceItemReaderBuilder<Rental>()
      .delegate(rentalFileReader())
      .name("multiFileReader")
      .resources(inputResources)
      .build();
  }

  @Bean
  public FlatFileItemReader<Rental> rentalFileReader() {
    return new FlatFileItemReaderBuilder<Rental>()
      .name("simpleItemReader")
      .delimited()
      .names("title", "isbn", "user")
      .fieldSetMapper(new RecordFieldSetMapper<>(Rental.class))
      .build();
  }

  @Bean
  public JdbcBatchItemWriter<Rental> raceDBWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Rental>()
      .sql("INSERT INTO rental (TITLE, ISBN, USER) VALUES (:title, :isbn, :user)")
      .dataSource(dataSource)
      .beanMapped()
      .build();
  }
}
