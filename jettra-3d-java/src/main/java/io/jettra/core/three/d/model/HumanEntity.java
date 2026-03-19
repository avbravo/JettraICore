package io.jettra.core.three.d.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HumanEntity {
    public float x, y, z;
    public float rotation;
    public float targetX, targetY, targetZ;
    public String name;
    public String action;
    public List<String> history = new ArrayList<>();
    public int r, g, b, a = 255;
    public float animTimer;
    public boolean isSelected;
    public float buildTimer;
    public float energy;
    public float socialNeed;
    public float intelligence;
    public float homeX, homeY, homeZ;
    public boolean hasHome;
    public String currentThought;
    public boolean isWolf;
    public boolean isAnimal;
    public boolean isCar;
    public int entityType;
    public String dream;
    public String goal;
    public String job;
    public String familyID;
    public String partner;
    public float appCooldown;
    public float thoughtTimer;
    public boolean isTraveling;
    public float travelTimer;

    public HumanEntity() {}

    public void addHistory(String msg) {
        history.add(msg);
        if (history.size() > 3) {
            history.remove(0);
        }
    }
}
