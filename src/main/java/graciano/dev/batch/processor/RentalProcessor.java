package graciano.dev.batch.processor;

import graciano.dev.batch.domain.Rental;
import org.springframework.batch.item.ItemProcessor;


public class RentalProcessor implements ItemProcessor<Rental, Rental> {
  @Override
  public Rental process(Rental item) throws Exception {
    return item.id() % 2 == 0 ? null :
      new Rental(item.id(), item.title(), item.isbn(), item.user().toUpperCase());
  }
}
