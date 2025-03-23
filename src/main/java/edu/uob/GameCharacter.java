package edu.uob;

public class GameCharacter extends GameEntity{
    private int health;

    GameCharacter(){
        super();
        this.health = -1;
    }

    GameCharacter(String name, String description, String location, int health){
        super(name, description, location);
        this.health = health;
    }
}
