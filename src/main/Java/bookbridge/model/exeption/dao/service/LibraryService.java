package bookbridge.service;
import bookbridge.dao.*;
import bookbridge.model.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
public final class LibraryService {
 private final LibraryDao dao=new LibraryDao();
 private static String required(String value,String label)throws LibraryException{if(value==null||value.isBlank())throw new LibraryException(label+" is required.");return value.trim();}
 private <T> T run(LibraryDao.Work<T> work)throws Exception {return dao.transaction(work);}
 public void addBook(String isbn,String title)throws Exception{
  isbn=required(isbn,"ISBN");title=required(title,"Title");final String i=isbn,t=title;
  run(c->{if(!dao.query(c,"SELECT isbn FROM books WHERE isbn=?",r->r.getString(1),i).isEmpty())throw new LibraryException("Duplicate ISBN: "+i);dao.update(c,"INSERT INTO books(isbn,title,status) VALUES(?,?,'AVAILABLE')",i,t);return null;});
 }
 public void register(String id,String name,Member.Type type)throws Exception{
  id=required(id,"Member ID");name=required(name,"Name");final String i=id,n=name;
  run(c->{if(!dao.query(c,"SELECT id FROM members WHERE id=?",r->r.getString(1),i).isEmpty())throw new LibraryException("Duplicate member ID: "+i);dao.update(c,"INSERT INTO members(id,name,type) VALUES(?,?,?)",i,n,type.name());return null;});
 }
 private Member member(Connection c,String id)throws Exception{
  return dao.query(c,"SELECT id,name,type FROM members WHERE id=?",r->new Member(r.getString(1),r.getString(2),Member.Type.valueOf(r.getString(3))),id).stream().findFirst().orElseThrow(()->new LibraryException("Member not found: "+id));
 }
 private Book book(Connection c,String isbn)throws Exception{
  return dao.query(c,"SELECT isbn,title,status FROM books WHERE isbn=?",r->new Book(r.getString(1),r.getString(2),r.getString(3)),isbn).stream().findFirst().orElseThrow(()->new LibraryException("Book not found: "+isbn));
 }
 public String issue(String memberId,String isbn,int days)throws Exception{
  memberId=required(memberId,"Member ID");isbn=required(isbn,"ISBN");final String m=memberId,i=isbn;
  return run(c->{Member member=member(c,m);Book book=book(c,i);
   if(days<1||days>member.type().maxDays)throw new LibraryException("Allowed loan duration for "+member.type()+": 1-"+member.type().maxDays+" days.");
   long active=dao.query(c,"SELECT id FROM loans WHERE member_id=? AND return_date IS NULL",r->r.getString(1),m).size();
   if(active>=member.type().maxActive)throw new LibraryException("Concurrent loan limit reached: "+member.type().maxActive);
   if(!book.status().equals("AVAILABLE"))throw new LibraryException("Book unavailable: "+i);
   if(!dao.query(c,"SELECT id FROM reservations WHERE isbn=? AND status='PENDING'",r->r.getLong(1),i).isEmpty())throw new LibraryException("Book is reserved for another member; process reservation first.");
   String id=UUID.randomUUID().toString();LocalDate today=LocalDate.now();
   dao.update(c,"INSERT INTO loans(id,member_id,isbn,checkout_date,due_date) VALUES(?,?,?,?,?)",id,m,i,today.toString(),today.plusDays(days).toString());
   dao.update(c,"UPDATE books SET status='CHECKED_OUT' WHERE isbn=? AND status='AVAILABLE'",i);
   return id;
  });
 }
 public void returnBook(String loanId)throws Exception{
  final String id=required(loanId,"Loan ID");
  run(c->{List<String> rows=dao.query(c,"SELECT isbn FROM loans WHERE id=? AND return_date IS NULL",r->r.getString(1),id);
   if(rows.isEmpty())throw new LibraryException("Active loan not found: "+id);
   String isbn=rows.get(0);dao.update(c,"UPDATE loans SET return_date=? WHERE id=? AND return_date IS NULL",LocalDate.now().toString(),id);
   dao.update(c,"UPDATE books SET status='AVAILABLE' WHERE isbn=? AND status='CHECKED_OUT'",isbn);return null;});
 }
 public long reserve(String memberId,String isbn)throws Exception{
  final String m=required(memberId,"Member ID"),i=required(isbn,"ISBN");
  return run(c->{member(c,m);Book b=book(c,i);if(!b.status().equals("CHECKED_OUT"))throw new LibraryException("Only checked-out books can be reserved.");
   if(!dao.query(c,"SELECT id FROM reservations WHERE member_id=? AND isbn=? AND status='PENDING'",r->r.getLong(1),m,i).isEmpty())throw new LibraryException("Duplicate pending reservation.");
   dao.update(c,"INSERT INTO reservations(member_id,isbn,requested_on,status) VALUES(?,?,?,'PENDING')",m,i,LocalDate.now().toString());
   return dao.query(c,"SELECT last_insert_rowid()",r->r.getLong(1)).get(0);
  });
 }
 public void cancel(long reservationId)throws Exception{
  run(c->{dao.update(c,"UPDATE reservations SET status='CANCELLED' WHERE id=? AND status='PENDING'",reservationId);return null;});
 }
 public String processNextReservation(String isbn)throws Exception{
  final String i=required(isbn,"ISBN");
  return run(c->{Book b=book(c,i);if(!b.status().equals("AVAILABLE"))throw new LibraryException("Return the book before processing its reservation.");
   List<Reservation> q=dao.query(c,"SELECT id,member_id,isbn,requested_on,status FROM reservations WHERE isbn=? AND status='PENDING' ORDER BY id LIMIT 1",r->new Reservation(r.getLong(1),r.getString(2),r.getString(3),LocalDate.parse(r.getString(4)),r.getString(5)),i);
   if(q.isEmpty())throw new LibraryException("No pending reservation for "+i);
   Reservation r=q.get(0);Member m=member(c,r.memberId());
   long active=dao.query(c,"SELECT id FROM loans WHERE member_id=? AND return_date IS NULL",rs->rs.getString(1),m.id()).size();
   if(active>=m.type().maxActive)throw new LibraryException("Reserved member reached concurrent loan limit.");
   String id=UUID.randomUUID().toString();LocalDate now=LocalDate.now();
   dao.update(c,"INSERT INTO loans(id,member_id,isbn,checkout_date,due_date) VALUES(?,?,?,?,?)",id,m.id(),i,now.toString(),now.plusDays(m.type().maxDays).toString());
   dao.update(c,"UPDATE books SET status='CHECKED_OUT' WHERE isbn=? AND status='AVAILABLE'",i);
   dao.update(c,"UPDATE reservations SET status='FULFILLED' WHERE id=? AND status='PENDING'",r.id());return id;
  });
 }
 public List<Member> members()throws SQLException{return dao.members();}
 public List<Book> books(String s)throws SQLException{return dao.books(s==null?"":s);}
 public List<Loan> loans()throws SQLException{return dao.loans();}
 public List<Reservation> reservations()throws SQLException{return dao.reservations();}
 public String report()throws SQLException{
  List<Book> books=books("");List<Loan> loans=loans();List<Member> members=members();Map<String,Member> byId=new HashMap<>();for(Member m:members)byId.put(m.id(),m);
  long available=books.stream().filter(b->b.status().equals("AVAILABLE")).count();long active=loans.stream().filter(l->l.returnDate()==null).count();
  EnumMap<Member.Type,Long> counts=new EnumMap<>(Member.Type.class);for(Member.Type t:Member.Type.values())counts.put(t,0L);
  long fines=0;LocalDate now=LocalDate.now();for(Loan l:loans){Member m=byId.get(l.memberId());if(m==null)continue;if(l.returnDate()==null)counts.merge(m.type(),1L,Long::sum);long late=Math.max(0,ChronoUnit.DAYS.between(l.dueDate(),l.returnDate()==null?now:l.returnDate()));fines+=late*m.type().dailyFine;}
  return "Available books: "+available+"\nChecked-out books: "+(books.size()-available)+"\nActive loans: "+active+"\nActive by membership: "+counts+"\nEstimated fines: PHP "+fines;
 }
}
