package main;
import java.util.ArrayList;

class School {
    private String name;
    private ArrayList<Teacher> teachers;

    public School(String name) {
        this.name = name;
        this.teachers = new ArrayList<>();
    }

    public void addTeacher(Teacher teacher) {
        teachers.add(teacher);
    }

    public ArrayList<Teacher> getTeachers() {
        return teachers;
    }

    public String getName() {
        return name;
    }
}
