package main;
import java.util.ArrayList;

class Teacher {
    private String name;
    private int id;
    private ArrayList<Student> students;

    public Teacher(String name, int id) {
        this.name = name;
        this.id = id;
        this.students = new ArrayList<>();
    }

    public void addStudent(Student student) {
        students.add(student);
    }
    public ArrayList<Student> getStudents() {
        return students;
    }
    public String getName() {
        return name;
    }
    public int getId() {
        return id;
    }
}
