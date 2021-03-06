package pub.gusten.gbgcommuter.models;

import java.util.Objects;

import pub.gusten.gbgcommuter.models.departures.Departure;

public class SelectableLine {
    public boolean isSelected;
    public final Line line;

    public SelectableLine(String line, String bgColor, String fgColor){
        this.line = new Line(line, bgColor, fgColor);
        isSelected = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectableLine otherLine = (SelectableLine) o;
        return Objects.equals(line, otherLine.line);
    }

    public final static SelectableLine selectableLineFromDeparture(Departure departure) {
        return new SelectableLine(departure.getLine(), departure.getBgColor(), departure.getFgColor());
    }
}
