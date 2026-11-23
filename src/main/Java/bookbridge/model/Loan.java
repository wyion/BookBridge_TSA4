package bookbridge.model;
import java.time.LocalDate;
public record Loan(String id,String memberId,String isbn,LocalDate checkoutDate,LocalDate dueDate,LocalDate returnDate) {
 public String status(LocalDate today){return returnDate!=null?"RETURNED":today.isAfter(dueDate)?"OVERDUE":"ACTIVE";}
}
