package dev.graciano.batch.configuration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.sql.DataSource;

@EnableBatchProcessing
@Configuration
@EnableTask
public class SimpleJob {

  @Autowired
  private JobBuilderFactory jobBuilderFactory;

  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job importDataJob() {
    return jobBuilderFactory.get("simpleJob")
      .incrementer(new RunIdIncrementer())
      .start(simpleStep())
      .build();
  }

  @Bean
  public Step simpleStep() {
    return stepBuilderFactory.get("simpleStep")
      .<FieldSet, FieldSet>chunk(1)
      .reader(simpleReader(null))
      .writer(simpleWriter(null))
      .build();
  }

  @StepScope
  @Bean
  public FlatFileItemReader<FieldSet> simpleReader(
    @Value("#{jobParameters['inputFlatFile']}") Resource resource) {
    return new FlatFileItemReaderBuilder<FieldSet>()
      .name("SimpleItemReader")
      .resource(resource)
      .delimited()
      .names("position", "pilot")
      .fieldSetMapper(new PassThroughFieldSetMapper())
      .build();
  }

  @StepScope
  @Bean
  public JdbcBatchItemWriter<FieldSet> simpleWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<FieldSet>()
      .dataSource(dataSource)
      .sql("Insert into race (position, pilot) values (:position, :pilot)")
      .itemSqlParameterSourceProvider(fieldSet ->
        new MapSqlParameterSource()
          .addValue("position", fieldSet.readInt("position"))
          .addValue("pilot", fieldSet.readString("pilot")))
      .columnMapped()
      .build();
  }

//  public static void main(String[] args) {
//    String [] newArgs = new String[] {"inputFlatFile=classpath:/data/race1_results.csv"};
//    SpringApplication.run(SimpleJob.class, newArgs);
//  }
}
