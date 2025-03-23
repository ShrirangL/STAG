package edu.uob;

public class GameEntity
{
    protected final String name;
    protected final String description;
    protected String location;

    public GameEntity(){
        this.location = "";
        this.name = "";
        this.description = "";
    }

    public GameEntity(String name, String description, String location)
    {
        this.name = name;
        this.description = description;
        this.location = location;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getLocation(){
        return location;
    }

    public void setLocation(String location){
        this.location = location;
    }
}
