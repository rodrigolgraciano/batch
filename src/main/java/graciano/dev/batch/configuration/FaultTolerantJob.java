package graciano.dev.batch.configuration;

import graciano.dev.batch.domain.Rental;
import graciano.dev.batch.processor.RentalProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.RecordFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Profile("fault")
public class FaultTolerantJob {

    private static final Logger log = LoggerFactory.getLogger(FaultTolerantJob.class);

    @Bean
    public Job ftJob(JobRepository jobRepository) {
        return new JobBuilder("ft", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(ftStep(null, null))
                .build();
    }

    @Bean
    public Step nonFtStep(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager) {
        return new StepBuilder("ft", jobRepository)
                .<Rental, Rental>chunk(2, transactionManager)
                .reader(ftReader())
                .writer(ftWriter(null))
                .build();
    }


    @Bean
    public Step ftStep(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("ft", jobRepository)
                .<Rental, Rental>chunk(10, transactionManager)
                .reader(ftReader())
                .processor(new RentalProcessor())
                .writer(ftWriter(null))
                .faultTolerant()
                .noRollback(Exception.class)
                .skip(Exception.class).skipLimit(5)
                .retry(Exception.class).retryLimit(2)
                .listener(new SkipListener<Rental, Rental>() {
                    @Override
                    public void onSkipInRead(Throwable t) {
                        log.warn("Skipped - Error reading: {}", t.getMessage());
                    }

                    @Override
                    public void onSkipInWrite(Rental rental, Throwable t) {
                        log.warn("Skipped - Error writing: {}, cause: {}", rental, t.getMessage());
                    }

                    @Override
                    public void onSkipInProcess(Rental rental, Throwable t) {
                        log.warn("Skipped - Error processing: {} , cause: {}", rental, t.getMessage());
                    }
                })
                .listener(new ChunkListener() {
                    @Override
                    public void afterChunkError(ChunkContext context) {
                        log.warn(context.toString());
                    }
                })
                .listener(new ItemWriteListener<Rental>() {
                    @Override
                    public void onWriteError(Exception exception, Chunk<? extends Rental> items) {
                        log.warn("Error while persisting: " + items.size());
                    }

                    @Override
                    public void beforeWrite(Chunk<? extends Rental> items) {
                        log.info("Will persist: " + items.size() + " items");
                    }

                    @Override
                    public void afterWrite(Chunk<? extends Rental> items) {
                        log.info("Persisted " + items.size() + " items");
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Rental> ftReader() {
        return new FlatFileItemReaderBuilder<Rental>()
                .name("ftReader")
                .resource(new ClassPathResource("libft.csv"))
                .delimited()
                .names("title", "isbn", "user")
                .fieldSetMapper(new RecordFieldSetMapper<>(Rental.class))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Rental> ftWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Rental>()
                .dataSource(dataSource)
                .beanMapped()
                .sql("INSERT INTO rental (TITLE, ISBN, USER) VALUES (:title, :isbn, :user)")
                .build();
    }
}
