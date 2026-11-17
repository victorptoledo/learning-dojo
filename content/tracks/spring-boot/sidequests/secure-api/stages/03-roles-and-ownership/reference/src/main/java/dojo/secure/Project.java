package dojo.secure;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.Set;
import java.util.TreeSet;

/** A project: one owner, any number of members. */
@Entity
public class Project {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String owner;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> members = new TreeSet<>();

    protected Project() {
    }

    public Project(String name, String owner) {
        this.name = name;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public Set<String> getMembers() {
        return members;
    }
}
