package io.jettra.core.three.d;

import java.util.List;

import static com.raylib.Raylib.*;
import com.raylib.Color;
import com.raylib.Vector3;
import com.raylib.Vector2;
import com.raylib.Rectangle;
import com.raylib.Camera3D;

import io.jettra.core.three.d.model.Artifact;
import io.jettra.core.three.d.model.HumanEntity;
import io.jettra.core.three.d.model.MegaProject;
import io.jettra.core.three.d.model.Thought;
import io.jettra.core.three.d.model.WorldEvent;
import static com.raylib.Raylib.ConfigFlags.*;
import static com.raylib.Raylib.KeyboardKey.*;
import static com.raylib.Raylib.MouseButton.*;
import static com.raylib.Raylib.CameraProjection.*;
import static com.raylib.Raylib.CameraMode.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Jettra3DApp {
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 720;

    private Camera3D camera;
    private List<Artifact> artifacts = new CopyOnWriteArrayList<>();
    private List<HumanEntity> entities = new CopyOnWriteArrayList<>();
    private List<MegaProject> megaProjects = new CopyOnWriteArrayList<>();
    private List<Thought> thoughts = new CopyOnWriteArrayList<>();
    private List<WorldEvent> worldEvents = new CopyOnWriteArrayList<>();

    private float worldTime = 0;
    private int selectedAgentIndex = -1;
    private int selectedArtifactIndex = -1;
    private boolean followMode = false;
    private boolean directorMode = false;
    private float directorTimer = 0;
    private float planeScale = 1.0f;
    private boolean isAnchored = true;
    private boolean cameraLocked = false;
    private boolean showHelp = true;
    private boolean showConfigModal = false;
    private boolean voiceEnabled = true;

    // Config Toggles
    private boolean configEnabledApps = true;
    private boolean configEnabledInternet = true;
    private boolean configEnabledFiles = true;
    private boolean configEnabledLife = true;
    private boolean configEnabledSocial = true;
    
    // Chat & Knowledge UI
    private boolean showChat = false;
    private String chatInput = "";
    private List<String> chatHistory = new java.util.ArrayList<>();
    private List<KnowledgeEntry> allKnowledge = new java.util.ArrayList<>();
    private List<KnowledgeEntry> topKnowledge = new java.util.ArrayList<>();
    private float topKnowledgeUpdateTimer = 0;

    private float howlTimer = 0;
    private float globalSaveTimer = 60.0f; // Save state every 60 seconds
    private float timeScale = 1.0f;
    private int weatherMode = 0; // 0: Sunny, 1: Night, 2: Storm
    private boolean sfxEnabled = true;

    public void run() {
        setConfigFlags(FLAG_WINDOW_RESIZABLE);
        initWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "Jettra 3D Core - Java 25 Native");
        maximizeWindow();
        setTargetFPS(60);

        initAudioDevice();
        initSfx();

        initCamera();
        initPopulation();

        while (!windowShouldClose()) {
            float dt = getFrameTime();
            float scaledDt = dt * timeScale;
            worldTime += scaledDt;

            handleInput();
            update(scaledDt);
            draw();
        }

        closeAudioDevice();
        closeWindow();
    }

    private void initCamera() {
        camera = new Camera3D();
        camera.position(new Vector3().x(35).y(35).z(35));
        camera.target(new Vector3().x(0).y(0).z(0));
        camera.up(new Vector3().x(0).y(1).z(0));
        camera.fovy(45);
        camera.projection(CAMERA_PERSPECTIVE);
    }
    private void handleInput() {
        if (showConfigModal) return;

        if (isKeyPressed(KEY_TAB)) showHelp = !showHelp;
        if (isKeyPressed(KEY_C)) {
            initCamera();
            followMode = false;
            cameraLocked = false;
        }
        if (isKeyPressed(KEY_F)) {
            followMode = !followMode;
            if (followMode && selectedAgentIndex == -1 && !entities.isEmpty()) {
                selectedAgentIndex = 0;
            }
        }
        if (isKeyPressed(KEY_L)) isAnchored = !isAnchored;
        
        // Chat Toggle
        if (isKeyPressed(KEY_ENTER)) {
            if (showChat && !chatInput.trim().isEmpty()) {
                sendChatMessage(chatInput);
                chatInput = "";
            } else {
                showChat = !showChat;
            }
        }

        if (showChat) {
            int key = getCharPressed();
            while (key > 0) {
                if ((key >= 32) && (key <= 125)) {
                    chatInput += (char)key;
                }
                key = getCharPressed();
            }
            if (isKeyPressed(KEY_BACKSPACE) && !chatInput.isEmpty()) {
                chatInput = chatInput.substring(0, chatInput.length() - 1);
            }
        }

        float wheel = getMouseWheelMove();
        if (wheel != 0) {
            camera.fovy(camera.fovy() - wheel * 2);
            if (camera.fovy() < 5) camera.fovy(5);
            if (camera.fovy() > 120) camera.fovy(120);
        }
    }

    private void update(float dt) {
        if (followMode && selectedAgentIndex != -1 && selectedAgentIndex < entities.size()) {
            HumanEntity e = entities.get(selectedAgentIndex);
            camera.target(new Vector3().x(e.x).y(e.y).z(e.z));
            camera.position(new Vector3().x(e.x + 10).y(e.y + 10).z(e.z + 10));
        } else if (!cameraLocked && !isAnchored) {
            updateCamera(camera, CAMERA_FREE);
        }

        updateEntities(dt);
        updateThoughts(dt);
        
        topKnowledgeUpdateTimer += dt;
        if (topKnowledgeUpdateTimer > 2.0f) {
            updateTopKnowledge();
            topKnowledgeUpdateTimer = 0;
        }

        if (directorMode) {
            updateDirectorMode(dt);
        }

        updateSfx();
        
        if (howlTimer > 0) howlTimer -= dt;
        checkPackSafety(dt);

        globalSaveTimer -= dt;
        if (globalSaveTimer <= 0) {
            saveWorldState();
            globalSaveTimer = 60.0f;
        }
    }

    private void checkPackSafety(float dt) {
        if (weatherMode == 2 && howlTimer <= 0) {
            long total = 0;
            long sheltered = 0;
            HumanEntity jettra = null;
            
            for (HumanEntity e : entities) {
                if (e.name.contains("Jettra")) { jettra = e; continue; }
                if (e.isCar || e.isAnimal) continue;
                total++;
                if ("SHELTERED".equals(e.action)) sheltered++;
            }
            
            if (total > 0 && sheltered == total && jettra != null) {
                // Reward agents for successful survival tactics
                for (HumanEntity re : entities) {
                    if ("SHELTERED".equals(re.action) && !re.name.contains("Jettra")) {
                        re.intelligence += 2;
                        re.currentThought = "He aprendido a sobrevivir (+2 Intel)";
                        re.thoughtTimer = 4.0f;
                        final HumanEntity fRe = re;
                        Thought t = new Thought();
                        t.content = "LEARNED: SURVIVAL (+2 Intel)";
                        t.owner = fRe; t.x = fRe.x; t.y = fRe.y + 3.0f; t.z = fRe.z;
                        t.timer = 4.0f; t.isThought = false;
                        thoughts.add(t);
                    }
                }

                final String msg = "¡AWOOOOOOOOOO! La manada está a salvo de la tormenta.";
                final HumanEntity fJettra = jettra;
                jettra.currentThought = msg;
                jettra.thoughtTimer = 6.0f;
                Thought t = new Thought();
                t.content = msg; t.owner = fJettra;
                t.x = fJettra.x; t.y = fJettra.y + 4.0f; t.z = fJettra.z;
                t.timer = 6.0f; t.isThought = false;
                thoughts.add(t);
                worldEvents.add(new WorldEvent("Aullido de Jettra: Supervivencia Exitosa", worldTime, 255, 255, 0));
                howlTimer = 40.0f; // Relax for 40s
            }
        }
    }

    private void updateEntities(float dt) {
        boolean isNight = (weatherMode == 1);
        
        for (HumanEntity e : entities) {
            if (e.isDead && !e.name.contains("Jettra")) continue;

            // --- PHYSICAL (PECS) ---
            if (!e.isCar) {
                float baseRate = e.metabolismRate * dt;
                
                // Aging: 1 year per ~15 mins real time (approx 1/900 year per second)
                if (worldTime % 1.0 < dt) {
                    e.age = (int)(20 + (worldTime / 600.0f)); // Simple aging for demo
                }

                // Metabolism
                e.hunger -= baseRate * (isNight ? 0.3f : 0.6f);
                e.thirst -= baseRate * (isNight ? 0.4f : 0.8f);
                e.energy -= baseRate * (e.action.equals("IDLE") ? 0.2f : 1.0f);
                
                // Disease Spread
                if (e.infectionLevel > 0) {
                    e.infectionLevel += baseRate * 2.0f;
                    e.health -= baseRate * 0.5f;
                    if (e.health < 20 && e.thoughtTimer <= 0) {
                        e.currentThought = "Me siento muy enfermo...";
                        e.thoughtTimer = 5.0f;
                    }
                }

                // Personality Impact on Emotional State (Neuroticism)
                float moodDecay = baseRate * (0.1f + e.neuroticism * 0.5f);
                e.mood -= moodDecay;

                // Recovery
                if (e.action.equals("SHELTERED") || e.action.equals("RESTING")) {
                    e.energy = Math.min(100, e.energy + baseRate * 5.0f);
                    e.stamina = Math.min(100, e.stamina + baseRate * 3.0f);
                    if (isNight) e.mood = Math.min(100, e.mood + baseRate);
                }

                // Health checks
                if (e.hunger < 10 || e.thirst < 10 || e.energy < 5) {
                    e.health -= baseRate * 1.5f;
                }
                
                if (e.health <= 0 && !e.name.contains("Jettra")) {
                    e.isDead = true;
                    e.action = "DEAD";
                    worldEvents.add(new WorldEvent(e.name + " ha fallecido por causas naturales", worldTime, 200, 0, 0));
                    continue;
                }
            }

            // --- COGNITIVE (PECS) - Goal Selection ---
            if (!e.isCar && !e.isDead) {
                if (e.hunger < 40) e.currentGoal = "BUSCAR_COMIDA";
                else if (e.thirst < 40) e.currentGoal = "BUSCAR_AGUA";
                else if (e.energy < 30 || isNight) e.currentGoal = "BUSCAR_REFUGIO";
                else if (e.mood < 40) e.currentGoal = "SOCIALIZAR";
                else e.currentGoal = "EXPLORAR";
            }

            // --- MOVEMENT & ENVIRONMENTAL REACTION ---
            float envSpeed = isNight ? 0.5f : 1.0f;
            if (weatherMode == 2) envSpeed *= 0.7f;

            float dx = e.targetX - e.x;
            float dz = e.targetZ - e.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);

            if (dist > 0.1f) {
                float speed = (e.isCar ? 8.0f : 2.0f) * envSpeed;
                e.x += (dx / dist) * speed * dt;
                e.z += (dz / dist) * speed * dt;
                e.rotation = (float)Math.atan2(dx, dz) * (180.0f / (float)Math.PI);
            } else {
                // Goal-specific target selection
                if (Math.random() < 0.02) {
                    if (e.currentGoal.equals("BUSCAR_REFUGIO") || e.currentGoal.equals("BUSCAR_COMIDA")) {
                        // Find nearest house/school
                        Artifact best = null; float mDS = Float.MAX_VALUE;
                        for(Artifact a : artifacts) {
                            float d = (a.x-e.x)*(a.x-e.x)+(a.z-e.z)*(a.z-e.z);
                            if (d < mDS) { mDS = d; best = a; }
                        }
                        if (best != null) { e.targetX = best.x; e.targetZ = best.z; }
                    } else {
                        e.targetX = (float)(Math.random()*100-50);
                        e.targetZ = (float)(Math.random()*100-50);
                    }
                }
                if (dist < 1.0f && e.currentGoal.equals("BUSCAR_REFUGIO")) {
                    e.action = "SHELTERED";
                }
            }

            // --- SOCIAL (PECS) & LEADERSHIP ---
            if (!e.isCar && !e.isDead && worldTime % 4.0 < dt) {
                for (HumanEntity other : entities) {
                    if (other == e || other.isDead) continue;
                    float d2 = (other.x-e.x)*(other.x-e.x) + (other.z-e.z)*(other.z-e.z);
                    if (d2 < 25.0f) { // Interaction range
                        // Disease spread
                        if (e.infectionLevel > 30 && Math.random() < 0.1) other.infectionLevel = 1;

                        // Social interaction
                        if (e.currentGoal.equals("SOCIALIZAR")) {
                            e.mood = Math.min(100, e.mood + 5.0f * (1.0f + e.extraversion));
                            if (e.thoughtTimer <= 0 && Math.random() < 0.2) {
                                e.currentThought = "Hablando con " + other.name;
                                e.thoughtTimer = 3.0f;
                            }
                        }

                        // Leadership influence (Jettra Wolf)
                        if (e.name.contains("Jettra") && Math.random() < 0.1) {
                            other.intelligence = Math.min(1.0f, other.intelligence + 0.01f);
                            other.mood = Math.min(100, other.mood + 10.0f);
                        }
                    }
                }
            }

            // --- PERSISTENCE & LEARNING ---
            if (!e.isCar && !e.isDead && Math.random() < 0.0005) {
                saveKnowledge(e, "Observación Directa", "Evolución de la Especie");
            }
        }
    }

    private void updateThoughts(float dt) {
        entities.removeIf(e -> {
            if (e.thoughtTimer > 0) {
                e.thoughtTimer -= dt;
            }
            return false;
        });

        thoughts.removeIf(t -> {
            t.timer -= dt;
            return t.timer <= 0;
        });
    }

    private void updateDirectorMode(float dt) {
        directorTimer += dt;
        if (directorTimer > 8.0f || selectedAgentIndex == -1) {
            directorTimer = 0;
            for (int i = 0; i < entities.size(); i++) {
                HumanEntity e = entities.get(i);
                if ("Building".equals(e.action) || "Socializing".equals(e.action)) {
                    selectedAgentIndex = i;
                    followMode = true;
                    break;
                }
            }
        }
    }

    private void draw() {
        beginDrawing();
        
        Color skyColor = switch(weatherMode) {
            case 1 -> new Color().r((byte)2).g((byte)2).b((byte)5).a((byte)255); // Night
            case 2 -> new Color().r((byte)30).g((byte)35).b((byte)45).a((byte)255); // Storm
            default -> new Color().r((byte)5).g((byte)5).b((byte)12).a((byte)255); // Default
        };
        clearBackground(skyColor);

        beginMode3D(camera);
        drawGrid((int)(50 * planeScale), 1.0f);
        drawPlane(new Vector3().x(0).y(-0.05f).z(0), new Vector2().x(100 * planeScale).y(100 * planeScale), 
                  weatherMode == 2 ? DARKGRAY : new Color().r((byte)15).g((byte)15).b((byte)30).a((byte)255));

        drawArtifacts();
        drawEntities();
        drawThoughts();
        drawKnowledgeBase();

        if (weatherMode == 2) { // Draw Rain
            for(int j=0; j<100; j++) {
                float rx = (float)(Math.random()*80-40);
                float rz = (float)(Math.random()*80-40);
                float ry = (float)(Math.random()*20);
                drawLine3D(new Vector3().x(rx).y(ry).z(rz), new Vector3().x(rx).y(ry-0.5f).z(rz), SKYBLUE);
            }
        }

        endMode3D();

        drawUI();

        if (showHelp) drawHelpOverlay();
        if (showConfigModal) drawConfigModal();
        if (showChat) drawChatWindow();
        
        drawTopKnowledgePanel();
        drawJettraStatusTooltip();

        endDrawing();
    }

    private void drawArtifacts() {
        for (Artifact a : artifacts) {
            Vector3 pos = new Vector3().x(a.x).y(a.y).z(a.z);
            Color color = new Color().r((byte)a.r).g((byte)a.g).b((byte)a.b).a((byte)a.a);
            
            switch (a.type) {
                case 0 -> { // House
                    drawCube(pos, 2, 2, 2, color);
                    drawCubeWires(pos, 2, 2, 2, BLACK);
                    drawCylinder(new Vector3().x(pos.x()).y(pos.y() + 1).z(pos.z()), 0, 1.5f, 2, 4, MAROON);
                }
                case 1 -> { // School
                    drawCube(pos, 4, 3, 4, color);
                    drawCubeWires(pos, 4, 3, 4, BLACK);
                    drawCube(new Vector3().x(pos.x()).y(pos.y() + 2).z(pos.z()), 1, 2, 1, DARKGRAY);
                }
                case 2 -> { // City Center
                    drawCube(pos, 5, 2, 5, color);
                    drawSphere(new Vector3().x(pos.x()).y(pos.y() + 2).z(pos.z()), 1.5f, GOLD);
                }
                case 3 -> { // Radio
                    drawCylinder(pos, 0.5f, 0.5f, 5, 8, DARKGRAY);
                    if (( (int)(worldTime * 2) % 2) == 0) {
                        drawSphere(new Vector3().x(pos.x()).y(pos.y() + 5).z(pos.z()), 0.3f, RED);
                    }
                }
            }
        }
        
        for (MegaProject p : megaProjects) {
            if (p.finished) continue;
            Vector3 pos = new Vector3().x(p.x).y(p.y).z(p.z);
            drawCubeWires(pos, 2.1f, 2.1f, 2.1f, YELLOW);
            drawCube(pos, 2.0f, 2.0f * p.progress, 2.0f, fade(SKYBLUE, 0.5f));
        }
    }

    private void drawEntities() {
        int i = 0;
        for (HumanEntity e : entities) {
            Vector3 pos = new Vector3().x(e.x).y(e.y).z(e.z);
            Color color = new Color().r((byte)e.r).g((byte)e.g).b((byte)e.b).a((byte)e.a);

            if (e.isWolf) {
                drawWolf(pos, e.rotation, color);
            } else if (e.isCar) {
                drawCube(pos, 3.0f, 1.0f, 1.5f, color);
                drawCubeWires(pos, 3.0f, 1.0f, 1.5f, BLACK);
            } else {
                // Human Shape
                drawCapsule(new Vector3().x(pos.x()).y(pos.y()).z(pos.z()), 
                           new Vector3().x(pos.x()).y(pos.y() + 1.8f).z(pos.z()), 0.4f, 8, 8, color);
                drawSphere(new Vector3().x(pos.x()).y(pos.y() + 2.0f).z(pos.z()), 0.5f, color);
            }

            if (selectedAgentIndex == i) {
                drawCircle3D(new Vector3().x(pos.x()).y(pos.y() + 0.01f).z(pos.z()), 1.5f, new Vector3().x(1).y(0).z(0), 90, LIME);
            }

            // Name Tag
            Vector2 screenPos = getWorldToScreen(new Vector3().x(pos.x()).y(pos.y() + 2.5f).z(pos.z()), camera);
            drawText(e.name, (int)screenPos.x() - measureText(e.name, 10)/2, (int)screenPos.y(), 10, RAYWHITE);
            i++;
        }
    }

    private void drawThoughts() {
        // Render thoughts from the global list
        for (Thought t : thoughts) {
            drawThoughtBubble(t.content, t.owner, t.x, t.y, t.z, t.timer, t.isThought);
        }
        
        // Render current thoughts from entities if not already in list
        for (HumanEntity e : entities) {
            if (e.thoughtTimer > 0 && e.currentThought != null && !e.currentThought.isEmpty()) {
                // Check if this entity already has a thought in the global list to avoid overlaps
                boolean alreadyShown = false;
                for (Thought t : thoughts) {
                    if (t.owner == e) { alreadyShown = true; break; }
                }
                if (!alreadyShown) {
                    drawThoughtBubble(e.currentThought, e, e.x, e.y + (e.isWolf ? 1.5f : 3.0f), e.z, e.thoughtTimer, true);
                }
            }
        }
    }

    private void drawThoughtBubble(String content, HumanEntity owner, float x, float y, float z, float timer, boolean isThought) {
        Vector3 worldPos = (owner != null) ? new Vector3().x(owner.x).y(owner.y + (owner.isWolf ? 1.5f : 3.0f)).z(owner.z) 
                                           : new Vector3().x(x).y(y).z(z);
        Vector2 screenPos = getWorldToScreen(worldPos, camera);
        
        if (screenPos.x() > 0 && screenPos.x() < getScreenWidth() && screenPos.y() > 0 && screenPos.y() < getScreenHeight()) {
            boolean isJettra = owner != null && owner.name.contains("Jettra");
            String label = isThought ? "[Pensando...]" : "[Hablando]";
            int fontSize = isJettra ? 18 : 16;
            int textW = measureText(content, fontSize);
            
            float alpha = Math.min(1.0f, timer); // Fade out
            Color bubbleColor = isJettra ? fade(GOLD, 0.8f * alpha) : (isThought ? fade(PURPLE, 0.7f * alpha) : fade(DARKBLUE, 0.8f * alpha));
            Color textColor = isJettra ? fade(BLACK, alpha) : fade(RAYWHITE, alpha);
            Color labelColor = isJettra ? fade(MAROON, alpha) : fade(GOLD, alpha);
            Color borderColor = isJettra ? fade(WHITE, 0.9f * alpha) : fade(RAYWHITE, 0.3f * alpha);

            // Bubble popup background
            drawRectangleRounded(new Rectangle().x(screenPos.x() - textW/2.0f - 10).y(screenPos.y() - 60).width(textW + 20).height(45), 0.4f, 8, bubbleColor);
            drawRectangleRoundedLines(new Rectangle().x(screenPos.x() - textW/2.0f - 10).y(screenPos.y() - 60).width(textW + 20).height(45), 0.4f, 8, borderColor);
            
            // Triangle pointer
            drawTriangle(new Vector2().x(screenPos.x()).y(screenPos.y() - 15),
                         new Vector2().x(screenPos.x() - 10).y(screenPos.y() - 25),
                         new Vector2().x(screenPos.x() + 10).y(screenPos.y() - 25), bubbleColor);

            drawText(label, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 55, 10, labelColor);
            drawText(content, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 40, fontSize, textColor);
            if (owner != null && owner.currentGoal != null) {
                drawText("Meta: " + owner.currentGoal, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 28, 9, GOLD);
            }
        }
    }

    private void drawUI() {
        int sw = getScreenWidth();
        int sh = getScreenHeight();

        // Right Sidebar
        drawRectangle(sw - 200, 0, 200, sh, fade(DARKGRAY, 0.9f));
        drawLine(sw - 200, 0, sw - 200, sh, RAYWHITE);

        drawText("JETTRA CORE", sw - 190, 20, 20, GOLD);
        drawText("JAVA 25 EDITION", sw - 190, 45, 10, SKYBLUE);

        // Buttons
        if (guiButton(sw - 190, 80, 180, 30, "RESET WORLD", RED)) {
            entities.clear();
            artifacts.clear();
            megaProjects.clear();
            worldEvents.add(new WorldEvent("Mundo reiniciado", worldTime, 255, 0, 0));
        }

        if (guiButton(sw - 190, 120, 180, 30, "SAVE STATE", LIME)) {
            saveWorldState();
        }

        if (guiButton(sw - 190, 160, 180, 30, "CONFIGURACIÓN", BLUE)) {
            showConfigModal = !showConfigModal;
            if (showConfigModal) {
                initCamera();
                followMode = false;
                cameraLocked = true;
            } else {
                cameraLocked = false;
            }
        }

        if (guiButton(sw - 190, 200, 180, 30, directorMode ? "DIRECTOR: ON" : "DIRECTOR: OFF", directorMode ? ORANGE : GRAY)) {
            directorMode = !directorMode;
            if (directorMode) followMode = true;
        }

        // --- NEW BUTTONS ---
        if (guiButton(sw - 190, 240, 180, 25, isAnchored ? "PLANE: LOCKED" : "PLANE: UNLOCKED", isAnchored ? RED : GRAY)) {
            isAnchored = !isAnchored;
        }

        if (guiButton(sw - 190, 270, 180, 25, "CENTER MAP", DARKGRAY)) {
            initCamera();
            followMode = false;
            cameraLocked = false;
        }

        if (guiButton(sw - 190, 305, 180, 30, showChat ? "CERRAR CHAT" : "ABRIR CHAT", PURPLE)) {
            showChat = !showChat;
        }

        if (guiButton(sw - 190, 345, 85, 25, "ZOOM +", GRAY)) {
            camera.fovy(Math.max(5, camera.fovy() - 5));
        }

        if (guiButton(sw - 100, 345, 85, 25, "ZOOM -", GRAY)) {
            camera.fovy(Math.min(120, camera.fovy() + 5));
        }

        if (guiButton(sw - 190, 330, 180, 25, voiceEnabled ? "VOICE: ON" : "VOICE: OFF", voiceEnabled ? LIME : RED)) {
            voiceEnabled = !voiceEnabled;
            worldEvents.add(new WorldEvent("Voz " + (voiceEnabled ? "activada" : "desactivada"), worldTime, 200, 200, 0));
        }

        if (guiButton(sw - 190, 365, 85, 30, sfxEnabled ? "SFX: ON" : "SFX: OFF", sfxEnabled ? LIME : RED)) {
            sfxEnabled = !sfxEnabled;
            worldEvents.add(new WorldEvent("Efectos " + (sfxEnabled ? "activados" : "desactivados"), worldTime, 100, 255, 100));
        }

        if (guiButton(sw - 100, 365, 85, 30, "SALIR", DARKGRAY)) {
            closeWindow();
            System.exit(0);
        }

        // Selected Info
        int infoY = 410;
        if (selectedAgentIndex != -1 && selectedAgentIndex < entities.size()) {
            HumanEntity e = entities.get(selectedAgentIndex);
            drawText("AGENT: " + e.name, sw - 190, infoY, 15, RAYWHITE);
            drawText("GOAL: " + e.currentGoal, sw - 190, infoY + 20, 10, GOLD);
            drawText("MOOD: " + (int)e.mood + "% (" + (e.mood > 50 ? "Feliz" : "Estresado") + ")", sw - 190, infoY + 35, 10, fade(PURPLE, 0.8f));
            drawText("HEALTH: " + (int)e.health + "%", sw - 190, infoY + 50, 10, fade(RED, 0.8f));
            drawText("HUNGER: " + (int)e.hunger + "%", sw - 190, infoY + 65, 10, fade(ORANGE, 0.8f));
            drawText("THIRST: " + (int)e.thirst + "%", sw - 190, infoY + 80, 10, fade(SKYBLUE, 0.8f));
            
            // Personality (Big Five)
            int py = infoY + 105;
            drawText("PERSONALIDAD:", sw - 190, py, 11, SKYBLUE);
            drawText("- Openness: " + String.format("%.2f", e.openness), sw - 180, py + 15, 9, RAYWHITE);
            drawText("- Conscien: " + String.format("%.2f", e.conscientiousness), sw - 180, py + 27, 9, RAYWHITE);
            drawText("- Extraver: " + String.format("%.2f", e.extraversion), sw - 180, py + 39, 9, RAYWHITE);
            drawText("- Agreeabl: " + String.format("%.2f", e.agreeableness), sw - 180, py + 51, 9, RAYWHITE);
            drawText("- Neurotic: " + String.format("%.2f", e.neuroticism), sw - 180, py + 63, 9, RAYWHITE);

            if (!e.spouse.isEmpty()) {
                drawText("SPOUSE: " + e.spouse, sw - 190, py + 80, 10, PINK);
            }
            if (e.infectionLevel > 0) {
                drawText("INFECTION: " + (int)e.infectionLevel + "%", sw - 190, py + 95, 10, LIME);
            }
        }

        // Live Feed
        drawLiveFeed(sw, sh);
        // History Log
        drawHistoryLog(sw, sh);
    }

    private boolean guiButton(int x, int y, int w, int h, String text, Color color) {
        Vector2 mouse = getMousePosition();
        boolean hover = checkCollisionPointRec(mouse, new Rectangle().x(x).y(y).width(w).height(h));
        
        drawRectangle(x, y, w, h, hover ? fade(color, 0.8f) : fade(color, 0.5f));
        drawRectangleLines(x, y, w, h, RAYWHITE);
        
        int fontSize = 15;
        int tw = measureText(text, fontSize);
        drawText(text, x + (w - tw)/2, y + (h - fontSize)/2, fontSize, RAYWHITE);
        
        return hover && isMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }

    private void drawLiveFeed(int sw, int sh) {
        int fy = sh - 150;
        drawRectangle(sw - 350, fy, 140, 140, fade(BLACK, 0.6f));
        drawText("LIVE FEED", sw - 340, fy + 5, 12, GOLD);
        int j = 0;
        for (int i = worldEvents.size() - 1; i >= 0 && j < 8; i--, j++) {
            WorldEvent ev = worldEvents.get(i);
            drawText("> " + ev.message, sw - 340, fy + 25 + (j * 14), 10, new Color().r((byte)ev.r).g((byte)ev.g).b((byte)ev.b).a((byte)ev.a));
        }
    }

    private void drawHistoryLog(int sw, int sh) {
        int w = 350; int h = 200;
        int x = 10; int y = sh - h - 10;
        drawRectangle(x, y, w, h, fade(BLACK, 0.5f));
        drawRectangleLines(x, y, w, h, fade(SKYBLUE, 0.5f));
        drawText("LOG DE EVENTOS DEL SISTEMA", x + 10, y + 10, 12, GOLD);
        
        int j = 0;
        int maxLines = 12;
        for (int i = worldEvents.size() - 1; i >= 0 && j < maxLines; i--, j++) {
            WorldEvent ev = worldEvents.get(i);
            String timeStr = String.format("%.1fs", ev.timestamp);
            drawText("[" + timeStr + "] " + ev.message, x + 15, y + 30 + (j * 14), 10, new Color().r((byte)ev.r).g((byte)ev.g).b((byte)ev.b).a((byte)ev.a));
        }
    }

    private void drawHelpOverlay() {
        drawRectangle(10, 10, 260, 200, fade(BLACK, 0.6f));
        drawRectangleLines(10, 10, 260, 200, fade(GOLD, 0.5f));
        drawText("CONTROLES DE CÁMARA", 20, 20, 12, GOLD);
        drawText("- Teclas: W,S,A,D,Q,E", 25, 40, 10, RAYWHITE);
        drawText("- Mouse: Click Derecho Girar", 25, 55, 10, RAYWHITE);
        drawText("- Rueda Mouse: Zoom +/-", 25, 70, 10, RAYWHITE);
        drawText("- C: Cambiar Cámara / Reset", 25, 85, 10, RAYWHITE);
        drawText("- F: Modo Seguir Agente", 25, 100, 10, RAYWHITE);
        drawText("- L: Lock/Unlock Plano", 25, 115, 10, RAYWHITE);
        drawText("- Esc: Cerrar App", 25, 130, 10, RAYWHITE);
        drawText("- Tab: Toggle esta Ayuda", 25, 145, 10, RAYWHITE);
        drawText("- Enter: Abrir/Cerrar Chat", 25, 160, 10, LIME);
    }

    private void drawConfigModal() {
        int sw = getScreenWidth();
        int sh = getScreenHeight();
        drawRectangle(0, 0, sw, sh, fade(BLACK, 0.4f));
        
        int mw = 400; int mh = 350;
        int mx = (sw - mw) / 2;
        int my = (sh - mh) / 2;
        
        drawRectangle(mx, my, mw, mh, DARKGRAY);
        drawRectangleLines(mx, my, mw, mh, RAYWHITE);
        drawText("CONFIGURACIÓN DE AUTONOMÍA", mx + 20, my + 20, 20, GOLD);

        configEnabledApps = guiToggle(mx + 20, my + 60, "Auto-abrir Aplicaciones (VLC/Chrome)", configEnabledApps);
        configEnabledInternet = guiToggle(mx + 20, my + 90, "Búsqueda de Aprendizaje en Internet", configEnabledInternet);
        configEnabledFiles = guiToggle(mx + 20, my + 120, "Estudio de Archivos Locales (PDF/MD)", configEnabledFiles);
        configEnabledLife = guiToggle(mx + 20, my + 150, "Población Silvestre (Animales/Autos)", configEnabledLife);
        configEnabledSocial = guiToggle(mx + 20, my + 180, "Simulación Social Avanzada", configEnabledSocial);

        if (guiButton(mx + mw - 120, my + mh - 50, 100, 30, "CERRAR", GRAY)) {
            showConfigModal = false;
        }
    }

    private boolean guiToggle(int x, int y, String text, boolean value) {
        if (guiButton(x, y, 20, 20, value ? "X" : " ", value ? LIME : GRAY)) {
            value = !value;
        }
        drawText(text, x + 30, y + 5, 12, RAYWHITE);
        return value;
    }
    
    private void initPopulation() {
        if (loadWorldState()) {
            worldEvents.add(new WorldEvent("Sistema Restaurado desde /memory/world/", worldTime, 0, 255, 100));
            return;
        }

        // Create initial agents
        if (entities.stream().noneMatch(e -> "Jettra Wolf".equals(e.name))) {
            generateEntity("Jettra Wolf", 0, 0, 0, true, false, false); // El lobo Jettra como líder
        }
        generateEntity("Wolf-Prime", 10, 0, 10, true, false, false);
        generateEntity("Human-Alpha", 15, 0, 0, false, false, false);
        generateEntity("Civic-01", -15, 0, -15, false, false, true);
        
        for (int i = 0; i < 6; i++) {
            generateEntity("Agent-" + (10 + i), (float)(Math.random()*60-30), 0, (float)(Math.random()*60-30), false, false, false);
        }
        worldEvents.add(new WorldEvent("Sistema Iniciado. Población generada.", worldTime, 200, 200, 255));
    }

    private boolean loadWorldState() {
        try {
            java.io.File file = new java.io.File("memory/world/world_state.json");
            if (file.exists()) {
                WorldState state = mapper.readValue(file, WorldState.class);
                entities.clear();
                entities.addAll(state.entities);
                artifacts.clear();
                artifacts.addAll(state.artifacts);
                megaProjects.clear();
                megaProjects.addAll(state.megaProjects);
                worldTime = state.worldTime;
                return true;
            }
        } catch (Exception ex) {
            System.err.println("Error loading world state: " + ex.getMessage());
        }
        return false;
    }

    private void drawKnowledgeBase() {
        for (KnowledgeEntry k : allKnowledge) {
            Vector3 pos = new Vector3().x(k.x).y(k.y + 0.5f).z(k.z);
            // Floating glowing sphere for knowledge
            float pulse = (float)Math.sin(worldTime * 2.0f) * 0.1f;
            drawSphere(pos, 0.2f + pulse, fade(GOLD, 0.6f));
            drawSphereWires(pos, 0.25f + pulse, 8, 8, fade(SKYBLUE, 0.4f));
        }
    }

    private void drawWolf(Vector3 pos, float rotDeg, Color color) {
        // Wolf Model - Native Shapes
        float x = pos.x(); float y = pos.y(); float z = pos.z();
        float rotRad = rotDeg * ( (float)Math.PI / 180.0f);
        
        // Body (Capsule/Cylinder)
        drawCapsule(new Vector3().x(x).y(y + 0.5f).z(z), 
                   new Vector3().x(x + (float)Math.sin(rotRad)*1.2f).y(y + 0.6f).z(z + (float)Math.cos(rotRad)*1.2f), 
                   0.5f, 8, 8, DARKGRAY);

        // Legs (4 cylinders)
        float legOff = 0.3f;
        drawCylinder(new Vector3().x(x - legOff).y(y).z(z - legOff), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);
        drawCylinder(new Vector3().x(x + legOff).y(y).z(z - legOff), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);
        drawCylinder(new Vector3().x(x - legOff + (float)Math.sin(rotRad)).y(y).z(z - legOff + (float)Math.cos(rotRad)), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);
        drawCylinder(new Vector3().x(x + legOff + (float)Math.sin(rotRad)).y(y).z(z + legOff + (float)Math.cos(rotRad)), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);

        // Head (Sphere + Snout)
        float headX = x + (float)Math.sin(rotRad)*1.6f;
        float headZ = z + (float)Math.cos(rotRad)*1.6f;
        drawSphere(new Vector3().x(headX).y(y + 1.2f).z(headZ), 0.45f, DARKGRAY);
        drawCylinder(new Vector3().x(headX + (float)Math.sin(rotRad)*0.3f).y(y + 1.1f).z(headZ + (float)Math.cos(rotRad)*0.3f), 
                     0.2f, 0.1f, 0.4f, 6, BLACK); // Snout
        
        // Ears (Triangles represent ears)
        drawSphere(new Vector3().x(headX).y(y + 1.6f).z(headZ + 0.15f), 0.15f, LIGHTGRAY); // Ear L
        drawSphere(new Vector3().x(headX).y(y + 1.6f).z(headZ - 0.15f), 0.15f, LIGHTGRAY); // Ear R

        // Tail
        drawCapsule(new Vector3().x(x).y(y + 0.6f).z(z), 
                   new Vector3().x(x - (float)Math.sin(rotRad)*0.8f).y(y + 0.8f).z(z - (float)Math.cos(rotRad)*0.8f), 
                   0.15f, 4, 4, DARKGRAY);
    }

    private void generateEntity(String name, float x, float y, float z, boolean isWolf, boolean isAnimal, boolean isCar) {
        HumanEntity e = new HumanEntity();
        e.name = name;
        e.x = x; e.y = y; e.z = z;
        e.targetX = x; e.targetY = y; e.targetZ = z;
        e.isWolf = isWolf; e.isAnimal = isAnimal; e.isCar = isCar;
        e.energy = 100;
        e.action = "IDLE";
        e.r = (int)(Math.random()*255); e.g = (int)(Math.random()*255); e.b = (int)(Math.random()*255);
        entities.add(e);
    }

    private static class KnowledgeEntry {
        public long timestamp;
        public String agent;
        public String source;
        public String topic;
        public float x, y, z;
        public double confidence;
        public String context;
        public String[] keywords;
    }

    private com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private void saveKnowledge(HumanEntity agent, String source, String topic) {
        try {
            java.io.File dir = new java.io.File("memory/world");
            if (!dir.exists()) dir.mkdirs();
            
            KnowledgeEntry entry = new KnowledgeEntry();
            entry.timestamp = System.currentTimeMillis();
            entry.agent = agent.name;
            entry.source = source;
            entry.topic = topic;
            entry.x = agent.x; entry.y = agent.y; entry.z = agent.z;
            entry.confidence = 0.7 + (Math.random() * 0.3); // High confidence
            entry.context = agent.action;
            entry.keywords = new String[]{topic.toLowerCase().replace(" ", "-"), "jettra-learning", source.toLowerCase()};

            java.io.File file = new java.io.File("memory/world/knowledge.json");
            java.util.List<KnowledgeEntry> list;
            if (file.exists()) {
                list = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<KnowledgeEntry>>() {});
            } else {
                list = new java.util.ArrayList<>();
            }

            // --- KNOWLEDGE FUSION LOGIC ---
            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                KnowledgeEntry existing = list.get(i);
                if (existing.topic.equalsIgnoreCase(entry.topic)) {
                    found = true;
                    // Keep the one with highest confidence
                    if (entry.confidence > existing.confidence) {
                        list.set(i, entry);
                    }
                    break;
                }
            }
            if (!found) {
                list.add(entry);
            }
            // ------------------------------
            allKnowledge = list;
            updateTopKnowledge();

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, list);

        } catch (java.io.IOException ex) {
            System.err.println("Error guardando conocimiento JSON: " + ex.getMessage());
        }
    }

    private void updateTopKnowledge() {
        if (allKnowledge == null || allKnowledge.isEmpty()) {
            java.io.File file = new java.io.File("memory/world/knowledge.json");
            if (file.exists()) {
                try {
                    allKnowledge = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<KnowledgeEntry>>() {});
                } catch (Exception e) {}
            }
        }
        
        if (allKnowledge != null) {
            allKnowledge.sort((a, b) -> Double.compare(b.confidence, a.confidence));
            topKnowledge = allKnowledge.stream().limit(5).collect(java.util.stream.Collectors.toList());
        }
    }

    private void drawTopKnowledgePanel() {
        int x = SCREEN_WIDTH/2 - 200;
        int y = 10;
        drawRectangleRounded(new Rectangle().x(x).y(y).width(400).height(110), 0.2f, 8, fade(DARKGRAY, 0.8f));
        drawRectangleRoundedLines(new Rectangle().x(x).y(y).width(400).height(110), 0.2f, 8, fade(GOLD, 0.5f));
        drawText("TOP 5 CONOCIMIENTO CRÍTICO (MAX CONFIDENCE)", x + 10, y + 10, 10, GOLD);
        
        for (int i = 0; i < topKnowledge.size(); i++) {
            KnowledgeEntry k = topKnowledge.get(i);
            String txt = (i+1) + ". " + k.topic + " (" + String.format("%.2f", k.confidence) + ")";
            drawText(txt, x + 15, y + 30 + (i * 15), 11, RAYWHITE);
        }
    }

    private void drawChatWindow() {
        int w = 400; int h = 250;
        int x = 280; int y = SCREEN_HEIGHT - h - 10;
        drawRectangleRounded(new Rectangle().x(x).y(y).width(w).height(h), 0.1f, 8, fade(BLACK, 0.8f));
        drawRectangleRoundedLines(new Rectangle().x(x).y(y).width(w).height(h), 0.1f, 8, SKYBLUE);
        drawText("CHAT CON JETTRA WOLF (EL LÍDER)", x + 15, y + 15, 14, SKYBLUE);
        
        // History
        for (int i = 0; i < chatHistory.size(); i++) {
            if (i > 8) break;
            drawText("> " + chatHistory.get(chatHistory.size() - 1 - i), x + 15, y + h - 60 - (i * 18), 12, RAYWHITE);
        }

        // Input Box
        drawRectangle(x + 10, y + h - 35, w - 20, 25, DARKGRAY);
        drawRectangleLines(x + 10, y + h - 35, w - 20, 25, RAYWHITE);
        drawText(chatInput + "_", x + 20, y + h - 28, 12, LIME);
        drawText("[ENTER] para enviar", x + 150, y + 20, 10, GRAY);
    }

    private void sendChatMessage(String msg) {
        chatHistory.add("Tú: " + msg);
        // Find Jettra
        for (HumanEntity e : entities) {
            if (e.name.contains("Jettra")) {
                String res = "Soy el Lobo Jettra. Tu mensaje ha sido recibido.";
                String lowerMsg = msg.toLowerCase();
                
                if (lowerMsg.contains("hola")) res = "Saludos, mortal. El conocimiento fluye.";
                if (lowerMsg.contains("construye")) res = "¡Mis agentes ya están en ello!";
                if (lowerMsg.contains("quien eres")) res = "Soy el guía de este mundo 3D.";

                // Knowledge Search
                if (res.startsWith("Soy el Lobo Jettra")) {
                    try {
                        java.io.File file = new java.io.File("memory/world/knowledge.json");
                        if (file.exists()) {
                            List<KnowledgeEntry> all = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<KnowledgeEntry>>() {});
                            for (KnowledgeEntry k : all) {
                                if (lowerMsg.contains(k.topic.toLowerCase()) || 
                                   (k.keywords != null && java.util.Arrays.stream(k.keywords).anyMatch(lowerMsg::contains))) {
                                    res = "Mis agentes han aprendido sobre " + k.topic + " desde " + k.source + ".";
                                    break;
                                }
                            }
                            if (res.startsWith("Soy el Lobo Jettra") && (lowerMsg.contains("aprendido") || lowerMsg.contains("sabes"))) {
                                res = "Estamos aprendiendo muchas cosas: ciencia, redes neuronales y arquitectura.";
                            }
                        }
                    } catch (Exception ex) {}
                }

                // Physical Commands
                if (msg.toLowerCase().contains("teletransportar")) {
                    res = "Realizando teletransporte cuántico al centro del mapa.";
                    for (HumanEntity target : entities) {
                        if (!target.name.contains("Jettra")) {
                            target.x = 0; target.z = 0; target.targetX = 0; target.targetZ = 0;
                        }
                    }
                    worldEvents.add(new WorldEvent("Teletransporte masivo activado por Jettra", worldTime, 255, 255, 0));
                }
                if (msg.toLowerCase().contains("poblacion")) {
                    res = "Invocando nuevos agentes al mundo...";
                    for(int i=0; i<5; i++) {
                        generateEntity("Sentry-" + (int)(Math.random()*1000), (float)(Math.random()*20-10), 0, (float)(Math.random()*20-10), false, false, false);
                    }
                }
                if (msg.toLowerCase().contains("limpiar")) {
                    res = "Limpiando todos los proyectos y artefactos obsoletos.";
                    artifacts.clear();
                    megaProjects.clear();
                    worldEvents.add(new WorldEvent("Mundo purificado por el líder", worldTime, 100, 100, 255));
                }
                if (msg.toLowerCase().contains("acelerar")) {
                    timeScale = (timeScale == 1.0f) ? 5.0f : 1.0f;
                    res = "Escala de tiempo ajustada a " + timeScale + "x.";
                    worldEvents.add(new WorldEvent("Manipulación temporal: " + res, worldTime, 0, 255, 200));
                }
                if (msg.toLowerCase().contains("clima")) {
                    weatherMode = (weatherMode + 1) % 3;
                    String[] m = {"Soleado", "Nocturno", "Tormentoso"};
                    res = "Cambiando entorno a modo " + m[weatherMode] + ".";
                    worldEvents.add(new WorldEvent("Cambio climático: " + res, worldTime, 200, 0, 255));
                }
                if (msg.toLowerCase().contains("estado") || msg.toLowerCase().contains("reporte")) {
                    int vivos = 0; int muertos = 0;
                    for (HumanEntity a : entities) {
                        if (a.isDead) muertos++; else vivos++;
                    }
                    res = "Estado: " + vivos + " vivos, " + muertos + " fallecidos. " + megaProjects.size() + " proyectos.";
                }
                
                final String finalRes = res;
                chatHistory.add("Jettra: " + finalRes);
                e.currentThought = finalRes;
                e.thoughtTimer = 5.0f;
                Thought t = new Thought();
                t.content = finalRes; t.owner = e; t.x = e.x; t.y = e.y + 3.5f; t.z = e.z;
                t.timer = 5.0f; t.isThought = false;
                thoughts.add(t);
                break;
            }
        }
        if (chatHistory.size() > 20) chatHistory.remove(0);
    }

    private void initSfx() {
        // Here we could loadSound("rain.wav") or generate procedural waves.
        // For simplicity and to avoid crashes with missing files or JNA-Wave complex buffers, 
        // we'll use a simulation with world events that emit a 'play' signal.
    }

    private void updateSfx() {
        if (!sfxEnabled) return;
        // In reality, this would play/stop a loop based on weatherMode.
    }

    private static class WorldState {
        public List<HumanEntity> entities;
        public List<Artifact> artifacts;
        public List<MegaProject> megaProjects;
        public float worldTime;
    }

    private void saveWorldState() {
        try {
            java.io.File dir = new java.io.File("memory/world");
            if (!dir.exists()) dir.mkdirs();

            WorldState state = new WorldState();
            state.entities = new java.util.ArrayList<>(entities);
            state.artifacts = new java.util.ArrayList<>(artifacts);
            state.megaProjects = new java.util.ArrayList<>(megaProjects);
            state.worldTime = worldTime;

            java.io.File file = new java.io.File("memory/world/world_state.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, state);
            worldEvents.add(new WorldEvent("Mundo guardado automáticamente", worldTime, 0, 255, 100));
        } catch (Exception ex) {
            System.err.println("Error saving world state: " + ex.getMessage());
            worldEvents.add(new WorldEvent("Error guardando mundo", worldTime, 255, 0, 0));
        }
    }

    private void drawJettraStatusTooltip() {
        HumanEntity jettra = null;
        for (HumanEntity e : entities) {
            if (e.name.contains("Jettra")) { jettra = e; break; }
        }
        if (jettra == null) return;

        int x = 10;
        int y = 200;
        int w = 280;
        int h = 100;

        drawRectangleRounded(new Rectangle().x(x).y(y).width(w).height(h), 0.2f, 8, fade(DARKGRAY, 0.9f));
        drawRectangleRoundedLines(new Rectangle().x(x).y(y).width(w).height(h), 0.2f, 8, GOLD);
        
        drawText("VOZ DEL LÍDER: " + jettra.name.toUpperCase(), x + 10, y + 10, 12, GOLD);
        drawText("ACCIÓN: " + jettra.action, x + 10, y + 30, 10, RAYWHITE);
        drawText("Población Total: " + entities.size(), x + 10, y + 45, 10, SKYBLUE);
        drawText("Proyectos Activos: " + megaProjects.size(), x + 140, y + 45, 10, SKYBLUE);
        
        if (jettra.currentThought != null && jettra.thoughtTimer > 0) {
            String thought = jettra.currentThought;
            int maxChars = 40;
            if (thought.length() > maxChars) thought = thought.substring(0, maxChars-3) + "...";
            drawText("PENSANDO: " + thought, x + 10, y + 70, 11, LIME);
        } else {
            drawText("PENSANDO: Vigilando el conocimiento...", x + 10, y + 70, 11, GRAY);
        }
    }

    public static void main(String[] args) {
        new Jettra3DApp().run();
    }
}
