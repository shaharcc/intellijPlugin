package main;
public class Main {
    public static void main(String[] args) {
        School Greendale = new School("Greendale");
        Teacher Dean = new Teacher("Craig", 123);
        Student Troy = new Student("Troy", 456);
        Dean.addStudent(Troy);
    }
}
