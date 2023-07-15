package graciano.dev.batch.domain;

import java.io.Serializable;

public record Rental(String title, String isbn, String user) implements Serializable {
}
