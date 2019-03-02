package Clases;

public class Personas {
    private String personid;
    private String name;

    public Personas() {
    }

    public Personas(String personid, String name) {
        this.personid = personid;
        this.name = name;
    }

    public String getPersonid() {
        return personid;
    }

    public void setPersonid(String personid) {
        this.personid = personid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
