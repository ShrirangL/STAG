package edu.uob;

public class GameEntity
{
    protected final String name;
    protected final String description;
    protected String location;

    public GameEntity(String name, String description, String location)
    {
        this.name = name;
        this.description = description;
        this.location = location;
    }

    /**
     * Retrieves name of this entity
     * @return name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Retrieves description of this entity
     * @return description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Retrieves current location name of this entity
     * @return location name
     */
    public String getLocation(){
        return location;
    }

    /**
     * Sets location of this entity
     * @param location location name
     */
    public void setLocation(String location){
        this.location = location;
    }
}
