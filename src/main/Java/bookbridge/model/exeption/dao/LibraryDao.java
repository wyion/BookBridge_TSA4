package bookbridge.dao;
import bookbridge.model.*;
import java.sql.*;
import java.time.*;
import java.util.*;
public final class LibraryDao {
 public interface Work<T>{T run(Connection c)throws Exception;}
 public <T> T transaction(Work<T> work)throws Exception{
  try(Connection c=Database.open()){
   c.setAutoCommit(false);
   try {T result=work.run(c);c.commit();return result;}
   catch(Exception e){c.rollback();throw e;}
  }
 }
 public int update(Connection c,String sql,Object... values)throws SQLException{
  try(PreparedStatement p=c.prepareStatement(sql)){
   bind(p,values);int n=p.executeUpdate();if(n!=1)throw new SQLException("Expected one affected row, got "+n);return n;
  }
 }
 private void bind(PreparedStatement p,Object... values)throws SQLException{
  for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);
 }
 public interface Mapper<T>{T map(ResultSet r)throws SQLException;}
 public <T> List<T> query(Connection c,String sql,Mapper<T> mapper,Object... values)throws SQLException{
  List<T> rows=new ArrayList<>();
  try(PreparedStatement p=c.prepareStatement(sql)){
   bind(p,values);try(ResultSet r=p.executeQuery()){while(r.next())rows.add(mapper.map(r));}
  }
  return rows;
 }
 public <T> List<T> read(String sql,Mapper<T> mapper,Object... values)throws SQLException{
  try(Connection c=Database.open()){return query(c,sql,mapper,values);}
 }
 public List<Member> members()throws SQLException{return read("SELECT id,name,type FROM members ORDER BY id",r->new Member(r.getString(1),r.getString(2),Member.Type.valueOf(r.getString(3))));}
 public List<Book> books(String search)throws SQLException{return read("SELECT isbn,title,status FROM books WHERE isbn LIKE ? OR title LIKE ? ORDER BY title",r->new Book(r.getString(1),r.getString(2),r.getString(3)),"%"+search+"%","%"+search+"%");}
 public List<Loan> loans()throws SQLException{return read("SELECT id,member_id,isbn,checkout_date,due_date,return_date FROM loans ORDER BY id",r->new Loan(r.getString(1),r.getString(2),r.getString(3),LocalDate.parse(r.getString(4)),LocalDate.parse(r.getString(5)),r.getString(6)==null?null:LocalDate.parse(r.getString(6))));}
 public List<Reservation> reservations()throws SQLException{return read("SELECT id,member_id,isbn,requested_on,status FROM reservations ORDER BY id",r->new Reservation(r.getLong(1),r.getString(2),r.getString(3),LocalDate.parse(r.getString(4)),r.getString(5)));}
}
