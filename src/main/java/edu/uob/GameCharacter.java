package edu.uob;

public class GameCharacter extends GameEntity{
    GameCharacter(){
        super();
    }
    GameCharacter(String name, String description, String location, int health){
        super(name, description, location);
    }
}
