package course.examples.nt118.model;

import java.util.List;

public class Location {
    private String type;
    private List<Double> coordinates;
    private String name;

    public Location() {}

    public String getType() {
        return type;
    }

    public List<Double> getCoordinates() {
        return coordinates;
    }

    public String getName() {
        return name;
    }
}