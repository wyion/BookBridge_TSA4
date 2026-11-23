package bookbridge.dao;
import java.sql.*;
import java.nio.file.*;
public final class Database {
 private Database(){}
 public static Connection open() throws SQLException {
  String path=System.getProperty("bookbridge.db", "bookbridge.db");
  Connection c=DriverManager.getConnection("jdbc:sqlite:"+path);
  try(Statement s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");}
  return c;
 }
 public static void initialize() throws Exception {
  String sql=Files.readString(Path.of("schema.sql"));
  try(Connection c=open();Statement s=c.createStatement()){
   for(String statement:sql.split(";")){if(!statement.isBlank())s.execute(statement);}
  }
 }
}
