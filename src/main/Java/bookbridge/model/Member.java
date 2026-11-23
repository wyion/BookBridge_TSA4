package bookbridge.model;
public record Member(String id,String name,Type type) {
 public enum Type { STUDENT(14,5,2), FACULTY(30,2,5), GUEST(7,10,1);
 public final int maxDays,dailyFine,maxActive;
 Type(int d,int f,int a){maxDays=d;dailyFine=f;maxActive=a;}}
}
