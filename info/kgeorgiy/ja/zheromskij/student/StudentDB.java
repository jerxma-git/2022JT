package info.kgeorgiy.ja.zheromskij.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    private static final Comparator<Student> ORDER_BY_NAME = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparing(Student::getId);

    private static String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private <T> List<T> toList(Stream<T> stream) {
        return stream.collect(Collectors.toList());
    }

    private <T> Stream<T> mappedStream(Collection<Student> students, Function<Student, T> mapper) {
        return students.stream().map(mapper);
    }

    private <T> List<T> mappedList(Collection<Student> students, Function<Student, T> mapper) {
        return toList(mappedStream(students, mapper));
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mappedList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mappedList(students, Student::getLastName);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mappedList(students, StudentDB::getFullName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mappedList(students, Student::getGroup);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mappedStream(students, Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.naturalOrder())
                .map(Student::getFirstName)
                .orElse("");
    }


    private Stream<Student> sortStudentsByName(Stream<Student> students) {
        return students.sorted(ORDER_BY_NAME);
    }


    private <T> Stream<Student> filterByField(Collection<Student> students, Function<Student, T> getter, T target) {
        return students.stream().filter(s -> getter.apply(s).equals(target));
    }

    private List<Student> listByName(Stream<Student> students) {
        return toList(sortStudentsByName(students));
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return toList(students.stream().sorted(Comparator.comparing(Student::getId)));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return listByName(students.stream());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return listByName(filterByField(students, Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return listByName(filterByField(students, Student::getLastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return listByName(filterByField(students, Student::getGroup, group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }
}