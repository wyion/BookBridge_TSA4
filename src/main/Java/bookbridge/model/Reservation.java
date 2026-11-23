package bookbridge.model;
import java.time.LocalDate;
public record Reservation(long id,String memberId,String isbn,LocalDate requestedOn,String status) {}
